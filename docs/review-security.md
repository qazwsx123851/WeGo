# WeGo 安全審查報告

> 審查日期：2026-02-18（第三次審查，基於 2026-02-14 版本更新）
> 審查範圍：Spring Boot 3.x + Thymeleaf + JavaScript + Supabase
> 審查員：security-reviewer agent

---

## 審查摘要

| 分類 | 🔴 Critical | 🟡 Warning | 🔵 Suggestion |
|------|:-----------:|:----------:|:-------------:|
| SQL Injection | 0 | 0 | 0 |
| XSS | 0 | 1 | 1 |
| 認證與授權 | 0 | 0 | 2 |
| CSRF | 0 | 0 | 0 |
| Session 管理 | 0 | 0 | 1 |
| API Key / 機密保護 | 0 | 1 | 0 |
| CSP / Security Headers | 0 | 2 | 0 |
| 依賴安全 | 0 | 1 | 0 |
| .gitignore | 0 | 0 | 0 |
| CORS | 0 | 0 | 0 |
| **小計** | **0** | **5** | **4** |

> 與上次審查（2026-02-14）相比：
> - 天氣 API 已加入 IP-based 速率限制（`WeatherApiController.java:63`，30/min）— Warning #2 已解決
> - Session Fixation 已明確設定 `changeSessionId()`（`SecurityConfig.java:121`）— Suggestion #4 已解決
> - HSTS 已設定（`SecurityConfig.java:78-81`，31536000s + includeSubDomains）— Suggestion #6 已解決
> - Google Maps Embed API Key 暴露於前端 HTML 為新增 Warning

---

## 1. SQL Injection

**結論：安全 (0 問題)**

所有 `@Query` 查詢均使用 JPQL 參數綁定（`:param` 語法），無原生 SQL（`nativeQuery = true`），無 `JdbcTemplate`，無字串拼接查詢。

已檢查的 Repository（28+ 個 `@Query`）：
- `ExpenseSplitRepository.java`（11 個 @Query）
- `ActivityRepository.java`（2 個 @Query）
- `TodoRepository.java`（2 個 @Query）
- `InviteLinkRepository.java`（3 個 @Query）
- `TripRepository.java`（2 個 @Query）
- `DocumentRepository.java`（6 個 @Query，含 LIKE 搜尋使用 `CONCAT('%', :search, '%')` 安全綁定）
- `ExpenseRepository.java`（3 個 @Query）

---

## 2. XSS (Cross-Site Scripting)

### 2.1 Thymeleaf 模板

**結論：安全** - 零 `th:utext` 使用，所有動態內容均使用 `th:text` 安全渲染。無 `th:onclick` 字串拼接。

### 🟡 Warning #1: Chat AI 回應透過 innerHTML 渲染

**檔案**: `src/main/resources/static/js/chat.js:366`、`chat.js:451`

```javascript
p.innerHTML = formatReply(text);  // Line 366
p.innerHTML = formatted;          // Line 451
```

**分析**: `formatReply()` 函式先透過 `WeGo.escapeHtml()` 跳脫所有使用者/AI 文字（`chat.js:504`），再進行 Markdown 格式化。`inlineMd()` 函式（`chat.js:550-552`）僅處理 `**bold**` 替換。跳脫在格式化之前，注入向量被阻擋。但若 AI 回覆包含特殊格式可能產生意外 HTML。

**風險**: 低。`escapeHtml()` 使用 DOM-based 跳脫（`textContent` → `innerHTML`），涵蓋所有 5 個關鍵 HTML entity。

**建議**: 考慮引入 DOMPurify 作為最後一道防線，在 `innerHTML` 賦值前清理 HTML。

### 🔵 Suggestion #1: 大量 innerHTML 用於靜態 UI 元件

**檔案**: 多個 Thymeleaf 模板和 JS 檔案（約 50+ 處）

大多數 innerHTML 用途為注入靜態 SVG 圖示或 UI 元件。渲染使用者資料的位置均正確使用 `escapeHtml()` 跳脫：
- `todo.js:665-694` — todo 標題、描述、指派人名稱
- `activity/create.html:752-758` — 地點搜尋結果
- `expense-statistics.js:393-394` — 成員暱稱與頭像
- `route-optimizer.js:290` — 地點名稱

