# WeGo 專案品質審查報告

**日期:** 2026-02-21（第八次審查 — 全面獨立審查）
**審查團隊:** 5 位 Opus 4.6 Agent（架構、安全、前端、效能、測試）
**專案版本:** main branch (commit fb4468f)
**審查範圍:** 架構、安全、前端、效能、測試

---

## 總覽評分表

| 面向 | 分數 (1-10) | 較上次 | 說明 |
|------|:-----------:|:------:|------|
| 架構設計 | **8.6** | ↓0.2 | 更嚴格審查 Controller 分層，PlaceApi/DirectionApi 業務邏輯洩漏新發現 |
| 安全性 | **8.5** | — | 零 SQLi/XSS、完整安全標頭、無 Critical，CSP unsafe-inline 為主要扣分 |
| 前端品質 | **8.4** | ↓0.1 | 15/15 模組化完成，但 inline JS/CSS 和 heading 層級問題仍在 |
| 效能 | **9.0** | ↑0.2 | 確認外部 API 共享連線池和 Circuit Breaker 已到位，修正先前報告遺漏 |
| 測試覆蓋 | **8.5** | — | 1,182 測試全通過（+7 補寫）、104 端點 100% 覆蓋 |
| **整體** | **8.6** | — | Critical 問題歸零，各面向均無系統性問題 |

> **評分說明**：本次為第八次全面獨立審查，由 5 位 Agent 從零開始審查整個 codebase，評分基於當前程式碼實際狀態。

---

## 專案規模

| 指標 | 數值 |
|------|------|
| REST API 端點 | 64（14 Controllers） |
| Web 端點 | 40（15 Controllers） |
| 單元測試 | 1,182（88 檔案） |
| E2E 測試 | 12 spec 檔 |
| JaCoCo 覆蓋率 | 76%（指令）/ 58%（分支） |
| JavaScript 模組 | 15 |
| Thymeleaf 模板 | 34 |
| Caffeine 快取 | 12 + 2 獨立管理 |

---

## Critical 問題優先處理清單

### 🔴 Critical（0 項 — 已全部修復）

無。所有歷史 Critical 問題已修復。

### 🟡 Warning 重點項目（Top 10）

| # | 面向 | 問題 | 檔案 | 影響 |
|---|------|------|------|------|
| W1 | 架構 | PlaceApiController/DirectionApiController 包含快取+速率限制業務邏輯 | `controller/api/PlaceApiController.java:81-135` | 違反分層原則 |
| W2 | 架構 | DocumentApiController 直接注入 StorageClient 跳過 Service | `controller/api/DocumentApiController.java:53` | 違反分層原則 |
| W3 | 架構 | getUserMap/buildPlaceLookup/getFileExtension 重複 | 多處 | 違反 DRY |
| W4 | 效能 | 批次重算交通時間同步阻塞 | `TransportCalculationService.java:403` | 50 景點需 5 秒+ |
| W5 | 效能 | getExpensesByTrip / getDocumentsByTrip 不帶分頁 | `ExpenseService.java:155`, `DocumentService.java:217` | 極端場景全量載入 |
| W6 | 效能 | SupabaseStorageClient 未共享連線池 | `SupabaseStorageClient.java:56-64` | 每次請求建新 TCP 連線 |
| W7 | 安全 | CSP `script-src` 含 `unsafe-inline` | `SecurityConfig.java:60` | 削弱 XSS 防護 |
| W8 | 前端 | 2 模板 inline `<script>` + 4 模板 inline `<style>` | `trip/create.html` 等 | CSP/可維護性 |
| W9 | 前端 | todo.js 仍使用 onclick 字串拼接 | `todo.js:673-678`, `todo/list.html` | 與其他模組不一致 |
| W10 | 前端 | app.js 1190 行，超過 800 行建議上限 | `static/js/app.js` | 可維護性 |

---

## 問題總覽

| 面向 | 🔴 Critical | 🟡 Warning | 🔵 Suggestion | 小計 |
|------|:-----------:|:----------:|:-------------:|:----:|
| 架構 | 0 | 11 | 8 | 19 |
| 安全 | 0 | 5 | 3 | 8 |
| 前端 | 0 | 19 | 13 | 32 |
| 效能 | 0 | 7 | 4 | 11 |
| 測試 | 0 | 0 | 4 | 4 |
| **合計** | **0** | **42** | **32** | **74** |

---

# 第 1 章：架構設計（8.6/10）

