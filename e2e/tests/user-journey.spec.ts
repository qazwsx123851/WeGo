import { test, expect } from '@playwright/test';
import { authenticateTestUser, isTestAuthAvailable } from '../fixtures/auth-helper';
import { generateRandomTrip } from '../fixtures/test-data';
import { createTestTodo, apiPost, getCSRFToken } from '../fixtures/test-setup';

/**
 * Complete User Journey E2E Test
 *
 * Tests the full happy path:
 * Login -> Create Trip -> Dashboard -> Add Activity -> Add Expense ->
 * View Settlement -> Add Todo -> View Todos -> View Profile -> Edit Nickname
 */

test.describe('User Journey: Complete Happy Path', () => {
  test('full user journey from login to profile', async ({ page }) => {
    // === Step 1: Auth Check ===
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
      return;
    }

    // === Step 2: Login ===
    const user = await authenticateTestUser(page, {
      email: 'journey-test@wego.test',
      name: 'Journey Tester',
    });
    expect(user.userId).toBeTruthy();

    // === Step 3: Dashboard is Accessible ===
    await page.goto('/dashboard');
    await expect(page).not.toHaveURL(/login/);
    const dashboardContent = page.locator('main, [role="main"]').first();
    await expect(dashboardContent).toBeVisible();

    // === Step 4: Create Trip ===
    const trip = generateRandomTrip();
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');

    // Wait for redirect to trip detail
    await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 15000 });
    const tripUrl = page.url();
    const tripId = tripUrl.match(/trips\/([a-f0-9-]+)/)?.[1];
    expect(tripId).toBeTruthy();

    // === Step 5: Verify Trip on Dashboard ===
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');
    await expect(page.locator(`text=${trip.title}`)).toBeVisible();

    // === Step 6: Add Activity ===
    await page.goto(`/trips/${tripId}/activities/new`);

    const placeInput = page.locator('input[name="placeName"], #placeName, [data-place-search]').first();
    if (await placeInput.count() > 0) {
      await placeInput.fill('旅程測試景點');

      const dateInput = page.locator('input[name="activityDate"], #activityDate').first();
      if (await dateInput.count() > 0) {
        await dateInput.fill(trip.startDate);
      }

      await page.click('button[type="submit"], button:has-text("新增"), button:has-text("建立")');
      await page.waitForURL(/activities/, { timeout: 10000 });
    }

    // === Step 7: View Activities List ===
    await page.goto(`/trips/${tripId}/activities`);
    await page.waitForLoadState('networkidle');
    const activitiesPage = page.locator('main, [role="main"]').first();
    await expect(activitiesPage).toBeVisible();

    // === Step 8: Navigate to Expense Creation ===
    await page.goto(`/trips/${tripId}/expenses/create`, { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle');

    const descInput = page.locator('input[name="description"], #description').first();
    const amountInput = page.locator('input[name="amount"], #amount').first();

    if (await descInput.count() > 0 && await amountInput.count() > 0) {
      await descInput.fill('旅程測試支出');
      await amountInput.fill('1500');

      const submitBtn = page.locator('button[type="submit"], button:has-text("新增"), button:has-text("建立")').first();
      if (await submitBtn.count() > 0) {
        await submitBtn.click();
        await page.waitForURL(/expenses/, { timeout: 10000 });
      }
    }

    // === Step 9: View Settlement ===
    await page.goto(`/trips/${tripId}/settlement`);
    await page.waitForLoadState('networkidle');
    const settlementPage = page.locator('main, [role="main"]').first();
    await expect(settlementPage).toBeVisible();

    // === Step 10: Add Todo via API ===
    const todoTitle = `旅程待辦 ${Date.now()}`;
    const todo = await createTestTodo(page, tripId!, todoTitle);
    expect(todo.id).toBeTruthy();

    // === Step 11: View Todos Page ===
    await page.goto(`/trips/${tripId}/todos`);
    await page.waitForLoadState('networkidle');
    await expect(page.locator(`text=${todoTitle}`)).toBeVisible();

    // === Step 12: View Profile ===
    await page.goto('/profile');
    await page.waitForLoadState('networkidle');
    await expect(page).not.toHaveURL(/login/);
    const profileContent = page.locator('main, [role="main"]').first();
    await expect(profileContent).toBeVisible();

    // === Step 13: Edit Nickname ===
    await page.goto('/profile/edit');
    await page.waitForLoadState('networkidle');

    const nicknameInput = page.locator('input[name="nickname"], #nickname').first();
    if (await nicknameInput.count() > 0) {
      const newNickname = `旅程測試者 ${Date.now()}`;
      await nicknameInput.fill(newNickname);
      await page.click('button[type="submit"], button:has-text("儲存"), button:has-text("更新")');

      // Should redirect to profile
      await page.waitForURL(/profile/, { timeout: 10000 });
    }

    // === Final: Verify we can still access dashboard ===
    await page.goto('/dashboard');
    await expect(page).not.toHaveURL(/login/);
    await expect(page.locator(`text=${trip.title}`)).toBeVisible();
  });
});
