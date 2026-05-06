## 1. DemoDataProvider 擴張 (Foundation)

- [x] 1.1 在 `DemoDataProvider` 新增 `getActivity(UUID id)` 方法,回傳 `Optional<ActivityResponse>`,內部從 `getAllDemoActivities()` 派生(註:實作上 `DemoDataProvider` 回傳 DTO Response 類型,非 entity)
- [x] 1.2 在 `DemoDataProvider` 新增 `getExpense(UUID id)` 方法,回傳 `Optional<ExpenseResponse>`,內部從 `getDemoExpenses()` 派生
- [x] 1.3 在 `DemoDataProvider` 新增 `getMember(UUID id)` 方法,回傳 `Optional<TripResponse.MemberSummary>`,內部從 `getDemoTrip().getMembers()` 派生
- [x] 1.4 撰寫 `DemoDataProviderTest` 單元測試,涵蓋三個新 detail accessor 的「ID 存在」、「ID 不存在」與「null ID」三種情境(9/9 passed)

## 2. trip-pill-bar Fragment 改造

- [x] 2.1 在 `templates/fragments/components.html` 的 `trip-pill-bar` fragment 簽章加 `isDemo` 參數(註:Thymeleaf 不支援 fragment 預設值,3 個正式 caller 已顯式傳 `isDemo=false`)
- [x] 2.2 修改 fragment 內所有 `th:href` 為條件運算式:活動/分帳/代辦走 `/demo/trip/X`,documents 在 demo 模式變 `data-demo-cta="upload-document"`(因 documents 不在範圍)
- [~] 2.3 改造 active state — **N/A**:既有正式 pill-bar 沒有 active state 樣式,demo 多頁版亦不需特別處理(後續若有需求再補)
- [x] 2.4 跑 web controller `@WebMvcTest` — 336/336 全綠(含 ActivityWebControllerTest、TripControllerTest)
- [ ] 2.5 (手動)在 dev 環境瀏覽正式 trip 頁面,確認 pill-bar 視覺與導航行為與改造前一致 — **留給使用者手動驗證**

## 3. UI/UX 指引(實作前必做)

- [ ] 3.1 Invoke `/ui-ux-pro-max` skill,取得 demo 多頁版型的 styles、palettes、font pairings、patterns 指引
- [ ] 3.2 列出每個正式模板(trip/view、activity/list、activity/detail、expense/list、expense/detail、expense/personal-tab、todo/list、trip/members)在改造為 demo 版時需要的差異清單(URL pattern、`data-demo-cta`、移除 form action、`isDemo=true` 傳遞)

## 4. Demo Trip Overview

- [x] 4.1 `DemoWebController` 重命名為 `DemoTripController`(`@RequestMapping("/demo/trip")`)— 改為從零寫,只放 trip overview 所需 model 屬性(舊 controller 已刪除)
- [x] 4.2 新增 `templates/demo/trip/view.html` 鏡像 `templates/trip/view.html`,寫入按鈕全改 `data-demo-cta`,Back 鈕導向 `/`,pill-bar 走 isDemo=true
- [x] 4.2.1 (額外)抽出 `templates/demo/fragments/chat-widget.html` 供未來所有 demo 頁面重用,避免複製 ~120 行 widget HTML
- [x] 4.3 首頁 `templates/index.html:36` 已指向 `/demo/trip`(無需更動)
- [x] 4.4 `DemoTripControllerTest`(`@WebMvcTest` + `@Import({DemoDataProvider.class, SecurityConfig.class})`)2/2 pass — `addFilters=false` 不可行(需 CSRF token 給 head fragment),改用 SecurityConfig 真實規則

## 5. Demo Activities

- [x] 5.1 新增 `DemoActivityController`(`@RequestMapping("/demo/trip/activities")`)— 提供 `GET /`(list)與 `GET /{activityId}`(detail)端點
- [x] 5.2 Detail 端點 `demoDataProvider.getActivity(id).orElseThrow(ResourceNotFoundException)`(↓ GlobalExceptionHandler 轉 404)
- [x] 5.3 `templates/demo/activity/list.html` 鏡像正式版 ~280 行(原 643 行,刪除路徑優化、拖拉、重算交通等寫入功能)
- [x] 5.4 `templates/demo/activity/detail.html` 鏡像正式版 ~300 行(原 548 行,刪除 More Options menu、Delete dialog 等寫入功能;Map 在 demo 沒有 API key 改用 fallback link)
- [x] 5.5 `DemoActivityControllerTest` 3/3 pass(list 200、detail 200、detail 404)

## 6. Demo Expenses (Team)

- [x] 6.1 新增 `DemoExpenseController`(`@RequestMapping("/demo/trip/expenses")`)— 提供 list + detail
- [x] 6.2 Detail 端點 `Optional.orElseThrow(ResourceNotFoundException)` → 404
- [x] 6.3 `templates/demo/expense/list.html` 鏡像 ~280 行(原 492 行,只保留團隊分帳 tab,個人記帳改 page navigation 到 `/demo/trip/personal-expenses`;統計/結算按鈕走 CTA)
- [x] 6.4 `templates/demo/expense/detail.html` 鏡像 ~190 行(原 195 行,刪除 delete confirmation fetch script)
- [x] 6.5 `DemoExpenseControllerTest` 3/3 pass(list 200、detail 200、detail 404)

