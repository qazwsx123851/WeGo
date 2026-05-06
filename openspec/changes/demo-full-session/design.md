## Context

WeGo 已於 commit `28bae3a` 上線一版 demo:首頁有「先看看 Demo」按鈕指向 `/demo/trip`,後端 `DemoWebController` 將 `DemoDataProvider` 提供的硬編碼東京行程一次性塞進單一 view (`templates/demo/trip-view.html`,1072 行),所有 trip overview / activities / expenses (團隊+個人) / todos / members 共用 in-page tab 切換,寫入按鈕透過 `data-demo-cta` 屬性被 `static/js/demo.js` 攔截到 CTA modal。

正式 app 採截然不同的 UX 結構 — `trip/view.html` 只負責 trip overview,activities / expenses / todos / members 各為獨立頁面,透過 `fragments/components::trip-pill-bar` 在 `/trips/{id}/...` 路徑間導航。Demo 與正式體驗的版面、互動、資訊密度完全脫鉤。

本次改動目標:讓 demo 結構**完全鏡像**正式 app,使用者在 demo 點擊 pill-bar 切換 tab 時實際是頁面跳轉,demo 模板 1:1 對應正式模板的版型與資訊呈現。

**Constraints**:
- 不引入 demo 寫入(維持唯讀,寫入動作全靠 `data-demo-cta` CTA 攔截)
- 不變動 SecurityConfig 既有 `/demo/**` permitAll 規則
- 不變動 `DemoDataProvider` 的「in-memory 硬編碼」資料供應策略
- 不變動既有 `/api/demo/chat` 端點與 `ChatService.demoChat()`

## Goals / Non-Goals

**Goals:**
- Demo 路由結構與正式 app 1:1 對應(`/trips/{id}/X` ↔ `/demo/trip/X`)
- Demo 模板版型與正式模板 1:1 對應,使用同一份 fragments(toast、pill-bar、chat-widget、head)
- `trip-pill-bar` fragment 一份程式碼同時 serve demo 與正式模式(透過 `isDemo` 參數)
- `DemoDataProvider` 補齊 detail 取用方法(`getActivity(id)`、`getExpense(id)`、`getMember(id)`),讓 detail 頁能渲染
- 退役舊的單頁模板 `templates/demo/trip-view.html` 與其專用 tab 切換邏輯
- 對應 demo controller 都有 `@WebMvcTest` 涵蓋(維持專案測試慣例)

**Non-Goals:**
- Demo 寫入功能(維持只讀,所有寫入按鈕透過 `data-demo-cta` 攔截)
- 合成 `UserPrincipal`、guest 帳號、session-bound demo 狀態
- Statistics / Settlement / Documents 頁面的 demo 鏡像(現有 demo 也未涵蓋,留待後續 change 處理)
- 鏡像表單頁面 `*/new`、`*/edit`(寫入按鈕一律 CTA 攔截,使用者不會被導到表單頁)
- 多個 demo trip 或 trip list `/demo/trips`(維持單一 demo 行程,進入點 `/demo/trip`)
- 假 UUID URL(`/demo/trips/{demo-uuid}`,維持簡潔的 `/demo/trip`)
- Dashboard / Profile 頁面 demo 鏡像

## Decisions

### Decision 1: 路徑策略 — 用 `/demo/trip` 不帶 ID

**選擇**: 採 `/demo/trip`、`/demo/trip/activities`、`/demo/trip/activities/{activityId}` 等路徑,**不引入** demo trip 假 UUID。

**理由**:
- Demo 永遠只有一個行程,不需要 ID 區辨。
- `/demo/trips/{uuid}/activities/{aid}` 的 UUID 對使用者無意義,反而暴露實作細節。
- `DemoDataProvider` 已有 fixed `getDemoTrip()`,沒有「demo trip 的 ID」這個概念存在於資料層。
- Activity / Expense detail 路徑下保留實際 entity ID(來自 `DemoDataProvider`),例如 `/demo/trip/activities/{activityId}` 的 `activityId` 是 demo activity 的 UUID — 這部分 ID 必須保留,因為 detail 頁要靠它去 `DemoDataProvider.getActivity(id)`。

**替代方案**: `/demo/trips/{固定 demo-uuid}/...` 與正式 URL 一致 — 拒絕,理由如上。

### Decision 2: Controller 拆分 — 鏡像正式 controller 結構

**選擇**: 為 demo 新增 5 個 controller + 重構現有 `DemoWebController`:

