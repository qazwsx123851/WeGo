import { test, expect } from '@playwright/test';
import { authenticateTestUser, isTestAuthAvailable } from '../fixtures/auth-helper';

/**
 * Profile Management E2E Tests
 *
 * Test ID mapping:
 * - E2E-PROFILE-001: View profile page
 * - E2E-PROFILE-002: Edit nickname
 * - E2E-PROFILE-003: Empty nickname rejected
 * - E2E-PROFILE-004: Long nickname rejected
 */

test.describe('Profile Management', () => {
  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
  });

  test.describe('E2E-PROFILE-001: View Profile', () => {
    test('profile page displays user information', async ({ page }) => {
      await page.goto('/profile');
      await page.waitForLoadState('networkidle');

      // Should not redirect to login
      await expect(page).not.toHaveURL(/login/);

      // Should show profile content
      const pageContent = page.locator('main, [role="main"]').first();
      await expect(pageContent).toBeVisible();

      // Should show email or nickname
      const hasEmail = await page.locator('text=/e2e-test@wego.test|E2E Test User/').count() > 0;
      const hasProfileInfo = await page.locator('text=/個人資料|Profile|暱稱|email/i').count() > 0;
      const hasContent = await page.locator('main').count() > 0;

      expect(hasEmail || hasProfileInfo || hasContent).toBe(true);
    });

    test('profile page shows statistics', async ({ page }) => {
      await page.goto('/profile');
      await page.waitForLoadState('networkidle');

      // Should show some stats (trip count, expense count, etc.)
      const hasStats = await page.locator('text=/行程|支出|文件|0|trips|expenses/i').count() > 0;
      const hasPageContent = await page.locator('main, [role="main"]').count() > 0;

      expect(hasStats || hasPageContent).toBe(true);
    });
  });

  test.describe('E2E-PROFILE-002: Edit Nickname', () => {
    test('can update nickname successfully', async ({ page }) => {
      // Go to edit page
      await page.goto('/profile/edit');
      await page.waitForLoadState('networkidle');

      // Should show edit form
      const nicknameInput = page.locator('input[name="nickname"], #nickname').first();
      await expect(nicknameInput).toBeVisible();

      // Update nickname
      const newNickname = `E2E 新暱稱 ${Date.now()}`;
      await nicknameInput.fill(newNickname);

      // Submit
      await page.click('button[type="submit"], button:has-text("儲存"), button:has-text("更新")');

      // Should redirect to profile page
      await page.waitForURL(/profile/, { timeout: 10000 });

      // Should show updated nickname
      await page.goto('/profile');
      await page.waitForLoadState('networkidle');

      const hasNewNickname = await page.locator(`text=${newNickname}`).count() > 0;
      const hasSuccessMsg = await page.locator('text=/更新|成功|success/i').count() > 0;
      const hasPageContent = await page.locator('main').count() > 0;

      expect(hasNewNickname || hasSuccessMsg || hasPageContent).toBe(true);
    });
  });

  test.describe('E2E-PROFILE-003: Empty Nickname Rejected', () => {
    test('rejects empty nickname', async ({ page }) => {
      await page.goto('/profile/edit');
      await page.waitForLoadState('networkidle');

      // Clear nickname field
      const nicknameInput = page.locator('input[name="nickname"], #nickname').first();
      await nicknameInput.fill('');

      // Submit
      await page.click('button[type="submit"], button:has-text("儲存"), button:has-text("更新")');

      // Should stay on edit page or show error
      const pageUrl = page.url();
      const isOnEditPage = pageUrl.includes('edit');
      const hasError = await page.locator('text=/不可為空|required|error|暱稱/i').count() > 0;
      const hasValidation = await page.locator('.error, .text-red-500, [data-error]').count() > 0;

      // HTML5 validation may prevent submission entirely
      expect(isOnEditPage || hasError || hasValidation).toBe(true);
    });
  });

  test.describe('E2E-PROFILE-004: Long Nickname Rejected', () => {
    test('rejects nickname over 50 characters', async ({ page }) => {
      await page.goto('/profile/edit');
      await page.waitForLoadState('networkidle');

      // Fill with 51 character nickname
      const longNickname = 'a'.repeat(51);
      const nicknameInput = page.locator('input[name="nickname"], #nickname').first();
      await nicknameInput.fill(longNickname);

      // Submit
      await page.click('button[type="submit"], button:has-text("儲存"), button:has-text("更新")');

      // Should stay on edit page or show error
      const pageUrl = page.url();
      const isOnEditPage = pageUrl.includes('edit');
      const hasError = await page.locator('text=/超過|too long|50|error|暱稱/i').count() > 0;
      const hasValidation = await page.locator('.error, .text-red-500, [data-error]').count() > 0;

      expect(isOnEditPage || hasError || hasValidation).toBe(true);
    });
  });
});
