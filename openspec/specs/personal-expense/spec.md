## ADDED Requirements

### Requirement: 個人記帳 Tab 顯示
行程分帳頁（`/trips/{id}/expenses`）SHALL 在頁面頂部顯示 [團隊分帳] | [個人記帳] 兩個 Tab。預設顯示 [團隊分帳] Tab。Tab 切換不保留 URL state，每次進入頁面重置為預設。

#### Scenario: 切換至個人記帳 Tab
- **WHEN** 使用者點擊 [個人記帳] Tab
- **THEN** 頁面顯示個人記帳內容（個人花費總覽、圖表、費用列表），隱藏團隊分帳內容

#### Scenario: 切換回團隊分帳 Tab
- **WHEN** 使用者點擊 [團隊分帳] Tab
- **THEN** 頁面顯示原有團隊分帳內容，隱藏個人記帳內容

---

### Requirement: 個人花費總覽卡片
個人記帳 Tab 頂部 SHALL 顯示總覽卡片，包含：個人總花費（換算 baseCurrency）、日均花費（總花費 ÷ 行程天數，endDate 為 null 時不顯示日均）、[+] 新增按鈕。

#### Scenario: 有 endDate 的行程
- **WHEN** 行程設定了 startDate 與 endDate
- **THEN** 總覽卡片顯示個人總花費與日均花費

#### Scenario: 無 endDate 的行程
- **WHEN** 行程未設定 endDate
- **THEN** 總覽卡片只顯示個人總花費，不顯示日均花費

#### Scenario: 無任何個人支出
- **WHEN** 使用者在此行程尚無分帳份額或手動支出
- **THEN** 總覽卡片顯示 $0，並顯示空白提示引導新增第一筆

---

### Requirement: 個人預算設定
每位行程成員 SHALL 可為自己設定單一總預算（BigDecimal，nullable）。預算儲存於 `TripMember.personalBudget`，以 trip.baseCurrency 為單位。

#### Scenario: 首次進入個人記帳 Tab（未設定預算）
- **WHEN** 使用者首次進入個人記帳 Tab 且尚未設定預算
- **THEN** 顯示提示卡：[設定預算] 與 [略過] 按鈕

#### Scenario: 略過預算提示
- **WHEN** 使用者點擊 [略過]
- **THEN** 提示卡消失，summary 區顯示淡色 [設定預算] 連結，不再主動顯示提示卡

#### Scenario: 設定預算
- **WHEN** 使用者點擊 [設定預算] 並輸入金額後確認
- **THEN** 預算儲存至 TripMember.personalBudget，總覽卡片顯示預算進度條

#### Scenario: 修改預算
- **WHEN** 使用者點擊預算進度條旁的 [✎] icon
- **THEN** 顯示編輯 modal，使用者可修改預算金額

---

### Requirement: 個人預算進度條
設定預算後，總覽卡片 SHALL 顯示進度條，依花費比例呈現不同狀態：
- 0–79%：綠色進度條
- 80–99%：黃色進度條，顯示「快達到預算上限」提示
- 100%+：紅色進度條，顯示「已超支 $X」警示

#### Scenario: 花費低於 80% 預算
- **WHEN** 個人總花費 < 預算 × 0.8
- **THEN** 進度條呈綠色，顯示「剩餘 $X」

#### Scenario: 花費介於 80–99% 預算
- **WHEN** 個人總花費 >= 預算 × 0.8 且 < 預算
- **THEN** 進度條呈黃色，顯示「快達到預算上限」

#### Scenario: 花費超過 100% 預算
- **WHEN** 個人總花費 >= 預算
- **THEN** 進度條呈紅色，顯示「已超支 $X」

---

### Requirement: 類別圓餅圖（嵌入）
個人記帳 Tab SHALL 在總覽卡片下方嵌入類別圓餅圖，顯示各類別佔個人總花費的百分比。金額統一換算為 trip.baseCurrency。圖表與每日趨勢圖可切換顯示。

#### Scenario: 顯示類別分布
- **WHEN** 使用者查看類別圓餅圖
- **THEN** 圖表顯示所有類別的花費佔比（自動同步 + 手動支出合計）

#### Scenario: 僅有單一類別支出
- **WHEN** 所有個人支出均屬同一類別
- **THEN** 圓餅圖顯示 100% 單一類別，不顯示其他

---

### Requirement: 每日花費長條圖（嵌入）
個人記帳 Tab SHALL 提供每日花費長條圖，X 軸為行程日期（從早到晚，左到右），Y 軸為當日個人總花費（換算 baseCurrency）。當天無支出顯示空白長條（$0）。超過 7 天時支援橫向捲動。

#### Scenario: 點擊長條篩選當日明細
- **WHEN** 使用者點擊某一天的長條
- **THEN** 費用列表篩選為僅顯示該日支出，長條呈現選中狀態

