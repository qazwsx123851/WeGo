## Why

目前 WeGo 的分帳功能僅支援團隊分帳（多人均攤），使用者無法追蹤自己在行程中的個人實際花費。旅遊者需要同時掌握「我欠誰多少錢」與「我這趟旅遊花了多少」兩種不同視角，個人記帳功能補足這個缺口，讓每位成員都能即時掌握自己的消費全貌。

## What Changes

- 在 `/trips/{id}/expenses` 頁新增 **[個人記帳] Tab**，與現有 [團隊分帳] Tab 並列
- 新增 `PersonalExpense` entity，供使用者手動記錄個人支出
- `TripMember` 新增 `personalBudget` 欄位，支援個人行程預算設定
- 自動聚合使用者在該行程的 `ExpenseSplit` 份額作為個人支出的唯讀來源
- 個人記帳統計：嵌入類別圓餅圖 + 每日花費長條圖（可鑽取當日明細）
- **BREAKING**: 費用類別新增 `SHOPPING`（購物）與 `HEALTH`（健康），共 7 個類別，團隊分帳與個人記帳統一使用

## Capabilities

### New Capabilities

- `personal-expense`: 個人記帳核心功能——手動新增/編輯/刪除個人支出、自動聚合分帳份額、統計圖表（類別分布 + 每日趨勢）、個人預算設定與超支警示

### Modified Capabilities

- 無現有 spec 需要修改（分帳功能尚未有 spec 文件）

## Impact

**Entity / DB**
- 新增 `personal_expenses` 資料表（PersonalExpense entity）
- `trip_members` 新增 `personal_budget` 欄位（nullable BigDecimal）
- 費用類別 Enum 新增 `SHOPPING`、`HEALTH`

**Backend**
- 新增 `PersonalExpenseService`（合併 ExpenseSplit 查詢 + PersonalExpense CRUD）
- 新增 `PersonalExpenseApiController`（REST CRUD，userId 驗證）
- 修改 `ExpenseWebController`（加入 Tab 切換邏輯與個人記帳 model data）
- 修改 `TripMemberService`（預算欄位 CRUD）

**Frontend**
- 修改 `expense/list.html`（加入 Tab 切換 UI）
- 新增 `expense/personal-tab.html` fragment（個人記帳內容）
- 修改 `expense/create.html`（新增類別）
- 新增個人支出新增/編輯表單 template

**Permission**
- `PersonalExpense` 所有操作須驗證 `userId == currentUser`，其他成員不可存取
- Viewer 角色亦可使用個人記帳（與 role 無關，屬個人資料）

**UI/UX**
- `/ui-ux-pro-max` skill 須於實作 Tab UI、圖表嵌入、預算進度條時諮詢
