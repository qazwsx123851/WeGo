## Why

目前的 Demo (commit `28bae3a`) 是一個 1072 行的單頁 (`templates/demo/trip-view.html`),把 trip overview / activities / expenses / todos / members 全部塞在同一張頁面用 in-page tab 切換 — 與正式 app 採用「pill-bar 多頁導航」的 UX 結構完全不一致。使用者在 demo 看到的版面、互動模式、資訊密度,都不等於註冊後實際的產品體驗,失去 demo 作為「真實產品預覽」的價值。

本次改動把 demo 重構成「結構完全鏡像正式 app 的多頁瀏覽」,使 demo 看到的內容與註冊後的內容一致,寫入動作仍由現有 `data-demo-cta` 機制攔截到 CTA modal(保留既有引導註冊流程)。

## What Changes

- **拆解現有單頁** demo: 退役 `templates/demo/trip-view.html`,改為多頁結構
- **新增 demo web controllers** 鏡像正式 web controllers 的 read-only GET 端點:
  - `DemoTripController` (擴充現有,改 serve trip overview)
  - `DemoActivityController` (list + detail)
  - `DemoExpenseController` (list + detail)
  - `DemoPersonalExpenseController` (personal-tab)
  - `DemoTodoController` (list)
  - `DemoMemberController` (members)
- **新增 demo 模板** 於 `templates/demo/{trip,activity,expense,todo}/`,結構與正式模板對齊
- **改造 `fragments/components::trip-pill-bar`** 支援 `isDemo` 參數,內部依旗標生成 `/demo/...` 或 `/trips/{id}/...` 連結
- **`DemoDataProvider` 擴張**: 補齊 members 列表細節、expense detail 取用方法、activity detail 取用方法
- **寫入按鈕一律走 `data-demo-cta` 攔截**: 不鏡像 `*/new`、`*/edit` 等寫入頁面;按鈕點擊由 `demo.js` 的 CTA modal 攔截(現有機制不變)
- **暫不納入** statistics / settlement / documents 三個頁面的 demo 鏡像(現有 demo 也未涵蓋)
- **URL 設計**: 單一 demo trip 採用 `/demo/trip`、`/demo/trip/activities`、`/demo/trip/expenses` 等路徑(不引入假 UUID)
- **進入點維持** `/demo/trip`(首頁「先看看 Demo」按鈕無需改動)
- **AI 聊天功能保留**: `/api/demo/chat` 與 `chat-widget` fragment 維持現狀

## Capabilities

### New Capabilities

- `demo-session`: Unauthenticated demo 多頁瀏覽 capability。涵蓋 demo 路由結構、模板鏡像策略、`isDemo` flag 在 fragments 與 controller model 的傳遞慣例、寫入動作攔截規則、`DemoDataProvider` 提供的合成資料邊界。

### Modified Capabilities

- 無。既有 `ai-chat` / `chat-ui` / `personal-expense` 的 spec-level 行為不變,僅 demo 端 read-only 路徑增加,不涉及 requirement 變更。

## Impact

**Affected layers**:
- **Controller (Web)**: 新增 5 個 demo controller(DemoActivityController / DemoExpenseController / DemoPersonalExpenseController / DemoTodoController / DemoMemberController),擴充 1 個現有(DemoWebController → 重新分工為 DemoTripController)
- **Service**: `DemoDataProvider` 擴張(新增 getMember(id)、getExpense(id)、getActivity(id) 等 detail 取用方法)
- **Template**: 新增 7-9 個 Thymeleaf 模板於 `templates/demo/`,移除 `templates/demo/trip-view.html`
- **Fragment**: 修改 `templates/fragments/components.html` 的 `trip-pill-bar` fragment(加 `isDemo` 參數)
- **Static JS**: `static/js/demo.js` 移除單頁 tab 切換邏輯(改由頁面跳轉),保留 CTA modal 攔截邏輯

**Unaffected**:
- **Repository / Entity / Database**: 無 schema 變更,不需 migration,demo 資料仍為 in-memory(`DemoDataProvider`)
- **Security**: `SecurityConfig` 既有 `/demo/**` permitAll 與 CSRF 豁免規則不變
- **API**: `/api/demo/chat` 端點與 `ChatService.demoChat()` 不變
- **Permission / Role**: Demo 不涉及 Owner/Editor/Viewer 角色判定(模板用固定 `canEdit=true` + `data-demo-cta` 攔截達到「按鈕可見但不能寫」效果)

**UI/UX**:
- 涉及多個新 Thymeleaf 模板與 fragment 改造 — 實作階段 **必須先 invoke `/ui-ux-pro-max` skill** 取得 styles/patterns 指引,確保 demo 多頁版型與正式 app 視覺一致。

**Risks**:
- `trip-pill-bar` fragment 是正式 trip 頁面的共用元件,改造時若 SpEL 寫法錯誤會同時影響 demo 與正式頁面。需嚴格遵循 `CLAUDE.md` 的 Thymeleaf SpEL 規範(避免 `${}` 內混用 `@{}`,使用 `?.` null-safe)。
- 模板複製貼上易產生內容漂移 — 需審查每個 demo 模板對應的正式模板,確保資訊呈現一致。