> 審查範圍：Controller / Service / Repository / Domain / Entity / DTO / Config / Exception

## 1.1 各子項評分

| 子項 | 分數 |
|------|:----:|
| 分層架構 | 8.5 |
| 依賴注入 | 8.5 |
| Entity/DTO 分離 | 8.5 |
| 例外處理 | 9.0 |
| SOLID 原則 | 8.5 |
| DRY 原則 | 7.5 |
| 命名規範 | 9.0 |
| Magic Number | 8.5 |
| 日誌使用 | 9.5 |

## 1.2 分層架構

### 優點

- **清晰的四層架構**：Controller → Service → Domain/Repository → Entity，層級邊界明確
- **Web 與 API Controller 分離**：`controller/web/` 和 `controller/api/` 分開，Web 回傳 Thymeleaf 視圖、API 回傳 `ApiResponse<T>` JSON
- **BaseWebController 提取共用邏輯**：`getCurrentUser()`、`loadTrip()`、`findCurrentMember()`、`canEdit()`、`isOwner()` 集中在基類
- **ViewHelper 層**：`TripViewHelper`、`ExpenseViewHelper`、`ActivityViewHelper` 將視圖準備邏輯從 Controller 中抽離
- **Domain 層獨立**：`PermissionChecker`、`DebtSimplifier`、`RouteOptimizer`、`ExpenseAggregator` 等放在 `domain/` 包中，不依賴 Spring 框架

### 問題

| 嚴重度 | 問題 | 檔案 | 建議 |
|--------|------|------|------|
| 🟡 | PlaceApiController/DirectionApiController 包含快取+速率限制業務邏輯 | `PlaceApiController.java:81-135`, `DirectionApiController.java:73-131` | 下沉至 Service 層 |
| 🟡 | DocumentApiController 直接注入 StorageClient 跳過 Service | `DocumentApiController.java:53-55` | 預覽/下載邏輯移至 DocumentService |
| 🟡 | TripController.createTrip 包含過多表單處理邏輯（90+ 行） | `TripController.java:97-189` | 提取 `populateFormModel()` 方法 |
| 🟡 | ExpenseWebController.showExpenses 方法近 100 行 | `ExpenseWebController.java:77-176` | 提取至 ExpenseViewHelper |
| 🔵 | TripService 職責偏多（715 行，12 依賴） | `TripService.java` | 可拆分 TripMemberService、CoverImageService |
| 🔵 | PersonalExpenseWebController 未繼承 BaseWebController | `PersonalExpenseWebController.java:37` | 風格不一致，功能無問題 |

## 1.3 依賴注入

### 優點

- 大量使用 `@RequiredArgsConstructor` + `private final` 構造器注入
- 無循環依賴
- `SettlementService` 使用 `@Nullable` 可選依賴

### 問題

| 嚴重度 | 問題 | 檔案 |
|--------|------|------|
| 🟡 | BaseWebController 使用 `@Autowired` Field Injection | `BaseWebController.java:31-35` |

## 1.4 Entity 與 DTO 分離

### 優點

- 完整 DTO 體系：`*Response` / `*Request` DTO，Entity 不直接暴露
- 統一 `fromEntity()` 靜態工廠方法
- `ApiResponse<T>` 統一包裝

### 問題

| 嚴重度 | 問題 | 檔案 |
|--------|------|------|
| 🟡 | TripResponse 使用 Setter 填充額外欄位，破壞 DTO 不可變性 | `TripResponse.java`, `TripService.java:125-128` |
| 🔵 | Response DTO `@Data` 可改為 `@Getter` + `@Builder` | 多個 Response DTO |
| 🔵 | ProfileController 直接傳遞 User Entity 到 Model | `ProfileController.java:80` |

## 1.5 例外處理

### 優點

- 雙層 ExceptionHandler：API JSON + Web 頁面
- 完整例外層級：`BusinessException` → `ResourceNotFoundException` / `ForbiddenException` 等
- errorCode 機制 + 安全的錯誤訊息（不洩漏內部資訊）

### 問題

| 嚴重度 | 問題 | 檔案 |
|--------|------|------|
| 🟡 | PlaceApiController/DirectionApiController 各自有 handleGoogleMapsException | 兩個 Controller |
| 🔵 | WebExceptionHandler `@Order(1)` 可移除 | `WebExceptionHandler.java:26` |

## 1.6 SOLID 原則

