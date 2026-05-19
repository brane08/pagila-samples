import { test, expect } from '@playwright/test';
import { mockStoresApi, MOCK_SALES_BY_STORE, MOCK_STAFF } from './mocks';

test.describe('Stores page', () => {
  test.beforeEach(async ({ page }) => {
    await mockStoresApi(page);
    await page.goto('/stores');
  });

  test('shows Sales by Store and Staff tabs', async ({ page }) => {
    const tabBar = page.locator('nav[mat-tab-nav-bar]');
    await expect(tabBar.getByRole('tab', { name: /Sales by Store/i })).toBeVisible();
    await expect(tabBar.getByRole('tab', { name: /Staff/i })).toBeVisible();
  });

  test('Sales by Store tab shows table with Store, Manager, Total Sales columns', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Sales by Store")');
    await expect(page).toHaveURL(/\/stores\/sales/);
    const table = page.locator('table[mat-table]');
    await expect(table).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Store' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Manager' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Total Sales' })).toBeVisible();
  });

  test('Sales by Store table renders mock data', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Sales by Store")');
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows).toHaveCount(MOCK_SALES_BY_STORE.data.length);
    await expect(rows.first()).toContainText(MOCK_SALES_BY_STORE.data[0].manager);
  });

  test('Staff tab navigates to /stores/staff', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Staff")');
    await expect(page).toHaveURL(/\/stores\/staff/);
  });

  test('Staff tab shows table with ID, Name, City, Country, Phone columns', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Staff")');
    const table = page.locator('table[mat-table]');
    await expect(table).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'ID' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Name' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'City' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Country' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Phone' })).toBeVisible();
  });

  test('Staff table renders mock staff rows', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Staff")');
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows).toHaveCount(MOCK_STAFF.data.length);
    await expect(rows.first()).toContainText(MOCK_STAFF.data[0].name);
    await expect(rows.first()).toContainText(MOCK_STAFF.data[0].city);
  });
});
