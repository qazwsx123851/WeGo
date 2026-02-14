# WeGo 開發貢獻指南

> 最後更新: 2026-02-13 | 自動生成自 pom.xml 和 .env.example
>
> **變更日誌**:
> - 2026-02-13: AI 旅遊聊天機器人 — Gemini API、安全強化（prompt injection 防護、circuit breaker、Unicode 驗證、OOM 修復）
> - 2026-02-13: 檔案管理頁效能優化 — Signed URL CDN 直連、Document N+1 批次查詢、Caffeine 快取
> - 2026-02-12: 例外處理收緊 (24→19 catch blocks)、模板 head fragment 統一 (27/27)、ViewHelper 單元測試、表單防重複提交
> - 2026-02-12: Auth 遷移 (`@CurrentUser UserPrincipal`)、新增 ViewHelper/common.js、Web Controller 測試全覆蓋、PermissionChecker 快取
> - 2026-02-12: 更新測試統計、移除程式碼範例、Service/E2E 表格更新
> - 2026-02-11: 新增 CSRF Token 使用方式、前後端開發注意事項、API 對照表連結
> - 2026-02-04: Phase 4 完成 - 安全強化、深色模式、E2E 測試、無障礙支援
> - 2026-02-03: Phase 3 完成 - 多幣別匯率、統計圖表、債務簡化
> - 2026-02-02: 遷移至 Google Routes API、修復 CI 測試、暫停自動部署
> - 2026-02-01: 新增 Transport Mode 系統、全域概覽頁面、Profile 頁面、統一錯誤處理
> - 2026-01-28: 新增 spring-dotenv 自動載入環境變數

## 技術棧

| 層級 | 技術 | 版本 |
|------|------|------|
| 後端 | Spring Boot | 3.2.2 |
| Java | OpenJDK | 17 |
| 前端模板 | Thymeleaf | (Spring Boot managed) |
| 安全 | Spring Security + OAuth2 | (Spring Boot managed) |
| CSS 框架 | Tailwind CSS | 3.4.1 |
| 動畫 | Lottie-web | CDN |
| 資料庫 | PostgreSQL (Supabase) | 15+ |
| 建置工具 | Maven | 3.9.x |
| Node.js | (Frontend build) | 20.11.0 |
| 環境變數 | spring-dotenv | 4.0.0 |
| Rate Limiting | Bucket4j | 8.7.0 |
| 覆蓋率 | JaCoCo | 0.8.11 |
| E2E 測試 | Playwright | 1.40+ |

---

## 開發進度

| Phase | 狀態 | 功能 | 測試 |
|-------|:----:|------|------|
| Phase 1 | ✅ | OAuth 登入、行程 CRUD、景點管理、交通預估 | Unit |
| Phase 2 | ✅ | 權限模型、代辦事項、智慧排序、天氣預報 | Unit |
| Phase 3 | ✅ | 多幣別匯率、統計圖表、債務簡化 | Unit |
| Phase 4 | ✅ | 安全強化、深色模式、E2E 測試、無障礙 | Unit + E2E |

**測試統計**: 1060 單元測試 (79 個測試檔案) + 11 個 E2E spec (Playwright)

---

## 開發環境設定

### 前置需求

- JDK 17+
- Maven 3.9+ (或使用專案內的 `./mvnw`)
- Node.js 20+ (Maven 會自動下載)
- PostgreSQL 資料庫 (建議使用 Supabase)

### 環境變數設定

複製 `.env.example` 為 `.env` 並設定：