- **SRP**：PermissionChecker、DebtSimplifier、RouteOptimizer 等職責清晰
- **OCP**：外部服務介面 + Mock 模式（5 組）
- **LSP**：Mock 實作遵循介面契約
- **ISP**：客戶端介面精簡（StorageClient 3 方法、GeminiClient 2 方法）
- **DIP**：全部透過介面注入

## 1.7 DRY 原則

### 優點

- BaseWebController、ViewHelper、TripConstants、FileValidationUtils、CurrencyConverter 消除重複

### 問題

| 嚴重度 | 問題 | 檔案 |
|--------|------|------|
| 🟡 | `getUserMap()` 在 ExpenseService 和 SettlementService 重複 | 兩個 Service |
| 🟡 | TripController model 屬性回填代碼重複 3 處 | `TripController.java` |
| 🟡 | `buildPlaceLookup()` 在 ActivityService 和 ChatService 重複 | 兩個 Service |
| 🟡 | PlaceApi/DirectionApi 快取樣板 copy-paste | 兩個 Controller |
| 🔵 | `getFileExtension()` 在 TripService 和 DocumentService 重複 | 兩個 Service |

## 1.8 命名規範

嚴格遵循專案命名規範（Entity 單數、`{Domain}Service`、`{Domain}Controller` / `{Domain}ApiController`）。

| 嚴重度 | 問題 |
|--------|------|
| 🔵 | 混用英文與中文錯誤訊息，建議統一 |

## 1.9 Magic Number

大部分已提取為常數。

| 嚴重度 | 問題 | 檔案 |
|--------|------|------|
| 🟡 | ExpenseService 容差值 `"0.01"` 出現兩次 | `ExpenseService.java:519,540` |
| 🔵 | TripController 分頁參數 `50` 硬編碼 | `TripController.java:57-58` |
| 🔵 | TripViewHelper 預設座標（台北 101）| `TripViewHelper.java:138-139` |

## 1.10 日誌使用

全面使用 SLF4J `@Slf4j`，零 `System.out.println`，分級合理，結構化參數，無敏感資訊洩漏。無重大問題。

## 1.11 架構問題統計

| 嚴重度 | 數量 |
|--------|:----:|
| 🔴 Critical | 0 |
| 🟡 Warning | 11 |
| 🔵 Suggestion | 8 |

---

# 第 2 章：前端品質（8.4/10）

> 審查範圍：34 個 Thymeleaf 模板、15 個 JavaScript 模組、2 個 CSS 檔案
> 審查版本：`6141afd` (main branch)

## 2.1 各子項評分

| 維度 | 分數 | 前次 | 變化 |
|------|:----:|:----:|:----:|
| Thymeleaf 模板 | 8.5 | 9.0 | -0.5 |
| JavaScript 品質 | 8.5 | 7.5 | +1.0 |
| AJAX / 錯誤處理 | 9.0 | 9.0 | = |
| Google Maps | 9.0 | 9.0 | = |
| CSS 品質 | 8.0 | 8.5 | -0.5 |
| SEO / 無障礙 | 7.5 | — | = |

## 2.2 前次 Critical 問題追蹤

| 問題 | 狀態 |
|------|:----:|
| `personal-expense.js` 全域函式 (Critical) | **已修復** — IIFE `PersonalExpense` 模組 |
| `CATEGORY_LABELS` 重複定義 (Warning) | **已修復** — `common.js` → `WeGo.CATEGORY_LABELS` |
| `personal-expense.js` inline onclick (Warning) | **已修復** — `data-action` 事件委派 |

## 2.3 Thymeleaf 模板

**優點**: Fragment 系統完善（head, layout, components, chat-widget），權限檢查統一使用 boolean model 屬性。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 | `fragments/layout.html` | dashboard-layout 完整重複 app-layout header HTML | 抽成獨立 fragment |
| 🟡 | `dashboard.html` | bottom-nav 為 inline HTML | 改用 fragment 引入 |
| 🟡 | `activity/detail.html:409-441` | bottom-nav inline HTML 重複 | 改為 `th:replace` |
| 🟡 | `activity/list.html:411-442` | 同上 | 同上 |
| 🟡 | `settlement.html:37-38` | `trip.startDate`/`trip.endDate` 無 null check | 加入 `?.` |
| 🔵 | `activity/list.html:119-131` | 時間範圍計算在模板中 | 移至 ViewHelper |
| 🔵 | `activity/detail.html:177` | 停留時間格式化 SpEL 過於複雜 | ViewHelper 格式化 |

**XSS 防護**: 全部使用 `th:text`（無 `th:utext`），無 `th:onclick` 拼接。

