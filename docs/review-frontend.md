# Frontend Review Report - WeGo

**Reviewer**: frontend-reviewer
**Date**: 2026-02-10
**Scope**: Thymeleaf templates, JavaScript, AJAX, CSS, SEO & Accessibility

---

## Executive Summary

The WeGo frontend is functional and well-organized with a clear module pattern in JavaScript, consistent Tailwind CSS usage, and proper dark mode support. However, there are several areas requiring attention:

- **8 templates** duplicate head content instead of using the `fragments/head` fragment
- **~80 occurrences** of `var` in inline template scripts (should use `const`/`let`)
- **7 console.log/warn/error** statements in production JS files
- **No AbortController** or fetch timeout handling across all AJAX modules
- **4 duplicated `escapeHtml`** implementations across JS modules
- **4 duplicated CSRF token** retrieval patterns across JS modules
- **1 memory leak**: `URL.createObjectURL` without `revokeObjectURL` in CoverImagePreview
- **1 known controller bug**: `inviteLink`/`inviteLinkExpiry` not provided by `showMembersPage()`

| Severity | Count |
|----------|-------|
| CRITICAL | 2 |
| HIGH | 8 |
| MEDIUM | 10 |
| LOW | 6 |
| **Total** | **26** |

---

## 1. Thymeleaf Templates

### 1.1 Fragment Reuse

**Severity: HIGH** | Affected: 8 templates

The following templates duplicate head content instead of using `th:replace="~{fragments/head :: head(title=...)}`:

| Template | Issue |
|----------|-------|
| `dashboard.html` | Duplicates head, header, and bottom-nav inline |
| `login.html` | Duplicates head content |
| `index.html` | Duplicates head content |
| `trip/create.html` | Duplicates head content |
| `activity/list.html` | Duplicates head content |
| `activity/detail.html` | Duplicates head content |
| `profile/index.html` | Duplicates head content |
| `profile/edit.html` | Duplicates head content |
| `error/error.html` | Duplicates head content |
| `error/404.html` | Duplicates head content |
| `error/500.html` | Duplicates head content |
| `error/403.html` | Duplicates head content |
| `document/upload.html` | Duplicates head content |
| `document/global-overview.html` | Duplicates head content |
| `expense/global-overview.html` | Duplicates head content |
| `expense/settlement.html` | Uses head fragment (good) but has large inline script |

**Recommendation**: Refactor all templates to use the head fragment. This reduces duplication and ensures consistent CSP meta tags, font loading, and dark mode FOUC prevention across all pages.

---

### 1.2 Controller Data Mismatch

**Severity: CRITICAL** | File: `trip/members.html`

`members.html` references `${inviteLink}` and `${inviteLinkExpiry}` in the template, but the `showMembersPage()` controller method does not add these attributes to the model. This means the invite link section will always show empty/null values.

**Recommendation**: Update the controller to provide `inviteLink` and `inviteLinkExpiry`, or remove the template section if it's not yet implemented.

---

### 1.3 SpEL Null-Safety

**Severity: MEDIUM** | Various templates

Most templates use null-safe navigation (`?.`) correctly. A few spots could benefit from additional safety:

- `trip/view.html:7`: `${trip.title}` - should be `${trip?.title}` if trip could be null
- `trip/create.html:72`: Uses string concatenation `'/trips/' + trip.id + '/edit'` which is safe when `isEdit` guard is true

---

### 1.4 Inline Scripts in Templates

**Severity: MEDIUM** | Affected: 6+ templates

Several templates contain large inline `<script>` blocks instead of using external JS files:

| Template | Lines of inline JS | Content |
|----------|-------------------|---------|
| `trip/create.html` | ~65 lines | Cover image preview + date validation (duplicates app.js CoverImagePreview) |
| `trip/members.html` | ~200+ lines | Member management AJAX, invite link copy, role changes |
| `activity/list.html` | ~120+ lines | Collapsible groups, date tab navigation, reorder loading |
| `document/upload.html` | ~200+ lines | File upload with XHR, validation, progress |
| `expense/settlement.html` | ~60+ lines | Settlement confirmation, toast notifications |
| `profile/edit.html` | ~30+ lines | Nickname validation, toast display |

**Recommendation**: Extract inline scripts to external JS modules. The `trip/create.html` cover image preview is already implemented in `app.js` as `CoverImagePreview` - the inline version is redundant.

---

### 1.5 Hardcoded Content

**Severity: LOW** | File: `index.html`

- Copyright year hardcoded as "2025" (line ~end of file)

**Recommendation**: Use `${#temporals.year(#temporals.createNow())}` or a server-side model attribute.

---

## 2. JavaScript

### 2.1 `var` Usage

**Severity: MEDIUM** | Affected: Templates only