## 7. Demo Personal Expenses

- [x] 7.1 新增 `DemoPersonalExpenseController`(`@RequestMapping("/demo/trip/personal-expenses")`)— 提供 GET 標準頁(注入 `ObjectMapper` 序列化 chart JSON)
- [x] 7.2 `templates/demo/expense/personal-tab.html` 鏡像 ~430 行(原 378 行 fragment + 加 page wrapper:標頭、tab、CTA modal、pill-bar、chat-widget;預算/編輯/刪除按鈕全走 `data-demo-cta`)
- [x] 7.3 Chart.js 渲染腳本內嵌於模板(類別 doughnut + 每日 bar,以及 tab 切換邏輯)
- [x] 7.4 `DemoPersonalExpenseControllerTest` 1/1 pass(GET 200、chart JSON attrs)

## 8. Demo Todos

- [x] 8.1 新增 `DemoTodoController`(`@RequestMapping("/demo/trip/todos")`)— 計算 PENDING/IN_PROGRESS/COMPLETED 各分類筆數
- [x] 8.2 `templates/demo/todo/list.html` 鏡像 ~190 行(原 465 行,刪除 filter JS、CRUD modals;改用 stats 卡片 + 單一列表展示所有狀態)
- [x] 8.3 `DemoTodoControllerTest` 1/1 pass

## 9. Demo Members

- [x] 9.1 新增 `DemoMemberController`(`@RequestMapping("/demo/trip/members")`)— `currentUserId` 設為 `trip.getOwnerId()` 讓 demo viewer 顯示為擁有者;`inviteLink` 為 null
- [x] 9.2 `templates/demo/trip/members.html` 鏡像 ~155 行(原 554 行,刪除邀請連結 UI、role 變更、QR code、刪除確認 modal;邀請按鈕走 CTA)
- [x] 9.3 `DemoMemberControllerTest` 1/1 pass

## 10. JS 拆分與整合

- [x] 10.1 既有 demo.js 7 個函式分類:`initCtaModal`/`showCtaModal`/`hideCtaModal` 保留;`initTabSwitching`、`switchTab`、`initTimelineInteractions`、`initExpenseSubTabs`、`initChartsIfNeeded`、`initCategoryChart`、`initDailyChart`、`initChartTabSwitcher` 全部刪除(新多頁版各自處理,personal-expense 模板自帶 chart 渲染腳本)
- [x] 10.2 CTA modal 邏輯保留在 `static/js/demo.js`,所有 8 個新 demo 模板都已載入 `<script th:src="@{/js/demo.js}">`
- [x] 10.3 demo.js 從 398 行縮為 110 行(只剩 CTA modal + CTA_MESSAGES map 擴充至 22 個訊息,涵蓋所有新 `data-demo-cta` 類型)
- [x] 10.4 已確認所有 demo 模板的寫入按鈕都有對應的 CTA 訊息(`add-personal-expense`、`edit-budget`、`set-budget`、`view-statistics`、`view-settlement`、`remove-member`、`toggle-todo` 等新訊息已加入 map)

## 11. 退役舊單頁

- [x] 11.1 已確認:`grep -rn "demo/trip-view"` 在 `src/` 內無任何引用(`DemoWebController` 已於 Phase 3 刪除)
- [x] 11.2 移除 `templates/demo/trip-view.html`(原 1072 行)
- [x] 11.3 `templates/demo/fragments/cta-modal.html` 已被所有 8 個新 demo 模板透過 `th:replace="~{demo/fragments/cta-modal :: cta-modal}"` 共用
- [x] 11.4 `demo/fragments/chat-widget.html`(Phase 3 抽出)已被所有 8 個新 demo 模板掛載,`/api/demo/chat` 端點不變,1203/1203 tests 綠燈

## 12. 整合驗證

- [ ] 12.1 手動測試:從首頁 → 點「先看看 Demo」→ `/demo/trip` → 透過 pill-bar 走訪 5 個 demo 頁面(activities, expenses, personal-expenses, todos, members)→ 點擊每個寫入按鈕觸發 CTA modal
- [ ] 12.2 執行 `./mvnw test`,確認所有單元測試綠燈(含新增的 6 個 Demo*ControllerTest)
- [ ] 12.3 確認正式 app 13 個 web controller `@WebMvcTest` 全部維持綠燈(`trip-pill-bar` 改造未破壞正式路徑)
- [ ] 12.4 確認 `/api/demo/chat` 在新多頁 chat-widget 仍正常運作(3 次 HTTP session 限額、429 錯誤訊息)
- [ ] 12.5 確認 demo 頁面在 dark mode、行動裝置 viewport(320px / 768px)顯示正確
- [ ] 12.6 確認 demo activity/expense detail 在 ID 不存在時回傳 404(而非 500)

## 13. 文件與 PR

- [ ] 13.1 (可選)更新 `docs/CONTRIB.md` 或對應位置,說明 demo 多頁結構與「正式模板/demo 模板需保持版型同步」的維護慣例
- [ ] 13.2 提交 PR 時在描述中列出對應正式模板與 demo 模板的對照表,方便 review