| 變數 | 必須 | 說明 | 格式範例 |
|------|:----:|------|----------|
| `DATABASE_URL` | ✅ | PostgreSQL 連線 URL | `jdbc:postgresql://host:5432/db` |
| `DATABASE_USERNAME` | ✅ | 資料庫使用者 | `postgres` |
| `DATABASE_PASSWORD` | ✅ | 資料庫密碼 | |
| `SUPABASE_URL` | ✅ | Supabase 專案 URL | `https://xxx.supabase.co` |
| `SUPABASE_SERVICE_KEY` | ✅ | Supabase **Service Role** Key | `eyJhbGciOiJIUzI1NiIs...` |
| `GOOGLE_CLIENT_ID` | ✅ | Google OAuth Client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | ✅ | Google OAuth Secret | `GOCSPX-xxx` |
| `GOOGLE_MAPS_API_KEY` | ❌ | Google Maps API (可選) | |
| `GOOGLE_MAPS_ENABLED` | ❌ | 啟用 Google Maps | `true` / `false` |
| `GOOGLE_MAPS_USE_ROUTES_API` | ❌ | 使用 Routes API (推薦) | `true` / `false` |
| `OPENWEATHERMAP_API_KEY` | ❌ | 天氣 API (可選) | |
| `OPENWEATHERMAP_ENABLED` | ❌ | 啟用天氣 API | `true` / `false` |
| `EXCHANGERATE_API_KEY` | ❌ | 匯率 API (可選) | |
| `EXCHANGERATE_ENABLED` | ❌ | 啟用匯率 API | `true` / `false` |
| `GEMINI_API_KEY` | ❌ | Gemini AI API Key (可選) | |
| `GEMINI_ENABLED` | ❌ | 啟用 AI 聊天 | `true` / `false` |

### 載入環境變數

專案使用 `spring-dotenv` 自動載入 `.env` 檔案，無需手動 export。只需複製 `.env.example` 並填入實際值，然後直接啟動即可。

> **注意**: 單元測試使用 H2 記憶體資料庫，不需要 .env 檔案。

---

## Maven 指令參考

| 指令 | 說明 |
|------|------|
| `./mvnw spring-boot:run` | 啟動開發伺服器 (port 8080) |
| `./mvnw test` | 執行單元測試 |
| `./mvnw verify` | 執行測試 + 整合測試 |
| `./mvnw clean package -DskipTests` | 建置 JAR (略過測試) |
| `./mvnw jacoco:report` | 產生測試覆蓋率報告 |
| `./mvnw compile -Dskip.frontend=true` | 編譯 (略過前端建置) |
| `./mvnw checkstyle:check` | 程式碼風格檢查 |

### E2E 測試指令

E2E 測試位於 `e2e/` 目錄，使用 Playwright 框架：

| 指令 | 說明 |
|------|------|
| `cd e2e && npm install` | 安裝 Playwright |
| `npx playwright test` | 執行所有 E2E 測試 |
| `npx playwright test --ui` | 開啟測試 UI |
| `npx playwright test auth.spec` | 執行特定測試檔 |
| `npx playwright show-report` | 查看測試報告 |

### 覆蓋率報告位置

覆蓋率報告產生於 `target/site/jacoco/index.html`。

### 前端建置 (Tailwind CSS)

前端建置整合在 Maven 生命週期中，會自動執行。手動執行時，進入 `src/main/frontend` 目錄執行 `npm install` 與 `npm run build`（輸出至 `dist/styles.css`），開發時可使用 `npm run watch` 監聽變更。

> **重要**: 若在 JavaScript 中動態使用 Tailwind class，需在 `input.css` 中定義組件類別或在 `tailwind.config.js` 中使用 safelist，否則 JIT 模式不會生成對應的 CSS。

---

## 開發工作流程

### 1. 建立分支

分支命名規則: `feature/P{phase}-{module}-{id}-{description}`

### 2. TDD 開發

遵循 RED-GREEN-REFACTOR 循環：先撰寫失敗的測試，再撰寫最小程式碼使測試通過，最後重構並保持測試通過。

### 3. 執行測試

使用 `./mvnw test` 執行所有單元測試。

### 4. 確認覆蓋率

使用 `./mvnw jacoco:report` 產生覆蓋率報告，目標為整體覆蓋率 >= 80%。

### 5. 提交變更

Commit 類型: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

---

## 測試規範

### 測試覆蓋率目標

| 層級 | 目標 |
|------|------|
| Service | >= 80% |
| Domain | >= 90% |
| Controller | >= 70% |
| **整體** | **>= 80%** |

### 測試命名規範

測試方法採用 `methodName_scenario_expectedBehavior` 格式命名，例如 `createTrip_withValidInput_shouldReturnCreatedTrip` 或 `createTrip_withNullTitle_shouldThrowValidationException`。