All 6 external JS files (`app.js`, `expense.js`, `todo.js`, `drag-reorder.js`, `route-optimizer.js`, `expense-statistics.js`) correctly use `const`/`let`. However, **~80 `var` declarations** exist in inline `<script>` blocks within templates.

The FOUC prevention script uses `var` by design (runs before DOM, needs widest compatibility), which is acceptable. But inline scripts in `trip/create.html`, `activity/list.html`, `document/upload.html`, `expense/settlement.html`, and `profile/edit.html` should migrate to `const`/`let`.

**Recommendation**: When extracting inline scripts to modules (see 1.4), convert all `var` to `const`/`let`.

---

### 2.2 Console Statements in Production

**Severity: MEDIUM** | 7 occurrences

| File | Line | Statement |
|------|------|-----------|
| `app.js` | 471 | `console.error('Weather load error:', error)` |
| `route-optimizer.js` | 26 | `console.error('CSRF token not found...')` |
| `todo.js` | 35 | `console.error('CSRF token not found...')` |
| `expense.js` | 222 | `console.error('Failed to get exchange rate:', error)` |
| `expense.js` | 239 | `console.warn('Invalid currency code format:', ...)` |
| `expense.js` | 264 | `console.error('Exchange rate API error:', error)` |
| `expense.js` | 267 | `console.warn('Using stale cached rate')` |

**Recommendation**: Replace with a centralized logger that can be silenced in production, or remove them entirely and rely on error boundaries/user-facing error messages.

---

### 2.3 Memory Leak: ObjectURL Not Revoked

**Severity: HIGH** | File: `app.js:651`

```
const objectUrl = URL.createObjectURL(file);
```

`CoverImagePreview` creates an object URL for image preview but never calls `URL.revokeObjectURL()`. Each file selection leaks a blob reference.

**Recommendation**: Revoke the previous object URL before creating a new one:
```js
if (this.currentObjectUrl) URL.revokeObjectURL(this.currentObjectUrl);
this.currentObjectUrl = URL.createObjectURL(file);
```

---

### 2.4 innerHTML Usage

**Severity: MEDIUM** | 20+ occurrences across 5 files

All JS modules use `innerHTML` to render dynamic content. Positively, all modules implement their own `escapeHtml()` function and use it consistently for user-controlled data. This mitigates XSS risk.

However:
- `app.js:492` (`WeatherUI.buildForecastCard`) uses template literals with `iconUrl` in innerHTML - if `iconUrl` is user-controllable, this is an XSS vector.
- `expense.js:400` uses an emoji (`submitBtn.innerHTML = '<span class="animate-spin">...</span> ...'`) which is fine but inconsistent with the SVG icon pattern used elsewhere.

**Recommendation**: Consider using DOM APIs (`createElement`, `textContent`) for critical sections, or centralize the `escapeHtml` utility (see 3.2).

---

### 2.5 Duplicated `escapeHtml` Implementations

**Severity: HIGH** | 4 separate implementations

| File | Line |
|------|------|
| `app.js` | 110 |
| `route-optimizer.js` | 417 |
| `todo.js` | 731 |
| `expense-statistics.js` | 22 |

All four are identical implementations using a `<textarea>` trick.

**Recommendation**: Extract to a shared utility module or add to `app.js` as a global utility exposed on `window`.

---

## 3. AJAX & Fetch Patterns

### 3.1 No Fetch Timeout / AbortController

**Severity: HIGH** | All AJAX modules

None of the 6 JS files use `AbortController` or implement fetch timeouts. The only timeout reference is `app.js:425` which is a weather cache TTL, not a fetch timeout.

If the server is slow or unresponsive, fetch calls will hang indefinitely with no user feedback.

**Recommendation**: Create a shared `fetchWithTimeout` wrapper:
```js
async function fetchWithTimeout(url, options = {}, timeout = 10000) {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeout);
    try {
        const response = await fetch(url, { ...options, signal: controller.signal });
        return response;
    } finally {
        clearTimeout(id);
    }
}
```

---

### 3.2 Duplicated CSRF Token Retrieval

**Severity: MEDIUM** | 4+ locations

CSRF token retrieval is duplicated across:

| Location | Pattern |
|----------|---------|
| `todo.js:29-36` | `document.querySelector('meta[name="_csrf"]')` |
| `route-optimizer.js:20-27` | `document.querySelector('meta[name="_csrf"]')` |
| `drag-reorder.js:46-51` | `document.querySelector('meta[name="_csrf"]')` |
| Inline scripts (members.html, settlement.html, upload.html) | Same pattern |

**Recommendation**: Extract to a shared utility function in `app.js`:
```js
window.WeGo = {
    getCsrfToken() { return document.querySelector('meta[name="_csrf"]')?.content; },
    getCsrfHeader() { return document.querySelector('meta[name="_csrf_header"]')?.content; }
};
```

---

