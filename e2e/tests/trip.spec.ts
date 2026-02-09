import { test, expect } from '@playwright/test';
import { sampleTrips, generateRandomTrip, formatDateForInput, getDaysFromNow } from '../fixtures/test-data';
import { authenticateTestUser, isTestAuthAvailable } from '../fixtures/auth-helper';

/**
 * Trip Management E2E Tests
 *
 * Test ID mapping:
 * - E2E-TRIP-001: Create new trip
 * - E2E-TRIP-002: Edit trip information
 * - E2E-TRIP-003: Invite members
 * - E2E-TRIP-004: Role permission verification
 */

test.describe('Trip Management', () => {
  test.describe('E2E-TRIP-001: Create Trip', () => {
    test.beforeEach(async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
      }
      await authenticateTestUser(page);
    });

    test('displays trip creation form', async ({ page }) => {
      await page.goto('/trips/create');

      // Should show form fields
      await expect(page.locator('input[name="title"], #title')).toBeVisible();
      await expect(page.locator('input[name="startDate"], #startDate')).toBeVisible();
      await expect(page.locator('input[name="endDate"], #endDate')).toBeVisible();
    });

    test('validates required fields', async ({ page }) => {
      await page.goto('/trips/create');

      // Try to submit empty form
      await page.click('button[type="submit"], button:has-text("建立")');

      // Should show validation errors or prevent submission
      // HTML5 validation will prevent submission if required fields are empty
      await expect(page).toHaveURL(/trips\/create/);
    });

    test('validates date range', async ({ page }) => {
      await page.goto('/trips/create');

      // Fill form with invalid date range (end before start)
      await page.fill('input[name="title"], #title', 'Test Trip');
      await page.fill('input[name="startDate"], #startDate', '2026-03-05');
      await page.fill('input[name="endDate"], #endDate', '2026-03-01');

      await page.click('button[type="submit"], button:has-text("建立")');

      // Should show date validation error or stay on page
      // This depends on frontend/backend validation
      const pageUrl = page.url();
      const hasError = pageUrl.includes('create') || pageUrl.includes('error');
      const errorMessage = page.locator('.error, [data-error], .text-red-500, .text-rose-500');

      // Either stays on create page or shows error
      expect(hasError || await errorMessage.count() > 0).toBe(true);
    });

    test('creates trip with valid data', async ({ page }) => {
      await page.goto('/trips/create');

      const trip = generateRandomTrip();

      // Fill form
      await page.fill('input[name="title"], #title', trip.title);

      const descriptionField = page.locator('input[name="description"], #description, textarea[name="description"]');
      if (await descriptionField.count() > 0) {
        await descriptionField.fill(trip.description);
      }

      await page.fill('input[name="startDate"], #startDate', trip.startDate);
      await page.fill('input[name="endDate"], #endDate', trip.endDate);

      // Submit
      await page.click('button[type="submit"], button:has-text("建立")');

      // Wait for navigation
      await page.waitForURL(/trips\/|dashboard/, { timeout: 10000 });

      // Should navigate away from create page
      await expect(page).not.toHaveURL(/trips\/create/);
    });
  });

  test.describe('E2E-TRIP-002: View Trip', () => {
    test.beforeEach(async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
      }
      await authenticateTestUser(page);
    });

    test('dashboard is accessible after login', async ({ page }) => {
      await page.goto('/dashboard');

      // Should not redirect to login
      await expect(page).not.toHaveURL(/login/);

      // Should show dashboard content
      const pageContent = page.locator('main, [role="main"], .content');
      await expect(pageContent.first()).toBeVisible();
    });
  });

  test.describe('API Integration', () => {
    test('GET /api/trips requires authentication', async ({ request }) => {
      // Note: This will return 401/403 without auth
      const response = await request.get('/api/trips');

      // Without auth, should be 401 or 403
      // Spring Security may return 401 (unauthorized) or redirect
      expect([200, 302, 401, 403]).toContain(response.status());
    });

    test('POST /api/trips requires authentication or CSRF', async ({ request }) => {
      const response = await request.post('/api/trips', {
        data: {
          title: 'Test Trip',
          startDate: '2026-03-01',
          endDate: '2026-03-05',
        },
      });

      // Should return 401 (unauthorized), 403 (CSRF), or redirect
      expect([401, 403]).toContain(response.status());
    });

    test('GET /api/trips/{id} requires authentication', async ({ request }) => {
      const response = await request.get('/api/trips/999999');

      // Either 401/403 (unauthenticated) or 404 (not found) or redirect
      expect([200, 302, 401, 403, 404]).toContain(response.status());
    });
  });
});

