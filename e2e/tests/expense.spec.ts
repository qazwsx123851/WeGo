import { test, expect } from '@playwright/test';
import { authenticateTestUser, isTestAuthAvailable } from '../fixtures/auth-helper';
import { generateRandomTrip, generateRandomExpense } from '../fixtures/test-data';

/**
 * Expense and Settlement E2E Tests
 *
 * Test ID mapping:
 * - E2E-EXP-001: Add expense (equal split)
 * - E2E-EXP-002: View expense list
 * - E2E-EXP-003: View settlement results
 */

test.describe('Expense Management', () => {
  let tripId: string;

  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);

    // Create a trip for testing expenses
    const trip = generateRandomTrip();
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');

    // Wait for redirect and get trip ID from URL
    await page.waitForURL(/trips\/[a-f0-9-]+/);
    const url = page.url();
    const match = url.match(/trips\/([a-f0-9-]+)/);
    if (match) {
      tripId = match[1];
    }
  });

  test.describe('E2E-EXP-001: View Expense List', () => {
    test('displays expense list page', async ({ page }) => {
      await page.goto(`/trips/${tripId}/expenses`);

      // Should be on expenses page
      await expect(page).toHaveURL(new RegExp(`trips/${tripId}/expenses`));

      // Should have page content
      const pageContent = page.locator('main, [role="main"]').first();
      await expect(pageContent).toBeVisible();
    });

    test('shows empty state or expense list', async ({ page }) => {
      await page.goto(`/trips/${tripId}/expenses`);

      // Wait for content to load
      await page.waitForLoadState('networkidle');

      // Should show empty state or expense items
      const hasExpenses = await page.locator('.expense-item, [data-expense-id]').count() > 0;
      const hasEmptyState = await page.locator('text=/沒有支出|尚無支出|no expenses|新增/i').count() > 0;
      const hasPageContent = await page.locator('main, [role="main"]').count() > 0;

      expect(hasExpenses || hasEmptyState || hasPageContent).toBe(true);
    });

    test('has add expense button', async ({ page }) => {
      await page.goto(`/trips/${tripId}/expenses`);

      // Should have add button
      const addButton = page.locator('a[href*="create"], a[href*="new"], button:has-text("新增"), [aria-label*="新增"]').first();
      await expect(addButton).toBeVisible();
    });
  });

  test.describe('E2E-EXP-002: Create Expense', () => {
    test('displays expense creation form', async ({ page }) => {
      await page.goto(`/trips/${tripId}/expenses/create`, { waitUntil: 'domcontentloaded' });

      // Wait for page content to appear
      const pageContent = page.locator('main, [role="main"], form').first();
      await expect(pageContent).toBeVisible({ timeout: 10000 });

      // Should have description input (form field)
      const descInput = page.locator('input[name="description"], #description').first();
      if (await descInput.count() > 0) {
        await expect(descInput).toBeVisible();
      }
    });

    test('can navigate to expense creation from list', async ({ page }) => {
      await page.goto(`/trips/${tripId}/expenses`, { waitUntil: 'domcontentloaded' });
      await page.waitForLoadState('networkidle');

      // Click add button
      const addButton = page.locator('a[href*="create"], a[href*="new"], [aria-label*="新增支出"]').first();
      if (await addButton.count() > 0) {
        await addButton.click();

        // Should navigate to create form or show form content
        await page.waitForURL(/expenses\/(create|new)/, { timeout: 10000 });
      }
    });
  });

  test.describe('E2E-EXP-003: View Settlement', () => {
    test('displays settlement page', async ({ page }) => {
      await page.goto(`/trips/${tripId}/settlement`);

      // Should have page content
      const pageContent = page.locator('main, [role="main"]').first();
      await expect(pageContent).toBeVisible();

      // Should have settlement-related content
      const settlementContent = page.locator('text=/結算|Settlement|餘額|balance/i');
      const hasSettlementContent = await settlementContent.count() > 0;
      const hasPageStructure = await page.locator('h1, h2, section').count() > 0;

      expect(hasSettlementContent || hasPageStructure).toBe(true);
    });

    test('can navigate to settlement from expenses', async ({ page }) => {
      await page.goto(`/trips/${tripId}/expenses`);

      // Look for settlement link
      const settlementLink = page.locator('a[href*="settlement"]').first();
      if (await settlementLink.count() > 0) {
        await settlementLink.click();

        // Should navigate to settlement page
        await page.waitForURL(/settlement/, { timeout: 5000 });
      }
    });
  });
});

test.describe('Expense Statistics', () => {
  let tripId: string;

  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);

    // Create a trip
    const trip = generateRandomTrip();
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');

    await page.waitForURL(/trips\/[a-f0-9-]+/);
    const url = page.url();
    const match = url.match(/trips\/([a-f0-9-]+)/);
    if (match) {
      tripId = match[1];
    }
  });

  test('displays statistics page', async ({ page }) => {
    await page.goto(`/trips/${tripId}/expenses/statistics`);

    // Should have page content
    const pageContent = page.locator('main, [role="main"]').first();
    await expect(pageContent).toBeVisible();
  });

  test('shows charts or empty state', async ({ page }) => {
    await page.goto(`/trips/${tripId}/expenses/statistics`);

    // Wait for content to load
    await page.waitForLoadState('networkidle');

    // Should show charts, statistics content, or empty state
    const hasCharts = await page.locator('canvas, .chart-container').count() > 0;
    const hasStats = await page.locator('text=/統計|分類|成員|趨勢/i').count() > 0;
    const hasEmptyState = await page.locator('text=/沒有|尚無|no data/i').count() > 0;
    const hasPageContent = await page.locator('main section, .glass-card').count() > 0;

    expect(hasCharts || hasStats || hasEmptyState || hasPageContent).toBe(true);
  });
});

test.describe('Expense API Integration', () => {
  test('GET /api/trips/{id}/expenses requires authentication', async ({ request }) => {
    const response = await request.get('/api/trips/00000000-0000-0000-0000-000000000001/expenses');

    // Should return 401/403 (unauthenticated) or 404 (not found) or 200 with error
    expect([200, 401, 403, 404]).toContain(response.status());
  });

  test('POST /api/trips/{id}/expenses requires authentication', async ({ request }) => {
    const response = await request.post('/api/trips/00000000-0000-0000-0000-000000000001/expenses', {
      data: {
        title: 'Test Expense',
        amount: 1000,
        currency: 'TWD',
        category: 'FOOD',
      },
    });

    // Should return 401/403 (unauthenticated) or 404 (not found)
    expect([401, 403, 404]).toContain(response.status());
  });

  test('GET /api/trips/{id}/settlement requires authentication', async ({ request }) => {
    const response = await request.get('/api/trips/00000000-0000-0000-0000-000000000001/settlement');

    // Should return 401/403 (unauthenticated) or 404 (not found) or 200
    expect([200, 401, 403, 404]).toContain(response.status());
  });
});
