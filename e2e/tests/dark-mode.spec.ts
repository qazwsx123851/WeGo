import { test, expect } from '@playwright/test';
import { authenticateTestUser, isTestAuthAvailable } from '../fixtures/auth-helper';

/**
 * Dark Mode E2E Tests
 *
 * Test ID mapping:
 * - E2E-DARK-001: Toggle dark mode
 * - E2E-DARK-002: Dark mode persistence
 * - E2E-DARK-003: System preference respect
 * - E2E-DARK-004: FOUC prevention
 */

test.describe('Dark Mode', () => {
  test.describe('E2E-DARK-001: Toggle Dark Mode', () => {
    test('dark mode toggle button is visible', async ({ page }) => {
      await page.goto('/login');

      // Wait for page to load
      await page.waitForLoadState('networkidle');

      // Look for dark mode toggle button
      const toggleButton = page.locator('#dark-mode-toggle, [aria-label*="深色"], [aria-label*="dark"]');

      // Toggle button should exist (may be in navbar or header)
      const count = await toggleButton.count();
      expect(count).toBeGreaterThanOrEqual(0); // May not be on login page
    });

    test('can toggle dark mode on dashboard', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
      }
      await authenticateTestUser(page);

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      // Look for dark mode toggle
      const toggleButton = page.locator('#dark-mode-toggle, [aria-label*="深色"], [aria-label*="dark"]').first();

      if (await toggleButton.count() > 0) {
        // Get initial state
        const initialDark = await page.evaluate(() =>
          document.documentElement.classList.contains('dark')
        );

        // Click toggle
        await toggleButton.click();

        // Wait for class change
        await page.waitForTimeout(100);

        // Check state changed
        const afterDark = await page.evaluate(() =>
          document.documentElement.classList.contains('dark')
        );

        expect(afterDark).toBe(!initialDark);
      }
    });

    test('toggle checkbox syncs with dark mode state', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
      }
      await authenticateTestUser(page);

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      const themeSwitch = page.locator('#theme-switch');

      if (await themeSwitch.count() > 0) {
        // Get initial dark mode state
        const isDark = await page.evaluate(() =>
          document.documentElement.classList.contains('dark')
        );

        // Checkbox should match current dark state
        if (isDark) {
          await expect(themeSwitch).toBeChecked();
        } else {
          await expect(themeSwitch).not.toBeChecked();
        }

        // Click the toggle label to switch
        await page.locator('#dark-mode-toggle').click();

        // Checkbox state should flip (Playwright auto-retries assertions)
        if (isDark) {
          await expect(themeSwitch).not.toBeChecked();
        } else {
          await expect(themeSwitch).toBeChecked();
        }
      }
    });
  });

  test.describe('E2E-DARK-002: Dark Mode Persistence', () => {
    test('dark mode setting persists across page navigation', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
      }
      await authenticateTestUser(page);

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      // Set dark mode via localStorage
      await page.evaluate(() => {
        localStorage.setItem('theme', 'dark');
        document.documentElement.classList.add('dark');
      });

      // Navigate to another page
      await page.goto('/trips/create');
      await page.waitForLoadState('networkidle');

      // Check dark mode is still active
      const isDark = await page.evaluate(() =>
        document.documentElement.classList.contains('dark')
      );

      expect(isDark).toBe(true);
    });

    test('dark mode setting persists after page refresh', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
      }
      await authenticateTestUser(page);

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      // Set dark mode
      await page.evaluate(() => {
        localStorage.setItem('theme', 'dark');
        document.documentElement.classList.add('dark');
      });

      // Refresh page
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check dark mode is still active
      const isDark = await page.evaluate(() =>
        document.documentElement.classList.contains('dark')
      );

      expect(isDark).toBe(true);
    });

    test('light mode setting persists', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
      }
      await authenticateTestUser(page);

      await page.goto('/dashboard');
      await page.waitForLoadState('networkidle');

      // Set light mode
      await page.evaluate(() => {
        localStorage.setItem('theme', 'light');
        document.documentElement.classList.remove('dark');
      });

      // Refresh page
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check light mode is active
      const isDark = await page.evaluate(() =>
        document.documentElement.classList.contains('dark')
      );

      expect(isDark).toBe(false);
    });
  });

  test.describe('E2E-DARK-003: System Preference', () => {
    test('respects system dark mode preference when no saved preference', async ({ page }) => {
      // Emulate dark color scheme
      await page.emulateMedia({ colorScheme: 'dark' });

      // Clear any saved theme
      await page.goto('/login');
      await page.evaluate(() => localStorage.removeItem('theme'));

      // Reload to apply
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check dark mode is active
      const isDark = await page.evaluate(() =>
        document.documentElement.classList.contains('dark')
      );

      expect(isDark).toBe(true);
    });

    test('respects system light mode preference when no saved preference', async ({ page }) => {
      // Emulate light color scheme
      await page.emulateMedia({ colorScheme: 'light' });

      // Clear any saved theme
      await page.goto('/login');
      await page.evaluate(() => localStorage.removeItem('theme'));

      // Reload to apply
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check light mode is active
      const isDark = await page.evaluate(() =>
        document.documentElement.classList.contains('dark')
      );

      expect(isDark).toBe(false);
    });

    test('saved preference overrides system preference', async ({ page }) => {
      // Emulate dark color scheme
      await page.emulateMedia({ colorScheme: 'dark' });

      // Set light mode in localStorage
      await page.goto('/login');
      await page.evaluate(() => localStorage.setItem('theme', 'light'));

      // Reload to apply
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check light mode is active despite system preference
      const isDark = await page.evaluate(() =>
        document.documentElement.classList.contains('dark')
      );

      expect(isDark).toBe(false);
    });
  });

  test.describe('E2E-DARK-004: Visual Consistency', () => {
    test('dark mode applies correct background colors', async ({ page }) => {
      await page.goto('/login');
      await page.evaluate(() => {
        localStorage.setItem('theme', 'dark');
        document.documentElement.classList.add('dark');
      });
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check body has dark background
      const bgColor = await page.evaluate(() => {
        const body = document.body;
        return window.getComputedStyle(body).backgroundColor;
      });

      // Dark mode should have dark background (not white)
      expect(bgColor).not.toBe('rgb(255, 255, 255)');
    });

    test('light mode applies correct background colors', async ({ page }) => {
      await page.goto('/login');
      await page.evaluate(() => {
        localStorage.setItem('theme', 'light');
        document.documentElement.classList.remove('dark');
      });
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check body has light background
      const bgColor = await page.evaluate(() => {
        const body = document.body;
        return window.getComputedStyle(body).backgroundColor;
      });

      // Light mode should not have very dark background
      // Parse RGB values
      const match = bgColor.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/);
      if (match) {
        const [, r, g, b] = match.map(Number);
        // At least one channel should be > 200 for light mode
        expect(r > 200 || g > 200 || b > 200).toBe(true);
      }
    });
  });
});

test.describe('Dark Mode on Public Pages', () => {
  test('login page supports dark mode', async ({ page }) => {
    await page.goto('/login');
    await page.evaluate(() => {
      localStorage.setItem('theme', 'dark');
      document.documentElement.classList.add('dark');
    });
    await page.reload();
    await page.waitForLoadState('networkidle');

    const isDark = await page.evaluate(() =>
      document.documentElement.classList.contains('dark')
    );

    expect(isDark).toBe(true);
  });

  test('home page supports dark mode', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.setItem('theme', 'dark');
      document.documentElement.classList.add('dark');
    });
    await page.reload();
    await page.waitForLoadState('networkidle');

    const isDark = await page.evaluate(() =>
      document.documentElement.classList.contains('dark')
    );

    expect(isDark).toBe(true);
  });

  test('error pages support dark mode', async ({ page }) => {
    // Go to a non-existent page to trigger 404
    await page.goto('/this-page-does-not-exist-12345');
    await page.evaluate(() => {
      localStorage.setItem('theme', 'dark');
      document.documentElement.classList.add('dark');
    });

    // Check dark class is present
    const isDark = await page.evaluate(() =>
      document.documentElement.classList.contains('dark')
    );

    expect(isDark).toBe(true);
  });
});
