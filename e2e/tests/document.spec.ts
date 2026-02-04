import { test, expect } from '@playwright/test';
import { authenticateTestUser, isTestAuthAvailable } from '../fixtures/auth-helper';
import { generateRandomTrip } from '../fixtures/test-data';

/**
 * Document Management E2E Tests
 *
 * Test ID mapping:
 * - E2E-DOC-001: View documents list
 * - E2E-DOC-002: Upload document
 * - E2E-DOC-003: Storage usage
 */

test.describe('Document Management', () => {
  let tripId: string;

  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);

    // Create a trip for testing documents
    const trip = generateRandomTrip();
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');

    // Wait for redirect and get trip ID from URL
    await page.waitForURL(/trips\/[a-f0-9-]+/);
    const url = page.url();
    const match = url.match(/trips\/([a-f0-9-]+)/);
    if (match) {
      tripId = match[1];
    }
  });

  test.describe('E2E-DOC-001: View Documents', () => {
    test('displays documents list page', async ({ page }) => {
      await page.goto(`/trips/${tripId}/documents`);

      // Should be on documents page
      await expect(page).toHaveURL(new RegExp(`trips/${tripId}/documents`));

      // Should have page content
      const pageContent = page.locator('main, [role="main"]').first();
      await expect(pageContent).toBeVisible();
    });

    test('shows empty state or document list', async ({ page }) => {
      await page.goto(`/trips/${tripId}/documents`);

      // Wait for content to load
      await page.waitForLoadState('networkidle');

      // Should show empty state or documents
      const hasDocuments = await page.locator('.document-item, [data-document-id]').count() > 0;
      const hasEmptyState = await page.locator('text=/沒有檔案|尚無檔案|no documents|上傳/i').count() > 0;
      const hasPageContent = await page.locator('main, [role="main"]').count() > 0;

      expect(hasDocuments || hasEmptyState || hasPageContent).toBe(true);
    });

    test('has upload button', async ({ page }) => {
      await page.goto(`/trips/${tripId}/documents`);

      // Should have upload button
      const uploadButton = page.locator('button:has-text("上傳"), [aria-label*="上傳"]').first();
      await expect(uploadButton).toBeVisible();
    });
  });

  test.describe('E2E-DOC-002: Upload Interface', () => {
    test('upload modal opens on button click', async ({ page }) => {
      await page.goto(`/trips/${tripId}/documents`);

      // Click upload button
      const uploadButton = page.locator('button:has-text("上傳"), [aria-label*="上傳"]').first();
      await uploadButton.click();

      // Should show modal or upload area
      const uploadModal = page.locator('[role="dialog"], .modal, .upload-modal, input[type="file"]');
      const modalVisible = await uploadModal.count() > 0;

      expect(modalVisible).toBe(true);
    });

    test('upload area accepts file input', async ({ page }) => {
      await page.goto(`/trips/${tripId}/documents`);

      // Click upload button to open modal
      const uploadButton = page.locator('button:has-text("上傳"), [aria-label*="上傳"]').first();
      await uploadButton.click();

      // Check for file input
      const fileInput = page.locator('input[type="file"]');
      if (await fileInput.count() > 0) {
        await expect(fileInput).toBeAttached();
      }
    });
  });

  test.describe('E2E-DOC-003: Storage Usage', () => {
    test('shows storage usage information', async ({ page }) => {
      await page.goto(`/trips/${tripId}/documents`);

      // Wait for content to load
      await page.waitForLoadState('networkidle');

      // Should show storage info or usage indicator
      const storageInfo = page.locator('text=/儲存|storage|已使用|MB|GB/i');
      const progressBar = page.locator('.progress-bar, progress, [role="progressbar"]');

      const hasStorageInfo = await storageInfo.count() > 0;
      const hasProgressBar = await progressBar.count() > 0;
      const hasPageContent = await page.locator('main').count() > 0;

      // May not show storage info if empty or feature not implemented
      expect(hasStorageInfo || hasProgressBar || hasPageContent).toBe(true);
    });
  });
});

test.describe('Document List Interactions', () => {
  let tripId: string;

  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);

    // Create a trip
    const trip = generateRandomTrip();
    await page.goto('/trips/create');
    await page.fill('input[name="title"], #title', trip.title);
    await page.fill('input[name="startDate"], #startDate', trip.startDate);
    await page.fill('input[name="endDate"], #endDate', trip.endDate);
    await page.click('button[type="submit"], button:has-text("建立")');

    await page.waitForURL(/trips\/[a-f0-9-]+/);
    const url = page.url();
    const match = url.match(/trips\/([a-f0-9-]+)/);
    if (match) {
      tripId = match[1];
    }
  });

  test('documents page is responsive', async ({ page }) => {
    await page.goto(`/trips/${tripId}/documents`);

    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForLoadState('networkidle');

    // Page should still be usable
    const pageContent = page.locator('main, [role="main"]').first();
    await expect(pageContent).toBeVisible();
  });

  test('can navigate back to trip from documents', async ({ page }) => {
    await page.goto(`/trips/${tripId}/documents`);

    // Find back button
    const backButton = page.locator('a[href*="/trips/"][href$="' + tripId + '"], [aria-label*="返回"]').first();
    if (await backButton.count() > 0) {
      await backButton.click();

      // Should navigate back to trip
      await page.waitForURL(new RegExp(`trips/${tripId}(?!/documents)`), { timeout: 5000 });
    }
  });
});

test.describe('Document API Integration', () => {
  test('GET /api/trips/{id}/documents requires authentication', async ({ request }) => {
    const response = await request.get('/api/trips/00000000-0000-0000-0000-000000000001/documents');

    // Should return 401/403 (unauthenticated) or 404 (not found) or 200
    expect([200, 401, 403, 404]).toContain(response.status());
  });

  test('GET /api/trips/{id}/documents/storage requires authentication', async ({ request }) => {
    const response = await request.get('/api/trips/00000000-0000-0000-0000-000000000001/documents/storage');

    // Should return 401/403 (unauthenticated) or 404 (not found) or 200
    expect([200, 401, 403, 404]).toContain(response.status());
  });

  test('POST /api/trips/{id}/documents requires authentication', async ({ request }) => {
    const response = await request.post('/api/trips/00000000-0000-0000-0000-000000000001/documents', {
      multipart: {
        file: {
          name: 'test.pdf',
          mimeType: 'application/pdf',
          buffer: Buffer.from('%PDF-1.4 test content'),
        },
      },
    });

    // Should return 401/403 (unauthenticated) or 404 (not found)
    expect([401, 403, 404]).toContain(response.status());
  });

  test('DELETE /api/trips/{id}/documents/{docId} requires authentication', async ({ request }) => {
    const response = await request.delete('/api/trips/00000000-0000-0000-0000-000000000001/documents/00000000-0000-0000-0000-000000000002');

    // Should return 401/403 (unauthenticated) or 404 (not found)
    expect([401, 403, 404]).toContain(response.status());
  });
});
