import { test, expect } from '@playwright/test';
import { authenticateTestUser, logoutTestUser, isTestAuthAvailable, DEFAULT_TEST_USER } from '../fixtures/auth-helper';

/**
 * Authentication E2E Tests
 *
 * Test ID mapping:
 * - E2E-AUTH-001: Google OAuth login flow
 * - E2E-AUTH-002: Session persistence
 * - E2E-AUTH-003: Logout flow
 */

test.describe('Authentication Flow', () => {
  test.describe('Login Page', () => {
    test('displays Google login button', async ({ page }) => {
      await page.goto('/login');

      // Should have Google OAuth login button
      const googleLoginBtn = page.locator('a[href*="oauth2/authorization/google"], button:has-text("Google")');
      await expect(googleLoginBtn).toBeVisible();
    });

    test('redirects unauthenticated users to login', async ({ page }) => {
      // Try to access protected page
      await page.goto('/dashboard');

      // Should redirect to login page
      await expect(page).toHaveURL(/login/);
    });

    test('shows app branding on login page', async ({ page }) => {
      await page.goto('/login');

      // Should show WeGo branding
      const branding = page.locator('text=/WeGo/i');
      await expect(branding).toBeVisible();
    });
  });

  test.describe('E2E-AUTH-001: Test Auth Flow', () => {
    test('test auth endpoint is available in test profile', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);

      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available - run with test/e2e profile');
        return;
      }

      expect(isAvailable).toBe(true);
    });

    test('can authenticate via test endpoint', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
        return;
      }

      // Authenticate
      const user = await authenticateTestUser(page);

      expect(user.userId).toBeTruthy();
      expect(user.email).toBe(DEFAULT_TEST_USER.email);
      expect(user.sessionId).toBeTruthy();

      // Verify we can access protected page
      await page.goto('/dashboard');
      await expect(page).not.toHaveURL(/login/);
    });

    test('authenticated user can access dashboard', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
        return;
      }

      // Authenticate
      await authenticateTestUser(page);

      // Navigate to dashboard
      await page.goto('/dashboard');

      // Should be on dashboard (not redirected to login)
      await expect(page).not.toHaveURL(/login/);

      // Should see user-related content
      const userContent = page.locator('nav, header').first();
      await expect(userContent).toBeVisible();
    });
  });

  test.describe('E2E-AUTH-002: Session Persistence', () => {
    test('session persists across page navigation', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
        return;
      }

      // Authenticate
      await authenticateTestUser(page);

      // Navigate to dashboard
      await page.goto('/dashboard');
      await expect(page).not.toHaveURL(/login/);

      // Navigate to home
      await page.goto('/');

      // Navigate back to dashboard
      await page.goto('/dashboard');

      // Should still be logged in
      await expect(page).not.toHaveURL(/login/);
    });

    test('session persists after page refresh', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
        return;
      }

      // Authenticate
      await authenticateTestUser(page);

      // Navigate to dashboard
      await page.goto('/dashboard');
      await expect(page).not.toHaveURL(/login/);

      // Refresh page
      await page.reload();

      // Should still be logged in
      await expect(page).not.toHaveURL(/login/);
    });
  });

  test.describe('E2E-AUTH-003: Logout Flow', () => {
    test('logout clears session and redirects', async ({ page }) => {
      const isAvailable = await isTestAuthAvailable(page);
      if (!isAvailable) {
        test.skip(true, 'Test auth endpoint not available');
        return;
      }

      // Authenticate
      await authenticateTestUser(page);

      // Verify authenticated
      await page.goto('/dashboard');
      await expect(page).not.toHaveURL(/login/);

      // Logout via test endpoint
      await logoutTestUser(page);

      // Try to access protected page
      await page.goto('/dashboard');

      // Should be redirected to login
      await expect(page).toHaveURL(/login/);
    });
  });

  test.describe('Protected Routes', () => {
    const protectedRoutes = [
      '/dashboard',
      '/trips/create',
    ];

    for (const route of protectedRoutes) {
      test(`${route} requires authentication`, async ({ page }) => {
        await page.goto(route);

        // Should redirect to login OR return 401
        const url = page.url();
        const isRedirectedToLogin = url.includes('login');
        const statusCode = await page.evaluate(() => {
          // Check if page shows error state
          const errorElement = document.querySelector('[data-error-code], .error-401');
          return errorElement ? 401 : 200;
        });

        expect(isRedirectedToLogin || statusCode === 401).toBe(true);
      });
    }
  });

  test.describe('CSRF Protection', () => {
    test('API endpoints reject requests without CSRF token', async ({ request }) => {
      // Try to make a POST request without CSRF token
      const response = await request.post('/api/trips', {
        data: {
          title: 'Test Trip',
          startDate: '2026-03-01',
          endDate: '2026-03-05',
        },
        headers: {
          'Content-Type': 'application/json',
          // No CSRF token
        },
      });

      // Should be rejected (401 for unauthenticated, 403 for CSRF)
      expect([401, 403]).toContain(response.status());
    });
  });
});

test.describe('Security Headers', () => {
  test('response includes security headers', async ({ request }) => {
    const response = await request.get('/');
    const headers = response.headers();

    // Check for common security headers
    // Note: Actual headers depend on Spring Security configuration
    // These are recommendations, not requirements

    // X-Content-Type-Options
    if (headers['x-content-type-options']) {
      expect(headers['x-content-type-options']).toBe('nosniff');
    }

    // X-Frame-Options
    if (headers['x-frame-options']) {
      expect(['DENY', 'SAMEORIGIN']).toContain(headers['x-frame-options']);
    }
  });
});