#### Scenario: 再次點擊取消篩選
- **WHEN** 使用者再次點擊已選中的長條
- **THEN** 費用列表恢復顯示全部支出，長條取消選中狀態

#### Scenario: 超過 7 天的行程
- **WHEN** 行程天數 > 7
- **THEN** 長條圖容器支援橫向捲動，每個長條維持固定寬度

#### Scenario: 某天無支出
- **WHEN** 行程中某日無任何個人支出
- **THEN** 該日顯示 $0 空白長條，保持時間軸連續

---

### Requirement: 個人支出費用列表
個人記帳 Tab SHALL 顯示費用列表，合併自動同步（🔄）與手動新增（✏️）兩種來源，依 expenseDate ASC 排序（expenseDate 為 null 者排列表最後）。

#### Scenario: 自動同步項目展開詳情
- **WHEN** 使用者點擊 🔄 標記的列表項目
- **THEN** 展開 inline 小卡片，顯示：原始金額（含原始幣種）、我的份額、付款人姓名、[查看完整分帳記錄] 連結

#### Scenario: 手動項目顯示操作按鈕
- **WHEN** 使用者查看 ✏️ 標記的列表項目
- **THEN** 右側顯示 [✎ 編輯] 和 [🗑 刪除] 兩個 icon 按鈕

#### Scenario: 刪除手動支出
- **WHEN** 使用者點擊 [🗑 刪除] 並確認
- **THEN** 該筆 PersonalExpense 從資料庫刪除，列表立即移除該項目

---

### Requirement: 手動新增個人支出
使用者 SHALL 可在個人記帳 Tab 點擊 [+] 進入新增表單，填寫個人支出資料。表單欄位：描述（必填）、金額（必填）、幣種、類別（7 個）、日期（限行程期間）、備註（可選）。無收據上傳、無付款人選擇、無分帳方式。

#### Scenario: 新增個人支出（基本欄位）
- **WHEN** 使用者填寫描述、金額後送出表單
- **THEN** PersonalExpense 儲存至資料庫（userId = currentUser, tripId = 當前行程），列表新增該項目

#### Scenario: 選擇非 baseCurrency 幣種
- **WHEN** 使用者選擇非 baseCurrency 的幣種（如 JPY）
- **THEN** 系統自動查詢 ExchangeRate-API 填入匯率；查詢失敗時 exchangeRate 設為 null（視為 1:1）

#### Scenario: 日期限制行程期間
- **WHEN** 行程設有 startDate 與 endDate
- **THEN** 日期選擇器的最小/最大日期限制在行程期間內

#### Scenario: 手動支出屬於自己
- **WHEN** 手動支出成功儲存
- **THEN** 該支出只對建立者可見，其他行程成員無法透過任何 API 讀取

---

### Requirement: 編輯手動個人支出
使用者 SHALL 可點擊 ✏️ 列表項目的 [✎] icon，進入編輯表單修改已新增的個人支出。表單欄位與新增表單相同。

#### Scenario: 成功編輯個人支出
- **WHEN** 使用者修改描述或金額後儲存
- **THEN** PersonalExpense 更新至資料庫，列表顯示更新後的資料

#### Scenario: 嘗試編輯他人支出
- **WHEN** 使用者直接存取他人 PersonalExpense 的編輯 API
- **THEN** 系統回傳 403 Forbidden

---

### Requirement: 費用類別擴充（7 個）
系統 SHALL 支援 7 個費用類別，同時適用於團隊分帳與個人記帳：FOOD（餐飲）、TRANSPORT（交通）、ACCOMMODATION（住宿）、SHOPPING（購物）、ENTERTAINMENT（娛樂）、HEALTH（健康）、OTHER（其他）。

#### Scenario: 新增支出選擇購物類別
- **WHEN** 使用者在新增個人或團隊支出時選擇「購物」類別
- **THEN** 支出以 SHOPPING 類別儲存並正確顯示

#### Scenario: 新增支出選擇健康類別
- **WHEN** 使用者在新增個人或團隊支出時選擇「健康」類別
- **THEN** 支出以 HEALTH 類別儲存並正確顯示

---

### Requirement: 個人支出隱私保護
PersonalExpense 的所有 CRUD 操作 SHALL 驗證 `resource.userId == currentUser.id`。自動同步的 ExpenseSplit 查詢也 SHALL 以 `userId = currentUser.id` 過濾，確保使用者只能看到自己的個人支出資料。

#### Scenario: 非擁有者嘗試讀取
- **WHEN** 使用者 B 嘗試存取使用者 A 的 PersonalExpense 詳情 API
- **THEN** 系統回傳 403 Forbidden，不洩漏資源是否存在

#### Scenario: 非擁有者嘗試刪除
- **WHEN** 使用者 B 嘗試刪除使用者 A 的 PersonalExpense
- **THEN** 系統回傳 403 Forbidden