| Demo Controller | RequestMapping | 鏡像對象 |
|-----------------|---------------|---------|
| `DemoTripController`(重構自 `DemoWebController`) | `/demo/trip` | `TripController` 的 `/trips/{id}` |
| `DemoActivityController` | `/demo/trip/activities` | `ActivityWebController` 的 `/trips/{tripId}/activities` |
| `DemoExpenseController` | `/demo/trip/expenses` | `ExpenseWebController` 的 `/trips/{tripId}/expenses` |
| `DemoPersonalExpenseController` | `/demo/trip/personal-expenses` | `PersonalExpenseWebController` |
| `DemoTodoController` | `/demo/trip/todos` | `TodoWebController` |
| `DemoMemberController` | `/demo/trip/members` | `MemberWebController` |

**理由**:
- 每個 demo controller 跟正式 controller 1:1 對應 — 結構清晰、未來 demo 內容調整時定位容易。
- 所有 demo controller 依賴同一個 `DemoDataProvider`,保持資料來源唯一。
- 命名沿用既有專案慣例(`{Domain}Controller`、`{Domain}WebController`)。

**替代方案**:
- 單一 `DemoWebController` 接所有路徑 — 拒絕,因為 controller 本身會膨脹回到 100+ 行,失去 path A 的「結構對齊」價值。

### Decision 3: Pill-bar fragment — 加 `isDemo` 參數

**選擇**: `fragments/components.html` 的 `trip-pill-bar` fragment 增加 `isDemo` 參數(預設 `false`),內部用條件運算式 + 字串生成連結,避免 `${}` 內混用 `@{}`。

**理由**:
- Pill-bar 是核心導航元件,demo 與正式版必須完全一致(版型、active state、icon)— 共用同一份程式碼是維護最低成本方案。
- Thymeleaf SpEL 不允許 `${}` 內混用 `@{}`,因此用條件運算式內字串拼接,並使用 `?.` null-safe(遵循 `CLAUDE.md` 規範)。
- `isDemo` 參數預設 `false` 可確保正式頁面呼叫端不需要改動。

**替代方案**:
- 為 demo 寫一份獨立的 `demo-pill-bar` fragment — 拒絕,會產生兩份易漂移的版型。
- 在 controller 端傳完整 URL 列表進模板 — 拒絕,把版型決策從 fragment 移到 controller,反向耦合。

**SpEL 寫法範例**(避免錯誤):

```html
<!-- 錯誤示範: 在 ${} 內使用 @{} (EL1059E) -->
<!-- DO NOT WRITE: ${isDemo ? @{/demo/trip/activities} : @{/trips/{id}/activities(id=${tripId})}} -->

<!-- 正確: 條件運算式外層分流,各自走 @{} 或字串 -->
<a th:href="${isDemo} ? '/demo/trip/activities' : @{/trips/{id}/activities(id=${tripId})}">
```

### Decision 4: 寫入動作 — 維持 `data-demo-cta` 攔截,不鏡像 form 頁面

**選擇**: 既有 `data-demo-cta` 屬性 + `demo.js` CTA modal 攔截機制不變;demo controllers 不提供 `*/new`、`*/edit` 等表單路由。

**理由**:
- 保持與既有 demo 體驗一致(使用者已熟悉 CTA 引導)。
- 鏡像表單頁面會讓工作量翻倍(每個 entity 一個 form 模板),收益低 — 表單 UX 細節在註冊後體驗即可。
- 寫入路徑(POST/PUT/DELETE)在 demo 模式下完全不可達,杜絕誤觸 demo data provider 邊界的風險。

### Decision 5: 退役舊單頁模板的時機

**選擇**: 採取「並存後切換」遷移策略:
1. 先建立所有新 demo controllers 與模板
2. 首頁 `index.html` 的「先看看 Demo」按鈕仍指向 `/demo/trip`(URL 不變)
3. 新 `DemoTripController` 取代舊 `DemoWebController` 處理 `/demo/trip`(同 URL 不同 view)
4. 移除 `templates/demo/trip-view.html` 與相關 cta-modal fragment 引用
5. `static/js/demo.js` 拆分:CTA modal 邏輯保留並通用化,tab 切換邏輯刪除

**理由**:
- 入口 URL `/demo/trip` 不變,首頁不需動。
- 切換時刻在第 3 步原子化發生(舊 controller bean 移除、新 controller 上線)。

### Decision 6: DemoDataProvider 擴張內容

**選擇**: 不變動既有方法簽章,僅新增 detail 取用方法:

