# Security Review Report - WeGo

**Reviewer**: security-reviewer (automated)
**Date**: 2026-02-12
**Branch**: main
**Scope**: Full codebase security audit

---

## Executive Summary

The WeGo project has a **solid security foundation** with proper OAuth2 authentication, CSRF protection, CSP headers, input validation, and a well-structured exception handler that avoids leaking stack traces. The codebase uses JPA with JPQL (no raw SQL or native queries), eliminating SQL injection risk. No `th:utext` usage was found, eliminating XSS through Thymeleaf. Rate limiting is implemented at both the global (per-IP) and per-user levels for external API proxies.

**Key risks** center around the Google Maps API key being exposed to the browser, overly permissive CSP with `unsafe-inline`, production `ddl-auto: update`, verbose error messages in production, and the weather API being unauthenticated without per-IP rate limiting.

| Severity | Count |
|----------|-------|
| Critical | 1 |
| Warning  | 6 |
| Suggestion | 5 |

---

## Detailed Findings

### 1. SQL Injection

**Status: PASS**

All database queries use Spring Data JPA with JPQL named parameters (`:paramName`). No `nativeQuery = true`, no `JdbcTemplate`, no string concatenation in queries found.

All `@Query` annotations (30+ across repositories) use parameterized JPQL exclusively.

---

### 2. XSS (Cross-Site Scripting)

**Status: PASS**

- Zero `th:utext` usage found in all Thymeleaf templates.
- All user-facing output uses `th:text` (auto-escaped).
- No `th:onclick` string concatenation patterns found.

---

### 3. Authentication & Authorization

#### 3a. Google Maps API Key Exposed to Frontend

| | |
|---|---|
| **Severity** | Warning |
| **File** | `src/main/java/com/wego/config/GlobalModelAttributes.java:41` |
| **Template** | `src/main/resources/templates/activity/detail.html:102` |

The Google Maps API key is injected into all Thymeleaf models via `@ModelAttribute("googleMapsApiKey")` and rendered directly in an iframe `src` attribute. This key is visible in browser page source.

**Recommendation**: Restrict the Google Maps API key via Google Cloud Console to specific HTTP referrers and the Maps Embed API only. Alternatively, proxy map requests through the backend.

#### 3b. Weather API Has No Authentication

| | |
|---|---|
| **Severity** | Warning |
| **File** | `src/main/java/com/wego/config/SecurityConfig.java:50` |
| **File** | `src/main/java/com/wego/controller/api/WeatherApiController.java` |

`/api/weather/**` is publicly accessible (no auth required) and has no per-IP rate limiting at the controller level. The global `RateLimitConfig` filter covers `/api/*` patterns, which mitigates this partially, but the weather endpoint could still be abused to generate costs on the OpenWeatherMap API.

**Recommendation**: Either require authentication for weather endpoints or add explicit per-IP rate limiting similar to PlaceApiController.

#### 3c. Test Auth Controller Profile Guard

| | |
|---|---|
| **Severity** | Suggestion |
| **File** | `src/main/java/com/wego/controller/TestAuthController.java:38` |

The `TestAuthController` is correctly guarded by `@Profile({"test", "e2e"})` and the endpoint is CSRF-exempt. This is safe as long as production never activates these profiles. The controller also exposes `sessionId` in its response (line 120), which is acceptable for test profiles only.

**Recommendation**: Add a startup check or integration test that verifies `TestAuthController` is NOT loaded in the default/prod profile.

#### 3d. No `@PreAuthorize` or Method-Level Security

| | |
|---|---|
| **Severity** | Suggestion |
| **File** | All API controllers |

`@EnableMethodSecurity` is declared but no `@PreAuthorize`, `@Secured`, or `@RolesAllowed` annotations are used. Authorization is enforced at the service layer by passing `userId` to services that check trip membership. This is a valid pattern but relies entirely on service-layer checks being correct.

**Recommendation**: Consider adding `@PreAuthorize` annotations for critical operations (delete trip, remove member) as defense-in-depth.

---

### 4. CSRF Protection

**Status: PASS with notes**

| | |
|---|---|
| **Severity** | Suggestion |
| **File** | `src/main/java/com/wego/config/SecurityConfig.java:48-51` |

CSRF uses `CookieCsrfTokenRepository.withHttpOnlyFalse()` which is correct for SPA/AJAX patterns. CSRF is exempted for `/api/health` and `/api/test/auth/**` -- both are reasonable exemptions. The weather endpoint (`/api/weather/**`) was previously CSRF-exempt but has been removed from the exemption list, which is correct since it only handles GET requests (CSRF protection applies to state-changing methods only).

---

### 5. Security Headers

#### 5a. CSP Allows `unsafe-inline` for Scripts and Styles

| | |
|---|---|
| **Severity** | Warning |
| **File** | `src/main/java/com/wego/config/SecurityConfig.java:60-61` |

The Content-Security-Policy includes `'unsafe-inline'` for both `script-src` and `style-src`. This significantly weakens XSS protection since inline scripts would be allowed to execute.

**Recommendation**: Migrate to nonce-based CSP (`'nonce-{random}'`) for inline scripts. For styles, `unsafe-inline` is more acceptable since Tailwind CSS utilities and inline styles are common.

#### 5b. Broad `img-src` Directive

| | |
|---|---|
| **Severity** | Suggestion |
| **File** | `src/main/java/com/wego/config/SecurityConfig.java:63` |

`img-src 'self' https: data: blob:` allows images from any HTTPS source. This is intentionally broad for user avatars (Google) and map tiles but could be narrowed.