---

## 3. 認證與授權

### 3.1 天氣 API（已改善）

**檔案**: `src/main/java/com/wego/controller/api/WeatherApiController.java:63`

天氣 API（`/api/weather/**`）雖為公開端點（`permitAll()`），但已加入 IP-based 速率限制（30/min），透過 `RateLimitService` 實現。此為本次審查確認已修復的項目。

### 🔵 Suggestion #2: TestAuthController 安全隔離正確

**檔案**: `src/main/java/com/wego/controller/TestAuthController.java:38`

```java
@Profile({"test", "e2e"})
```

測試用認證 bypass 端點正確使用 `@Profile` 限制。Response 中包含 `sessionId`（line 120），僅在測試環境中可接受。

**建議**: 新增整合測試驗證 `TestAuthController` 在 prod profile 下不會被載入。

### 🔵 Suggestion #3: 啟用但未使用 Method-Level Security

**檔案**: `src/main/java/com/wego/config/SecurityConfig.java:27`

```java
@EnableMethodSecurity
```

已宣告但未使用任何 `@PreAuthorize`、`@Secured` 或 `@RolesAllowed` 註解。授權完全由 Service 層的 `PermissionChecker` 處理（已驗證所有 9 個 Service 均正確注入使用）。

**建議**: 考慮對關鍵操作（刪除行程、移除成員）加入 `@PreAuthorize` 作為縱深防禦。

---

## 4. CSRF 設定

**結論：安全 (0 問題)**

**檔案**: `src/main/java/com/wego/config/SecurityConfig.java:47-51`

- `CookieCsrfTokenRepository.withHttpOnlyFalse()` - 正確（JS 需讀取 token）
- 豁免路徑：`/api/health`（無副作用）、`/api/test/auth/**`（僅 test/e2e profile）
- 前端正確透過 `WeGo.getCsrfToken()` 附加 CSRF token
- Session cookie 設定 `same-site: lax` 提供額外 CSRF 防護

---

## 5. Session 管理

### 🔵 Suggestion #4: Session Timeout 設定為 7 天

**檔案**: `src/main/resources/application.yml:67`

7 天的 session timeout 對旅遊規劃應用合理，但偏長。Session cookie 設定正確：`http-only: true`、`secure: true`、`same-site: lax`。Logout 正確 invalidate session 並刪除 JSESSIONID cookie。

已改善：
- Session Fixation 已明確設定 `changeSessionId()`（`SecurityConfig.java:121`）
- 最大 session 數為 1，過期重導至 `/login?expired=true`

---

## 6. API Key / 機密保護

### 🟡 Warning #2: Google Maps Embed API Key 暴露於前端 HTML

**檔案**: `src/main/resources/templates/activity/detail.html:82`、`src/main/java/com/wego/config/GlobalModelAttributes.java:41-43`

```html
th:src="'https://www.google.com/maps/embed/v1/place?key=' + ${googleMapsApiKey} + '&q=...'"
```

Google Maps Embed API Key 透過 `GlobalModelAttributes` 注入所有模板，並直接出現在 iframe src URL 中。雖然 Embed API Key 設計上可暴露於前端（Google 建議透過 HTTP referrer 限制），但需確認：

1. 該 Key 確實是 Embed-only API Key（非 Maps JavaScript API Key）
2. 已在 Google Cloud Console 設定 HTTP referrer 限制
3. 該 Key 未啟用其他高風險 API（如 Geocoding、Directions 等付費 API）

**建議**: 在 Google Cloud Console 確認 API Key 限制，僅允許 Maps Embed API + 特定 referrer domain。

### 其他正面發現

- 所有其他 API Key 均通過環境變數注入（`${ENV_VAR:}` 或 `${ENV_VAR}`），無硬編碼
- 前端 JavaScript 中無其他 API Key 暴露
- `ExchangeRateApiClient.sanitizeError()` 從錯誤訊息移除 API Key
- Production profile：`include-message: never`、`include-stacktrace: never`
- `.gitignore` 完整排除 `.env`、`application-local.yml`、`application-dev.yml`
- Test/E2E 使用明確假值（`e2e-client-id`、`e2e-api-key`）