```java
// 新增 (id-based detail accessor)
Optional<Activity> getActivity(UUID id);            // 從 getAllDemoActivities() 找
Optional<Expense> getExpense(UUID id);              // 從 getDemoExpenses() 找
Optional<TripMember> getMember(UUID id);            // 從 trip.getMembers() 找

// 既有 (保留不變)
Trip getDemoTrip();
List<Activity> getAllDemoActivities();
Map<LocalDate, List<Activity>> getDemoActivitiesByDate();
List<Expense> getDemoExpenses();
List<PersonalExpense> getDemoPersonalExpenses();
PersonalSummary getDemoPersonalSummary();
List<Todo> getDemoTodos();
SettlementResult getDemoSettlement();
List<LocalDate> getDemoDates();
Map<LocalDate, List<Expense>> getDemoExpensesByDate();
```

**理由**:
- 既有方法都在用,不可變動簽章。
- Detail 頁需要 entity-by-id 取得,新增 accessor 即可,不需重新設計資料結構。
- 回傳 `Optional` 讓 controller 統一以 `.orElseThrow(NotFoundException::new)` 轉 404,避免散落的 null check。

## Risks / Trade-offs

**[Risk] `trip-pill-bar` fragment 改造影響正式頁面**
→ Mitigation: `isDemo` 參數預設為 `false`,正式頁面呼叫處可不改動;改完後跑既有 `@WebMvcTest`(13 個 web controllers)驗證正式路徑不破。SpEL 寫法嚴格遵循 `CLAUDE.md` 的「不混用 `${}` 與 `@{}`」規範。

**[Risk] Demo 與正式模板版型漂移**
→ Mitigation: Demo 模板採「複製正式模板,改 URL pattern + 加 `isDemo=true` + 移除 form action,改 `data-demo-cta`」的固定流程,在 PR review checklist 列出每對模板比對項目。後續正式模板版型更動時,demo 視為「同樣需要更新」的對應檔案。

**[Risk] DemoDataProvider 新方法資料一致性**
→ Mitigation: 新增 detail accessor 內部用 `getDemoExpenses().stream().filter(e -> e.getId().equals(id)).findFirst()` 等方式從既有 list 派生,確保資料源唯一、不會出現「list 有但 detail 找不到」。

**[Risk] `static/js/demo.js` 拆分時遺漏功能**
→ Mitigation: 拆分前先列出 `demo.js` 所有函式,確認每個函式去向 (CTA modal 保留 / tab 切換刪除 / chart 渲染移到 demo expense 模板專用 script)。

**[Trade-off] Detail 頁 entity ID 可預測性**
→ Demo activity/expense ID 來自 `DemoDataProvider` 的硬編碼 UUID,任何改動都會讓 bookmark 過的 demo detail URL 失效。可接受 — demo 不對外承諾 URL 穩定性。

## Migration Plan

**Phase 1 — 新模板與 controllers 並存上線**
1. 擴張 `DemoDataProvider`,新增 detail accessor 方法 + 對應單元測試
2. 改造 `fragments/components::trip-pill-bar` fragment,加 `isDemo` 參數
3. 新增 5 個 demo controllers (Activity / Expense / PersonalExpense / Todo / Member) 與對應模板
4. 重構 `DemoWebController` → `DemoTripController`,改 serve 新版 trip overview 模板
5. 為每個新 demo controller 加 `@WebMvcTest`

**Phase 2 — 退役舊單頁**
6. 移除 `templates/demo/trip-view.html`
7. 整合 `templates/demo/fragments/cta-modal.html` 的引用方式(讓所有 demo 模板共用)
8. 拆分 `static/js/demo.js`:CTA modal 邏輯保留,tab 切換邏輯刪除

**Rollback 策略**:
- Phase 1 階段任何點都可 revert(舊 demo 仍可用)
- Phase 2 完成後 revert 需要還原舊單頁 — 第 6 步在 Phase 2 為原子操作,不會半完成

**驗證**:
- 新 demo 路徑全部能 200 OK 渲染(integration test)
- CTA modal 在每個 demo 頁面寫入按鈕點擊都能觸發
- 正式 app 路徑(13 個 web controllers)`@WebMvcTest` 全部維持綠燈
- `/api/demo/chat` 維持運作(既有測試應已涵蓋)

## Open Questions

- **Demo 模式提示**: Demo 頁面是否需要全域 banner 提示「您正在瀏覽 Demo,註冊後資料才會保留」?目前單頁版只在 hero header 有 demo 字樣。建議在 Phase 1 結束評估視覺一致性後決定,不阻塞本次改動。
- **AI 聊天 widget 在每頁是否都要顯示**: 正式 `trip-pill-bar` 路徑下的頁面都引用 `chat-widget` fragment,demo 多頁版面是否每頁都掛 chat-widget?暫定採「每頁都掛」(與正式一致),但 chat 計數器仍是「全 demo session 共用 3 次」。
