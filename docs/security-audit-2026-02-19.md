# WeGo Security Audit Report

**Date:** 2026-02-19
**Scope:** Full codebase security analysis
**Codebase:** Spring Boot 3.2.2 + Thymeleaf + Tailwind CSS + Supabase

---

## Executive Summary

The WeGo codebase demonstrates **mature security practices** across most areas. The architecture uses a layered design with centralized permission checking, parameterized queries, and proper input validation. However, the audit identified **3 high-severity**, **4 medium-severity**, and **5 low-severity** issues that should be addressed.

---

## Architecture Overview

```
Presentation Layer (26 Controllers)
  ├── Web Controllers (13) → Thymeleaf SSR
  └── API Controllers (13) → JSON REST
         │
Service Layer (28+ Services)
  ├── Core Services (Trip, Activity, Expense, Document, Todo)
  ├── Domain Services (PermissionChecker, Settlement, Statistics)
  └── External API Clients (Google Maps, Gemini, Supabase Storage, etc.)
         │
Data Layer (10 Repositories) → PostgreSQL via JPA
```

**Key Security Controls Present:**
- OAuth2 (Google) authentication - no password storage
- Role-based authorization (OWNER > EDITOR > VIEWER) via `PermissionChecker`
- CSRF protection (CookieCsrfTokenRepository)
- Rate limiting (Bucket4j, 100 req/min per IP)
- Content Security Policy headers
- Session management (max 1 session, 7-day timeout, secure cookies)
- File upload validation (MIME type + magic bytes)
- Parameterized JPQL queries (no raw SQL)
- Input sanitization on AI chat prompts

---

## Findings

### HIGH Severity

#### H1. Test Authentication Endpoint Accessible via URL Pattern in Production

**File:** `src/main/java/com/wego/config/SecurityConfig.java:92-93`
**File:** `src/main/java/com/wego/controller/TestAuthController.java:38`

The `SecurityConfig` permanently allows unauthenticated access to `/api/test/auth/**`:

```java
// SecurityConfig.java:92
"/api/test/auth/**"  // Test auth endpoint (only available in test/e2e profile)
```

While `TestAuthController` itself is gated by `@Profile({"test", "e2e"})`, the URL pattern is **always** permitted in the security filter chain. If any other bean accidentally registers a handler at `/api/test/auth/**` (e.g., through a library or misconfiguration), it would bypass authentication entirely.

Additionally, CSRF is disabled for this pattern:

```java
// SecurityConfig.java:50
.ignoringRequestMatchers("/api/health", "/api/test/auth/**")
```

**Risk:** Authentication bypass if the URL pattern is ever inadvertently served in production.

**Recommendation:**
- Make the security rule conditional on profile:
  ```java
  @Bean
  @Profile({"test", "e2e"})
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) { ... }
  ```
- Or remove `/api/test/auth/**` from the production `SecurityConfig` entirely, and define it only in a test-specific configuration class.

---

#### H2. CSRF Token Cookie HttpOnly Set to `false`

**File:** `src/main/java/com/wego/config/SecurityConfig.java:48`

```java
.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
```

The CSRF token cookie is intentionally non-HttpOnly so JavaScript can read it. This is a standard pattern for SPA/AJAX-heavy applications. However, combined with the CSP allowing `'unsafe-inline'` scripts (see M1), any XSS vulnerability would allow an attacker to read the CSRF token and perform state-changing requests on behalf of the user.

**Risk:** If XSS is ever introduced, CSRF protection is fully bypassed.

**Recommendation:**
- This is an acceptable trade-off IF the CSP is tightened (see M1).
- Consider using Spring Security's `CsrfTokenRequestAttributeHandler` with deferred loading instead, which avoids exposing the token in a cookie entirely.

---

#### H3. X-Forwarded-For Header Spoofing for Rate Limit Bypass

**File:** `src/main/java/com/wego/config/RateLimitConfig.java:153-170`

```java
private String getClientIP(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        // ...
        String clientIp = xForwardedFor.split(",")[0].trim();
        return clientIp;
    }
    return request.getRemoteAddr();
}
```

The rate limiter trusts the **leftmost** IP in the `X-Forwarded-For` header, which is trivially spoofable. An attacker can bypass rate limiting by sending a different fake IP in each request:

```
X-Forwarded-For: random-ip-1
X-Forwarded-For: random-ip-2
```

Each request gets its own rate limit bucket, effectively disabling rate limiting.

**Risk:** Complete rate limit bypass, enabling brute force attacks or API abuse.

**Recommendation:**
- Use the **rightmost untrusted** IP (the IP added by your reverse proxy), not the leftmost.
- Configure trusted proxy CIDR ranges and only trust X-Forwarded-For from those proxies.
- Spring Boot's `server.forward-headers-strategy=native` with `server.tomcat.remoteip.internal-proxies` handles this correctly when configured.

---

### MEDIUM Severity