---

## 7. CSP / Security Headers

### 🟡 Warning #3: CSP script-src 使用 'unsafe-inline'

**檔案**: `src/main/java/com/wego/config/SecurityConfig.java:60`

```java
"script-src 'self' 'unsafe-inline' https://unpkg.com https://cdn.jsdelivr.net; "
```

`'unsafe-inline'` 大幅降低 CSP 對 XSS 的防護效果。

**建議**: 中期目標：遷移至 nonce-based CSP，將所有 inline script 抽取為外部檔案。

### 🟡 Warning #4: CSP style-src 使用 'unsafe-inline'

**檔案**: `src/main/java/com/wego/config/SecurityConfig.java:61`

Style-src 的 `'unsafe-inline'` 風險較低（CSS injection 攻擊面有限）。Tailwind CSS 生成大量 utility class，短期可接受。

### 已改善

- **HSTS** 已設定（`SecurityConfig.java:78-81`）：`maxAgeInSeconds=31536000`、`includeSubDomains=true`
- **X-Frame-Options**: `sameOrigin()`（line 56）
- **Referrer-Policy**: `STRICT_ORIGIN_WHEN_CROSS_ORIGIN`（line 71）
- **Permissions-Policy**: `geolocation=(self), camera=(), microphone=()`（line 75）
- **X-Content-Type-Options**: Spring Security 預設啟用 `nosniff`

---

## 8. 依賴安全

### 🟡 Warning #5: Spring Boot 版本偏舊

**檔案**: `pom.xml:10`

```xml
<version>3.2.2</version>
```

Spring Boot 3.2.2 發佈於 2024-01。建議升級至最新 3.2.x patch 或 3.3.x 以獲得安全修復。

其他依賴版本合理：`caffeine`（Spring 管理）、`postgresql`（Spring 管理）。

---

## 9. .gitignore

**結論：安全 (0 問題)**

`.gitignore` 配置完善，已驗證下列檔案均未被 git 追蹤：
- `.env` 及所有變體
- `application-local.yml`、`application-dev.yml`
- `*.pem`、`*.key`、`*.p12`、`credentials.json`
- IDE 設定、build 產出、log 檔案

---

## 10. CORS

**結論：安全 (0 問題)**

未發現 `@CrossOrigin`、`CorsConfiguration` 或任何 CORS 設定。應用為 server-rendered Thymeleaf，所有 API 呼叫來自同源前端，無跨域問題。Spring Security 預設不允許 CORS。

---

## 11. 檔案上傳安全

**結論：安全 (0 問題)**

**檔案**: `src/main/java/com/wego/service/DocumentService.java:442-467`

- MIME 類型白名單：僅允許 `application/pdf`、`image/jpeg`、`image/png`
- Magic bytes 驗證：`FileValidationUtils.matchesMagicBytes()` 檢查實際檔案內容是否與宣告的 MIME 類型一致（防止偽造 Content-Type）
- 檔案大小限制：單檔 10MB、request 30MB
- Trip 儲存限制：100MB/trip
- 儲存檔名使用 `UUID.randomUUID()` 生成，防止路徑遍歷
- 預覽端點設定安全 header：`X-Content-Type-Options: nosniff`、`Content-Security-Policy: sandbox`

---

## 12. 其他正面發現

### 12.1 Docker 安全最佳實踐
- 多階段構建，runtime image 不含 JDK/原始碼
- 非 root 使用者 `wego` 運行
- Alpine 最小化攻擊面
- `-Djava.security.egd=file:/dev/./urandom` 避免熵不足

### 12.2 AI Chat 安全防護
- System prompt 含指令注入防禦（`ChatService.java:88-92`）
- `sanitizeField()` 清理 DB 欄位中的控制字元和零寬字元（`ChatService.java:449-464`）
- `sanitizeUserMessage()` 限制 UTF-8 byte length 2000 bytes + 移除控制字元（`ChatService.java:432-442`）
- 結構性分離：行程資料在 user message 而非 system prompt（防止 stored prompt injection）
- Rate limiting（`RateLimitService`，可配置 per-minute limit）
- Circuit breaker（3 failures → open，5 min cooldown）
- Max output tokens 限制防止 OOM