## 2.4 JavaScript 品質

**15/15** 個 JS 檔案全部模組化完成。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 | `todo.js` (renderTodoItem) | onclick 字串拼接 | 改為 `data-action` 事件委派 |
| 🟡 | `todo/list.html:46-47,69-93` | 多處 `onclick="TodoUI.xxx()"` | 同上 |
| 🟡 | `components.html:84` | Dark mode toggle inline onclick | 改為 `data-action` |
| 🟡 | `error/404.html:50` | `onclick="history.back()"` | 改為 `data-action` |
| 🟡 | `trip/create.html:252-318` | 整段 inline `<script>` | 提取至 `trip-form.js` |
| 🟡 | `activity/detail.html:527-573` | inline `<script>` | 提取至外部 JS |
| 🟡 | `app.js` | ~1190 行，超過 800 行上限 | 拆分 WeatherUI、TimePicker、DatePicker |
| 🟡 | `personal-expense.js:618` | 殘留 `console.error` | 移除 |
| 🔵 | `common.js` 多處 | 函式內部使用 `var` | 改用 `const` |
| 🔵 | `todo.js` 多處 | 混用 `var` 和 `const`/`let` | 統一 |
| 🔵 | `app.js` 多模組 | 10+ 個模組掛載至 `window` | 收歸 `WeGo` 命名空間 |

## 2.5 AJAX 與錯誤處理

統一使用 `WeGo.fetchWithTimeout()`，CSRF 完備，Toast 錯誤顯示，防重複提交。

| 嚴重度 | 檔案 | 問題 |
|--------|------|------|
| 🟡 | `expense-list.js` (fetchExpenseDetail) | 取得單一支出時呼叫全量 API 再 filter |
| 🔵 | `sortable-reorder.js`, `settlement.js`, `chat.js` | 部分呼叫直接使用 `fetch()` 而非 `fetchWithTimeout` |

## 2.6 Google Maps 功能

搜尋 300ms debounce、伺服器端代理、hidden fields、外部點擊關閉。無重大問題。

## 2.7 CSS 品質

Tailwind CSS v3.4.19，暗色模式完整，RWD 全面。

| 嚴重度 | 檔案 | 問題 |
|--------|------|------|
| 🟡 | `expense/list.html:365-406` | inline `<style>` animation keyframes |
| 🟡 | `todo/list.html:440-456` | inline `<style>` |
| 🟡 | `expense/create.html:401-483` | 大量 inline `<style>` |
| 🟡 | `document/list.html:430-467` | inline `<style>` |
| 🔵 | `expense/create.html:480` | `!important` 覆蓋 Flatpickr |

## 2.8 SEO 與無障礙

**優點**: aria-label、focus trap、skip-to-content、prefers-reduced-motion、裝飾性 SVG `aria-hidden`。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | `fragments/head.html` 缺少 `<meta name="description">` |
| 🟡 | `dashboard.html`、`expense/list.html` 缺少 `<h1>` |
| 🔵 | Toast 未設 `role="status"` / `aria-live` |
| 🔵 | Landing page 缺少 OG / Twitter Card meta |
| 🔵 | todo modal 缺少 focus trap |

## 2.9 已完成的良好實踐

1. 模組化 15/15、零全域汙染
2. XSS 防護完備（全 `th:text` + `WeGo.escapeHtml()`）
3. CSRF 統一處理、fetchWithTimeout 30s 超時
4. 全站 dark mode + FOUC 防閃
5. `data-action` 事件委派、防止重複提交（表單層 + API 層）
6. prefers-reduced-motion 支援、Focus trap、Chart.js 記憶體管理
7. 共用常量去重（`WeGo.CATEGORY_LABELS`/`CATEGORY_COLORS`）
8. Google Maps API 伺服器端代理

## 2.10 前端問題統計

| 維度 | 🔴 | 🟡 | 🔵 | 小計 |
|------|:--:|:--:|:--:|:----:|
| Thymeleaf 模板 | 0 | 5 | 3 | 8 |
| JavaScript 品質 | 0 | 7 | 3 | 10 |
| AJAX / 錯誤處理 | 0 | 1 | 1 | 2 |
| Google Maps | 0 | 0 | 2 | 2 |
| CSS 品質 | 0 | 4 | 1 | 5 |
| SEO / 無障礙 | 0 | 2 | 3 | 5 |

---

# 第 3 章：效能（9.0/10）