### 測試設定檔

測試使用 H2 記憶體資料庫，設定在 `src/test/resources/application-test.yml`

---

## 專案結構

```
src/
├── main/
│   ├── java/com/wego/
│   │   ├── WegoApplication.java    # Spring Boot 入口
│   │   ├── config/                 # 設定類別
│   │   ├── controller/
│   │   │   ├── web/                # 頁面控制器 (含 DocumentWebController, MemberWebController)
│   │   │   └── api/                # REST API
│   │   ├── service/                # 業務邏輯
│   │   ├── repository/             # 資料存取
│   │   ├── entity/                 # JPA 實體
│   │   ├── dto/                    # 資料傳輸物件
│   │   ├── domain/                 # 領域邏輯
│   │   ├── exception/              # 例外處理
│   │   └── security/               # OAuth2 相關
│   ├── resources/
│   │   ├── templates/              # Thymeleaf 模板 (27 個)
│   │   ├── static/                 # 靜態資源 (7 個 JS 模組)
│   │   └── application.yml
│   └── frontend/                   # Tailwind CSS 原始碼
└── test/
    └── java/com/wego/             # 測試類別 (74 個測試檔案)
```

---

## 認證系統

### Google OAuth 登入流程

```
使用者 ──▶ /login ──▶ Google OAuth ──▶ /login/oauth2/code/google
                                              │
                                              ▼
                                    CustomOAuth2UserService
                                              │
                              ┌───────────────┼───────────────┐
                              ▼               ▼               ▼
                        新使用者          舊使用者         錯誤
                              │               │               │
                              ▼               ▼               ▼
                         建立 User      更新 User      登入失敗
                              │               │
                              └───────┬───────┘
                                      ▼
                              UserPrincipal 放入 SecurityContext
                                      │
                                      ▼
                              重導向至 /dashboard
```