### 3.3 Duplicate Submission Prevention

**Severity: MEDIUM** | Partial coverage

Duplicate submission prevention exists in:
- `app.js:175-193` - `Loading.showButtonLoading()` disables button
- `route-optimizer.js:366,399` - Disables apply button during optimization
- `expense.js:399` - Disables submit button

Missing from:
- `trip/members.html` inline script (role change, remove member)
- `expense/settlement.html` inline script (settlement confirmation - has loading state but no early return on double-click)
- `document/upload.html` (file upload via XHR)

**Recommendation**: Ensure all form submissions and AJAX-triggered buttons are disabled during processing.

---

### 3.4 No Unified Error Handling

**Severity: HIGH** | All AJAX modules

Each module implements its own error handling pattern:
- `todo.js` uses `Toast.show()` for errors
- `expense.js` shows inline error text
- `route-optimizer.js` shows toast on some errors, console.error on others
- Inline scripts use `alert()` or custom toast implementations

**Recommendation**: Standardize on `Toast.show(message, 'error')` for all user-facing errors and create a shared fetch wrapper that handles common HTTP errors (401, 403, 404, 500).

---

## 4. CSS

### 4.1 `!important` Usage

**Severity: LOW** | 9 total occurrences

- `output.css`: 8 occurrences (Tailwind utility classes - expected)
- `styles.css`: 1 occurrence

This is minimal and acceptable. The Tailwind `!important` usages are from utility classes (standard behavior).

---

### 4.2 Potentially Unused CSS File

**Severity: LOW** | File: `output.css`

Both `styles.css` and `output.css` exist. Templates reference `styles.css`. If `output.css` is a leftover from a previous Tailwind build, it should be removed.

**Recommendation**: Verify if `output.css` is referenced anywhere. If not, remove it.

---

## 5. SEO & Accessibility

### 5.1 Missing Head Fragment = Missing Meta Tags

**Severity: HIGH** | 8+ templates

Templates that duplicate head content instead of using the fragment miss the CSRF meta tags and potentially have inconsistent `<meta name="description">` tags. See section 1.1 for the full list.

---

### 5.2 Semantic HTML

**Severity: LOW** | Generally good

The templates use semantic elements well:
- `<header>`, `<main>`, `<nav>`, `<footer>` used appropriately
- `<h1>` through `<h3>` maintain reasonable hierarchy
- `<form>` elements have proper labels

Minor issues:
- No `<skip-to-content>` link on any page
- Some icon-only buttons in inline scripts lack `aria-label`

---

### 5.3 Image Alt Text

**Severity: LOW** | Generally good

Most `<img>` elements have `alt` attributes. Avatar images use meaningful alt text like `alt="avatar"` or user names. The cover image preview uses `alt="封面預覽"`.

---

### 5.4 Focus Management in Modals

**Severity: MEDIUM** | File: `app.js` Modal module

The Modal module in `app.js` implements basic focus trapping (focuses first focusable element on open). However, it does not:
- Return focus to the triggering element on close
- Trap Tab key within the modal (keyboard user can tab out)

**Recommendation**: Implement full focus trap (intercept Tab/Shift+Tab) and restore focus to trigger element on close.

---

## 6. Google Maps

### 6.1 Maps Usage

No Google Maps JavaScript API initialization or marker management was found in the frontend JS files. The `GOOGLE_MAPS_API_KEY` environment variable exists in the backend, and `activity/create.html` references place selection, but the actual Maps integration appears to be server-side (Places API for geocoding) rather than client-side map rendering.

**Finding**: No client-side Google Maps issues to report.

---

## Summary Statistics

| Category | Critical | High | Medium | Low | Total |
|----------|----------|------|--------|-----|-------|
| Thymeleaf Templates | 1 | 1 | 2 | 1 | 5 |
| JavaScript | 0 | 2 | 3 | 0 | 5 |
| AJAX & Fetch | 0 | 3 | 2 | 0 | 5 |
| CSS | 0 | 0 | 0 | 2 | 2 |
| SEO & Accessibility | 0 | 1 | 1 | 2 | 4 |
| Google Maps | 0 | 0 | 0 | 0 | 0 |
| **Total** | **1** | **7** | **8** | **5** | **21** |

### Top 5 Recommendations (by impact)

1. **Fix `members.html` controller data mismatch** - CRITICAL - invite link section is broken
2. **Refactor templates to use head fragment** - HIGH - 8+ templates duplicate head content, causing maintenance burden and inconsistent meta tags
3. **Add fetch timeout/AbortController** - HIGH - all AJAX calls can hang indefinitely
4. **Fix ObjectURL memory leak in CoverImagePreview** - HIGH - blob references leak on each file selection
5. **Extract shared utilities (escapeHtml, CSRF, fetchWrapper)** - HIGH - reduces 4x code duplication across JS modules