> 審查範圍：Entity 關聯映射、FetchType、資料庫索引、分頁處理、N+1 查詢、快取策略、外部 API 呼叫、非同步處理、前端靜態資源、HikariCP 連線池

## 3.1 各子項評分

| 維度 | 分數 |
|------|:----:|
| Entity 設計 | 9.0 |
| 資料庫索引 | 8.5 |
| N+1 查詢 | 8.0 |
| 快取策略 | 9.5 |
| 外部 API 管理 | 8.5 |
| 非同步處理 | 7.5 |
| 前端資源 | 9.0 |
| 連線池/並行 | 9.0 |

## 3.2 Entity 關聯映射

所有 Entity 均使用扁平 UUID 欄位（非 JPA 關聯），完全避免 EAGER fetch、Hibernate Proxy 序列化、`LazyInitializationException` 問題。

## 3.3 資料庫索引

已建立完善索引覆蓋（Activity、Expense、ExpenseSplit、Document、Todo、Place、InviteLink、TripMember）。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | User 表缺少 `(provider, provider_id)` 複合索引，OAuth 登入核心查詢 |

## 3.4 分頁處理

正確使用分頁：TripRepository、ExpenseRepository、TodoRepository、DocumentRepository。

| 嚴重度 | 問題 | 檔案 |
|--------|------|------|
| 🟡 | `getExpensesByTrip` 載入行程所有支出 | `ExpenseService.java:155` |
| 🟡 | `getDocumentsByTrip` 不帶分頁 | `DocumentService.java:217` |
| 🔵 | `findByTripIdOrderedByDueDateAndStatus` 不帶分頁 | `TodoRepository.java:51` |

## 3.5 N+1 查詢

**已修復**: ExpenseService 批次、TripService 批次、ActivityService 批次、DocumentService 批次、ChatService 批次、PersonalExpenseService 3-query 模式、StatisticsCacheDelegate。

| 嚴重度 | 問題 | 檔案 |
|--------|------|------|
| 🟡 | 單筆 `buildExpenseResponse` 仍 3 次獨立查詢 | `ExpenseService.java:541-572` |
| 🟡 | `GlobalExpenseService.getUnsettledTrips` 潛在 N+1（M 行程 = 2M+2 查詢）| `GlobalExpenseService.java:110-123` |

## 3.6 快取策略

12 個 Caffeine 快取 + 2 個獨立管理快取，多層快取設計優秀。

| 快取名稱 | TTL | 用途 |
|----------|-----|------|
| `statistics-*` (3) | 5 min | 分類/趨勢/成員統計 |
| `exchange-rate*` (4) | 1h / 24h | 匯率主快取 + 降級 |
| `weather` | 6h | 天氣預報 |
| `places` | 5 min | 地點搜尋 |
| `directions` | 10 min | 路線規劃 |
| `settlement` | 1 min | 結算計算 |
| `permission-check` | 5s | 權限去重 |

## 3.7 外部 API 呼叫

共享連線池 Apache HttpClient 5（maxTotal=50, maxPerRoute=10），4/5 Client 有 Circuit Breaker。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | SupabaseStorageClient 仍使用獨立 `SimpleClientHttpRequestFactory`，未共享連線池，缺 Circuit Breaker |

## 3.8 非同步處理

`@EnableAsync` + `transportExecutor` 已配置。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | 批次重算交通 `batchRecalculateWithRateLimit` 同步阻塞（50 景點需 5 秒+）|

## 3.9 前端靜態資源

HTTP gzip 壓縮 + Content-hash 版本化 + 365 天長期快取。

| 嚴重度 | 問題 |
|--------|------|
| 🔵 | `common.js` 缺少 `defer` 屬性，阻塞頁面渲染 |

## 3.10 HikariCP 連線池

pool-size 10, minimum-idle 2, `open-in-view: false`。無需調整。

## 3.11 正面發現（25 項效能最佳實踐）

1. `open-in-view: false`
2. HikariCP 連線池配置合理
3. `@Transactional(readOnly = true)` 正確使用
4. TripService/ActivityService/ExpenseService/DocumentService/PersonalExpenseService 批次載入
5. PermissionChecker 5 秒 Caffeine 快取
6. ExchangeRateService 雙層快取
7. 4/5 外部 API Client Circuit Breaker
8. StatisticsCacheDelegate 正確解決 AOP self-invocation
9. DocumentService signedUrlCache
10. UUID 主鍵 + 應用層生成
11. Content-hash 版本化靜態資源 + 365 天快取
12. HttpClientConfig 共享連線池
13. Bucket4j per-IP Rate Limiting

