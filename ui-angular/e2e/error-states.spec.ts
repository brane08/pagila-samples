import { test, expect } from '@playwright/test';
import { mockHomeApi, mockSessionsApi } from './mocks';

const API = 'http://localhost:8001';

// ── Helpers ────────────────────────────────────────────────────────────────────

async function mockApiError(page: import('@playwright/test').Page, urlPattern: string, status = 500) {
  await page.route(urlPattern, route =>
    route.fulfill({ status, body: JSON.stringify({ error: 'Internal Server Error' }) })
  );
}

// ── Films error states ─────────────────────────────────────────────────────────

test.describe('Error states – Films', () => {
  test('table shows 0 rows when API returns 500', async ({ page }) => {
    await mockApiError(page, `${API}/films/@view*`);
    await page.goto('/films');
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows).toHaveCount(0);
  });

  test('paginator shows 0 of 0 when API fails', async ({ page }) => {
    await mockApiError(page, `${API}/films/@view*`);
    await page.goto('/films');
    await expect(page.locator('mat-paginator')).toContainText('0 of 0');
  });

  test('table shows 0 rows on empty API result', async ({ page }) => {
    await page.route(`${API}/films/@view*`, route =>
      route.fulfill({ json: { success: true, data: [], totalCount: 0 } })
    );
    await page.goto('/films');
    await expect(page.locator('table[mat-table] tr[mat-row]')).toHaveCount(0);
    await expect(page.locator('mat-paginator')).toContainText('0 of 0');
  });
});

// ── Actors error states ────────────────────────────────────────────────────────

test.describe('Error states – Actors', () => {
  test('table shows 0 rows when API returns 500', async ({ page }) => {
    await mockApiError(page, `${API}/actors/@view*`);
    await page.goto('/actors');
    await expect(page.locator('table[mat-table] tr[mat-row]')).toHaveCount(0);
  });
});

// ── Customers error states ─────────────────────────────────────────────────────

test.describe('Error states – Customers', () => {
  test('table shows 0 rows when API returns 500', async ({ page }) => {
    await mockApiError(page, `${API}/rentals/@customers*`);
    await page.goto('/customers');
    await expect(page.locator('table[mat-table] tr[mat-row]')).toHaveCount(0);
  });
});

// ── Home error states ──────────────────────────────────────────────────────────

test.describe('Error states – Home dashboard', () => {
  test('Total Revenue stat card is visible even when API fails', async ({ page }) => {
    await mockApiError(page, `${API}/films/@sales-by-category`);
    await mockApiError(page, `${API}/stores/@sales-by-store`);
    await page.goto('/');
    // The card skeleton must always render; rxResource shows progress bar while in error-retry state
    const card = page.locator('mat-card-content').filter({ hasText: 'Total Revenue' });
    await expect(card).toBeVisible();
  });

  test('home page does not crash on API error', async ({ page }) => {
    await mockApiError(page, `${API}/films/@sales-by-category`);
    await mockApiError(page, `${API}/stores/@sales-by-store`);
    const errors: string[] = [];
    page.on('pageerror', err => errors.push(err.message));
    await page.goto('/');
    await page.waitForTimeout(1000);
    expect(errors).toHaveLength(0);
  });
});

// ── Chat error states ──────────────────────────────────────────────────────────

test.describe('Error states – Chat', () => {
  test.beforeEach(async ({ page }) => {
    await mockHomeApi(page);
    await mockSessionsApi(page);
    await page.goto('/');
    await page.click('button[aria-label="Open AI Assistant"]');
  });

  test('network error shows fallback error message in bubble', async ({ page }) => {
    await page.route('http://localhost:8000/chat/stream', route => route.abort());
    await page.fill('textarea[placeholder*="Type your message"]', 'hello');
    await page.click('button[aria-label="Send"]');
    const aiBubble = page.locator('.message-bubble.message-ai').last();
    await expect(aiBubble).toContainText('could not reach the assistant', { timeout: 5000 });
  });

  test('HTTP error from chat stream shows fallback message', async ({ page }) => {
    await page.route('http://localhost:8000/chat/stream', route =>
      route.fulfill({ status: 503, body: 'Service Unavailable' })
    );
    await page.fill('textarea[placeholder*="Type your message"]', 'hello');
    await page.click('button[aria-label="Send"]');
    const aiBubble = page.locator('.message-bubble.message-ai').last();
    await expect(aiBubble).toContainText('could not reach the assistant', { timeout: 5000 });
  });

  test('sessions sidebar shows error message when /sessions fails', async ({ page }) => {
    // Override the sessions mock to return an error
    await page.unroute('http://localhost:8000/sessions');
    await page.route('http://localhost:8000/sessions', route =>
      route.fulfill({ status: 500, body: 'error' })
    );
    const sidebar = page.locator('.sessions-sidebar');
    await page.click('button[title="Refresh"]');
    await expect(sidebar).toContainText('Could not load sessions', { timeout: 5000 });
  });
});
