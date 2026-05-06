## ADDED Requirements

### Requirement: Demo 進入點 — Trip Overview

系統 SHALL 在 `GET /demo/trip` 提供無需登入即可瀏覽的 demo 行程總覽頁,版面與 `templates/trip/view.html` 結構對齊,使用 `DemoDataProvider` 提供的合成東京行程資料。

#### Scenario: 未登入使用者進入 demo 行程總覽

- **WHEN** 未登入使用者瀏覽 `GET /demo/trip`
- **THEN** 系統回應 200,並渲染與正式 trip overview 相同結構的頁面(hero cover + trip 資訊 + 統計卡片 + pill-bar 導航)
- **AND** 頁面上的「編輯行程」、「邀請成員」等寫入按鈕都帶有 `data-demo-cta` 屬性

#### Scenario: 已登入使用者瀏覽 demo

- **WHEN** 已登入使用者瀏覽 `GET /demo/trip`
- **THEN** 系統回應 200,渲染同樣的 demo 內容(不重新導向到正式 trip)

### Requirement: Demo Activities — 列表與詳情

系統 SHALL 提供 demo activities 列表頁與單筆 activity 詳情頁,版面與正式 `activity/list.html`、`activity/detail.html` 對齊。

#### Scenario: 瀏覽 demo activities 列表

- **WHEN** 使用者瀏覽 `GET /demo/trip/activities`
- **THEN** 系統回應 200,顯示 demo trip 所有活動以日期分組(timeline 結構)
- **AND** 「新增活動」按鈕帶有 `data-demo-cta="add-activity"` 屬性

#### Scenario: 瀏覽 demo activity 詳情

- **WHEN** 使用者瀏覽 `GET /demo/trip/activities/{activityId}`,且 `activityId` 為 `DemoDataProvider` 提供的有效 UUID
- **THEN** 系統回應 200,顯示該 activity 詳情(地點、時間、備註、地圖)
- **AND** 「編輯」、「刪除」按鈕帶有 `data-demo-cta` 屬性

#### Scenario: Activity ID 不存在

- **WHEN** 使用者瀏覽 `GET /demo/trip/activities/{activityId}`,且 `activityId` 不存在於 `DemoDataProvider`
- **THEN** 系統回應 404 並渲染錯誤頁

### Requirement: Demo Expenses — 列表、詳情與個人記帳

系統 SHALL 提供 demo 團隊費用列表、費用詳情、個人記帳 tab 三個頁面,版面與正式 `expense/list.html`、`expense/detail.html`、`expense/personal-tab.html` 對齊。

#### Scenario: 瀏覽 demo expenses 列表

- **WHEN** 使用者瀏覽 `GET /demo/trip/expenses`
- **THEN** 系統回應 200,顯示 demo trip 所有團隊費用、avatar stack、總額、人均
- **AND** 「新增費用」、「展開分帳明細」等寫入相關按鈕帶有 `data-demo-cta` 屬性

#### Scenario: 瀏覽 demo expense 詳情

- **WHEN** 使用者瀏覽 `GET /demo/trip/expenses/{expenseId}`,且 `expenseId` 為 `DemoDataProvider` 提供的有效 UUID
- **THEN** 系統回應 200,顯示該費用的金額、付款人、分攤對象、分攤明細

#### Scenario: 瀏覽 demo 個人記帳

- **WHEN** 使用者瀏覽 `GET /demo/trip/personal-expenses`
- **THEN** 系統回應 200,顯示個人記帳列表(AUTO+MANUAL 合併排序)、預算狀態、分類圓餅圖、每日趨勢圖
- **AND** 「新增記帳」按鈕帶有 `data-demo-cta="add-expense"` 屬性

### Requirement: Demo Todos — 列表

系統 SHALL 在 `GET /demo/trip/todos` 提供 demo 待辦事項列表頁,版面與正式 `todo/list.html` 對齊。

#### Scenario: 瀏覽 demo todos 列表

- **WHEN** 使用者瀏覽 `GET /demo/trip/todos`
- **THEN** 系統回應 200,顯示 demo trip 所有待辦事項(已完成/未完成分區)
- **AND** 「新增待辦」、「標記完成」等寫入按鈕帶有 `data-demo-cta` 屬性

### Requirement: Demo Members — 列表

系統 SHALL 在 `GET /demo/trip/members` 提供 demo 成員列表頁,版面與正式 `trip/members.html` 對齊。

#### Scenario: 瀏覽 demo 成員列表

- **WHEN** 使用者瀏覽 `GET /demo/trip/members`
- **THEN** 系統回應 200,顯示 demo trip 所有成員(角色、加入時間)
- **AND** 「邀請成員」、「移除成員」等寫入按鈕帶有 `data-demo-cta` 屬性

### Requirement: Pill-bar 導航支援 Demo 模式

`fragments/components::trip-pill-bar` fragment SHALL 接受 `isDemo` 布林參數,當為 `true` 時,所有導航連結指向 `/demo/trip/...` 路徑;為 `false`(預設)時指向 `/trips/{tripId}/...`。

#### Scenario: Demo 模式下 pill-bar 連結

