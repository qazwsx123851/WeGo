## 1. DB Migration & Entity 層

- [x] 1.1 建立 `personal_expenses` 資料表 migration（欄位：id, user_id, trip_id, description, amount, currency, exchange_rate, category, expense_date, note, created_at, updated_at；索引：user_id, trip_id, user_id+trip_id）
- [x] 1.2 在 `trip_members` 表新增 `personal_budget` 欄位（nullable BigDecimal）的 migration
- [x] 1.3 建立 `PersonalExpense` entity（@Entity、@Table、Lombok、欄位同 migration）
- [x] 1.4 在 `TripMember` entity 新增 `personalBudget` 欄位（nullable BigDecimal）
- [x] 1.5 在費用類別 Enum（或常數）新增 `SHOPPING`（購物）與 `HEALTH`（健康）

## 2. Repository 層

- [x] 2.1 建立 `PersonalExpenseRepository`（JPA，含 `findByUserIdAndTripId`、`deleteByIdAndUserId`）
- [x] 2.2 在 `ExpenseSplitRepository` 新增查詢：`findByUserIdAndTripIdWithExpense`（JOIN Expense，回傳 split amount、description、category、expenseDate、paidBy、expenseId）
- [x] 2.3 在 `TripMemberRepository` 新增 `findByTripIdAndUserId` 更新預算的方法

## 3. Service 層

- [x] 3.1 建立 `PersonalExpenseService`，實作：`getPersonalExpenses(userId, tripId)`（合併兩個來源、依 expenseDate ASC 排序）
- [x] 3.2 `PersonalExpenseService`：實作 `getPersonalSummary(userId, tripId)`（總花費換算 baseCurrency、日均、類別分布、每日金額 map）
- [x] 3.3 `PersonalExpenseService`：實作 `createPersonalExpense(userId, tripId, request)`（自動查匯率邏輯）
- [x] 3.4 `PersonalExpenseService`：實作 `updatePersonalExpense(id, userId, request)`（驗證 userId 所有權）
- [x] 3.5 `PersonalExpenseService`：實作 `deletePersonalExpense(id, userId)`（驗證 userId 所有權）
- [x] 3.6 在 `TripMemberService` 新增 `setPersonalBudget(tripId, userId, budget)` 方法
- [x] 3.7 建立 `PersonalExpenseItemResponse` DTO（含 source: AUTO|MANUAL、id、description、amount、originalAmount、originalCurrency、category、expenseDate、paidBy、tripExpenseId）
- [x] 3.8 建立 `PersonalExpenseSummaryResponse` DTO（totalAmount、dailyAverage、categoryBreakdown、dailyAmounts、budget、budgetStatus）

## 4. Service 層測試

- [x] 4.1 `PersonalExpenseServiceTest`：測試 getPersonalExpenses 合併排序邏輯（AUTO 先於 MANUAL 在同一日期時的排序）
- [x] 4.2 `PersonalExpenseServiceTest`：測試 getPersonalSummary 多幣種換算（exchangeRate null → 1:1）
- [x] 4.3 `PersonalExpenseServiceTest`：測試 createPersonalExpense 所有權設定
- [x] 4.4 `PersonalExpenseServiceTest`：測試 updatePersonalExpense 403 場景（他人 userId）
- [x] 4.5 `PersonalExpenseServiceTest`：測試 deletePersonalExpense 403 場景（他人 userId）
- [x] 4.6 `PersonalExpenseServiceTest`：測試預算狀態計算（green/yellow/red 三個閾值）

## 5. API Controller 層

- [x] 5.1 建立 `PersonalExpenseApiController`（`@RestController`，`/api/trips/{tripId}/personal-expenses`）
- [x] 5.2 實作 `GET /api/trips/{tripId}/personal-expenses`（回傳合併列表，驗證 userId）
- [x] 5.3 實作 `POST /api/trips/{tripId}/personal-expenses`（建立手動支出）
- [x] 5.4 實作 `PUT /api/trips/{tripId}/personal-expenses/{id}`（編輯手動支出，驗證所有權）
- [x] 5.5 實作 `DELETE /api/trips/{tripId}/personal-expenses/{id}`（刪除，驗證所有權）
- [x] 5.6 實作 `GET /api/trips/{tripId}/personal-expenses/summary`（回傳統計摘要）
- [x] 5.7 實作 `PUT /api/trips/{tripId}/personal-budget`（設定/更新個人預算）

## 6. API Controller 測試

- [x] 6.1 `PersonalExpenseApiControllerTest`：GET 列表（200 含資料）
- [x] 6.2 `PersonalExpenseApiControllerTest`：POST 建立（201 成功）
- [x] 6.3 `PersonalExpenseApiControllerTest`：PUT 他人資源（403）
- [x] 6.4 `PersonalExpenseApiControllerTest`：DELETE 他人資源（403）
- [x] 6.5 `PersonalExpenseApiControllerTest`：PUT 預算設定（200）

## 7. Web Controller 修改

- [x] 7.1 修改 `ExpenseWebController`（`/trips/{id}/expenses` GET）：加入個人記帳所需 model 屬性（personalSummary、hasBudget）
- [x] 7.2 建立 `PersonalExpenseWebController`（處理個人支出新增/編輯表單頁面的 GET/POST）
- [x] 7.3 `ExpenseWebControllerTest`：補充個人記帳 model 屬性的測試案例

## 8. 前端模板（Tab UI + 個人記帳內容）

> ⚠️ 開始前須諮詢 `/ui-ux-pro-max` skill，確認 Tab 元件、進度條、圖表嵌入的樣式方案

- [x] 8.1 修改 `expense/list.html`：新增 [團隊分帳] | [個人記帳] Tab 切換 UI（JavaScript client-side toggle）
- [x] 8.2 建立 `expense/personal-tab.html` fragment（含：總覽卡片、預算進度條、圖表切換按鈕、費用列表）
- [x] 8.3 實作預算提示卡邏輯（首次無預算時顯示，略過後隱藏並保留淡色連結）
- [x] 8.4 實作預算進度條三態（綠/黃/紅，依 budgetStatus 切換 CSS class）
- [x] 8.5 實作類別圓餅圖（Chart.js 或 ApexCharts，依現有統計頁使用的圖表庫）
- [x] 8.6 實作每日花費長條圖（橫向捲動容器，點擊篩選 + 再點取消）
- [x] 8.7 實作費用列表：🔄 點擊展開 inline 小卡片（顯示原始金額、付款人、分帳連結）
- [x] 8.8 實作費用列表：✏️ 右側 [✎][🗑] icon，[🗑] 需確認後呼叫 DELETE API

## 9. 前端模板（新增/編輯表單）

- [x] 9.1 建立 `expense/personal-create.html`（新增個人支出表單：描述、金額+幣種、7 個類別、日期、備註）
- [x] 9.2 建立 `expense/personal-edit.html`（編輯個人支出表單，預填現有資料）
- [x] 9.3 修改 `expense/create.html`（團隊版表單）：新增 SHOPPING（購物）、HEALTH（健康）類別 radio button

## 10. E2E 測試

- [x] 10.1 新增 `e2e/personal-expense.spec.ts`：Tab 切換顯示個人記帳內容
- [x] 10.2 E2E：新增手動個人支出、驗證列表出現 ✏️ 標記項目
- [x] 10.3 E2E：設定個人預算、驗證進度條出現
- [x] 10.4 E2E：點擊 🔄 自動同步項目展開 inline 卡片
- [x] 10.5 E2E：刪除手動支出、驗證列表移除
- [x] 10.6 E2E：點擊長條圖篩選當日明細、再點取消篩選
