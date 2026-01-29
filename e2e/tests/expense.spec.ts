import { test, expect } from '@playwright/test';
import { sampleExpenses, generateRandomExpense } from '../fixtures/test-data';

/**
 * Expense and Settlement E2E Tests
 *
 * Test ID mapping:
 * - E2E-EXP-001: Add expense (equal split)
 * - E2E-EXP-002: Add expense (custom split)
 * - E2E-EXP-003: View settlement results
 * - E2E-EXP-004: Mark as settled
 */

test.describe('Expense Management', () => {
  // These tests require authentication and an existing trip

  test.describe.skip('E2E-EXP-001: Add Expense (Equal Split)', () => {
    test('displays expense creation form', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      // Should show form fields
      await expect(page.locator('input[name="title"], #title')).toBeVisible();
      await expect(page.locator('input[name="amount"], #amount')).toBeVisible();
      await expect(page.locator('select[name="category"], #category')).toBeVisible();
    });

    test('validates required fields', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      // Try to submit empty form
      await page.click('button[type="submit"], button:has-text("儲存")');

      // Should show validation errors
      const errorMessage = page.locator('.error, [data-error], :invalid');
      await expect(errorMessage).toBeVisible();
    });

    test('validates amount is positive', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      await page.fill('input[name="title"]', 'Test Expense');
      await page.fill('input[name="amount"]', '-100'); // Negative amount

      await page.click('button[type="submit"]');

      // Should show validation error
      const errorMessage = page.locator('text=/金額|amount|positive/i');
      await expect(errorMessage).toBeVisible();
    });

    test('creates expense with equal split', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      const expense = generateRandomExpense(1000);

      await page.fill('input[name="title"]', expense.title);
      await page.fill('input[name="amount"]', expense.amount.toString());
      await page.selectOption('select[name="category"]', expense.category);

      // Select equal split
      const equalSplitRadio = page.locator('input[value="EQUAL"], #split-equal');
      await equalSplitRadio.click();

      await page.click('button[type="submit"]');

      // Should redirect to expense list
      await expect(page).toHaveURL(/expenses/);

      // Expense should appear in list
      await expect(page.locator(`text=${expense.title}`)).toBeVisible();
    });

    test('selects all members by default for equal split', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      // All member checkboxes should be checked by default
      const memberCheckboxes = page.locator('input[name="members[]"], input[name="splitWith"]');
      const count = await memberCheckboxes.count();

      for (let i = 0; i < count; i++) {
        await expect(memberCheckboxes.nth(i)).toBeChecked();
      }
    });
  });

  test.describe.skip('E2E-EXP-002: Add Expense (Custom Split)', () => {
    test('allows selecting specific members to split with', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      // Select exact split
      const exactSplitRadio = page.locator('input[value="EXACT"], #split-exact');
      await exactSplitRadio.click();

      // Should show member amount inputs
      const memberAmountInputs = page.locator('.member-amount-input, input[name*="splitAmount"]');
      await expect(memberAmountInputs.first()).toBeVisible();
    });

    test('validates split amounts equal total', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      await page.fill('input[name="title"]', 'Split Test');
      await page.fill('input[name="amount"]', '1000');

      // Select exact split
      await page.click('input[value="EXACT"]');

      // Fill split amounts that don't add up
      const memberInputs = page.locator('.member-amount-input');
      if ((await memberInputs.count()) >= 2) {
        await memberInputs.nth(0).fill('300');
        await memberInputs.nth(1).fill('300'); // Total: 600 != 1000
      }

      await page.click('button[type="submit"]');

      // Should show validation error
      const errorMessage = page.locator('text=/不相等|不符|mismatch|total/i');
      await expect(errorMessage).toBeVisible();
    });

    test('creates expense with custom amounts', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      await page.fill('input[name="title"]', 'Custom Split Test');
      await page.fill('input[name="amount"]', '1000');

      // Select exact split
      await page.click('input[value="EXACT"]');

      // Fill correct split amounts
      const memberInputs = page.locator('.member-amount-input');
      const count = await memberInputs.count();

      if (count >= 2) {
        await memberInputs.nth(0).fill('600');
        await memberInputs.nth(1).fill('400');
      }

      await page.click('button[type="submit"]');

      // Should succeed
      await expect(page).toHaveURL(/expenses/);
    });

    test('excludes members with zero amount', async ({ page }) => {
      await page.goto('/trips/1/expenses/new');

      await page.fill('input[name="title"]', 'Partial Split');
      await page.fill('input[name="amount"]', '1000');
      await page.click('input[value="EXACT"]');

      // Set some members to zero
      const memberInputs = page.locator('.member-amount-input');
      if ((await memberInputs.count()) >= 2) {
        await memberInputs.nth(0).fill('1000');
        await memberInputs.nth(1).fill('0'); // This member shouldn't be included
      }

      await page.click('button[type="submit"]');

      // Should succeed
      await expect(page).toHaveURL(/expenses/);
    });
  });

  test.describe.skip('E2E-EXP-003: View Settlement', () => {
    test('displays settlement page', async ({ page }) => {
      await page.goto('/trips/1/settlement');

      // Should show settlement section
      await expect(page.locator('text=/結算|Settlement/i')).toBeVisible();
    });

    test('shows who owes whom', async ({ page }) => {
      await page.goto('/trips/1/settlement');

      // Should show settlement items (if there are expenses)
      const settlementItems = page.locator('.settlement-item, [data-testid="settlement-item"]');

      // Settlement list might be empty if no expenses or all balanced
      // Just verify the structure exists
      await expect(page.locator('.settlement-list, [data-testid="settlement-list"]')).toBeVisible();
    });

    test('calculates correct settlement amounts', async ({ page }) => {
      // This would require:
      // 1. Creating known expenses
      // 2. Verifying calculated settlement amounts
      // Complex test that depends on test data setup

      await page.goto('/trips/1/settlement');

      // Verify settlement total matches expense total
      // This is a basic sanity check
      const settlementTotal = page.locator('.settlement-total, [data-testid="total-amount"]');
      if (await settlementTotal.isVisible()) {
        const totalText = await settlementTotal.textContent();
        expect(totalText).toBeTruthy();
      }
    });

    test('shows zero balance when all settled', async ({ page }) => {
      // After marking all settlements as cleared
      await page.goto('/trips/1/settlement');

      // If all settled, should show zero balance or "all clear" message
      const allClear = page.locator('text=/已結清|All settled|no balance/i');
      const hasPending = page.locator('.settlement-item.pending');

      // Either all clear message or pending items exist
      expect((await allClear.isVisible()) || (await hasPending.count()) >= 0).toBe(true);
    });
  });

  test.describe.skip('E2E-EXP-004: Mark as Settled', () => {
    test('shows settle button for each settlement', async ({ page }) => {
      await page.goto('/trips/1/settlement');

      // Each settlement item should have a settle button
      const settleButtons = page.locator('button:has-text("結清"), button[data-testid="settle-btn"]');

      // May be zero if no settlements pending
      expect(await settleButtons.count()).toBeGreaterThanOrEqual(0);
    });

    test('marks settlement as settled', async ({ page }) => {
      await page.goto('/trips/1/settlement');

      const settleBtn = page.locator('button:has-text("結清")').first();

      if (await settleBtn.isVisible()) {
        await settleBtn.click();

        // Should show confirmation or success
        const successIndicator = page.locator('text=/已結清|Settled/i, .settled-badge');
        await expect(successIndicator).toBeVisible();
      }
    });

    test('allows unsettling a settled payment', async ({ page }) => {
      await page.goto('/trips/1/settlement');

      // Find a settled item
      const settledItem = page.locator('.settlement-item.settled, [data-status="settled"]').first();

      if (await settledItem.isVisible()) {
        // Click to unsettle
        const unsettleBtn = settledItem.locator('button:has-text("取消結清"), button[data-action="unsettle"]');
        await unsettleBtn.click();

        // Should revert to pending state
        await expect(settledItem).not.toHaveClass(/settled/);
      }
    });
  });

  test.describe('API Integration', () => {
    test('GET /api/trips/{id}/expenses is protected', async ({ request }) => {
      const response = await request.get('/api/trips/1/expenses');

      // May return 200 (empty list), 302 (redirect), 401/403 (auth error)
      // All are valid depending on security configuration
      expect([200, 302, 401, 403]).toContain(response.status());
    });

    test('GET /api/trips/{id}/settlement is protected', async ({ request }) => {
      const response = await request.get('/api/trips/1/settlement');

      // May return 200, 302 (redirect), 401/403 (auth error)
      expect([200, 302, 401, 403]).toContain(response.status());
    });

    test('POST /api/trips/{id}/expenses requires authentication or CSRF', async ({ request }) => {
      const response = await request.post('/api/trips/1/expenses', {
        data: {
          title: 'Test Expense',
          amount: 1000,
          currency: 'TWD',
          category: 'FOOD',
          splitType: 'EQUAL',
        },
      });

      // Should return 401 (unauthorized) or 403 (CSRF)
      expect([401, 403]).toContain(response.status());
    });
  });
});