### 12.3 輸入驗證
- 所有 API Controller 的 `@RequestBody` 均使用 `@Valid`（已確認 16 處）
- DTO 層完整的 Bean Validation：`@NotBlank`、`@Size`、`@Min`、`@Max`、`@Pattern`（已確認 50+ 個 annotation）
- `PlaceApiController` 額外 sanitize 搜尋查詢和 place ID
- `UserService` 暱稱長度限制和清理
- `DocumentService` 檔案類型/大小/magic bytes 三重驗證
- Currency 欄位使用 `@Pattern("^[A-Z]{3}$")` 限制

### 12.4 邀請連結安全
- Token 格式驗證（`[A-Za-z0-9_-]+`，最長 64 字元）
- 過期檢查
- 無 open redirect 漏洞（所有重導向使用硬編碼路徑前綴）

### 12.5 PermissionChecker 覆蓋完整
所有 9 個相關 Service 均正確注入 `PermissionChecker`：
`ActivityService`、`ExpenseService`、`DocumentService`、`TodoService`、`TripService`、`InviteLinkService`、`ChatService`、`SettlementService`、`StatisticsService`

### 12.6 Production Profile 安全設定
- `server.error.include-message: never`（`application-prod.yml:4`）
- `server.error.include-binding-errors: never`（`application-prod.yml:5`）
- `server.error.include-stacktrace: never`（`application-prod.yml:6`）
- `jpa.hibernate.ddl-auto: validate`（`application-prod.yml:12`）
- `show-sql: false`（`application-prod.yml:13`）
- Logging level: `root: WARN`、`com.wego: INFO`

---

## 安全評分

| 維度 | 分數 (1-10) | 說明 |
|------|:-----------:|------|
| SQL Injection | 10 | 全部 JPQL 參數綁定，無原生 SQL |
| XSS | 8.5 | 全面 escapeHtml + 無 th:utext，但 CSP unsafe-inline 降低防護 |
| 認證與授權 | 9.5 | PermissionChecker 覆蓋完整，天氣 API 已加入速率限制 |
| CSRF | 9.5 | Cookie-based token 正確設定，前端整合良好 |
| Session 管理 | 9.5 | 完整設定，cookie 安全屬性正確，Session Fixation 已明確設定 |
| 機密保護 | 9 | 全部環境變數，prod 不洩漏細節，但 Embed API Key 需確認限制 |
| Security Headers | 8.5 | CSP 存在但有 unsafe-inline，HSTS 已設定 |
| 依賴安全 | 8 | 版本可更新但無已知 Critical CVE |
| 容器安全 | 9.5 | 多階段構建 + 非 root 用戶 |
| AI 安全 | 9 | Prompt injection 防禦 + rate limit + circuit breaker |
| 檔案上傳 | 9.5 | MIME 白名單 + magic bytes 驗證 + 大小限制 |

### 總體安全評分：**9.1 / 10**（較上次 8.9 提升，因修復天氣 API 速率限制、HSTS、Session Fixation）

---

## 改善建議（按優先順序）

| 優先級 | 建議 | 工時估計 |
|--------|------|----------|
| 🟡 中期 | 移除 CSP `unsafe-inline`，遷移至 nonce-based CSP | 2-3 天 |
| 🟡 短期 | 確認 Google Maps Embed API Key 已設定 referrer 限制 | 15 分鐘 |
| 🟡 短期 | 升級 Spring Boot 至最新 3.2.x patch 或 3.3.x | 0.5-1 天 |
| 🔵 低優先 | 關鍵操作加入 `@PreAuthorize` 縱深防禦 | 1 天 |
| 🔵 低優先 | 新增 prod profile TestAuthController 隔離測試 | 0.5 天 |
| 🔵 低優先 | 引入 DOMPurify 作為 chat innerHTML 最後防線 | 0.5 天 |