### API 端點

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/auth/me` | GET | 取得目前使用者資訊 |
| `/api/auth/logout` | POST | 登出 (API 版本) |
| `/logout` | GET | 登出 (Web 版本) |

### 使用 @CurrentUser 註解

在 Controller 方法參數中使用 `@CurrentUser UserPrincipal principal` 註解即可取得當前登入使用者的 ID 和 Email。

---

## 核心服務架構

### Service 層級

| Service | 職責 | 依賴 |
|---------|------|------|
| `UserService` | 使用者管理、OAuth 建立/更新 | UserRepository |
| `TripService` | 行程 CRUD、成員管理 | TripRepository, TripMemberRepository |
| `InviteLinkService` | 邀請連結建立/接受/管理 | InviteLinkRepository, TripMemberRepository |
| `ActivityService` | 景點 CRUD、排序、拖曳重排 | ActivityRepository, TransportCalculationService |
| `ExpenseService` | 支出記錄、分帳計算 | ExpenseRepository, SettlementService |
| `SettlementService` | 債務結算 | DebtSimplifier |
| `TodoService` | 代辦事項管理 | TodoRepository |
| `DocumentService` | 檔案上傳/下載/預覽（Signed URL 快取、批次查詢） | StorageClient, CacheManager |
| `TransportCalculationService` | 交通時間/距離計算、批次重算 | GoogleMapsClient, PlaceRepository |
| `GlobalExpenseService` | 跨行程支出統計 | ExpenseRepository, TripMemberRepository |
| `GlobalDocumentService` | 跨行程文件管理 | DocumentRepository, TripMemberRepository |
| `ExchangeRateService` | 匯率查詢與轉換 (8 種貨幣) | ExchangeRateClient, CacheManager |
| `StatisticsService` | 支出統計分析 (分類/趨勢/成員) | ExpenseRepository |
| `WeatherService` | 天氣預報 (5天) | WeatherClient, CacheManager |
| `PlaceService` | 地點查詢與建立 (find-or-create) | PlaceRepository |
| `ActivityViewHelper` | Activity 顯示邏輯（分組、日期、交通驗證） | - (純邏輯) |
| `ExpenseViewHelper` | Expense 顯示邏輯（分組、人均、分帳） | - (純邏輯) |
| `PermissionChecker` | 角色權限檢查 (含請求級 Caffeine 快取) | TripMemberRepository, CacheManager |
| `ChatService` | AI 聊天 prompt 組裝、輸入清理、Gemini 整合 | GeminiClient, TripRepository, ActivityRepository, PlaceRepository |
| `RateLimitService` | 應用層限流（Caffeine 滑動視窗） | Caffeine Cache |
| `CustomOAuth2UserService` | OAuth2 使用者處理 | UserRepository |
| `WebExceptionHandler` | Web 錯誤頁面處理 | - |

### Domain 層級 (核心演算法)

| Domain | 演算法 | 說明 |
|--------|--------|------|
| `RouteOptimizer` | Greedy Nearest Neighbor | 路線優化，O(n^2) 時間複雜度 |
| `DebtSimplifier` | Greedy Debt Settlement | 最小化交易次數的債務簡化 |
| `PermissionChecker` | Role-based | 基於角色的權限檢查 |
| `ExpenseAggregator` | Aggregation | 支出資料聚合與統計 |

### 外部服務整合

| Client | 外部 API | Fallback |
|--------|----------|----------|
| `GoogleMapsClientImpl` | Google Routes API (computeRouteMatrix) | MockGoogleMapsClient (Haversine) |
| `GoogleMapsClientImpl` | Google Places API (New) - searchText/details | MockGoogleMapsClient |
| `OpenWeatherMapClient` | OpenWeatherMap 5-day | MockWeatherClient |
| `SupabaseStorageClient` | Supabase Storage | MockStorageClient |
| `ExchangeRateApiClient` | ExchangeRate-API | MockExchangeRateClient (固定匯率) |
| `GeminiClientImpl` | Gemini API (gemini-2.5-flash) | MockGeminiClient (固定回覆) |

#### Google Routes API 遷移 (2026-02-02)

專案已從 Distance Matrix API 遷移至 Routes API：

| 特性 | Distance Matrix API (舊) | Routes API (新) |
|------|-------------------------|-----------------|
| Endpoint | `/distanceMatrix/json` | `/distanceMatrix/v2:computeRouteMatrix` |
| 認證 | URL 參數 `key=` | Header `X-Goog-Api-Key` |
| 功能 | 基本距離/時間 | 支援交通偏好、詳細資訊 |
| Fallback | N/A | TRANSIT 至 DRIVING 自動降級 |

環境變數 `GOOGLE_MAPS_USE_ROUTES_API` 設為 `true` 使用新 API（推薦），設為 `false` 則使用舊 Distance Matrix API。

新增 DTO 包括 `TransitPreferences`（交通偏好設定）、`TransitDetails`（轉乘詳細資訊）、`DirectionResult.ApiSource`（區分 API 來源）。

### Transport Mode 系統

交通模式系統支援多種運輸方式，並追蹤計算來源和警告：

**TransportMode 枚舉**:
| 模式 | 說明 | 支援 API 自動計算 |
|------|------|:------------------:|
| `WALKING` | 步行 | Yes |
| `TRANSIT` | 大眾運輸 | Yes |
| `DRIVING` | 開車 | Yes |
| `BICYCLING` | 騎車 | Yes |
| `FLIGHT` | 飛機 | No (手動輸入) |
| `HIGH_SPEED_RAIL` | 高鐵 | No (手動輸入) |
| `NOT_CALCULATED` | 不計算 | N/A |

**TransportSource 枚舉** (計算來源追蹤):
| 來源 | 說明 | Badge 樣式 |
|------|------|------------|
| `GOOGLE_API` | Google Maps 精確路線 | 綠色 |
| `HAVERSINE` | 直線距離估算 | 藍色 |
| `MANUAL` | 使用者手動輸入 | 紫色 |
| `NOT_APPLICABLE` | 不適用 (首個景點) | 灰色 |

**TransportWarning 枚舉** (警告提示):
| 警告 | 觸發條件 | 嚴重度 |
|------|----------|--------|
| `NONE` | 無問題 | - |
| `ESTIMATED_DISTANCE` | 使用 Haversine 估算 | info |
| `UNREALISTIC_WALKING` | 步行超過 5 km | warning |
| `UNREALISTIC_BICYCLING` | 騎車超過 30 km | warning |
| `VERY_LONG_DISTANCE` | 任何模式超過 100 km | warning |
| `NO_ROUTE_AVAILABLE` | Google API 無路線 | warning |

---

---

## CSRF Token 使用方式

### 基本設定

| 項目 | 值 |
|------|---|
| Cookie 名稱 | `XSRF-TOKEN` |
| Header 名稱 | `X-XSRF-TOKEN` |
| Repository | `CookieCsrfTokenRepository` (Spring default for cookie-based) |
| CSRF 豁免 | `/api/health`, `/api/weather/**`, `/api/test/auth/**` |

### 前端 JavaScript 使用方式

從 Thymeleaf 模板注入的 `<meta>` 標籤取得 CSRF token。共用工具函式位於 `static/js/common.js`，提供 `WeGo.getCsrfToken()` 和 `WeGo.getCsrfHeader()` 取得 CSRF token/header 名稱、`WeGo.fetchWithTimeout()` 附帶 30 秒 timeout 的 fetch 包裝器，以及 `WeGo.preventDoubleSubmit()` 防止表單重複提交（自動套用於 `data-prevent-double-submit` 屬性的表單）。所有 JS 模組已統一委託至 `common.js` 的共用實作。

### 規則

- 所有 `POST`/`PUT`/`DELETE` 請求**必須**附帶 CSRF token
- `GET` 請求**不需要** CSRF token (附帶也不影響，但屬於不必要的開銷)
- HTML form 使用 Thymeleaf `th:action` 會自動附帶 hidden CSRF input

---

## 前後端開發注意事項

### 雙介面並行架構

系統同時提供 Web Form 和 REST API 兩套介面：

| 介面 | Controller 位置 | 參數綁定 | 回傳格式 |
|------|----------------|----------|----------|
| Web Form | `controller/web/*` | `@RequestParam` | HTML (Thymeleaf) |
| REST API | `controller/api/*` | `@RequestBody` (JSON) | JSON (`ApiResponse`) |

> 新增 API endpoint 時，需考慮是否同時需要前端呼叫，或僅作為 API-only (供未來 mobile/SPA 使用)。
> 詳細前後端 API 對照表請參考 [api-reference.md](./api-reference.md)。

### API 路徑模式不一致

Activity/Expense 的 CRUD 路徑存在不一致：

| 操作 | 路徑模式 | 範例 |
|------|----------|------|
| 建立/列表 | `/api/trips/{tripId}/[resource]` | `POST /api/trips/{id}/activities` |
| 更新/刪除 | `/api/[resource]/{resourceId}` | `PUT /api/activities/{id}` |

這是有意的設計 (update/delete 透過 resourceId 即可唯一識別)，但開發新端點時請保持一致。

### 認證方式

| 介面 | 識別使用者方式 | 取得方式 |
|------|---------------|----------|
| Web Controller | `@CurrentUser UserPrincipal` | `principal.getUser()` (零 DB 查詢) |
| API Controller | `sub` attribute | `UUID.fromString(principal.getAttribute("sub"))` |

> **注意**: Web Controller 已於 2026-02-12 從 `@AuthenticationPrincipal OAuth2User` + `getUserByEmail()` 遷移至 `@CurrentUser UserPrincipal` + `principal.getUser()`，消除每次 Web 請求的額外 DB 查詢。

---

## 新增功能指南

### 全域概覽頁面

新增了跨行程的全域概覽功能：

| 頁面 | 路由 | Controller | 說明 |
|------|------|------------|------|
| 支出總覽 | `/expenses` | `GlobalExpenseController` | 所有行程的支出統計、待結算清單 |
| 文件總覽 | `/documents` | `GlobalDocumentController` | 所有行程的文件搜尋、篩選 |
| 個人檔案 | `/profile` | `ProfileController` | 使用者資料、統計、暱稱編輯 |

### 統一錯誤處理

專案使用雙層錯誤處理架構：

| Handler | 範圍 | 回傳格式 |
|---------|------|----------|
| `GlobalExceptionHandler` | API Controllers | JSON (ApiResponse) |
| `WebExceptionHandler` | Web Controllers | HTML (error/error.html) |

錯誤頁面特色：
- 動態圖示依錯誤類型變化 (404/403/401/500)
- 中文友善錯誤訊息
- 技術詳情可展開檢視
- 支援深色模式

### Activity 拖曳重排

景點列表支援拖曳重新排序：
- 使用原生 HTML5 Drag & Drop API
- 自動重新計算交通時間
- 支援跨日拖曳（變更日期）
- 前端即時回饋 + 後端持久化

### 批次重算交通時間

提供一鍵重新計算所有景點的交通資訊：
- 使用 Lottie 動畫顯示進度
- Google Maps API Rate Limiting (100ms 間隔)
- 超過限額自動降級為 Haversine 估算
- 返回詳細統計 (API 成功數、估算數、跳過數)

### 深色模式

支援深色模式切換：
- Navbar 右上角切換按鈕 (太陽/月亮圖示)
- 自動偵測系統偏好設定
- FOUC (Flash of Unstyled Content) 防護
- Chart.js 圖表主題自動響應
- localStorage 記住使用者偏好

**技術實作**:
- Tailwind CSS `darkMode: 'class'`
- `head` 內嵌同步腳本避免閃爍
- `themechange` 自訂事件通知圖表重繪

### 安全強化 (Phase 4)

Phase 4 完成的安全修復：

| 類別 | 修復項目 |
|------|----------|
| 認證 | API Controllers 認證檢查 |
| 授權 | IDOR 跨行程資料洩漏防護 |
| XSS | DOM XSS (innerHTML) 修復 |
| 驗證 | 分帳金額/百分比驗證 |
| Rate Limiting | Caffeine cache 防止記憶體耗盡 |
| 資料清理 | 刪除行程時級聯刪除相關資料 |
| Prompt Injection | 結構分離 — 行程資料移至 user message，sanitizeField 清理 |
| AI 回覆截斷 | maxOutputTokens (1500) + 5000 字安全截斷 |
| Circuit Breaker | Gemini API 連續 3 次失敗斷路，5 分鐘冷卻 |
| Unicode 繞過 | 零寬度字元清理 + 2KB byte 驗證 |
| OOM 防護 | RateLimitService ConcurrentHashMap → Caffeine |

### E2E 測試架構

Playwright E2E 測試覆蓋以下流程：

| 測試檔 | 測試數 | 覆蓋範圍 |
|--------|:------:|----------|
| `auth.spec.ts` | 26 | 登入/登出、OAuth Mock |
| `trip.spec.ts` | 18 | 行程 CRUD、邀請連結 |
| `activity.spec.ts` | 18 | 景點管理、拖曳重排 |
| `expense.spec.ts` | 12 | 支出記錄、分帳、統計 |
| `document.spec.ts` | 15 | 文件上傳、搜尋 |
| `todo.spec.ts` | 14 | 代辦事項管理 |
| `dark-mode.spec.ts` | 28 | 深色模式切換、持久化 |
| `member.spec.ts` | 8 | 成員管理、角色變更 |
| `settlement.spec.ts` | 6 | 結算流程 |
| `profile.spec.ts` | 5 | 個人檔案編輯 |
| `chat.spec.ts` | TBD | AI 聊天功能 |
| **總計** | **TBD** | 11 個 spec 檔案 |

---

## 相關文件

| 文件 | 說明 |
|------|------|
| [api-reference.md](./api-reference.md) | 前後端 API 對照表 |
| [RUNBOOK.md](./RUNBOOK.md) | 部署與維運手冊 |
| [requirements.md](./requirements.md) | 需求規格書 (PRD) |
| [software-design-document.md](./software-design-document.md) | 軟體設計文件 |
| [test-cases.md](./test-cases.md) | 測試案例規格書 |
| [tdd-guide.md](./tdd-guide.md) | TDD 開發指南 |
| [ui-design-guide.md](./ui-design-guide.md) | UI 設計指南 |
| [api-keys-setup.md](./api-keys-setup.md) | API Keys 設定指南 |
