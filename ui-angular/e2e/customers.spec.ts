import { test, expect } from '@playwright/test';
import { mockCustomersApi, MOCK_CUSTOMERS_PAGE } from './mocks';

test.describe('Customers page', () => {
  test.beforeEach(async ({ page }) => {
    await mockCustomersApi(page);
    await page.goto('/customers');
  });

  test('table has correct column headers', async ({ page }) => {
    const table = page.locator('table[mat-table]');
    await expect(table).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'ID' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Name' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'City' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Country' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Phone' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Notes' })).toBeVisible();
  });

  test('table renders mock customer rows', async ({ page }) => {
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows).toHaveCount(MOCK_CUSTOMERS_PAGE.data.length);
    await expect(rows.first()).toContainText(MOCK_CUSTOMERS_PAGE.data[0].name);
    await expect(rows.first()).toContainText(MOCK_CUSTOMERS_PAGE.data[0].city);
    await expect(rows.first()).toContainText(MOCK_CUSTOMERS_PAGE.data[0].country);
  });

  test('paginator shows total customer count', async ({ page }) => {
    const paginator = page.locator('mat-paginator');
    await expect(paginator).toBeVisible();
    await expect(paginator).toContainText(MOCK_CUSTOMERS_PAGE.totalCount.toString());
  });
});
