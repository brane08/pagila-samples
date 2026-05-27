import { test, expect } from '@playwright/test';
import { mockFilmsApi, mockFilmDetailApi, MOCK_FILMS_PAGE, MOCK_FILM_DETAIL, MOCK_FILM_ACTORS, MOCK_SALES_BY_CATEGORY } from './mocks';

test.describe('Films page', () => {
  test.beforeEach(async ({ page }) => {
    await mockFilmsApi(page);
    await page.goto('/films');
  });

  test('shows two tabs: Film List and Sales by Category', async ({ page }) => {
    const tabBar = page.locator('nav[mat-tab-nav-bar]');
    await expect(tabBar.locator('a[mat-tab-link]:has-text("Film List")')).toBeVisible();
    await expect(tabBar.locator('a[mat-tab-link]:has-text("Sales by Category")')).toBeVisible();
  });

  test('Film List tab shows table with correct columns', async ({ page }) => {
    const table = page.locator('table[mat-table]');
    await expect(table).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'ID.' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Title' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Description' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Category' })).toBeVisible();
  });

  test('Film List table renders mock rows', async ({ page }) => {
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows).toHaveCount(MOCK_FILMS_PAGE.data.length);
    await expect(rows.first()).toContainText(MOCK_FILMS_PAGE.data[0].title);
  });

  test('paginator shows correct total count', async ({ page }) => {
    const paginator = page.locator('mat-paginator');
    await expect(paginator).toBeVisible();
    await expect(paginator).toContainText(MOCK_FILMS_PAGE.totalCount.toString());
  });

  test('paginator has first/last buttons', async ({ page }) => {
    const paginator = page.locator('mat-paginator');
    await expect(paginator.locator('button[aria-label="First page"]')).toBeVisible();
    await expect(paginator.locator('button[aria-label="Last page"]')).toBeVisible();
  });

  test('Sales by Category tab is reachable', async ({ page }) => {
    await page.click('a[mat-tab-link]' + ':has-text("Sales by Category")');
    await expect(page).toHaveURL(/\/films\/sales/);
  });

  test('Sales by Category table has Category and Total Sales columns', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Sales by Category")');
    const table = page.locator('table[mat-table]');
    await expect(table).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Category' })).toBeVisible();
    await expect(table.locator('th').filter({ hasText: 'Total Sales' })).toBeVisible();
  });

  test('Sales by Category table renders mock rows', async ({ page }) => {
    await page.click('a[mat-tab-link]:has-text("Sales by Category")');
    const rows = page.locator('table[mat-table] tr[mat-row]');
    await expect(rows).toHaveCount(MOCK_SALES_BY_CATEGORY.data.length);
    await expect(rows.first()).toContainText(MOCK_SALES_BY_CATEGORY.data[0].category);
  });

  test('loading progress bar appears then disappears', async ({ page }) => {
    // Delay the API so we can observe the loading state
    await page.unroute(`http://localhost:8001/films/@view*`);
    await page.route('http://localhost:8001/films/@view*', async route => {
      await new Promise(r => setTimeout(r, 200));
      route.fulfill({ json: MOCK_FILMS_PAGE });
    });
    await page.goto('/films');
    // Progress bar should appear during load
    await expect(page.locator('mat-progress-bar')).toBeVisible();
    // Then disappear once data arrives
    await expect(page.locator('mat-progress-bar')).not.toBeVisible({ timeout: 5000 });
  });

  test('next page button triggers new API request with page=2', async ({ page }) => {
    const requests: string[] = [];
    page.on('request', req => {
      if (req.url().includes('@view')) requests.push(req.url());
    });

    const nextBtn = page.locator('button[aria-label="Next page"]');
    await expect(nextBtn).toBeEnabled();
    await nextBtn.click();

    await expect(page.locator('table[mat-table] tr[mat-row]')).toHaveCount(MOCK_FILMS_PAGE.data.length);
    const pageParams = requests.map(u => new URL(u).searchParams.get('page'));
    expect(pageParams).toContain('2');
  });

  test('film list rows are clickable (have routerLink)', async ({ page }) => {
    const firstRow = page.locator('table[mat-table] tr[mat-row]').first();
    await expect(firstRow).toHaveClass(/clickable-row/);
  });
});

// ── Film detail card ───────────────────────────────────────────────────────────

test.describe('Film detail card', () => {
  test.beforeEach(async ({ page }) => {
    await mockFilmsApi(page);
    await mockFilmDetailApi(page, MOCK_FILM_DETAIL.filmId);
    await page.goto(`/films/${MOCK_FILM_DETAIL.filmId}`);
  });

  test('shows film title', async ({ page }) => {
    await expect(page.locator('mat-card-title')).toContainText(MOCK_FILM_DETAIL.title, { timeout: 5000 });
  });

  test('shows film description', async ({ page }) => {
    await expect(page.locator('.description')).toContainText(MOCK_FILM_DETAIL.description, { timeout: 5000 });
  });

  test('shows rating badge', async ({ page }) => {
    await expect(page.locator('.rating-badge')).toContainText(MOCK_FILM_DETAIL.rating, { timeout: 5000 });
  });

  test('shows rental rate', async ({ page }) => {
    await expect(page.locator('mat-card-content')).toContainText('0.99', { timeout: 5000 });
  });

  test('shows genres as chips', async ({ page }) => {
    const chipSet = page.locator('mat-chip-set').first();
    await expect(chipSet).toContainText(MOCK_FILM_DETAIL.categories[0], { timeout: 5000 });
  });

  test('shows special features', async ({ page }) => {
    await expect(page.locator('mat-card-content')).toContainText(MOCK_FILM_DETAIL.specialFeatures[0], { timeout: 5000 });
  });

  test('shows cast from actors API', async ({ page }) => {
    const actor = MOCK_FILM_ACTORS.data[0];
    const fullName = `${actor.firstName} ${actor.lastName}`;
    await expect(page.locator('.actors-list')).toContainText(fullName, { timeout: 5000 });
  });

  test('back button is visible', async ({ page }) => {
    await expect(page.locator('button:has-text("Back")')).toBeVisible({ timeout: 5000 });
  });

  test('clicking a list row navigates to the film detail page', async ({ page }) => {
    await page.goto('/films');
    await mockFilmDetailApi(page, MOCK_FILMS_PAGE.data[0].filmId);
    const firstRow = page.locator('table[mat-table] tr[mat-row]').first();
    await firstRow.click();
    await expect(page).toHaveURL(new RegExp(`/films/${MOCK_FILMS_PAGE.data[0].filmId}`));
  });
});
