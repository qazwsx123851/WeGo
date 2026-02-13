import { test, expect } from '@playwright/test';
import { authenticateTestUser, isTestAuthAvailable } from '../fixtures/auth-helper';
import { apiPost, createTestTrip } from '../fixtures/test-setup';

/**
 * AI Chat Widget E2E Tests
 *
 * Test ID mapping:
 * - E2E-CHAT-001: Chat button visible on trip page
 * - E2E-CHAT-002: Open/close chat window
 * - E2E-CHAT-003: Send message and receive reply
 * - E2E-CHAT-004: Character counter
 * - E2E-CHAT-005: Empty message cannot be sent
 * - E2E-CHAT-006: Chat API requires authentication
 */

test.describe('AI Chat Widget', () => {
  let tripId: string;

  test.beforeEach(async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
    tripId = await createTestTrip(page);
  });

  test.describe('E2E-CHAT-001: Chat Button Visibility', () => {
    test('chat toggle button is visible on trip detail page', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      const chatBtn = page.locator('#chat-toggle-btn');
      await expect(chatBtn).toBeVisible();
      await expect(chatBtn).toHaveAttribute('aria-label', '開啟 AI 旅遊助手');
    });

    test('chat toggle button is visible on activities page', async ({ page }) => {
      await page.goto(`/trips/${tripId}/activities`);
      await page.waitForLoadState('networkidle');

      const chatBtn = page.locator('#chat-toggle-btn');
      await expect(chatBtn).toBeVisible();
    });

    test('chat toggle button is visible on todos page', async ({ page }) => {
      await page.goto(`/trips/${tripId}/todos`);
      await page.waitForLoadState('networkidle');

      const chatBtn = page.locator('#chat-toggle-btn');
      await expect(chatBtn).toBeVisible();
    });
  });

  test.describe('E2E-CHAT-002: Open/Close Chat Window', () => {
    test('clicking toggle button opens chat window', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      const chatWindow = page.locator('#chat-window');

      // Chat window should start hidden (scale-0)
      await expect(chatWindow).toHaveClass(/scale-0/);

      // Click toggle button to open
      await page.click('#chat-toggle-btn');

      // Chat window should now be visible (scale-100)
      await expect(chatWindow).toHaveClass(/scale-100/);
      await expect(chatWindow).toHaveClass(/opacity-100/);

      // Close icon should be visible, open icon hidden
      await expect(page.locator('#chat-icon-close')).toBeVisible();
      await expect(page.locator('#chat-icon-open')).toBeHidden();
    });

    test('clicking toggle again closes chat window', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      // Open
      await page.click('#chat-toggle-btn');
      await expect(page.locator('#chat-window')).toHaveClass(/scale-100/);

      // Close
      await page.click('#chat-toggle-btn');
      await expect(page.locator('#chat-window')).toHaveClass(/scale-0/);

      // Open icon should be visible again
      await expect(page.locator('#chat-icon-open')).toBeVisible();
      await expect(page.locator('#chat-icon-close')).toBeHidden();
    });

    test('chat window shows welcome message', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      await page.click('#chat-toggle-btn');

      // Welcome message should be visible
      const welcomeMsg = page.locator('#chat-messages').locator('text=WeGo 旅遊助手');
      await expect(welcomeMsg).toBeVisible();
    });

    test('chat input is focused after opening', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      await page.click('#chat-toggle-btn');

      // Input should be focused
      const chatInput = page.locator('#chat-input');
      await expect(chatInput).toBeFocused();
    });
  });

  test.describe('E2E-CHAT-003: Send Message and Receive Reply', () => {
    test('can send a message and receive bot reply', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      // Open chat
      await page.click('#chat-toggle-btn');

      // Type a message
      const chatInput = page.locator('#chat-input');
      await chatInput.fill('推薦一家好吃的餐廳');

      // Send button should be enabled
      const sendBtn = page.locator('#chat-send-btn');
      await expect(sendBtn).toBeEnabled();

      // Click send
      await sendBtn.click();

      // User message should appear
      const userMsg = page.locator('#chat-messages .flex.justify-end').last();
      await expect(userMsg).toContainText('推薦一家好吃的餐廳');

      // Loading dots should appear briefly
      const loadingDots = page.locator('#chat-loading-dots');
      // Dots may disappear quickly with mock, so just check the flow completes

      // Bot reply should eventually appear (mock returns quickly)
      // Wait for a bot reply bubble (the second .flex.gap-2, after welcome message)
      const botReply = page.locator('#chat-messages .flex.gap-2').nth(1);
      await expect(botReply).toBeVisible({ timeout: 40000 });

      // Input should be re-enabled after reply
      await expect(chatInput).toBeEnabled({ timeout: 40000 });
    });

    test('can send message with Enter key', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      await page.click('#chat-toggle-btn');

      const chatInput = page.locator('#chat-input');
      await chatInput.fill('附近有什麼景點');
      await chatInput.press('Enter');

      // User message should appear
      const userMsg = page.locator('#chat-messages .flex.justify-end').last();
      await expect(userMsg).toContainText('附近有什麼景點');

      // Wait for bot reply
      const botReplies = page.locator('#chat-messages .flex.gap-2');
      await expect(botReplies).toHaveCount(2, { timeout: 40000 }); // welcome + reply
    });
  });

  test.describe('E2E-CHAT-004: Character Counter', () => {
    test('character counter updates as user types', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      await page.click('#chat-toggle-btn');

      const charCount = page.locator('#chat-char-count');
      await expect(charCount).toHaveText('0/500');

      // Type some text
      const chatInput = page.locator('#chat-input');
      await chatInput.fill('Hello');

      await expect(charCount).toHaveText('5/500');
    });

    test('character counter turns red near limit', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      await page.click('#chat-toggle-btn');

      const chatInput = page.locator('#chat-input');
      const charCount = page.locator('#chat-char-count');

      // Type 450+ chars (90% of 500)
      const longText = 'a'.repeat(460);
      await chatInput.fill(longText);

      await expect(charCount).toHaveText('460/500');
      await expect(charCount).toHaveClass(/text-red-500/);
    });
  });

  test.describe('E2E-CHAT-005: Empty Message Validation', () => {
    test('send button is disabled when input is empty', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      await page.click('#chat-toggle-btn');

      const sendBtn = page.locator('#chat-send-btn');
      await expect(sendBtn).toBeDisabled();
    });

    test('send button enables when text is entered', async ({ page }) => {
      await page.goto(`/trips/${tripId}`);
      await page.waitForLoadState('networkidle');

      await page.click('#chat-toggle-btn');

      const sendBtn = page.locator('#chat-send-btn');
      const chatInput = page.locator('#chat-input');

      await expect(sendBtn).toBeDisabled();

      await chatInput.fill('test');
      await expect(sendBtn).toBeEnabled();

      // Clear input
      await chatInput.fill('');
      await expect(sendBtn).toBeDisabled();
    });
  });
});

test.describe('Chat API Integration', () => {
  test('POST /api/trips/{id}/chat requires authentication', async ({ request }) => {
    const response = await request.post('/api/trips/00000000-0000-0000-0000-000000000001/chat', {
      data: { message: 'Hello' },
      headers: { 'Content-Type': 'application/json' },
    });

    // Should return 401/403 (unauthenticated)
    expect([401, 403]).toContain(response.status());
  });

  test('POST /api/trips/{id}/chat validates message', async ({ page }) => {
    const isAvailable = await isTestAuthAvailable(page);
    if (!isAvailable) {
      test.skip(true, 'Test auth endpoint not available');
    }
    await authenticateTestUser(page);
    const tripId = await createTestTrip(page);

    // Send empty message
    const result = await apiPost(page, `/api/trips/${tripId}/chat`, { message: '' });
    expect(result.status).toBe(400);
  });
});
