/**
 * Authentication Helper for WeGo E2E Tests
 *
 * Provides utilities for authenticating in E2E tests using the
 * test authentication endpoint (only available in test/e2e profile).
 */

import { Page, BrowserContext, APIRequestContext } from '@playwright/test';

export interface AuthenticatedUser {
  userId: string;
  email: string;
  name: string;
  sessionId: string;
}

/**
 * Default test user credentials
 */
export const DEFAULT_TEST_USER = {
  email: 'e2e-test@wego.test',
  name: 'E2E Test User',
};

/**
 * Authenticates a test user using the test auth endpoint.
 *
 * This method should be called at the beginning of tests that require
 * authentication. It creates/uses a test user and establishes a session.
 *
 * @param page Playwright page instance
 * @param user Optional user info (email, name)
 * @returns Authenticated user info including userId and sessionId
 */
export async function authenticateTestUser(
  page: Page,
  user?: { email?: string; name?: string }
): Promise<AuthenticatedUser> {
  const baseURL = process.env.BASE_URL || 'http://localhost:8080';
  const email = user?.email || DEFAULT_TEST_USER.email;
  const name = user?.name || DEFAULT_TEST_USER.name;

  // Call test auth endpoint
  const response = await page.request.post(`${baseURL}/api/test/auth/login`, {
    data: { email, name },
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Failed to authenticate test user: ${response.status()} ${text}`);
  }

  const result = await response.json();

  if (!result.success) {
    throw new Error(`Authentication failed: ${JSON.stringify(result)}`);
  }

  return {
    userId: result.userId,
    email: result.email,
    name: result.name,
    sessionId: result.sessionId,
  };
}

/**
 * Authenticates using the API request context directly.
 *
 * Useful for API-level testing without a page context.
 *
 * @param request Playwright API request context
 * @param user Optional user info
 * @returns Authenticated user info
 */
export async function authenticateViaApi(
  request: APIRequestContext,
  user?: { email?: string; name?: string }
): Promise<AuthenticatedUser> {
  const baseURL = process.env.BASE_URL || 'http://localhost:8080';
  const email = user?.email || DEFAULT_TEST_USER.email;
  const name = user?.name || DEFAULT_TEST_USER.name;

  const response = await request.post(`${baseURL}/api/test/auth/login`, {
    data: { email, name },
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok()) {
    const text = await response.text();
    throw new Error(`Failed to authenticate test user: ${response.status()} ${text}`);
  }

  const result = await response.json();

  if (!result.success) {
    throw new Error(`Authentication failed: ${JSON.stringify(result)}`);
  }

  return {
    userId: result.userId,
    email: result.email,
    name: result.name,
    sessionId: result.sessionId,
  };
}

/**
 * Logs out the current test session.
 *
 * @param page Playwright page instance
 */
export async function logoutTestUser(page: Page): Promise<void> {
  const baseURL = process.env.BASE_URL || 'http://localhost:8080';

  await page.request.post(`${baseURL}/api/test/auth/logout`, {
    headers: {
      'Content-Type': 'application/json',
    },
  });
}

/**
 * Checks if the test auth endpoint is available.
 *
 * The endpoint is only available when the application is running
 * with the "test" or "e2e" profile.
 *
 * @param page Playwright page instance
 * @returns true if test auth is available
 */
export async function isTestAuthAvailable(page: Page): Promise<boolean> {
  const baseURL = process.env.BASE_URL || 'http://localhost:8080';

  try {
    const response = await page.request.get(`${baseURL}/api/test/auth/health`);
    if (response.ok()) {
      const result = await response.json();
      return result.status === 'ok';
    }
    return false;
  } catch {
    return false;
  }
}

/**
 * Creates a pre-authenticated browser context for tests.
 *
 * This is useful for creating isolated test contexts with
 * different users.
 *
 * @param context Playwright browser context
 * @param user Optional user info
 * @returns Authenticated user info
 */
export async function authenticateContext(
  context: BrowserContext,
  user?: { email?: string; name?: string }
): Promise<AuthenticatedUser> {
  const page = await context.newPage();

  try {
    const authUser = await authenticateTestUser(page, user);
    return authUser;
  } finally {
    // Keep page open to maintain session
    // Test should close it when done
  }
}

/**
 * Creates test users for multi-user scenarios.
 *
 * @param page Playwright page instance
 * @param count Number of test users to create
 * @returns Array of authenticated users
 */
export async function createTestUsers(
  page: Page,
  count: number
): Promise<AuthenticatedUser[]> {
  const users: AuthenticatedUser[] = [];

  for (let i = 1; i <= count; i++) {
    const user = await authenticateTestUser(page, {
      email: `e2e-test-${i}@wego.test`,
      name: `E2E Test User ${i}`,
    });
    users.push(user);
  }

  return users;
}
