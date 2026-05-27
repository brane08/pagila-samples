import { test, expect } from '@playwright/test';
import { mockHomeApi, mockChatStream, mockSessionsApi, MOCK_SESSIONS } from './mocks';

test.describe('Chat dialog', () => {
  test.beforeEach(async ({ page }) => {
    await mockHomeApi(page);
    // Abort chat-agent calls so no real backend is needed
    await page.route('http://localhost:8000/**', route => route.abort());
    await page.goto('/');
  });

  test('AI Assistant toolbar button is visible', async ({ page }) => {
    await expect(page.locator('button[aria-label="Open AI Assistant"]')).toBeVisible();
  });

  test('clicking AI Assistant button opens chat dialog', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    const dialog = page.locator('mat-dialog-container');
    await expect(dialog).toBeVisible();
  });

  test('chat dialog shows "Your AI Assistant" header', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    await expect(page.locator('.chat-header')).toContainText('Your AI Assistant');
  });

  test('chat dialog shows greeting message', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    const messages = page.locator('.chat-messages');
    await expect(messages).toContainText('Hi! Ask me anything');
  });

  test('chat dialog has sessions sidebar', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    await expect(page.locator('.sessions-sidebar')).toBeVisible();
  });

  test('chat dialog has message input textarea', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    await expect(page.locator('textarea[placeholder*="Type your message"]')).toBeVisible();
  });

  test('chat dialog has send button', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    await expect(page.locator('button[aria-label="Send"]')).toBeVisible();
  });

  test('send button is disabled when input is empty', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    const sendBtn = page.locator('button[aria-label="Send"]');
    await expect(sendBtn).toBeDisabled();
  });

  test('send button enables when text is typed', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    await page.fill('textarea[placeholder*="Type your message"]', 'Hello');
    const sendBtn = page.locator('button[aria-label="Send"]');
    await expect(sendBtn).toBeEnabled();
  });

  test('close button dismisses the dialog', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    await page.click('button[aria-label="Close"]');
    await expect(page.locator('mat-dialog-container')).not.toBeVisible();
  });

  test('chat dialog shows thread ID in header', async ({ page }) => {
    await page.click('button[aria-label="Open AI Assistant"]');
    const header = page.locator('.chat-header');
    await expect(header).toContainText('session-');
  });
});

test.describe('Chat – SSE streaming', () => {
  test.beforeEach(async ({ page }) => {
    await mockHomeApi(page);
    await mockSessionsApi(page);
    await page.goto('/');
    await page.click('button[aria-label="Open AI Assistant"]');
  });

  test('streamed token tokens accumulate in the AI bubble', async ({ page }) => {
    await mockChatStream(page, [
      { type: 'token', content: 'Hello' },
      { type: 'token', content: ' world' },
      { type: 'done' },
    ]);
    await page.fill('textarea[placeholder*="Type your message"]', 'Hi there');
    await page.click('button[aria-label="Send"]');
    const aiBubble = page.locator('.message-bubble.message-ai').last();
    await expect(aiBubble).toContainText('Hello world', { timeout: 5000 });
  });

  test('tool_start event shows a tool badge', async ({ page }) => {
    await mockChatStream(page, [
      { type: 'tool_start', tool: 'search_films', input: { title: 'test' } },
      { type: 'tool_end',   tool: 'search_films' },
      { type: 'token', content: 'Found 3 films.' },
      { type: 'done' },
    ]);
    await page.fill('textarea[placeholder*="Type your message"]', 'Find films');
    await page.click('button[aria-label="Send"]');
    await expect(page.locator('.tool-badge')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.tool-badge')).toContainText('search_films');
  });

  test('streaming cursor disappears after done event', async ({ page }) => {
    await mockChatStream(page, [
      { type: 'token', content: 'Answer.' },
      { type: 'done' },
    ]);
    await page.fill('textarea[placeholder*="Type your message"]', 'question');
    await page.click('button[aria-label="Send"]');
    await expect(page.locator('.streaming-cursor')).not.toBeVisible({ timeout: 5000 });
  });

  test('send button is disabled while streaming and re-enabled after typing in the cleared input', async ({ page }) => {
    // Delay the done event so we can observe the disabled state
    await page.route('http://localhost:8000/chat/stream', async route => {
      await new Promise(r => setTimeout(r, 300));
      route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'text/event-stream' },
        body: 'data: {"type":"token","content":"Hi"}\n\ndata: {"type":"done"}\n\n',
      });
    });
    const textarea = page.locator('textarea[placeholder*="Type your message"]');
    await textarea.fill('hello');
    const sendBtn = page.locator('button[aria-label="Send"]');
    await sendBtn.click();
    // Should be disabled immediately after send (isLoading=true, textarea clearing)
    await expect(sendBtn).toBeDisabled();
    // After done event, textarea is cleared — isLoading=false but button stays disabled because input is empty
    await expect(page.locator('.streaming-cursor')).not.toBeVisible({ timeout: 5000 });
    // Typing new text re-enables the button
    await textarea.fill('follow-up');
    await expect(sendBtn).toBeEnabled();
  });
});

test.describe('Chat – keyboard shortcut', () => {
  test.beforeEach(async ({ page }) => {
    await mockHomeApi(page);
    await mockSessionsApi(page);
    await mockChatStream(page, [
      { type: 'token', content: 'Hi!' },
      { type: 'done' },
    ]);
    await page.goto('/');
    await page.click('button[aria-label="Open AI Assistant"]');
  });

  test('Enter key sends the message', async ({ page }) => {
    const textarea = page.locator('textarea[placeholder*="Type your message"]');
    await textarea.fill('Enter key test');
    await textarea.press('Enter');
    await expect(page.locator('.message-bubble.message-user')).toContainText('Enter key test');
  });

  test('Shift+Enter inserts newline without sending', async ({ page }) => {
    const textarea = page.locator('textarea[placeholder*="Type your message"]');
    await textarea.fill('line one');
    await textarea.press('Shift+Enter');
    await textarea.type('line two');
    // Should not have sent — no user message bubble yet
    const userBubbles = page.locator('.message-bubble.message-user');
    await expect(userBubbles).toHaveCount(0);
    // Textarea should contain both lines
    const value = await textarea.inputValue();
    expect(value).toContain('line one');
    expect(value).toContain('line two');
  });
});

test.describe('Chat – sessions sidebar', () => {
  test.beforeEach(async ({ page }) => {
    await mockHomeApi(page);
    // Register sessions mock LAST so it takes precedence over any catch-all added later
    await page.route('http://localhost:8000/chat/stream', route => route.abort());
    await mockSessionsApi(page);
    await page.goto('/');
    await page.click('button[aria-label="Open AI Assistant"]');
  });

  test('sessions sidebar lists mocked sessions', async ({ page }) => {
    const sidebar = page.locator('.sessions-sidebar');
    for (const s of MOCK_SESSIONS.sessions) {
      await expect(sidebar).toContainText(s.thread_id.slice(-15), { timeout: 5000 });
    }
  });

  test('new session button creates a fresh thread', async ({ page }) => {
    const oldThreadId = await page.locator('.chat-header small').textContent();
    await page.click('button[title="New session"]');
    const newThreadId = await page.locator('.chat-header small').textContent();
    expect(newThreadId).not.toBe(oldThreadId);
  });
});
