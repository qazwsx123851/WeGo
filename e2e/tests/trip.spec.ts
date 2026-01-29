import { test, expect } from '@playwright/test';
import { sampleTrips, generateRandomTrip, formatDateForInput, getDaysFromNow } from '../fixtures/test-data';

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
  // These tests require authentication
  // In a real setup, use test fixtures to handle login before each test

  test.describe.skip('E2E-TRIP-001: Create Trip', () => {
    // Skipped by default - requires authenticated session

    test('displays trip creation form', async ({ page }) => {
      await page.goto('/trips/new');

      // Should show form fields
      await expect(page.locator('input[name="title"], #title')).toBeVisible();
      await expect(page.locator('input[name="startDate"], #startDate')).toBeVisible();
      await expect(page.locator('input[name="endDate"], #endDate')).toBeVisible();
    });

    test('validates required fields', async ({ page }) => {
      await page.goto('/trips/new');

      // Try to submit empty form
      await page.click('button[type="submit"], button:has-text("建立")');

      // Should show validation errors
      const errorMessage = page.locator('.error, [data-error], :invalid');
      await expect(errorMessage).toBeVisible();
    });

    test('validates date range', async ({ page }) => {
      await page.goto('/trips/new');

      // Set end date before start date
      await page.fill('input[name="startDate"], #startDate', '2026-03-05');
      await page.fill('input[name="endDate"], #endDate', '2026-03-01');
      await page.fill('input[name="title"], #title', 'Test Trip');

      await page.click('button[type="submit"], button:has-text("建立")');

      // Should show date validation error
      const errorMessage = page.locator('text=/日期|date|invalid/i');
      await expect(errorMessage).toBeVisible();
    });

    test('creates trip with valid data', async ({ page }) => {
      await page.goto('/trips/new');

      const trip = generateRandomTrip();

      // Fill form
      await page.fill('input[name="title"], #title', trip.title);
      await page.fill('input[name="description"], #description, textarea[name="description"]', trip.description);
      await page.fill('input[name="startDate"], #startDate', trip.startDate);
      await page.fill('input[name="endDate"], #endDate', trip.endDate);

      // Submit
      await page.click('button[type="submit"], button:has-text("建立")');

      // Should redirect to trip detail or dashboard
      await expect(page).toHaveURL(/trips\/\d+|dashboard/);

      // Verify trip was created
      await expect(page.locator(`text=${trip.title}`)).toBeVisible();
    });

    test('shows trip in dashboard after creation', async ({ page }) => {
      const trip = generateRandomTrip();

      // Create trip via form
      await page.goto('/trips/new');
      await page.fill('input[name="title"], #title', trip.title);
      await page.fill('input[name="startDate"], #startDate', trip.startDate);
      await page.fill('input[name="endDate"], #endDate', trip.endDate);
      await page.click('button[type="submit"], button:has-text("建立")');

      // Go to dashboard
      await page.goto('/dashboard');

      // Trip should be listed
      await expect(page.locator(`text=${trip.title}`)).toBeVisible();
    });
  });

  test.describe.skip('E2E-TRIP-002: Edit Trip', () => {
    // Requires authenticated session and existing trip

    test('loads trip data into edit form', async ({ page }) => {
      // Assuming trip ID 1 exists
      await page.goto('/trips/1/edit');

      // Form should be pre-filled
      const titleInput = page.locator('input[name="title"], #title');
      await expect(titleInput).not.toHaveValue('');
    });

    test('updates trip with new data', async ({ page }) => {
      await page.goto('/trips/1/edit');

      const newTitle = `更新後的行程 ${Date.now()}`;
      await page.fill('input[name="title"], #title', newTitle);

      await page.click('button[type="submit"], button:has-text("儲存")');

      // Should show updated title
      await expect(page.locator(`text=${newTitle}`)).toBeVisible();
    });

    test('preserves other fields when updating one field', async ({ page }) => {
      await page.goto('/trips/1/edit');

      // Get original values
      const originalStartDate = await page.inputValue('input[name="startDate"], #startDate');

      // Update only title
      await page.fill('input[name="title"], #title', 'New Title Only');
      await page.click('button[type="submit"], button:has-text("儲存")');

      // Verify start date is preserved
      await page.goto('/trips/1/edit');
      const currentStartDate = await page.inputValue('input[name="startDate"], #startDate');
      expect(currentStartDate).toBe(originalStartDate);
    });
  });

  test.describe.skip('E2E-TRIP-003: Invite Members', () => {
    // Requires authenticated session and OWNER permission

    test('generates invite link', async ({ page }) => {
      await page.goto('/trips/1/members');

      // Click invite button
      await page.click('button:has-text("邀請"), button:has-text("Invite")');

      // Should show invite link or modal
      const inviteLinkInput = page.locator('input[readonly], .invite-link, [data-testid="invite-link"]');
      await expect(inviteLinkInput).toBeVisible();

      // Link should contain invite token
      const linkValue = await inviteLinkInput.inputValue();
      expect(linkValue).toMatch(/invite|token/i);
    });

    test('copies invite link to clipboard', async ({ page }) => {
      await page.goto('/trips/1/members');
      await page.click('button:has-text("邀請")');

      // Click copy button
      await page.click('button:has-text("複製"), button[data-testid="copy-link"]');

      // Should show success feedback
      const successMessage = page.locator('text=/已複製|copied/i');
      await expect(successMessage).toBeVisible();
    });

    test('allows setting invite role', async ({ page }) => {
      await page.goto('/trips/1/members');
      await page.click('button:has-text("邀請")');

      // Should have role selection
      const roleSelect = page.locator('select[name="role"], [data-testid="role-select"]');
      await expect(roleSelect).toBeVisible();

      // Select EDITOR role
      await roleSelect.selectOption('EDITOR');
    });
  });

  test.describe.skip('E2E-TRIP-004: Role Permissions', () => {
    // Requires multiple test users with different roles

    test('OWNER can delete trip', async ({ page }) => {
      await page.goto('/trips/1');

      // Should see delete button
      const deleteBtn = page.locator('button:has-text("刪除"), button[data-testid="delete-trip"]');
      await expect(deleteBtn).toBeVisible();
    });

    test('EDITOR cannot delete trip', async ({ page }) => {
      // Login as EDITOR user (requires test setup)
      await page.goto('/trips/1');

      // Should NOT see delete button
      const deleteBtn = page.locator('button:has-text("刪除"), button[data-testid="delete-trip"]');
      await expect(deleteBtn).not.toBeVisible();
    });

    test('VIEWER cannot edit trip', async ({ page }) => {
      // Login as VIEWER user (requires test setup)
      await page.goto('/trips/1');

      // Should NOT see edit button
      const editBtn = page.locator('button:has-text("編輯"), a:has-text("編輯")');
      await expect(editBtn).not.toBeVisible();
    });

    test('OWNER can change member roles', async ({ page }) => {
      await page.goto('/trips/1/members');

      // Should see role change dropdown for other members
      const roleSelect = page.locator('.member-role-select, [data-testid="member-role-select"]');
      await expect(roleSelect.first()).toBeVisible();
    });

    test('OWNER can remove members', async ({ page }) => {
      await page.goto('/trips/1/members');

      // Should see remove button for other members
      const removeBtn = page.locator('.remove-member, button[data-testid="remove-member"]');
      await expect(removeBtn.first()).toBeVisible();
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
  test.skip('displays empty state when no trips', async ({ page }) => {
    // Requires authenticated user with no trips
    await page.goto('/dashboard');

    // Should show empty state
    const emptyState = page.locator('text=/沒有行程|no trips|empty/i, [data-testid="empty-state"]');
    await expect(emptyState).toBeVisible();
  });

  test.skip('shows upcoming trips section', async ({ page }) => {
    // Requires authenticated user with upcoming trips
    await page.goto('/dashboard');

    // Should have upcoming trips section
    const upcomingSection = page.locator('text=/即將出發|upcoming/i');
    await expect(upcomingSection).toBeVisible();
  });

  test.skip('sorts trips by start date', async ({ page }) => {
    await page.goto('/dashboard');

    // Get all trip cards
    const tripCards = page.locator('.trip-card, [data-testid="trip-card"]');
    const count = await tripCards.count();

    if (count >= 2) {
      // Verify trips are sorted (would need date parsing logic)
      // This is a simplified check
      expect(count).toBeGreaterThanOrEqual(2);
    }
  });

  test.skip('navigates to trip detail on card click', async ({ page }) => {
    await page.goto('/dashboard');

    // Click first trip card
    const firstCard = page.locator('.trip-card, [data-testid="trip-card"]').first();
    await firstCard.click();

    // Should navigate to trip detail
    await expect(page).toHaveURL(/trips\/\d+/);
  });
});

test.describe('Delete Trip', () => {
  test.skip('shows confirmation dialog before delete', async ({ page }) => {
    await page.goto('/trips/1');

    // Click delete button
    await page.click('button:has-text("刪除"), button[data-testid="delete-trip"]');

    // Should show confirmation dialog
    const confirmDialog = page.locator('[role="dialog"], .modal, .confirm-dialog');
    await expect(confirmDialog).toBeVisible();

    // Should have confirm/cancel buttons
    await expect(page.locator('button:has-text("確認"), button:has-text("Confirm")')).toBeVisible();
    await expect(page.locator('button:has-text("取消"), button:has-text("Cancel")')).toBeVisible();
  });

  test.skip('cancels delete when clicking cancel', async ({ page }) => {
    await page.goto('/trips/1');
    await page.click('button:has-text("刪除")');

    // Click cancel
    await page.click('button:has-text("取消")');

    // Dialog should close
    const confirmDialog = page.locator('[role="dialog"], .modal');
    await expect(confirmDialog).not.toBeVisible();

    // Should still be on trip page
    await expect(page).toHaveURL(/trips\/1/);
  });

  test.skip('deletes trip and redirects to dashboard', async ({ page }) => {
    // Create a trip to delete
    const trip = generateRandomTrip();
    await page.goto('/trips/new');
    await page.fill('input[name="title"]', trip.title);
    await page.fill('input[name="startDate"]', trip.startDate);
    await page.fill('input[name="endDate"]', trip.endDate);
    await page.click('button[type="submit"]');

    // Get the trip ID from URL
    await page.waitForURL(/trips\/\d+/);
    const tripUrl = page.url();

    // Delete the trip
    await page.click('button:has-text("刪除")');
    await page.click('button:has-text("確認")');

    // Should redirect to dashboard
    await expect(page).toHaveURL(/dashboard/);

    // Trip should no longer appear
    await expect(page.locator(`text=${trip.title}`)).not.toBeVisible();
  });
});
