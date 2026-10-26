import { test, expect } from '@playwright/test';
import { mockCustomersApi, MOCK_CUSTOMERS_PAGE, MOCK_CUSTOMER_DETAIL } from './mocks';

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

test.describe('Customer detail card', () => {
  test.beforeEach(async ({ page }) => {
    await mockCustomersApi(page);
    await page.goto('/customers/1');
  });

  test('shows name and id', async ({ page }) => {
    const c = MOCK_CUSTOMER_DETAIL.data;
    await expect(page.locator('mat-card-title')).toContainText(c.name);
    await expect(page.locator('mat-card-subtitle')).toContainText(`#${c.id}`);
  });

  test('shows address details', async ({ page }) => {
    const c = MOCK_CUSTOMER_DETAIL.data;
    await expect(page.locator('.details')).toContainText(c.city);
    await expect(page.locator('.details')).toContainText(c.country);
  });

  test('has a back button', async ({ page }) => {
    await expect(page.getByRole('button', { name: /back/i })).toBeVisible();
  });

  test('clicking a list row opens the card', async ({ page }) => {
    await page.goto('/customers');
    await page.locator('table[mat-table] tr[mat-row]').first().click();
    await expect(page).toHaveURL(/\/customers\/1$/);
  });
});

test.describe('Customer detail card errors', () => {
  test('shows not-found state on 404', async ({ page }) => {
    await mockCustomersApi(page);
    await page.route('**/rentals/@customers/*', route => route.fulfill({ status: 404, body: '' }));
    await page.goto('/customers/99999');
    await expect(page.getByText('Customer not found.')).toBeVisible();
  });

  test('shows not-found state for a non-numeric id', async ({ page }) => {
    await mockCustomersApi(page);
    await page.goto('/customers/abc');
    await expect(page.getByText('Customer not found.')).toBeVisible();
  });
});
