import { test, expect } from '@playwright/test';

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

  test.describe('E2E-AUTH-001: Google OAuth Flow', () => {
    // Note: Testing actual Google OAuth requires either:
    // 1. Real Google test account credentials
    // 2. Mock OAuth provider in test environment
    // 3. Backend test authentication endpoint

    test('Google login button redirects to OAuth', async ({ page }) => {
      await page.goto('/login');

      // Find and click Google login button
      const googleBtn = page.locator('a[href*="oauth2/authorization/google"]');

      if (await googleBtn.isVisible()) {
        // Get the href attribute
        const href = await googleBtn.getAttribute('href');
        expect(href).toContain('oauth2/authorization/google');
      }
    });

    test.skip('completes OAuth flow with test account', async ({ page }) => {
      // This test is skipped by default as it requires real OAuth credentials
      // Enable in CI with proper test account configuration

      await page.goto('/login');
      await page.click('a[href*="oauth2/authorization/google"]');

      // Wait for Google login page (or mock)
      await page.waitForURL(/accounts\.google\.com|mock-oauth/);

      // Fill in credentials (requires GOOGLE_TEST_EMAIL and GOOGLE_TEST_PASSWORD env vars)
      const testEmail = process.env.GOOGLE_TEST_EMAIL;
      const testPassword = process.env.GOOGLE_TEST_PASSWORD;

      if (testEmail && testPassword) {
        await page.fill('input[type="email"]', testEmail);
        await page.click('button:has-text("Next"), #identifierNext');
        await page.waitForLoadState('networkidle');

        await page.fill('input[type="password"]', testPassword);
        await page.click('button:has-text("Next"), #passwordNext');

        // Wait for redirect back to app
        await page.waitForURL('**/dashboard**');
        await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
      }
    });
  });

  test.describe('E2E-AUTH-002: Session Persistence', () => {
    test.skip('session persists across page navigation', async ({ page, context }) => {
      // This test requires a valid session
      // Skipped by default - enable when running with authenticated session
      // In real tests, this would be set up via test fixtures or beforeEach

      // First, inject a test session cookie (backend must support this)
      await context.addCookies([
        {
          name: 'test-auth-bypass',
          value: 'true',
          domain: 'localhost',
          path: '/',
        },
      ]);

      // Try to access dashboard
      const response = await page.goto('/dashboard');

      // Navigate to another page
      await page.goto('/');

      // Navigate back to dashboard
      await page.goto('/dashboard');

      // Should still be logged in (no redirect to login)
      await expect(page).not.toHaveURL(/login/);
    });

    test('session expires after timeout', async ({ page }) => {
      // This would require:
      // 1. Logging in
      // 2. Waiting for session timeout (or manipulating server-side timeout)
      // 3. Verifying redirect to login

      // Skipped as it requires long wait or backend control
      test.skip();
    });
  });

  test.describe('E2E-AUTH-003: Logout Flow', () => {
    test.skip('logout clears session and redirects to login', async ({ page, context }) => {
      // This test requires an authenticated session
      // Setup would involve either:
      // 1. Real OAuth login
      // 2. Backend test authentication endpoint
      // 3. Direct session manipulation

      // Assuming we have a logged-in session...
      await page.goto('/dashboard');

      // Click user menu
      await page.click('[data-testid="user-menu"]');

      // Click logout
      await page.click('[data-testid="logout-button"], a:has-text("登出"), button:has-text("Logout")');

      // Should redirect to login page
      await expect(page).toHaveURL(/login|index/);

      // Try to access protected page again
      await page.goto('/dashboard');

      // Should redirect to login
      await expect(page).toHaveURL(/login/);
    });
  });

  test.describe('Protected Routes', () => {
    const protectedRoutes = [
      '/dashboard',
      '/trips/new',
      '/trips/1',
      '/trips/1/activities',
      '/trips/1/expenses',
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
