import { test, expect } from '@playwright/test';
import { mockStoresApi, mockStoreDetailApi, MOCK_SALES_BY_STORE, MOCK_STAFF, MOCK_STORE_DETAIL } from './mocks';

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

  test('staff rows are clickable', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Staff")');
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows.first()).toHaveCSS('cursor', 'pointer');
  });

  test('clicking staff row navigates to /stores/:storeId', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Staff")');
    const storeId = MOCK_STAFF.data[0].sid;
    await page.locator('table[mat-table] tr[mat-row]').first().click();
    await expect(page).toHaveURL(new RegExp(`/stores/${storeId}`));
  });
});

test.describe('Store detail card', () => {
  test.beforeEach(async ({ page }) => {
    await mockStoreDetailApi(page, 1);
    await page.goto('/stores/1');
  });

  test('shows store ID in title', async ({ page }) => {
    const storeId = MOCK_STORE_DETAIL.data.storeId;
    await expect(page.locator('mat-card-title')).toContainText(`Store ${storeId}`);
  });

  test('shows city and country in subtitle', async ({ page }) => {
    const { city } = MOCK_STORE_DETAIL.data.address;
    await expect(page.locator('mat-card-subtitle')).toContainText(city.city);
    await expect(page.locator('mat-card-subtitle')).toContainText(city.country.country);
  });

  test('shows manager name', async ({ page }) => {
    const m = MOCK_STORE_DETAIL.data.manager;
    await expect(page.locator('.meta-value').first()).toContainText(`${m.firstName} ${m.lastName}`);
  });

  test('shows current staff chips', async ({ page }) => {
    const staff = MOCK_STORE_DETAIL.data.currentStaff[0];
    await expect(page.locator('.staff-chip')).toContainText(`${staff.firstName} ${staff.lastName}`);
  });

  test('has a back button', async ({ page }) => {
    await expect(page.getByRole('button', { name: /back/i })).toBeVisible();
  });
});