## 3.12 效能問題統計

| 嚴重度 | 數量 |
|--------|:----:|
| 🔴 Critical | 0 |
| 🟡 Warning | 7 |
| 🔵 Suggestion | 4 |

---

# 第 4 章：安全性（8.5/10）

> 審查範圍：完整專案（Spring Boot 3.x + Thymeleaf + JavaScript）

## 4.1 SQL Injection

**結論：零風險** — 所有 `@Query` 使用 JPQL 參數綁定，無原生 SQL，無字串拼接查詢。

## 4.2 XSS

**結論：安全** — 零 `th:utext`，全部 `th:text` 自動轉義。前端 innerHTML 使用前均呼叫 `WeGo.escapeHtml()`。

| 嚴重度 | 問題 |
|--------|------|
| 🔵 | 天氣預報 alt 屬性未 escapeHtml（資料來源為受信任 API，風險極低）|
| 🔵 | todo.js onclick 字串拼接（UUID + enum，非使用者輸入）|

## 4.3 認證與授權

- OAuth2 Login（Google Provider）
- `@CurrentUser UserPrincipal` 統一取得身分
- `PermissionChecker` 集中式權限檢查（10 個 Service 注入）
- TestAuthController 僅在 `test`/`e2e` Profile 啟用

## 4.4 CSRF

`CookieCsrfTokenRepository` + cookie `XSRF-TOKEN` + header `X-XSRF-TOKEN`。前端 `WeGo.getCsrfToken()` 統一處理。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | `/api/weather/**` 在 CSRF 中未豁免但設為 permitAll（GET-only 無影響）|

## 4.5 Session 管理

`HttpOnly` + `Secure` + `SameSite=Lax` + `changeSessionId()` + `maximumSessions(1)`。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | Session 超時 7 天偏長 |

## 4.6 API Key 與機密管理

全部透過環境變數注入，無硬編碼。`.gitignore` 正確排除 `.env`、憑證檔、本地設定。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | Google Maps Embed Key 暴露於前端（標準用法，建議設 Referrer 限制）|

## 4.7 安全標頭

完整：CSP、HSTS、X-Frame-Options、Referrer-Policy、Permissions-Policy。生產環境隱藏錯誤詳情。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | CSP `script-src` 含 `unsafe-inline` |
| 🟡 | `img-src` 範圍較寬（`https:`）|

## 4.8 檔案上傳安全

MIME Type 白名單 + Magic Bytes 驗證 + 大小限制（單檔 10MB、總 30MB、行程 100MB）+ 權限檢查。

| 嚴重度 | 問題 |
|--------|------|
| 🔵 | HEIC 格式跳過 magic bytes 驗證 |

## 4.9 輸入驗證

所有 DTO 使用 Jakarta Bean Validation。AI Chat 有獨立 sanitize（零寬字元、控制字元、2KB 限制）。

## 4.10 速率限制

天氣 API 30/min (IP)、AI Chat 5/min (User)、全域 API Filter 層限制。

| 嚴重度 | 問題 |
|--------|------|
| 🟡 | `X-Forwarded-For` 可被偽造（已有安全註釋和長度檢查）|

## 4.11 日誌安全

無密碼、完整 token 或 API Key 被記錄。生產環境 `root: WARN`、`com.wego: INFO`。

## 4.12 安全問題統計

| 嚴重度 | 數量 |
|--------|:----:|
| 🔴 Critical | 0 |
| 🟡 Warning | 5 |
| 🔵 Suggestion | 3 |

---

# 第 5 章：測試覆蓋（8.5/10）

> 審查範圍：全專案單元測試、整合測試、E2E 測試

## 5.1 測試執行結果

| 指標 | 數值 |
|------|------|
| 測試總數 | **1,182** |
| 通過 / 失敗 / 錯誤 / 跳過 | 1,182 / 0 / 0 / 0 |
| 測試檔案數 | 88 |
| 執行時間 | ~30 秒 |
| JaCoCo 指令覆蓋率 | 76% |
| JaCoCo 分支覆蓋率 | 58% |

## 5.2 API 端點覆蓋

**104/104 端點 100% 測試覆蓋**（64 REST API + 40 Web 端點）。

### REST API 端點（14 Controllers, 64 端點）