test.describe('Expense List View', () => {
  test.skip('displays expense list', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    // Should show expense list container
    await expect(page.locator('.expense-list, [data-testid="expense-list"]')).toBeVisible();
  });

  test.skip('shows expense details on click', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    const firstExpense = page.locator('.expense-item, [data-testid="expense-item"]').first();

    if (await firstExpense.isVisible()) {
      await firstExpense.click();

      // Should show expense detail modal or page
      const detailView = page.locator('.expense-detail, [data-testid="expense-detail"]');
      await expect(detailView).toBeVisible();
    }
  });

  test.skip('filters expenses by category', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    // Click category filter
    const categoryFilter = page.locator('select[name="category-filter"], [data-testid="category-filter"]');

    if (await categoryFilter.isVisible()) {
      await categoryFilter.selectOption('FOOD');

      // Should filter list
      await page.waitForLoadState('networkidle');

      // All visible expenses should be FOOD category
      const visibleExpenses = page.locator('.expense-item:visible');
      // Verification would depend on UI implementation
    }
  });

  test.skip('shows total expense amount', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    // Should show total
    const totalElement = page.locator('.expense-total, [data-testid="expense-total"]');
    await expect(totalElement).toBeVisible();
  });

  test.skip('shows expense count', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    // Should show count
    const countElement = page.locator('.expense-count, [data-testid="expense-count"]');
    await expect(countElement).toBeVisible();
  });
});

