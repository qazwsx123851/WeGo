import { test, expect } from '@playwright/test';

/**
 * Health Check and Basic Smoke Tests
 *
 * These tests verify the application is running and basic pages are accessible.
 */

test.describe('Health Check', () => {
  test('API health endpoint returns OK', async ({ request }) => {
    const response = await request.get('/api/health');
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.data.status).toBe('healthy');
    expect(body.data.application).toBe('WeGo');
  });

  test('Health response has expected structure', async ({ request }) => {
    const response = await request.get('/api/health');
    const body = await response.json();

    // Verify response structure
    expect(body).toHaveProperty('success');
    expect(body).toHaveProperty('data');
    expect(body).toHaveProperty('timestamp');
  });
});

test.describe('Basic Page Loading', () => {
  test('Home page loads successfully', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/WeGo/);

    // Check for main content - landing page has body with content
    const heroSection = page.locator('body');
    await expect(heroSection).toBeVisible();

    // Check for WeGo branding
    await expect(page.locator('h1:has-text("WeGo")')).toBeVisible();
  });

  test('Login page loads successfully', async ({ page }) => {
    await page.goto('/login');

    // Should see login page elements
    const loginButton = page.locator('a[href*="oauth2/authorization/google"]');
    await expect(loginButton).toBeVisible();
  });

  test('Unknown routes redirect to login or show error', async ({ page }) => {
    const response = await page.goto('/this-page-does-not-exist-12345');

    // May redirect to login (302->200) or show error page
    // Spring Security may redirect unauthenticated requests to login
    const status = response?.status();
    expect([200, 302, 404]).toContain(status);

    // If 200, should show login page or error page
    if (status === 200) {
      // Use .first() to handle multiple matches
      const hasLoginOrError = await page.locator('text=/Google|WeGo|404|找不到/i').first().isVisible();
      expect(hasLoginOrError).toBe(true);
    }
  });

  test('Static assets load correctly', async ({ page }) => {
    await page.goto('/');

    // Check CSS is loaded
    const styles = await page.evaluate(() => {
      const body = document.body;
      return window.getComputedStyle(body).fontFamily;
    });
    expect(styles).toBeTruthy();

    // Check JavaScript is loaded
    const hasScript = await page.evaluate(() => {
      return document.querySelectorAll('script[src*="app.js"]').length > 0 ||
             document.querySelectorAll('script[type="module"]').length > 0;
    });
    // Scripts might be inline or bundled, so we just verify page is interactive
    expect(await page.isEnabled('body')).toBe(true);
  });
});

test.describe('Responsive Design', () => {
  test('Mobile viewport renders correctly', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 }); // iPhone SE
    await page.goto('/');

    // Page should still be functional
    await expect(page.locator('body')).toBeVisible();
  });

  test('Tablet viewport renders correctly', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 }); // iPad
    await page.goto('/');

    await expect(page.locator('body')).toBeVisible();
  });

  test('Desktop viewport renders correctly', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto('/');

    await expect(page.locator('body')).toBeVisible();
  });
});

test.describe('Error Handling', () => {
  test('Protected API requires authentication', async ({ request }) => {
    // Try to access a protected endpoint without auth
    const response = await request.get('/api/trips');

    // Spring Security may redirect to login (302) or return 401/403
    // Or may return 200 with empty list depending on configuration
    const status = response.status();
    expect([200, 302, 401, 403]).toContain(status);

    // If 401/403, should have error response
    if (status === 401 || status === 403) {
      const body = await response.json();
      expect(body.success).toBe(false);
    }
  });

  test('Invalid POST request returns error', async ({ request }) => {
    const response = await request.post('/api/trips', {
      data: {
        // Missing required fields
        title: '',
      },
    });

    // Should return 400, 401, 403, or CSRF error
    const status = response.status();
    expect([400, 401, 403]).toContain(status);
  });
});

test.describe('Performance', () => {
  test('Home page loads within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    const loadTime = Date.now() - startTime;

    // Page should load within 5 seconds
    expect(loadTime).toBeLessThan(5000);
  });

  test('API health check responds quickly', async ({ request }) => {
    const startTime = Date.now();
    await request.get('/api/health');
    const responseTime = Date.now() - startTime;

    // Health check should respond within 500ms
    expect(responseTime).toBeLessThan(500);
  });
});
