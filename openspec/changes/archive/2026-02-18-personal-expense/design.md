## Context

WeGo 目前的分帳功能（`Expense` + `ExpenseSplit`）以行程為單位、以群體視角呈現支出：誰付了多少、誰欠誰多少。但使用者缺乏個人視角——「這趟旅遊我自己花了多少」。

個人記帳功能需要整合兩種資料來源：
1. 自動來源：已存在的 `ExpenseSplit` 記錄（使用者的分帳份額）
2. 手動來源：使用者自行新增的個人支出（不參與分帳）

同時需要個人預算追蹤與統計圖表。

## Goals / Non-Goals

**Goals:**
- 在行程分帳頁新增個人記帳 Tab，顯示個人花費總覽、類別圓餅圖、每日長條圖
- 支援手動新增/編輯/刪除個人支出（PersonalExpense entity）
- 自動聚合 ExpenseSplit 份額作為唯讀個人支出記錄
- 個人預算設定（存於 TripMember.personalBudget）與超支警示
- 統計金額一律換算為 trip.baseCurrency 顯示
- 類別擴充至 7 個（新增 SHOPPING、HEALTH），團隊與個人統一

**Non-Goals:**
- 全域個人記帳（跨行程、與行程無關的日常記帳）
- 收據照片上傳（個人支出表單不含 receiptUrl）
- 隱藏或刪除自動同步的 ExpenseSplit 記錄
- 個人支出回寫至團隊分帳
- 分類預算或每日預算（僅支援單一總預算）

## Decisions

### 決策 1：自動同步採 View-based（即時查詢），不複製資料

**選擇：** 個人記帳中的「分帳份額」直接 JOIN `expense_splits` + `expenses` 查詢，不複製成獨立記錄。

**理由：** 零同步問題——團隊分帳金額修改或刪除時，個人視角自動反映最新狀態，不需要 event-driven 同步機制或 reconciliation job。

**捨棄方案：** 建立 `PersonalExpense` 並在分帳建立時寫入複製記錄，需處理更新/刪除同步，複雜度高且易產生資料不一致。

---

### 決策 2：手動個人支出建新 Entity（PersonalExpense），不重用 Expense

**選擇：** 新增 `PersonalExpense` entity，欄位為 `userId, tripId, description, amount, currency, exchangeRate, category, expenseDate, note, createdAt, updatedAt`。

**理由：** `Expense` entity 強制需要 `paidBy`（付款人）、`splitType`、`tripId`（不可 null），語意上是「群體支出」。個人支出無需付款人與分帳，強行重用會造成語意混亂和大量 nullable 欄位。

**捨棄方案：** 修改 `Expense` entity 加入 `isPersonal` flag 並讓 `tripId` 可 null，此方案污染核心 entity 並使現有查詢需要加 filter。

---

### 決策 3：個人預算存於 TripMember.personalBudget

**選擇：** `TripMember` 新增 nullable `personal_budget` BigDecimal 欄位。

**理由：** 個人預算天然是「某個成員在某個行程」的屬性，與 TripMember 一對一，無需獨立表。一個欄位的資訊量不值得開新表。

**捨棄方案：** 新建 `TripPersonalBudget` 表，設計過度，增加 JOIN 複雜度。

---

### 決策 4：統計金額換算為 trip.baseCurrency

**選擇：** 圓餅圖、長條圖、預算進度條的金額計算，皆先將 `amount × exchangeRate`，exchangeRate 為 null 時視為 1.0（同幣種）。

**理由：** 使用者設定的預算以 baseCurrency 計，混合幣種不換算則無法比較。此邏輯與現有 `Expense.getAmountInBaseCurrency()` 一致。

---

### 決策 5：服務層合併兩個資料來源

**選擇：** `PersonalExpenseService` 負責查詢兩個來源並合併成統一的 `PersonalExpenseItemResponse` DTO：

```
PersonalExpenseItemResponse
├── source: AUTO | MANUAL
├── id (MANUAL only, for edit/delete)
├── description
├── amount (in baseCurrency)
├── originalAmount + originalCurrency (for display)
├── category
├── expenseDate
├── paidBy (AUTO only, for inline card)
└── tripExpenseId (AUTO only, for link to team expense)
```

排序：`expenseDate` ASC（從遠到近），expenseDate 為 null 時排最後。

## Risks / Trade-offs

**[Risk] N+1 查詢：合併兩個來源時需要 JOIN 不同 table**
→ Mitigation：`PersonalExpenseService` 使用批次查詢（分別取得所有 splits 和 personalExpenses），在記憶體合併後排序，避免迴圈中查詢。

**[Risk] 匯率過期：手動支出的 exchangeRate 由新增當下的 ExchangeRate-API 填入，非支出日匯率**
→ Mitigation：Phase 1 接受此限制，與現有團隊分帳行為一致。未來可加「依支出日期查歷史匯率」。

**[Risk] 類別擴充（SHOPPING、HEALTH）影響現有統計圖表**
→ Mitigation：統計圖表已是動態渲染，新增類別只需更新 Enum 與前端 icon/color mapping，向後相容。

**[Risk] 個人記帳 Tab 預設不保留（URL 無 state）**
→ 接受此 trade-off：使用者每次進入頁面預設顯示團隊分帳 Tab，個人記帳為次要入口，簡化實作。

## Migration Plan

1. DB Migration：新增 `personal_expenses` 表、`trip_members.personal_budget` 欄位
2. 後端：新增 entity/repository/service/controller，修改現有 ExpenseWebController
3. 前端：修改 `expense/list.html` 加入 Tab 切換，新增個人記帳 fragment 與表單 template
4. 類別更新：Enum 新增兩個值，`expense/create.html` 加入新類別 radio button
5. 無資料遷移需求（現有資料皆可相容新類別）
6. Rollback：migration 可逆（drop column / drop table），前端改動可 feature flag 隔離

## Open Questions

- 行程超過 7 天時長條圖採橫向捲動（已決定），行動端觸控捲動的 UX 需在 `/ui-ux-pro-max` 諮詢時確認最佳實作。
- 預算 prompt「略過後是否再次顯示」：已決定略過後不再主動提示，summary 區保留淡色 [設定預算] 連結入口。
