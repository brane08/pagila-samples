import { test, expect } from '@playwright/test';
import { abortBackendRequests, mockHomeApi } from './mocks';

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await mockHomeApi(page);
    await page.goto('/');
  });

  test('toolbar shows Film Library title', async ({ page }) => {
    await expect(page.locator('mat-toolbar')).toContainText('Film Library');
  });

  test('toolbar has all five nav links', async ({ page }) => {
    const toolbar = page.locator('mat-toolbar');
    await expect(toolbar.getByRole('link', { name: 'Home' })).toBeVisible();
    await expect(toolbar.getByRole('link', { name: 'Films' })).toBeVisible();
    await expect(toolbar.getByRole('link', { name: 'Actors' })).toBeVisible();
    await expect(toolbar.getByRole('link', { name: 'Stores' })).toBeVisible();
    await expect(toolbar.getByRole('link', { name: 'Customers' })).toBeVisible();
  });

  test('toolbar has AI Assistant icon button', async ({ page }) => {
    await expect(page.locator('button[aria-label="Open AI Assistant"]')).toBeVisible();
  });

  test('navigates to /films and back to / via Home link', async ({ page }) => {
    await page.click('a[href="/films"]');
    await expect(page).toHaveURL(/\/films/);
    await page.click('a[href="/"]');
    await expect(page).toHaveURL('/');
  });

  test('navigates to /actors', async ({ page }) => {
    await page.click('a[href="/actors"]');
    await expect(page).toHaveURL(/\/actors/);
  });

  test('navigates to /stores', async ({ page }) => {
    await page.click('a[href="/stores"]');
    await expect(page).toHaveURL(/\/stores/);
  });

  test('navigates to /customers', async ({ page }) => {
    await page.click('a[href="/customers"]');
    await expect(page).toHaveURL(/\/customers/);
  });

  test('page title is Pagila UI Angular', async ({ page }) => {
    await expect(page).toHaveTitle('Pagila UI Angular');
  });
});
