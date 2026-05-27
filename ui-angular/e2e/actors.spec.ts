import { test, expect } from '@playwright/test';
import { mockActorsApi, mockActorDetailApi, MOCK_ACTORS_PAGE, MOCK_ACTOR_DETAIL } from './mocks';

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

  test('actor list rows are clickable', async ({ page }) => {
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows.first()).toHaveCSS('cursor', 'pointer');
  });

  test('clicking actor row navigates to /actors/:id', async ({ page }) => {
    const actorId = MOCK_ACTORS_PAGE.data[0].actorId;
    await mockActorDetailApi(page, actorId);
    await page.locator('table[mat-table] tr[mat-row]').first().click();
    await expect(page).toHaveURL(new RegExp(`/actors/${actorId}`));
  });
});

test.describe('Actor detail card', () => {
  test.beforeEach(async ({ page }) => {
    await mockActorDetailApi(page, 1);
    await page.goto('/actors/1');
  });

  test('shows actor name in title', async ({ page }) => {
    const actor = MOCK_ACTOR_DETAIL.data;
    await expect(page.locator('mat-card-title')).toContainText(`${actor.firstName} ${actor.lastName}`);
  });

  test('shows actor ID in subtitle', async ({ page }) => {
    await expect(page.locator('mat-card-subtitle')).toContainText('1');
  });

  test('shows filmography section', async ({ page }) => {
    await expect(page.locator('.filmography-section')).toBeVisible();
  });

  test('shows category labels from filmInfo', async ({ page }) => {
    await expect(page.locator('.category-label').first()).toContainText('Animation');
  });

  test('shows film chips within category', async ({ page }) => {
    await expect(page.locator('mat-chip').first()).toContainText('ACADEMY DINOSAUR');
  });

  test('has a back button', async ({ page }) => {
    await expect(page.getByRole('button', { name: /back/i })).toBeVisible();
  });
});
