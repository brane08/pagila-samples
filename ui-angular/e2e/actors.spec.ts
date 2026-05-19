import { test, expect } from '@playwright/test';
import { mockActorsApi, MOCK_ACTORS_PAGE } from './mocks';

test.describe('Actors page', () => {
  test.beforeEach(async ({ page }) => {
    await mockActorsApi(page);
    await page.goto('/actors');
  });

  test('shows Actors tab in nav bar', async ({ page }) => {
    const tabBar = page.locator('nav[mat-tab-nav-bar]');
    await expect(tabBar.getByRole('tab', { name: /Actors/i })).toBeVisible();
  });

  test('table has correct column headers', async ({ page }) => {
    const table = page.locator('table[mat-table]');
    await expect(table).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'ID' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'First Name' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Last Name' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Films' })).toBeVisible();
  });

  test('table renders mock actor rows', async ({ page }) => {
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows).toHaveCount(MOCK_ACTORS_PAGE.data.length);
    await expect(rows.first()).toContainText(MOCK_ACTORS_PAGE.data[0].firstName);
    await expect(rows.first()).toContainText(MOCK_ACTORS_PAGE.data[0].lastName);
  });

  test('paginator is present with total count', async ({ page }) => {
    const paginator = page.locator('mat-paginator');
    await expect(paginator).toBeVisible();
    await expect(paginator).toContainText(MOCK_ACTORS_PAGE.totalCount.toString());
  });
});