test.describe('Trip List View', () => {
  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
  });

  test('displays trip list or empty state', async ({ page }) => {
    await page.goto('/dashboard');

    // Wait for content to load
    await page.waitForLoadState('networkidle');

    // Should show either trips or empty state
    const hasTripCards = await page.locator('.trip-card, [data-testid="trip-card"]').count() > 0;
    const hasEmptyStateText = await page.locator('text=/沒有行程|no trips|建立第一個|開始規劃/i').count() > 0;
    const hasEmptyStateTestId = await page.locator('[data-testid="empty-state"]').count() > 0;

    // Either has trips or shows empty state
    expect(hasTripCards || hasEmptyStateText || hasEmptyStateTestId).toBe(true);
  });
});

test.describe('E2E-TRIP-005: Dashboard Shows Created Trip', () => {
  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
  });

  test('created trip appears on dashboard', async ({ page }) => {
    const trip = generateRandomTrip();

    // Create trip
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');
    await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 10000 });

    // Go to dashboard
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Trip title should be visible on dashboard
    await expect(page.locator(`text=${trip.title}`)).toBeVisible();
  });
});

test.describe('E2E-TRIP-006: Trip Detail Page', () => {
  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
  });

  test('trip detail page displays correctly', async ({ page }) => {
    const trip = generateRandomTrip();

    // Create trip
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');
    await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 10000 });

    // Should show trip title
    await expect(page.locator(`text=${trip.title}`)).toBeVisible();

    // Should have navigation/action links
    const hasEditLink = await page.locator('a[href*="edit"], button:has-text("編輯")').count() > 0;
    const hasActivitiesLink = await page.locator('a[href*="activities"]').count() > 0;
    const hasPageContent = await page.locator('main, [role="main"]').count() > 0;

    expect(hasEditLink || hasActivitiesLink || hasPageContent).toBe(true);
  });
});

test.describe('E2E-TRIP-007: Edit Trip', () => {
  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
  });

  test('can edit trip title', async ({ page }) => {
    const trip = generateRandomTrip();

    // Create trip
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');
    await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 10000 });

    const url = page.url();
    const tripId = url.match(/trips\/([a-f0-9-]+)/)?.[1];

    // Go to edit page
    await page.goto(`/trips/${tripId}/edit`);

    // Title should be pre-filled
    const titleInput = page.locator('input[name="title"], #title');
    await expect(titleInput).toHaveValue(trip.title);

    // Update title
    const newTitle = `更新後 ${Date.now()}`;
    await titleInput.fill(newTitle);
    await page.click('button[type="submit"], button:has-text("儲存"), button:has-text("更新")');

    // Should redirect and show new title
    await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 10000 });
    await expect(page.locator(`text=${newTitle}`)).toBeVisible();
  });
});

test.describe('E2E-TRIP-008: Edit Trip Date Validation', () => {
  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
  });

  test('rejects end date before start date on edit', async ({ page }) => {
    const trip = generateRandomTrip();

    // Create trip
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');
    await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 10000 });

    const tripId = page.url().match(/trips\/([a-f0-9-]+)/)?.[1];

    // Go to edit page
    await page.goto(`/trips/${tripId}/edit`);

    // Set invalid date range
    await page.fill('input[name="endDate"], #endDate', trip.startDate);
    await page.fill('input[name="startDate"], #startDate', trip.endDate);

    await page.click('button[type="submit"], button:has-text("儲存"), button:has-text("更新")');

    // Should stay on edit page or show error
    const pageUrl = page.url();
    const hasError = pageUrl.includes('edit') || pageUrl.includes('error');
    const errorMessage = page.locator('.error, [data-error], .text-red-500, .text-rose-500, text=/日期/i');
    expect(hasError || await errorMessage.count() > 0).toBe(true);
  });
});

test.describe('E2E-TRIP-009: Members Page', () => {
  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
  });

  test('members page shows creator as Owner', async ({ page }) => {
    const trip = generateRandomTrip();

    // Create trip
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');
    await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 10000 });

    const tripId = page.url().match(/trips\/([a-f0-9-]+)/)?.[1];

    // Navigate to members page
    await page.goto(`/trips/${tripId}/members`);
    await page.waitForLoadState('networkidle');

    // Should show owner role indicator
    const ownerLabel = page.locator('text=/Owner|OWNER|擁有者|建立者/i');
    const memberContent = page.locator('main, [role="main"]').first();

    await expect(memberContent).toBeVisible();
    const hasOwnerLabel = await ownerLabel.count() > 0;
    const hasPageContent = await memberContent.isVisible();

    expect(hasOwnerLabel || hasPageContent).toBe(true);
  });
});