#### M1. Content Security Policy Allows `'unsafe-inline'` for Scripts

**File:** `src/main/java/com/wego/config/SecurityConfig.java:60`

```java
"script-src 'self' 'unsafe-inline' https://unpkg.com https://cdn.jsdelivr.net; "
```

`'unsafe-inline'` in `script-src` effectively negates most XSS protection provided by CSP. If any injection point is found (even in an error message or URL parameter reflected in the page), inline scripts can execute.

**Risk:** Reduced XSS defense-in-depth. CSP becomes informational rather than protective.

**Recommendation:**
- Replace `'unsafe-inline'` with nonce-based CSP: `script-src 'self' 'nonce-<random>'`.
- Generate a unique nonce per request and add it to all `<script>` tags.
- This requires updating all Thymeleaf templates to include the nonce attribute.

---

#### M2. Weather API Endpoints Unauthenticated

**File:** `src/main/java/com/wego/config/SecurityConfig.java:91`

```java
"/api/weather/**"
```

The weather API endpoints are publicly accessible without authentication. While they are rate-limited at the IP level, they proxy requests to the OpenWeatherMap API using the server's API key.

**Risk:** An unauthenticated user could abuse the weather endpoint to exhaust the server's OpenWeatherMap API quota or use it as an open proxy to enumerate location data.

**Recommendation:**
- Require authentication for `/api/weather/**`.
- If public access is intentional, add stricter rate limiting specifically for this endpoint.

---

#### M3. Session Fixation Protection Not Explicitly Configured

**File:** `src/main/java/com/wego/config/SecurityConfig.java:114-118`

The session management configuration does not explicitly set session fixation protection:

```java
.sessionManagement(session -> session
    .maximumSessions(1)
    .expiredUrl("/login?expired=true")
);
```

Spring Security defaults to `changeSessionId` strategy, which is correct. However, this should be explicitly configured to prevent accidental regression if defaults change.

**Recommendation:**
```java
.sessionManagement(session -> session
    .sessionFixation().changeSessionId()
    .maximumSessions(1)
    .expiredUrl("/login?expired=true")
);
```

---

#### M4. Email Logged in Plain Text in `CustomOAuth2UserService`

**File:** `src/main/java/com/wego/security/CustomOAuth2UserService.java:100,103`

```java
log.info("Updated existing user: id={}, email={}", user.getId(), user.getEmail());
// ...
log.info("Created new user: id={}, email={}", user.getId(), user.getEmail());
```

While `maskEmail()` is used at DEBUG level (line 93), the INFO-level logs expose the full email address. In production, INFO is the active level, meaning every login writes the user's email to logs in plain text.

**Risk:** PII exposure in log files. Log aggregation systems, monitoring dashboards, or log files could expose user emails.

**Recommendation:**
- Use `maskEmail(user.getEmail())` in the INFO-level log statements as well.

---

### LOW Severity

#### L1. `image/heic` MIME Type Bypasses Magic Bytes Validation

**File:** `src/main/java/com/wego/service/DocumentService.java:496-499`

```java
// Skip validation for HEIC (complex format, allow if declared)
if ("image/heic".equals(declaredType)) {
    return true;
}
```

HEIC files skip magic bytes validation entirely. An attacker could upload any file type by declaring it as `image/heic`.

**Risk:** Malicious file upload disguised as HEIC image.

**Recommendation:**
- Implement HEIC magic bytes validation (HEIC uses the ISO Base Media File Format with `ftyp` box starting at offset 4, containing `heic`, `heix`, `hevc`, or `hevx`).
- Or remove `image/heic` from the allowed MIME types if it's not actively needed.

---

#### L2. Open Redirect Potential in OAuth2 Default Success URL

**File:** `src/main/java/com/wego/config/SecurityConfig.java:101`

```java
.defaultSuccessUrl("/dashboard")
```

The OAuth2 login uses a hardcoded success URL, which is safe. However, if the application ever adds a `redirect` or `returnUrl` parameter for post-login redirection, open redirect vulnerabilities could arise. Currently, there is no evidence of this, but it should be noted as a design consideration.

**Risk:** None currently, but a common pattern that introduces vulnerabilities when modified.

---

#### L3. Lack of `Strict-Transport-Security` (HSTS) Header

**File:** `src/main/java/com/wego/config/SecurityConfig.java:52-77`

The security headers include CSP, X-Frame-Options, Referrer-Policy, and Permissions-Policy, but **HSTS is not configured**. Spring Security does not enable HSTS by default for non-HTTPS connections.

**Risk:** Users may access the application over HTTP initially, making them vulnerable to SSL stripping attacks.

**Recommendation:**
```java
.headers(headers -> headers
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
    )
    // ... existing headers
)
```

---

#### L4. `MethodArgumentTypeMismatchException` Handler Leaks Parameter Values

**File:** `src/main/java/com/wego/exception/GlobalExceptionHandler.java:148-154`

