import { test, expect } from '@playwright/test';
import { mockHomeApi } from './mocks';

test.describe('AI assistant toolbar button', () => {
  test('opens the genai-assistant UI in a new tab', async ({ page }) => {
    await mockHomeApi(page);
    await page.addInitScript(() => {
      (window as any).__opened = [];
      window.open = (...args: any[]) => {
        (window as any).__opened.push(args);
        return null;
      };
    });
    await page.goto('/');

    await page.click('button[aria-label="Open AI Assistant"]');

    const opened = await page.evaluate(() => (window as any).__opened);
    expect(opened).toEqual([['http://localhost:8000/ui', '_blank', 'noopener']]);
  });
});