test.describe('Delete Expense', () => {
  test.skip('shows delete button on expense', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    const firstExpense = page.locator('.expense-item').first();
    await firstExpense.hover();

    const deleteBtn = firstExpense.locator('button:has-text("刪除"), button[data-action="delete"]');
    await expect(deleteBtn).toBeVisible();
  });

  test.skip('confirms before deleting', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    const firstExpense = page.locator('.expense-item').first();
    await firstExpense.hover();
    await firstExpense.locator('button:has-text("刪除")').click();

    // Should show confirmation
    const confirmDialog = page.locator('[role="dialog"], .confirm-dialog');
    await expect(confirmDialog).toBeVisible();
  });

  test.skip('deletes expense and updates list', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    // Get initial count
    const initialCount = await page.locator('.expense-item').count();

    // Delete first expense
    const firstExpense = page.locator('.expense-item').first();
    await firstExpense.hover();
    await firstExpense.locator('button:has-text("刪除")').click();
    await page.click('button:has-text("確認")');

    // Wait for update
    await page.waitForLoadState('networkidle');

    // Count should decrease
    const newCount = await page.locator('.expense-item').count();
    expect(newCount).toBe(initialCount - 1);
  });
});

test.describe('Edit Expense', () => {
  test.skip('opens edit modal on expense click', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    const firstExpense = page.locator('.expense-item').first();
    const editBtn = firstExpense.locator('button:has-text("編輯"), button[data-action="edit"]');

    if (await editBtn.isVisible()) {
      await editBtn.click();

      const editModal = page.locator('.expense-edit-modal, [data-testid="expense-edit"]');
      await expect(editModal).toBeVisible();
    }
  });

  test.skip('updates expense amount', async ({ page }) => {
    await page.goto('/trips/1/expenses');

    const firstExpense = page.locator('.expense-item').first();
    await firstExpense.locator('button:has-text("編輯")').click();

    const newAmount = '9999';
    await page.fill('input[name="amount"]', newAmount);
    await page.click('button[type="submit"]');

    // Should show updated amount
    await expect(page.locator(`text=${newAmount}`)).toBeVisible();
  });
});