| Controller | 端點數 | 覆蓋 |
|-----------|:------:|:----:|
| HealthController | 1 | ✅ |
| AuthApiController | 2 | ✅ |
| TripApiController | 12 | ✅ |
| ActivityApiController | 7 | ✅ |
| ExpenseApiController | 9 | ✅ |
| TodoApiController | 6 | ✅ |
| DocumentApiController | 8 | ✅ |
| PlaceApiController | 2 | ✅ |
| DirectionApiController | 1 | ✅ |
| StatisticsApiController | 3 | ✅ |
| ExchangeRateApiController | 4 | ✅ |
| WeatherApiController | 2 | ✅ |
| ChatApiController | 1 | ✅ |
| PersonalExpenseApiController | 6 | ✅ |

### Web 端點（15 Controllers, 40 端點）

| Controller | 端點數 | 覆蓋 |
|-----------|:------:|:----:|
| HomeController | 2 | ✅ |
| TripController | 6 | ✅ |
| ActivityWebController | 9 | ✅ |
| ExpenseWebController | 7 | ✅ |
| PersonalExpenseWebController | 4 | ✅ |
| TodoWebController | 1 | ✅ |
| DocumentWebController | 2 | ✅ |
| MemberWebController | 1 | ✅ |
| SettlementWebController | 1 | ✅ |
| InviteController | 2 | ✅ |
| ProfileController | 3 | ✅ |
| GlobalExpenseController | 1 | ✅ |
| GlobalDocumentController | 1 | ✅ |
| ErrorController | 1 | ✅ |

## 5.3 JaCoCo 覆蓋率

| 套件 | 指令覆蓋率 | 分支覆蓋率 |
|------|:----------:|:----------:|
| `domain.geo` | 100% | n/a |
| `domain.route` | 99% | 82% |
| `config` | 98% | 64% |
| `domain.permission` | 96% | 50% |
| `domain.settlement` | 94% | 83% |
| `security` | 90% | 77% |
| `controller.api` | 83% | 60% |
| `service` | 78% | 66% |
| `controller.web` | 72% | 56% |
| `entity` | 51% | 28% |
| `dto.request` | 16% | 37% |
| **全專案** | **76%** | **58%** |

> `dto.request` 和 `entity` 覆蓋率低因 Lombok 自動生成的 getter/setter/builder，實際業務邏輯覆蓋率更高。

## 5.4 測試檔案清單（88 檔）

- **Controller 測試**: 28 檔（14 API + 14 Web）
- **Service 測試**: 22 檔（含 3 ViewHelper）
- **External Client 測試**: 10 檔
- **Entity 測試**: 11 檔
- **Domain/Other 測試**: 13 檔
- **Repository 測試**: 4 檔
- **E2E 測試**: 12 spec 檔

## 5.5 E2E 覆蓋

| 測試檔案 | 流程 |
|----------|------|
| `auth.spec.ts` | OAuth2 登入 |
| `dashboard.spec.ts` | 儀表板 |
| `trip-crud.spec.ts` | 行程 CRUD |
| `activity-crud.spec.ts` | 景點 CRUD |
| `expense-crud.spec.ts` | 支出 CRUD |
| `todo-crud.spec.ts` | 待辦 CRUD |
| `document.spec.ts` | 文件上傳/下載 |
| `member.spec.ts` | 成員管理 |
| `settlement.spec.ts` | 分帳流程 |
| `invite.spec.ts` | 邀請連結 |
| `personal-expense.spec.ts` | 個人支出 |
| `profile.spec.ts` | 個人檔案 |

## 5.6 各子項評分

| 維度 | 分數 | 說明 |
|------|:----:|------|
| 覆蓋率完整度 | 8.5 | 76% 指令覆蓋率，104 端點 100% 覆蓋 |
| 測試品質 | 9.0 | `@Nested` 分組、`@DisplayName`，正反路徑皆測 |
| 邊界測試 | 7.5 | 基本完善，部分邊界可加強 |
| E2E 覆蓋 | 8.5 | 12 spec 覆蓋所有主要流程 |
| 測試可維護性 | 9.0 | 統一模式、共用 setup |
| 測試速度 | 9.0 | 1,182 測試 30 秒完成 |

## 5.7 建議

| 嚴重度 | 建議 |
|--------|------|
| 🔵 | 增加 integration test（`@DataJpaTest`）驗證 query 正確性 |
| 🔵 | 增加 edge case 測試（超大分頁、並發、極端值、邊界） |
| 🔵 | E2E Settlement 流程擴充更多場景 |
| 🔵 | 考慮 PIT Mutation Testing 驗證測試有效性 |

---