- **WHEN** demo 模板呼叫 `<div th:replace="~{fragments/components :: trip-pill-bar(tripId=${trip.id}, isDemo=true)}"></div>`
- **THEN** 渲染出來的 pill-bar 連結中,「行程」項目 href 為 `/demo/trip`、「活動」為 `/demo/trip/activities`、「費用」為 `/demo/trip/expenses`、「待辦」為 `/demo/trip/todos`、「成員」為 `/demo/trip/members`

#### Scenario: 正式模式下 pill-bar 連結

- **WHEN** 正式模板呼叫 `<div th:replace="~{fragments/components :: trip-pill-bar(tripId=${trip.id})}"></div>`(未傳 `isDemo`)
- **THEN** 渲染出來的 pill-bar 連結中,所有項目仍指向 `/trips/{tripId}/...`,行為與改造前一致

#### Scenario: Pill-bar SpEL 安全性

- **WHEN** Fragment 渲染含有 `tripId` 為 null 或字串的情境
- **THEN** Fragment 不拋出 EL1007E、EL1059E 錯誤(寫法符合 `CLAUDE.md` 的 SpEL 規範)

### Requirement: Demo 寫入動作攔截

所有 demo 頁面上對應「寫入」語意的按鈕(新增 / 編輯 / 刪除 / 邀請 / 上傳 / 設定預算)SHALL 帶有 `data-demo-cta` 屬性,並由 `static/js/demo.js`(或拆分後的對等 JS)在點擊時攔截、阻止預設行為、彈出 CTA modal。

#### Scenario: 點擊 demo 頁面上的「新增活動」按鈕

- **WHEN** 使用者在 `GET /demo/trip/activities` 點擊「新增活動」按鈕(該按鈕帶 `data-demo-cta="add-activity"`)
- **THEN** 預設行為被阻止(不發生表單提交、不發生頁面導航)
- **AND** CTA modal 彈出,顯示「想要新增自己的景點?」訊息與註冊引導

#### Scenario: Demo 頁面不提供寫入路由

- **WHEN** 使用者直接瀏覽 `GET /demo/trip/activities/new` 或 `GET /demo/trip/activities/{id}/edit`
- **THEN** 系統回應 404(demo controllers 不提供寫入相關的 GET 端點)

### Requirement: Demo 路由免認證且免 CSRF

`SecurityConfig` SHALL 維持 `/demo/**` 路徑為 `permitAll` 且 CSRF 豁免,確保未登入使用者可瀏覽所有 demo 頁面;`/api/demo/**` 同樣維持既有 permitAll 與 CSRF 豁免規則。

#### Scenario: 未登入使用者瀏覽 demo 任意頁面

- **WHEN** 未登入使用者(無 session、無 OAuth2 cookie)瀏覽 `/demo/trip`、`/demo/trip/activities`、`/demo/trip/expenses`、`/demo/trip/todos`、`/demo/trip/members` 任一路徑
- **THEN** 系統不重新導向到登入頁,直接回應 200 並渲染對應 demo 頁面

### Requirement: AI 聊天 Widget 維持運作

Demo 頁面 SHALL 在每個多頁路由都掛載 `fragments/chat-widget::chat-widget` fragment,且 `/api/demo/chat` 端點與 `ChatService.demoChat()` 行為不變(每個 HTTP session 限 3 次對話)。

#### Scenario: Demo 頁面開啟 chat widget

- **WHEN** 使用者在任一 demo 頁面點擊 chat widget 觸發按鈕
- **THEN** Widget 展開,使用者可送出訊息
- **AND** 訊息透過 `POST /api/demo/chat` 路由,由 `ChatService.demoChat()` 處理,不需要 CSRF token、不需要 OAuth2

#### Scenario: Demo chat session 限額耗盡

- **WHEN** 使用者在同一 HTTP session 內已成功送出 3 次 chat 訊息,試圖送出第 4 次
- **THEN** 系統回應 429,訊息為 rate limit exhausted
- **AND** Widget 顯示「想要無限暢聊 AI 助手?」CTA 提示

### Requirement: DemoDataProvider 提供 Detail 取用方法

`DemoDataProvider` SHALL 提供以 entity ID 取得單筆資料的方法,以支援 demo detail 頁面渲染;若 ID 不存在,方法應回傳空 `Optional`(由各 demo controller 使用 `.orElseThrow` 轉為 404)。

#### Scenario: 取得 demo activity 單筆

- **WHEN** Demo controller 呼叫 `demoDataProvider.getActivity(activityId)` 且該 ID 存在於 `getAllDemoActivities()`
- **THEN** 回傳 `Optional<Activity>` 包含對應 entity

#### Scenario: 取得 demo expense 單筆

- **WHEN** Demo controller 呼叫 `demoDataProvider.getExpense(expenseId)` 且該 ID 存在於 `getDemoExpenses()`
- **THEN** 回傳 `Optional<Expense>` 包含對應 entity 與其 splits

#### Scenario: 取得 demo member 單筆

- **WHEN** Demo controller 呼叫 `demoDataProvider.getMember(memberId)` 且該 ID 存在於 `getDemoTrip().getMembers()`
- **THEN** 回傳 `Optional<TripMember>` 包含對應 entity

#### Scenario: ID 不存在

- **WHEN** Demo controller 以不存在的 ID 呼叫任一 detail accessor
- **THEN** 方法回傳空 `Optional`,controller 將其轉換為 HTTP 404
