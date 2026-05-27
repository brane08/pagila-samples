import { test, expect } from '@playwright/test';
import { mockHomeApi, MOCK_SALES_BY_CATEGORY, MOCK_SALES_BY_STORE } from './mocks';

test.describe('Home page', () => {
  test.beforeEach(async ({ page }) => {
    await mockHomeApi(page);
    await page.goto('/');
  });

  test('renders stat cards section', async ({ page }) => {
    // The three stat-card area must exist
    const cards = page.locator('mat-card');
    await expect(cards.first()).toBeVisible();
  });

  test('shows Total Revenue card with data after load', async ({ page }) => {
    const revenueLbl = page.locator('mat-card-content').filter({ hasText: 'Total Revenue' });
    await expect(revenueLbl).toBeVisible();
    // Wait for loading bar to disappear
    await expect(page.locator('mat-progress-bar').first()).not.toBeVisible({ timeout: 5000 }).catch(() => {});
    const totalRevenue = MOCK_SALES_BY_CATEGORY.data.reduce((s, d) => s + d.totalSales, 0);
    const formatted = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(totalRevenue);
    await expect(revenueLbl).toContainText(formatted);
  });

  test('shows Film Categories count', async ({ page }) => {
    const catCard = page.locator('mat-card-content').filter({ hasText: 'Film Categories' });
    await expect(catCard).toBeVisible();
    const count = MOCK_SALES_BY_CATEGORY.data.length.toString();
    await expect(catCard).toContainText(count);
  });

  test('shows store revenue cards', async ({ page }) => {
    for (const store of MOCK_SALES_BY_STORE.data) {
      const storeCard = page.locator('mat-card-content').filter({ hasText: store.store.split(' - ')[0] });
      await expect(storeCard.first()).toBeVisible();
    }
  });

  test('shows Revenue by Film Category chart card', async ({ page }) => {
    await expect(page.locator('mat-card-title').filter({ hasText: 'Revenue by Film Category' })).toBeVisible();
  });

  test('shows Revenue by Store chart card', async ({ page }) => {
    await expect(page.locator('mat-card-title').filter({ hasText: 'Revenue by Store' })).toBeVisible();
  });
});
