/**
 * Shared E2E Test Setup Utilities
 *
 * Provides helper functions for creating test data, handling CSRF tokens,
 * and making API calls during E2E tests.
 */

import { Page } from '@playwright/test';
import { generateRandomTrip, TestTrip } from './test-data';
import { authenticateTestUser, AuthenticatedUser } from './auth-helper';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

/**
 * Creates a trip via the web form and returns the trip ID.
 * This is the most reliable method since the web form handles
 * Place creation and other dependencies automatically.
 */
export async function createTestTrip(
  page: Page,
  trip?: Partial<TestTrip>
): Promise<string> {
  const tripData = { ...generateRandomTrip(), ...trip };

  await page.goto('/trips/create');
  await page.fill('input[name="title"], #title', tripData.title);

  const descField = page.locator('input[name="description"], #description, textarea[name="description"]');
  if (await descField.count() > 0 && tripData.description) {
    await descField.fill(tripData.description);
  }

  await page.fill('input[name="startDate"], #startDate', tripData.startDate);
  await page.fill('input[name="endDate"], #endDate', tripData.endDate);

  await page.click('button[type="submit"], button:has-text("建立")');

  // Wait for redirect to trip detail page
  await page.waitForURL(/trips\/[a-f0-9-]+/, { timeout: 15000 });

  const url = page.url();
  const match = url.match(/trips\/([a-f0-9-]+)/);
  if (!match) {
    throw new Error(`Failed to extract tripId from URL: ${url}`);
  }

  return match[1];
}

/**
 * Extracts the CSRF token from the XSRF-TOKEN cookie.
 * Spring Security stores the CSRF token in a cookie named XSRF-TOKEN
 * when using CookieCsrfTokenRepository.
 */
export async function getCSRFToken(page: Page): Promise<string> {
  // First visit a page to get the cookie set
  const cookies = await page.context().cookies();
  const csrfCookie = cookies.find(c => c.name === 'XSRF-TOKEN');

  if (csrfCookie) {
    return csrfCookie.value;
  }

  // If no cookie found, visit a page to trigger cookie creation
  await page.goto('/dashboard');
  await page.waitForLoadState('networkidle');

  const refreshedCookies = await page.context().cookies();
  const refreshedCsrf = refreshedCookies.find(c => c.name === 'XSRF-TOKEN');

  if (!refreshedCsrf) {
    throw new Error('CSRF token cookie (XSRF-TOKEN) not found');
  }

  return refreshedCsrf.value;
}

/**
 * Makes an authenticated POST request with CSRF token.
 */
export async function apiPost<T = unknown>(
  page: Page,
  url: string,
  data: Record<string, unknown>
): Promise<{ status: number; data: T }> {
  const csrfToken = await getCSRFToken(page);

  const response = await page.request.post(`${BASE_URL}${url}`, {
    data,
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': csrfToken,
    },
  });

  const status = response.status();
  let body: T;

  try {
    body = await response.json();
  } catch {
    body = (await response.text()) as unknown as T;
  }

  return { status, data: body };
}

/**
 * Makes an authenticated PUT request with CSRF token.
 */
export async function apiPut<T = unknown>(
  page: Page,
  url: string,
  data: Record<string, unknown>
): Promise<{ status: number; data: T }> {
  const csrfToken = await getCSRFToken(page);

  const response = await page.request.put(`${BASE_URL}${url}`, {
    data,
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': csrfToken,
    },
  });

  const status = response.status();
  let body: T;

  try {
    body = await response.json();
  } catch {
    body = (await response.text()) as unknown as T;
  }

  return { status, data: body };
}

/**
 * Makes an authenticated DELETE request with CSRF token.
 */
export async function apiDelete<T = unknown>(
  page: Page,
  url: string
): Promise<{ status: number; data: T | null }> {
  const csrfToken = await getCSRFToken(page);

  const response = await page.request.delete(`${BASE_URL}${url}`, {
    headers: {
      'X-XSRF-TOKEN': csrfToken,
    },
  });

  const status = response.status();

  if (status === 204) {
    return { status, data: null };
  }

  let body: T | null;
  try {
    body = await response.json();
  } catch {
    body = null;
  }

  return { status, data: body };
}

/**
 * Standard API response wrapper from WeGo backend.
 */
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

/**
 * Creates a todo via the API and returns the created todo.
 */
export async function createTestTodo(
  page: Page,
  tripId: string,
  title: string
): Promise<{ id: string; title: string }> {
  const result = await apiPost<ApiResponse<{ id: string; title: string }>>(
    page,
    `/api/trips/${tripId}/todos`,
    { title }
  );

  if (result.status !== 201 || !result.data.data) {
    throw new Error(`Failed to create todo: status=${result.status}, body=${JSON.stringify(result.data)}`);
  }

  return result.data.data;
}

/**
 * Creates an activity via the web form endpoint (bypasses Place API dependency).
 * The web form automatically creates a Place entity.
 */
export async function createTestActivity(
  page: Page,
  tripId: string,
  opts?: {
    placeName?: string;
    activityDate?: string;
    startTime?: string;
    notes?: string;
  }
): Promise<void> {
  await page.goto(`/trips/${tripId}/activities/new`);

  // Fill place name
  const placeInput = page.locator('input[name="placeName"], #placeName, [data-place-search]').first();
  await placeInput.fill(opts?.placeName ?? '測試地點');

  // Fill date
  const dateInput = page.locator('input[name="activityDate"], #activityDate').first();
  if (await dateInput.count() > 0 && opts?.activityDate) {
    await dateInput.fill(opts.activityDate);
  }

  // Fill start time if provided
  if (opts?.startTime) {
    const timeInput = page.locator('input[name="startTime"], #startTime').first();
    if (await timeInput.count() > 0) {
      await timeInput.fill(opts.startTime);
    }
  }

  // Fill notes if provided
  if (opts?.notes) {
    const notesInput = page.locator('textarea[name="notes"], #notes, input[name="notes"]').first();
    if (await notesInput.count() > 0) {
      await notesInput.fill(opts.notes);
    }
  }

  // Submit form
  await page.click('button[type="submit"], button:has-text("新增"), button:has-text("建立")');

  // Wait for redirect back to activities list
  await page.waitForURL(/trips\/[a-f0-9-]+\/activities/, { timeout: 10000 });
}

/**
 * Authenticates and creates a trip, returning both user info and trip ID.
 * Common setup pattern for tests that need both auth + trip.
 */
export async function setupAuthenticatedTrip(
  page: Page,
  user?: { email?: string; name?: string },
  trip?: Partial<TestTrip>
): Promise<{ user: AuthenticatedUser; tripId: string }> {
  const authUser = await authenticateTestUser(page, user);
  const tripId = await createTestTrip(page, trip);
  return { user: authUser, tripId };
}