```java
String message = "Parameter '" + ex.getName() + "' has invalid value: " + ex.getValue();
```

The actual user-supplied value is included in the error response. If a user sends a crafted malicious value, it could be reflected back in the response.

**Risk:** Low-severity information disclosure / reflected content.

**Recommendation:**
- Return a generic message: `"Parameter 'X' has an invalid format"` without including the actual value.

---

#### L5. Permission Cache TTL Could Allow Stale Access

**File:** `src/main/java/com/wego/domain/permission/PermissionChecker.java`
**Related:** `src/main/java/com/wego/config/CacheConfig.java`

The `PermissionChecker` uses a Caffeine cache for permission lookups. If a user is removed from a trip, they could retain access for up to the cache TTL duration (configured as 5 seconds based on comments).

**Risk:** Brief window where revoked permissions are still honored. The 5-second TTL makes this a very small window, but it should be documented.

**Recommendation:**
- The current 5-second TTL is reasonable. Consider adding explicit cache invalidation in `TripService.removeMember()` and `TripService.changeMemberRole()` for immediate effect.

---

## Positive Security Findings

These are security controls that are **correctly implemented**:

| Area | Assessment | Details |
|------|-----------|---------|
| **SQL Injection** | PASS | All queries use JPQL with named `@Param` parameters. No native SQL or string concatenation found in 10 repository classes. |
| **XSS (Server-side)** | PASS | No `th:utext` found in 31 Thymeleaf templates. All output uses `th:text` (auto-escaped). |
| **XSS (Client-side)** | PASS | All JS files use `WeGo.escapeHtml()` or `this.escapeHtml()` before `innerHTML`. `textContent` used for plain data. Chat `formatReply()` escapes before markdown parsing. |
| **CSRF** | PASS | CookieCsrfTokenRepository enabled. All AJAX calls include CSRF token from meta tag. |
| **File Upload** | PASS | Magic bytes validation, MIME type whitelist, size limits (10MB/file, 100MB/trip), UUID-based file naming. |
| **IDOR Prevention** | PASS | Repository queries use compound keys (`tripId + activityId`). Document service verifies `document.getTripId().equals(tripId)`. |
| **Secrets Management** | PASS | All API keys use environment variables. `.gitignore` excludes `.env`, `*.pem`, `*.key`. No hardcoded secrets found. |
| **Error Handling** | PASS | Generic 500 errors in production (`"An unexpected error occurred"`). Stack traces suppressed (`include-message: never`). |
| **Prompt Injection** | PASS | Chat system separates system prompt from user context. User-controlled fields sanitized via `sanitizeField()`. Security rules in system prompt. |
| **Rate Limiting** | PARTIAL | Present (100 req/min per IP + 5 req/min per user for chat), but X-Forwarded-For spoofable (see H3). |
| **Session Security** | PASS | HttpOnly, Secure, SameSite=Lax cookies. Max 1 session per user. 7-day timeout. JSESSIONID deleted on logout. |
| **Dependency Security** | PASS | Spring Boot 3.2.2, no known critical CVEs at time of audit. JaCoCo enforces 80% code coverage. |
| **Authorization** | PASS | Centralized `PermissionChecker` with consistent Role-based checks. Every service method validates permissions before mutation. |

---

## Summary Table

| ID | Severity | Title | Status |
|----|----------|-------|--------|
| H1 | HIGH | Test auth URL pattern always permitted in SecurityConfig | Open |
| H2 | HIGH | CSRF cookie non-HttpOnly + unsafe-inline CSP | Open |
| H3 | HIGH | X-Forwarded-For spoofing bypasses rate limiting | Open |
| M1 | MEDIUM | CSP allows `'unsafe-inline'` for scripts | Open |
| M2 | MEDIUM | Weather API unauthenticated | Open |
| M3 | MEDIUM | Session fixation protection not explicitly configured | Open |
| M4 | MEDIUM | Email logged in plain text at INFO level | Open |
| L1 | LOW | HEIC bypasses magic bytes validation | Open |
| L2 | LOW | Open redirect consideration for future changes | Informational |
| L3 | LOW | Missing HSTS header | Open |
| L4 | LOW | Type mismatch handler leaks parameter values | Open |
| L5 | LOW | Permission cache TTL allows brief stale access | Informational |

---

## Recommended Priority Order

1. **H3** - Fix X-Forwarded-For handling (quick fix, high impact)
2. **H1** - Make test auth URL conditional on profile (quick fix, eliminates risk)
3. **M4** - Mask emails in INFO logs (quick fix, PII compliance)
4. **L4** - Remove parameter values from error responses (quick fix)
5. **M1/H2** - Implement nonce-based CSP (medium effort, significant security improvement)
6. **L3** - Add HSTS header (quick fix)
7. **M3** - Explicitly configure session fixation protection (quick fix)
8. **M2** - Require authentication for weather API (small change)
9. **L1** - Add HEIC magic bytes validation or remove HEIC support (small change)