# 改善 Roadmap

## 立即（低成本、高影響）

| # | 項目 | 面向 |
|---|------|------|
| 1 | User 表加 `(provider, provider_id)` 複合索引 | 效能 |
| 2 | `common.js` 加 `defer` 屬性 | 效能 |
| 3 | ExpenseService 容差值 `"0.01"` 提取為常數 | 架構 |

## 短期（1-2 週）

| # | 項目 | 面向 |
|---|------|------|
| 4 | PlaceApi/DirectionApi 快取+速率限制下沉至 Service | 架構 |
| 5 | 消除 inline JS/CSS | 前端 |
| 6 | 重複代碼消除（getUserMap、getFileExtension、buildPlaceLookup）| 架構 |
| 7 | SupabaseStorageClient 改用共享連線池 | 效能 |
| 8 | todo.js inline onclick 改為 data-action | 前端 |
| 9 | 補齊 `<meta description>` 和 heading 層級 | 前端 |

## 中期（1-2 個月）

| # | 項目 | 面向 |
|---|------|------|
| 10 | TripService 職責拆分 | 架構 |
| 11 | Response DTO 不可變性（`@Data` → `@Getter` + `@Builder`）| 架構 |
| 12 | 批次重算交通非同步化 | 效能 |
| 13 | 錯誤訊息語言統一 | 架構 |
| 14 | CSP 移除 unsafe-inline | 安全 |
| 15 | 測試覆蓋率提升至 80% | 測試 |

## 長期（3+ 個月）

| # | 項目 | 面向 |
|---|------|------|
| 16 | Session 超時優化 | 安全 |
| 17 | `app.js` 拆分 | 前端 |
| 18 | Mutation Testing (PIT) | 測試 |
| 19 | WCAG 2.1 AA 合規 | 前端 |

---

# 歷史趨勢

| 日期 | 架構 | 安全 | 前端 | 效能 | 測試 | 整體 |
|------|:----:|:----:|:----:|:----:|:----:|:----:|
| 2026-02-06 | 5.5 | 7.0 | 6.0 | 6.5 | 5.0 | **7.0** |
| 2026-02-13 | 8.5 | 8.5 | 8.0 | 8.5 | 9.0 | **8.7** |
| 2026-02-14 | 8.6 | 8.9 | 7.5 | 7.0 | 8.5 | **8.1** |
| 2026-02-18 (審查) | 8.9 | 9.1 | 7.5 | 7.8 | 8.7 | **8.4** |
| 2026-02-18 (改善後) | 9.2 | 9.2 | 7.8 | 8.5 | 9.0 | **8.7** |
| 2026-02-20 (改善後) | 9.2 | 9.2 | 9.0 | 8.8 | 9.0 | **9.0** |
| 2026-02-20 (獨立審查) | 8.8 | 8.5 | 8.5 | 8.8 | 8.5 | **8.6** |
| **2026-02-21 (獨立審查)** | **8.6** | **8.5** | **8.4** | **9.0** | **8.5** | **8.6** |

> **2026-02-21 審查說明**：架構評分從 8.8 降至 8.6，反映更嚴格的分層審查發現 PlaceApi/DirectionApi Controller 業務邏輯洩漏。效能從 8.8 升至 9.0，因修正先前報告中的遺漏（外部 API 連線池和 Circuit Breaker 實際已到位）。整體維持 8.6 穩定。

---

# 結論

WeGo 專案整體品質優秀，綜合健康度 **8.6/10**。五個審查面向均無 Critical 問題：

- **零 SQL Injection、零 XSS（th:utext）、零 System.out**
- **1,182 測試全部通過**，104 端點 100% 覆蓋
- **多層快取架構**完善（12 Caffeine 快取 + StatisticsCacheDelegate）
- **外部 API 管理成熟**（共享連線池 + Circuit Breaker + Rate Limiting）
- **集中式權限檢查**（PermissionChecker）確保授權一致性
- **外部服務全面介面化**（5 組 Client + Mock），支援 fallback

主要改善方向集中在：架構分層（Controller 業務邏輯下沉）、前端技術債（inline JS/CSS、app.js 拆分）、效能微調（SupabaseStorageClient 連線池、批次重算非同步化）。這些問題均不影響功能正確性和安全性，可按 Roadmap 逐步改善。

專案架構設計成熟穩定，適合持續迭代開發。

---

*本報告由 5 位 Opus 4.6 Agent 獨立審查後由 Lead 彙整（2026-02-21）。*
