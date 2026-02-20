# WeGo 安全審查報告

> 審查日期：2026-02-20
> 審查範圍：完整專案（Spring Boot 3.x + Thymeleaf + JavaScript）
> 審查工具：人工程式碼審查

---

## 目錄

1. [SQL Injection](#1-sql-injection)
2. [XSS（跨站腳本攻擊）](#2-xss跨站腳本攻擊)
3. [認證與授權](#3-認證與授權)
4. [CSRF](#4-csrf)
5. [Session 管理](#5-session-管理)
6. [API Key 與機密管理](#6-api-key-與機密管理)
7. [.gitignore 與機密排除](#7-gitignore-與機密排除)
8. [CORS](#8-cors)
9. [安全標頭](#9-安全標頭)
10. [檔案上傳安全](#10-檔案上傳安全)
11. [輸入驗證](#11-輸入驗證)
12. [速率限制](#12-速率限制)
13. [日誌安全](#13-日誌安全)
14. [整體評分](#整體評分)

---

## 1. SQL Injection

**結論：低風險**

所有 `@Query` 查詢均使用 JPQL 參數綁定（`:param` 語法），無原生 SQL（`nativeQuery = true`），無 `JdbcTemplate`，無字串拼接查詢。

**檢查範圍：**
- `ExpenseSplitRepository.java` — 14 個 `@Query`，全部使用 `:param` 綁定
- `ActivityRepository.java` — 2 個 `@Query`，全部使用 `:param` 綁定
- `TodoRepository.java` — 2 個 `@Query`，全部使用 `:param` 綁定
- `TripRepository.java` — 2 個 `@Query`，全部使用 `:param` 綁定
- `InviteLinkRepository.java` — 3 個 `@Query`，全部使用 `:param` 綁定
- `DocumentRepository.java` — 5 個 `@Query`，全部使用 `:param` 綁定
- `ExpenseRepository.java` — 3 個 `@Query`，全部使用 `:param` 綁定

無發現 SQL Injection 風險。

---

## 2. XSS（跨站腳本攻擊）

### 2.1 Thymeleaf 模板

**結論：安全**

全域掃描結果：**零個** `th:utext` 使用，全部使用 `th:text`（自動 HTML 轉義）。Thymeleaf 預設的自動轉義機制提供了良好的 XSS 防護。

### 2.2 JavaScript innerHTML 使用

全域共有約 50 處 `innerHTML` 使用，需逐一評估：

#### 🔵 Suggestion — 天氣預報卡片未對 API 回傳值做 escapeHtml

**檔案：** `src/main/resources/static/js/app.js:572`

```javascript
alt="${forecast.description || forecast.condition}"
```

天氣 API 回傳的 `description` / `condition` 直接嵌入 HTML `alt` 屬性，未經 `escapeHtml`。雖然 `alt` 屬性在正常瀏覽器中不會執行腳本，但若 OpenWeatherMap API 回傳的值含有 `"` 字元可能破壞 HTML 結構。

**風險等級：** 極低（資料來源為受信任的外部 API，非使用者輸入）

**建議：** 使用 `WeGo.escapeHtml()` 包裝 `forecast.description`。

#### 🔵 Suggestion — todo.js 中使用 onclick 字串拼接

**檔案：** `src/main/resources/static/js/todo.js:673-678`

```javascript
const clickHandler = window.CAN_EDIT
    ? `onclick="TodoUI.showEditModal('${todo.id}')"`
    : '';
const toggleHandler = window.CAN_EDIT
    ? `onclick="TodoUI.toggleStatus('${todo.id}', '${todo.status}')"`
    : '';
```

`todo.id` 是 UUID（由後端產生），`todo.status` 是 enum 值，兩者均不受使用者控制，風險極低。但仍建議改用 `data-*` 屬性 + 事件委派模式，避免 inline event handler。

#### 安全的 innerHTML 使用

以下模組在使用 `innerHTML` 前均正確呼叫 `WeGo.escapeHtml()` 處理使用者可控資料：

- `todo.js` — `escapeHtml(todo.title)`, `escapeHtml(todo.description)`, `escapeHtml(todo.assigneeName)`
- `chat.js:504` — `formatReply` 先呼叫 `WeGo.escapeHtml(text)` 再做 Markdown 格式化
- `expense-statistics.js` — `escapeHtml(cat.category)`, `escapeHtml(member.nickname)`
- `activity-form.js` — `WeGo.escapeHtml(place.name)`, `WeGo.escapeHtml(place.address)`
- `route-optimizer.js` — `escapeHtml(placeName)`
- `app.js:84` — Toast 訊息使用 `escapeHtml(message)`
- `expense-list.js` — 使用 `textContent` 而非 `innerHTML` 設定使用者資料

僅靜態 SVG/樣式類 `innerHTML` 使用（loading spinner、圖示等）不含使用者資料，安全。

---

## 3. 認證與授權

### 3.1 認證機制

**結論：安全**

- OAuth2 Login 使用 Google Provider，配置正確
- `CustomOAuth2UserService` 處理使用者建立/更新
- Web Controller 使用 `@CurrentUser UserPrincipal` 取得身分（零 DB 查詢）
- API Controller 統一使用 `@CurrentUser UserPrincipal` 取得身分

**檔案：** `src/main/java/com/wego/config/SecurityConfig.java:84-101`

```java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**",
        "/favicon.ico", "/favicon.svg", "/.well-known/**",
        "/api/health", "/api/weather/**", "/api/test/auth/**"
    ).permitAll()
    .anyRequest().authenticated()
)
```

所有非公開端點均需認證，配置正確。

### 3.2 授權機制

**結論：安全且完善**

`PermissionChecker` (`src/main/java/com/wego/domain/permission/PermissionChecker.java`) 提供集中式權限檢查：

- `canView` — 任何成員
- `canEdit` — OWNER + EDITOR
- `canDelete` — 僅 OWNER
- `canManageMembers` — 僅 OWNER
- `canInvite` — OWNER + EDITOR

所有 Service 層（ActivityService、ExpenseService、DocumentService、TodoService、ChatService、PersonalExpenseService、InviteLinkService、TripService、SettlementService、StatisticsService）均注入 `PermissionChecker` 並在操作前執行權限檢查。

### 3.3 TestAuthController

**檔案：** `src/main/java/com/wego/controller/TestAuthController.java:38`

```java
@Profile({"test", "e2e"})
```

安全 — TestAuthController 僅在 `test` 或 `e2e` Profile 下啟用，生產環境不會載入。搭配 `@Profile` 註解確保測試端點不會暴露於生產。

### 3.4 Weather API 公開端點

**檔案：** `src/main/java/com/wego/controller/api/WeatherApiController.java`

安全 — 天氣 API 設計為公開端點，有獨立的 IP 速率限制（30/min），且不涉及敏感資料。

---

## 4. CSRF

**結論：安全**

**檔案：** `src/main/java/com/wego/config/SecurityConfig.java:47-51`

```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .ignoringRequestMatchers("/api/health", "/api/test/auth/**")
)
```

- 使用 `CookieCsrfTokenRepository`，cookie 名稱 `XSRF-TOKEN`，header `X-XSRF-TOKEN`
- 豁免端點合理：`/api/health`（唯讀健康檢查）、`/api/test/auth/**`（僅 test/e2e profile）
- 前端 `common.js` 提供 `WeGo.getCsrfToken()` 和 `WeGo.getCsrfHeader()` 工具函數

🟡 Warning — `/api/weather/**` 在 CSRF 中**未被豁免**但設為 `permitAll`。由於 Weather API 是 GET-only（`@GetMapping`），瀏覽器不會在 GET 請求中附帶 CSRF token 檢查，因此不影響功能。但若未來新增 POST 端點需注意。

---

## 5. Session 管理

**結論：安全**

**檔案：** `src/main/resources/application.yml:66-71`

```yaml
session:
  timeout: 7d
  cookie:
    http-only: true
    secure: true
    same-site: lax
```

- Session Cookie 設定 `HttpOnly`：防止 JavaScript 存取
- Session Cookie 設定 `Secure`：僅 HTTPS 傳輸
- `SameSite=Lax`：防止 CSRF 攻擊
- Session 固定攻擊防護：`changeSessionId()` 策略

**檔案：** `src/main/java/com/wego/config/SecurityConfig.java:120-124`

```java
.sessionManagement(session -> session
    .sessionFixation(fixation -> fixation.changeSessionId())
    .maximumSessions(1)
    .expiredUrl("/login?expired=true")
)
```

- 限制最大同時 Session 為 1（防止帳號共用）
- Session 過期後導向登入頁

🟡 Warning — Session 超時設定為 7 天，對於旅遊規劃應用可能合理（使用者可能數天才回來），但較一般應用的 30 分鐘偏長。建議評估是否適合專案需求。

---

## 6. API Key 與機密管理

**結論：大致安全**

所有 API Key 均透過環境變數注入：

```yaml
# application.yml
google-maps:
  api-key: ${GOOGLE_MAPS_API_KEY:}
  embed-api-key: ${GOOGLE_MAPS_EMBED_API_KEY:}
openweathermap:
  api-key: ${OPENWEATHERMAP_API_KEY:}
exchangerate:
  api-key: ${EXCHANGERATE_API_KEY:}
gemini:
  api-key: ${GEMINI_API_KEY:}
```

無硬編碼的 API Key。全域掃描 `sk-`、`AIza`、`eyJ`、`ghp_` 等模式均未匹配。

### 🟡 Warning — Google Maps Embed API Key 暴露於前端

**檔案：** `src/main/java/com/wego/config/GlobalModelAttributes.java:20-43`

```java
@Value("${wego.external-api.google-maps.embed-api-key:}")
private String googleMapsApiKey;

@ModelAttribute("googleMapsApiKey")
public String googleMapsApiKey() {
    return googleMapsApiKey;
}
```

**檔案：** `src/main/resources/templates/activity/detail.html:81-82`

```html
<iframe th:src="'https://www.google.com/maps/embed/v1/place?key=' + ${googleMapsApiKey} + ..."
```

Google Maps Embed API Key 暴露於前端 HTML 中。這是 Google Maps Embed API 的標準用法（此 Key 設計為公開使用），但建議：

1. 確認使用的是**僅限 Embed 的獨立 Key**（`embed-api-key`），而非主要的 Maps API Key
2. 在 Google Cloud Console 中設定 **HTTP Referrer 限制**，僅允許生產網域
3. 限制此 Key 僅啟用 Maps Embed API

---

## 7. .gitignore 與機密排除

**結論：安全且完善**

**檔案：** `.gitignore`

正確排除：
- `.env`、`.env.local`、`.env.*.local` — 環境變數檔
- `*.pem`、`*.key`、`*.p12` — 憑證檔
- `credentials.json` — 認證檔
- `application-dev.yml`、`application-local.yml` — 本地設定
- `.claude/` — Claude Code 設定
- `.idea/`、`.vscode/` — IDE 設定

驗證結果：`application-dev.yml` 確認**未被追蹤** (`git ls-files` 無結果)。

---

## 8. CORS

**結論：安全**

全域掃描結果：**無** `CorsConfiguration`、`@CrossOrigin`、或 CORS 相關配置。

Spring Security 預設不允許跨域請求，等同最嚴格的 CORS 政策。專案作為 Server-Side Rendering (Thymeleaf) 應用，不需要 CORS 配置。

---

## 9. 安全標頭

**結論：優秀**

**檔案：** `src/main/java/com/wego/config/SecurityConfig.java:53-81`

| 標頭 | 設定 | 評估 |
|------|------|------|
| `X-Frame-Options` | `SAMEORIGIN` | 防止 Clickjacking |
| `Content-Security-Policy` | 完整設定 | 見下方分析 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 安全預設 |
| `Permissions-Policy` | `geolocation=(self), camera=(), microphone=()` | 限制瀏覽器功能 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | 強制 HTTPS |

### CSP 分析

```
default-src 'self';
script-src 'self' 'unsafe-inline' https://unpkg.com https://cdn.jsdelivr.net;
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net;
font-src 'self' https://fonts.gstatic.com data:;
img-src 'self' https: data: blob:;
connect-src 'self' https://unpkg.com https://lottie.host https://cdn.jsdelivr.net;
frame-src 'self' https://www.google.com https://maps.google.com https://*.supabase.co;
frame-ancestors 'self'
```

🟡 Warning — `script-src` 包含 `'unsafe-inline'`，削弱了 CSP 對 XSS 的防護能力。理想情況下應使用 nonce 或 hash，但 Thymeleaf 與 Tailwind CSS 的整合通常需要 `unsafe-inline`。

🟡 Warning — `img-src` 設為 `https: data: blob:`，允許載入任何 HTTPS 來源的圖片。對於使用者上傳頭像和地圖顯示是必要的，但範圍較寬。

### 生產環境額外設定

**檔案：** `src/main/resources/application-prod.yml`

```yaml
server:
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never
```

正確隱藏錯誤詳情，防止資訊洩漏。

---

## 10. 檔案上傳安全

**結論：安全**

**檔案：** `src/main/java/com/wego/service/DocumentService.java:57-62`
**檔案：** `src/main/java/com/wego/domain/file/FileValidationUtils.java`

安全措施：
1. **MIME Type 白名單** — 僅允許 `application/pdf`、`image/jpeg`、`image/png`、`image/heic`
2. **Magic Bytes 驗證** — 使用 `FileValidationUtils.matchesMagicBytes()` 驗證檔案實際內容與宣告的 MIME Type 一致
3. **檔案大小限制** — 單檔 10MB、總請求 30MB (`application.yml:76-77`)
4. **行程總容量限制** — 每個行程 100MB (`wego.document.max-total-size-mb: 100`)
5. **權限檢查** — 上傳前檢查使用者是否有編輯權限

Magic bytes 驗證覆蓋 PDF（`%PDF`）、JPEG（`FFD8FF`）、PNG（`89504E47`）、WebP（`RIFF...WEBP`），有效防止 Content-Type 欺騙。

🔵 Suggestion — HEIC 格式跳過 magic bytes 驗證（`FileValidationUtils.java:51`），原因是 HEIC 容器格式複雜。如需更嚴格驗證，可考慮使用 Apache Tika 做 HEIC 內容偵測。

---

## 11. 輸入驗證

**結論：完善**

### Bean Validation

所有 DTO Request 均使用 Jakarta Bean Validation 註解：

| 註解 | 使用 | 範例 |
|------|------|------|
| `@NotBlank` | 必填字串 | `CreateTripRequest.title` |
| `@NotNull` | 必填物件 | `CreateExpenseRequest.amount` |
| `@Size` | 字串長度 | `max=200`, `max=1000` |
| `@Min` / `@Max` | 數值範圍 | `day >= 1`, `duration <= 1440` |
| `@Pattern` | 格式驗證 | 幣別 `^[A-Z]{3}$` |
| `@Valid` | 巢狀驗證 | `CreateExpenseRequest.splits` |

所有 API Controller 均在參數上使用 `@Valid` 觸發驗證。

### AI Chat 輸入清理

**檔案：** `src/main/java/com/wego/service/ChatService.java:432-463`

- `sanitizeUserMessage()` — 移除零寬字元、控制字元、限制 2KB 位元組
- `sanitizeField()` — 移除換行/tab/格式字元、多空格合併、長度截斷

有效防止 prompt injection 和大量文字攻擊。

---

## 12. 速率限制

**結論：完善**

| 端點 | 限制 | 實作 |
|------|------|------|
| 天氣 API | 30/min (IP) | `WeatherApiController` 內建 |
| AI Chat | 5/min (User) | `ChatService` + `RateLimitService` |
| 全域 API | Filter 層限制 | `RateLimitConfig` |

速率限制使用 Caffeine cache，記憶體效率高。

🟡 Warning — `X-Forwarded-For` 可被偽造。

**檔案：** `src/main/java/com/wego/config/RateLimitConfig.java:142-161`

```java
// Security note: X-Forwarded-For can be spoofed.
String xForwardedFor = request.getHeader("X-Forwarded-For");
```

程式碼中已有安全註釋，且有長度檢查（防止超長標頭）。在反向代理（如 Railway）後方使用時，`X-Forwarded-For` 的第一個 IP 通常可信。建議在部署時確認反向代理會**覆寫**（而非追加）此標頭。

---

## 13. 日誌安全

**結論：安全**

全域掃描日誌中是否包含 password、token、secret、key 等敏感字串：

- `MockStorageClient.java` — 記錄 storage key（檔案路徑），非敏感
- `ExchangeRateApiClient.java:83` — 記錄 `API key configured: true/false`（布林值），未洩漏實際 Key
- `GeminiClientImpl.java:202` — 記錄 token 截斷警告，非敏感
- `InviteController.java:112` — 記錄邀請連結的 token preview（截斷版本）

無密碼、完整 token 或 API Key 被記錄到日誌中。

**生產環境日誌等級：**

```yaml
# application-prod.yml
logging:
  level:
    root: WARN
    com.wego: INFO
```

生產環境降低日誌等級，減少敏感資訊洩漏風險。

---

## 14. 其他安全面向

### JPA Open-in-View

**檔案：** `src/main/resources/application.yml:36`

```yaml
open-in-view: false
```

已關閉 `open-in-view`，避免意外的延遲載入和資料庫連接洩漏。

### 伺服器錯誤處理

**檔案：** `src/main/resources/application.yml:82-84`

```yaml
server:
  error:
    include-message: never
    include-binding-errors: never
```

預設隱藏錯誤訊息（dev profile 除外）。

### Logout 安全

**檔案：** `src/main/java/com/wego/config/SecurityConfig.java:113-118`

```java
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
)
```

登出時正確銷毀 session 並清除 cookie。

---

## 問題摘要

| # | 嚴重程度 | 問題 | 位置 |
|---|---------|------|------|
| 1 | 🟡 Warning | CSP `script-src` 含 `unsafe-inline` | `SecurityConfig.java:60` |
| 2 | 🟡 Warning | Session 超時 7 天偏長 | `application.yml:67` |
| 3 | 🟡 Warning | `img-src` 範圍較寬（`https:`） | `SecurityConfig.java:63` |
| 4 | 🟡 Warning | Google Maps Embed Key 暴露於前端 | `GlobalModelAttributes.java:20`、`detail.html:82` |
| 5 | 🟡 Warning | `X-Forwarded-For` 可被偽造（已有註釋） | `RateLimitConfig.java:154` |
| 6 | 🔵 Suggestion | 天氣預報 alt 屬性未 escapeHtml | `app.js:572` |
| 7 | 🔵 Suggestion | todo.js 使用 onclick 字串拼接 | `todo.js:673-678` |
| 8 | 🔵 Suggestion | HEIC 跳過 magic bytes 驗證 | `FileValidationUtils.java:51` |

---

## 整體評分

**安全評分：8.5 / 10**

### 優勢
- **零 SQL Injection 風險** — 全面使用 JPQL 參數綁定
- **零 th:utext** — Thymeleaf 全面使用自動轉義
- **完善的 XSS 防護** — 前端一致使用 `WeGo.escapeHtml()` 處理使用者資料
- **集中式權限檢查** — `PermissionChecker` 提供一致的授權邏輯
- **完整的安全標頭** — CSP、HSTS、X-Frame-Options、Referrer-Policy、Permissions-Policy
- **檔案上傳雙重驗證** — MIME Type 白名單 + Magic Bytes 驗證
- **輸入驗證完善** — 所有 DTO 使用 Bean Validation、AI Chat 有獨立清理
- **機密管理正確** — 全部透過環境變數、無硬編碼
- **完善的速率限制** — 天氣 API、AI Chat、全域 API 均有限制
- **Profile 隔離** — TestAuthController 僅在 test/e2e 啟用

### 可改進空間
- CSP 移除 `unsafe-inline`（需配合 nonce 機制）
- Session 超時可考慮縮短或加入 idle timeout
- 確認 Google Maps Embed Key 已在 GCP 設定 Referrer 限制

### 無 🔴 Critical 問題

專案在安全方面展現了高度的安全意識和一致的實作品質。所有發現的問題均為 Warning 或 Suggestion 等級，無需緊急修復。
