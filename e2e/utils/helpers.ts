import { Page, expect, BrowserContext } from '@playwright/test';

/**
 * Helper utilities for WeGo E2E Tests
 */

// =====================
// Authentication Helpers
// =====================

/**
 * Login with a mock session (for testing without real OAuth)
 *
 * Note: This requires the backend to support test mode authentication.
 * In production tests, use the real OAuth flow.
 */
export async function loginWithMockSession(page: Page, userId: string): Promise<void> {
  // For testing, we inject a session cookie or use a test endpoint
  // This depends on how the backend implements test authentication
  await page.context().addCookies([
    {
      name: 'JSESSIONID',
      value: `test-session-${userId}`,
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);
}

/**
 * Check if user is logged in by looking for dashboard elements
 */
export async function isLoggedIn(page: Page): Promise<boolean> {
  try {
    await page.waitForSelector('[data-testid="user-menu"]', { timeout: 3000 });
    return true;
  } catch {
    return false;
  }
}

/**
 * Logout the current user
 */
export async function logout(page: Page): Promise<void> {
  // Click user menu, then logout button
  await page.click('[data-testid="user-menu"]');
  await page.click('[data-testid="logout-button"]');
  await page.waitForURL('**/login**');
}

// =====================
// Navigation Helpers
// =====================

/**
 * Navigate to a trip's detail page
 */
export async function goToTrip(page: Page, tripId: string): Promise<void> {
  await page.goto(`/trips/${tripId}`);
  await page.waitForLoadState('networkidle');
}

/**
 * Navigate to dashboard
 */
export async function goToDashboard(page: Page): Promise<void> {
  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');
}

/**
 * Navigate to trip creation page
 */
export async function goToCreateTrip(page: Page): Promise<void> {
  await page.goto('/trips/new');
  await page.waitForLoadState('networkidle');
}

// =====================
// Wait Helpers
// =====================

/**
 * Wait for API call to complete
 */
export async function waitForApiCall(page: Page, urlPattern: string | RegExp): Promise<void> {
  await page.waitForResponse((response) => {
    if (typeof urlPattern === 'string') {
      return response.url().includes(urlPattern);
    }
    return urlPattern.test(response.url());
  });
}

/**
 * Wait for toast notification to appear
 */
export async function waitForToast(page: Page, text?: string): Promise<void> {
  const toastSelector = '[data-testid="toast"]';
  await page.waitForSelector(toastSelector, { state: 'visible' });

  if (text) {
    await expect(page.locator(toastSelector)).toContainText(text);
  }
}

/**
 * Wait for loading indicator to disappear
 */
export async function waitForLoadingComplete(page: Page): Promise<void> {
  const loadingSelector = '[data-testid="loading"], .spinner, .loading';
  try {
    await page.waitForSelector(loadingSelector, { state: 'hidden', timeout: 30000 });
  } catch {
    // Loading indicator might not exist, which is fine
  }
}

// =====================
// Form Helpers
// =====================

/**
 * Fill a form field by label
 */
export async function fillFormFieldByLabel(page: Page, label: string, value: string): Promise<void> {
  const input = page.locator(`label:has-text("${label}") + input, label:has-text("${label}") input`);
  await input.fill(value);
}

/**
 * Select an option from a dropdown
 */
export async function selectOption(page: Page, selector: string, value: string): Promise<void> {
  await page.selectOption(selector, value);
}

/**
 * Fill date input
 */
export async function fillDateInput(page: Page, selector: string, date: string): Promise<void> {
  await page.fill(selector, date);
}

// =====================
// Assertion Helpers
// =====================

/**
 * Assert that an element contains specific text
 */
export async function assertText(page: Page, selector: string, text: string): Promise<void> {
  await expect(page.locator(selector)).toContainText(text);
}

/**
 * Assert that URL contains specific path
 */
export async function assertUrlContains(page: Page, path: string): Promise<void> {
  await expect(page).toHaveURL(new RegExp(path));
}

/**
 * Assert element is visible
 */
export async function assertVisible(page: Page, selector: string): Promise<void> {
  await expect(page.locator(selector)).toBeVisible();
}

/**
 * Assert element is not visible
 */
export async function assertNotVisible(page: Page, selector: string): Promise<void> {
  await expect(page.locator(selector)).not.toBeVisible();
}

// =====================
// Data Helpers
// =====================

/**
 * Get text content of an element
 */
export async function getTextContent(page: Page, selector: string): Promise<string | null> {
  return page.locator(selector).textContent();
}

/**
 * Get count of elements matching selector
 */
export async function getElementCount(page: Page, selector: string): Promise<number> {
  return page.locator(selector).count();
}

/**
 * Extract trip ID from URL
 */
export function extractTripIdFromUrl(url: string): string | null {
  const match = url.match(/\/trips\/(\d+)/);
  return match ? match[1] : null;
}

// =====================
// API Helpers
// =====================

/**
 * Make a direct API call (useful for test setup)
 */
export async function apiCall<T>(
  context: BrowserContext,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  endpoint: string,
  data?: Record<string, unknown>
): Promise<T> {
  const baseUrl = process.env.BASE_URL || 'http://localhost:8080';
  const page = await context.newPage();

  try {
    const response = await page.request[method.toLowerCase() as 'get' | 'post' | 'put' | 'delete'](
      `${baseUrl}${endpoint}`,
      data ? { data } : undefined
    );

    const json = await response.json();
    return json as T;
  } finally {
    await page.close();
  }
}

// =====================
// Screenshot Helpers
// =====================

/**
 * Take a full-page screenshot with timestamp
 */
export async function takeScreenshot(page: Page, name: string): Promise<void> {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  await page.screenshot({
    path: `test-results/screenshots/${name}-${timestamp}.png`,
    fullPage: true,
  });
}

// =====================
// Cleanup Helpers
// =====================

/**
 * Clear all cookies and storage
 */
export async function clearBrowserData(context: BrowserContext): Promise<void> {
  await context.clearCookies();
}
