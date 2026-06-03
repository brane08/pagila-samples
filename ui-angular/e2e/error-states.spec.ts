import { test, expect } from '@playwright/test';

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