---

### 6. Sensitive Data Exposure

#### 6a. Error Messages Include Details in Production

| | |
|---|---|
| **Severity** | Critical |
| **File** | `src/main/resources/application.yml:83-84` |

`application.yml` 中 `server.error.include-message` 和 `server.error.include-binding-errors` 皆設為 `always`。此設定會導致 Spring Boot 預設錯誤頁面和 JSON 回應包含完整錯誤訊息與 binding error 細節。在生產環境中，可能洩漏內部實作細節、類別名稱和資料庫 Schema 資訊。

**Recommendation**: 生產環境應設為 `never` 或 `on_param`，並確保 `include-stacktrace` 也設為 `never`。

Note: The `GlobalExceptionHandler` handles most API exceptions well (with generic messages for 500 errors), but any unhandled exception paths or web controller errors will fall through to Spring Boot's default error handling, which will expose details.

#### 6b. Hibernate DDL-Auto Set to `update` in Production Config

| | |
|---|---|
| **Severity** | Warning |
| **File** | `src/main/resources/application.yml:28` |

`jpa.hibernate.ddl-auto` 設為 `update`，在生產環境中可能導致非預期的 Schema 變更和潛在資料遺失。JPA Entity 變更可能會靜默修改資料庫約束。

**Recommendation**: Use `validate` or `none` for production. Use migration tools (Flyway/Liquibase) for schema changes.

#### 6c. Verbose SQL Logging in Production Config

| | |
|---|---|
| **Severity** | Warning |
| **File** | `src/main/resources/application.yml:92-93` |

`org.hibernate.SQL` 設為 `DEBUG`，`BasicBinder` 設為 `TRACE`。生產環境中的 DEBUG/TRACE SQL 日誌會輸出完整 SQL 查詢和綁定參數值，可能暴露敏感資料。

**Recommendation**: Set to `WARN` or `ERROR` for production.

---

### 7. Session Management

**Status: PASS**

- Session timeout: 7 days (reasonable for a travel app)
- Cookie settings: `http-only: true`, `secure: true`, `same-site: lax` -- all correct
- Maximum sessions per user: 1 (good)
- Logout invalidates session and deletes JSESSIONID cookie

---

### 8. Secrets Management

**Status: PASS**

- All sensitive values (`DATABASE_URL`, `SUPABASE_SERVICE_KEY`, `GOOGLE_CLIENT_SECRET`, API keys) use `${ENV_VAR}` environment variable substitution
- `.gitignore` properly excludes `.env`, `*.pem`, `*.key`, `credentials.json`, and local config files
- No hardcoded API keys, passwords, or tokens found in source code
- Test/E2E profiles use clearly fake values (`test-client-id`, `e2e-api-key`)

---

### 9. CORS Configuration

**Status: Not explicitly configured**

| | |
|---|---|
| **Severity** | Suggestion |
| **File** | N/A |

No explicit CORS configuration found (`CorsConfiguration`, `@CrossOrigin`, or `cors` in config). Spring Security defaults to no CORS allowed, which is correct for a server-rendered Thymeleaf app. If the REST API is intended for external consumption later, CORS would need to be configured.

---

### 10. File Upload Security

**Status: PASS（已改善）**

| | |
|---|---|
| **Severity** | 已修復 |
| **File** | `src/main/resources/application.yml:77-78` |

`max-file-size` 為 10MB，`max-request-size` 已從 100MB 降至 30MB，更符合安全需求。原先 100MB 的設定過高，可能被用於記憶體耗盡攻擊。

The document upload controller properly validates file types (PDF, JPEG, PNG) and sizes at the service layer, and the preview endpoint sets `X-Content-Type-Options: nosniff` and a sandbox CSP -- both excellent practices.

---

### 11. Invite Token Security

**Status: PASS**

The `InviteController` properly validates token format with regex (`[A-Za-z0-9_-]+`), enforces max length (64 chars), checks expiry, and validates membership. No open redirect vulnerabilities -- all redirects use hardcoded path prefixes (`/trips/`, `/dashboard`, `/login`).

---

## Summary Statistics

| Category | Status |
|----------|--------|
| SQL Injection | PASS - All JPQL parameterized |
| XSS | PASS - No th:utext, all output escaped |
| Authentication | PASS - OAuth2 + session-based |
| Authorization | PASS (service-layer enforced) |
| CSRF | PASS |
| Session Management | PASS |
| Secrets Management | PASS |
| CORS | PASS (default deny) |
| Invite Security | PASS |
| CSP Headers | WARNING - unsafe-inline |
| Error Disclosure | CRITICAL - production error details exposed |
| DDL-Auto | WARNING - should be validate/none in prod |
| SQL Logging | WARNING - verbose in production |
| Weather API | WARNING - unauthenticated |
| API Key Exposure | WARNING - Google Maps key in templates |
| File Upload | PASS - max-request-size 已降至 30MB（已修復） |
| Rate Limiting | PASS - Global + per-user |

---

## Priority Recommendations

1. **[CRITICAL]** Change `server.error.include-message` and `include-binding-errors` to `never` for production
2. **[HIGH]** Change `ddl-auto` to `validate` for production; use Flyway/Liquibase
3. **[HIGH]** Remove DEBUG/TRACE SQL logging in production config
4. **[MEDIUM]** Migrate CSP from `unsafe-inline` to nonce-based for scripts
5. **[MEDIUM]** Restrict Google Maps API key via referrer restrictions in Google Cloud Console
6. **[LOW]** Add rate limiting to weather API or require authentication
