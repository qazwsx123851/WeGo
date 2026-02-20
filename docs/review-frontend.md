# Frontend Review Report - WeGo

**Reviewer**: frontend-reviewer
**Date**: 2026-02-20 (updated)
**Branch**: main
**Scope**: Thymeleaf templates, JavaScript, AJAX, CSS, SEO & Accessibility

---

## Executive Summary

WeGo 的前端架構成熟度良好，共用工具模組 (`common.js`) 提供統一的 XSS 防護、CSRF 處理、fetch timeout 封裝與防重複提交機制，所有外部 JS 檔案均已整合至此共用層。程式碼品質在多次迭代後有顯著提升。

**主要優點**：
- 所有 JS 模組的 `escapeHtml`、`getCsrfToken`、`getCsrfHeader` 均已委派至 `WeGo.*` 共用工具
- 所有外部 JS 檔案已清除 `console.log`/`warn`/`error`
- 所有模板已統一使用 `fragments/head` fragment
- `fetchWithTimeout` 已有 AbortController 支援

**2026-02-20 更新**：以下主要問題已全部修復：
- ~~大量 inline `onclick` handler（50+ 處）~~ → ✅ 已改用 `data-action` + 事件委派
- ~~多個模板含有大量行內 JavaScript~~ → ✅ 6 模板已抽取為外部 JS 模組
- ~~行內腳本仍有 4 處 `console.error`/`console.warn`~~ → ✅ 已清除
- ~~3 處使用 `alert()` 取代 Toast 元件~~ → ✅ 已改用 Toast
- ~~Modal focus trap 不完整~~ → ✅ 已實作 Tab/Shift+Tab 循環 + 關閉恢復 focus
- ~~缺 skip-to-content~~ → ✅ 已新增
- ~~版權年份硬編碼~~ → ✅ 已動態化

| Severity | Count |
|----------|-------|
| HIGH | 0 |
| MEDIUM | 1 |
| LOW | 1 |
| INFO | 3 |
| **Total** | **5** |

---

## 1. Thymeleaf Templates

### 1.1 Fragment 使用 -- 已全面改善

**Severity: INFO** | 已修復

所有模板（含 `error/*.html`、`login.html`、`index.html` 等）均已使用 `th:replace="~{fragments/head :: head(title=...)}"` 載入統一 head fragment。CSRF meta tags、字體載入、暗黑模式 FOUC 防止腳本已全面一致。

---

### 1.2 行內腳本過多

**Severity: ~~HIGH~~ → RESOLVED** | ✅ 2026-02-20 已修復

所有 6 個模板的行內腳本已抽取為外部 JS 模組：

| 模板 | 原行內 JS | 外部模組 | 狀態 |
|------|----------|---------|:----:|
| `expense/settlement.html` | ~60 行 | `settlement.js` | ✅ |
| `activity/list.html` | ~260 行 | `activity-list.js` | ✅ |
| `activity/create.html` | ~370 行 | `activity-form.js` | ✅ |
| `trip/members.html` | ~240 行 | `member-management.js` | ✅ |
| `expense/list.html` | ~480 行 | `expense-list.js` | ✅ |
| `document/list.html` | ~555 行 | `document-list.js` | ✅ |

所有模組遵循 IIFE + 事件委派模式，統一使用 `WeGo.*` 共用工具。

---

### 1.3 SpEL Null-Safety

**Severity: MEDIUM** | 各模板

大部分模板已正確使用 null-safe navigation (`?.`)。少數可改善之處：

- `trip/members.html:58`: `${trip.title}` -- 若 trip 保證非 null 則可接受
- `activity/list.html:24`: `${trip.title}` -- 同上
- `activity/list.html:120-122`: `dayGroup.value[0]?.startTime` -- 已使用 `?.`，正確

**整體評估**：SpEL null-safety 處理良好，無 CRITICAL 風險。

---

### 1.4 Hardcoded 內容

**Severity: ~~LOW~~ → RESOLVED** | ✅ 2026-02-20 已修復

~~版權年份硬編碼為 2025~~ → 已改用 `th:text="${#temporals.year(#temporals.createNow())}"`。

---

## 2. JavaScript

### 2.1 共用工具整合 -- 已全面改善

**Severity: INFO** | 已修復

所有外部 JS 模組已統一使用 `WeGo.*` 共用工具：

| 工具 | 整合狀態 |
|------|---------|
| `WeGo.escapeHtml()` | `app.js`、`todo.js`、`route-optimizer.js`、`expense-statistics.js`、`chat.js` 均委派 |
| `WeGo.getCsrfToken()` / `getCsrfHeader()` | `todo.js`、`route-optimizer.js`、`drag-reorder.js`、`expense.js`、`chat.js` 均委派 |
| `WeGo.fetchWithTimeout()` | `chat.js`、`activity/list.html` 行內腳本已使用 |

---

### 2.2 Console 語句

**Severity: ~~MEDIUM~~ → RESOLVED** | ✅ 2026-02-20 已修復

行內腳本已抽取為外部模組，console 語句已全數清除或替換為 Toast 訊息。

---

### 2.3 `var` 使用

**Severity: LOW** | 5 處

`var` 使用量已大幅減少：
- `fragments/head.html`: 2 處（FOUC 防止腳本，需最大相容性，**可接受**）
- `activity/list.html`: 3 處（行內腳本中的 `var response`、`var data`、`var msg`）

**建議**：行內腳本中的 `var` 在抽取為外部模組時一併轉換為 `const`/`let`。

---

### 2.4 `alert()` 使用

**Severity: ~~MEDIUM~~ → RESOLVED** | ✅ 2026-02-20 已修復

行內腳本已抽取為外部模組，`alert()` 已改用 `Toast.error()` 或 `Toast.warning()`。

---

### 2.5 innerHTML 安全性

**Severity: LOW** | 各模組

所有動態 HTML 渲染均透過 `WeGo.escapeHtml()` 防護使用者輸入。唯一潛在風險：

- `app.js` `WeatherUI.buildForecastCard()`: `iconUrl` 由 API 回傳的天氣圖示 URL 直接嵌入 `<img src=>`。因為來源是 OpenWeatherMap API（受信任），風險極低。

**整體評估**：XSS 防護良好，無需立即修改。

---

## 3. AJAX & Fetch Patterns

### 3.1 Fetch Timeout 覆蓋率

**Severity: MEDIUM** | 部分覆蓋

`WeGo.fetchWithTimeout()` 已實作於 `common.js`，目前使用狀況：

| 模組 | 使用 fetchWithTimeout | 備註 |
|------|:--------------------:|------|
| `chat.js` | 是 | |
| `activity/list.html` 重算交通 | 是 | 60 秒 timeout |
| 行內腳本（members.html、expense/list.html 等） | 否 | 使用 `WeGo.fetchWithTimeout` 或原生 fetch 未確認 |

**建議**：行內腳本抽取為外部模組時，統一使用 `WeGo.fetchWithTimeout()`。

---

### 3.2 防重複提交

**Severity: MEDIUM** | 部分覆蓋

`WeGo.preventDoubleSubmit()` 自動附加至 `form[data-prevent-double-submit]` 表單。

已覆蓋：
- 所有使用 `data-prevent-double-submit` attribute 的表單
- `route-optimizer.js` 按鈕 disable 邏輯
- `activity/list.html` 重算交通按鈕 disable 邏輯

缺失：
- `trip/members.html` 行內腳本的角色變更、移除成員 AJAX
- `document/list.html` 行內腳本的上傳、刪除 AJAX
- `expense/list.html` 行內腳本的刪除 AJAX

**建議**：行內腳本的 AJAX 操作應統一在請求期間 disable 按鈕。

---

### 3.3 錯誤處理一致性

**Severity: LOW** | 各模組

| 模組 | 錯誤處理方式 |
|------|-------------|
| `todo.js` | `Toast.show()` |
| `chat.js` | 自訂 UI bubble |
| `route-optimizer.js` | `Toast` |
| `expense.js` | Inline error text |
| 行內腳本 | `Toast`、`alert()`、`console.error` 混用 |

**建議**：統一使用 `Toast.error(message)` 處理所有使用者可見錯誤。

---

## 4. Inline Event Handlers (onclick)

### 4.1 大量 onclick 使用

**Severity: ~~HIGH~~ → MOSTLY RESOLVED** | ✅ 2026-02-20 大部分已修復

6 個主要模板的 onclick handler 已改用 `data-action` + 事件委派：

| 模板 | 原 onclick 數量 | 狀態 |
|------|:-----------:|:----:|
| `document/list.html` | 12 | ✅ 改 `data-action` |
| `trip/members.html` | 8 | ✅ 改 `data-action` |
| `expense/list.html` | 7 | ✅ 改 `data-action` |
| `expense/settlement.html` | 1 | ✅ 改 `data-action` |
| `activity/list.html` | 1 | ✅ 改 `data-action` |
| `activity/create.html` | 行內 JS | ✅ 抽取至外部模組 |

剩餘少量 onclick（低優先）：

| 模板 | onclick 數量 | 說明 |
|------|:-----------:|---------|
| `todo/list.html` | 8 | 已有獨立 `todo.js` 模組 |
| `fragments/components.html` | 4 | `DarkMode.toggle()`, toast dismiss |
| `error/*.html` | 5 | `history.back()`, `location.reload()` |
| `document/global-overview.html` | 3 | 需配合 Global Document 模組化 |
| `document/upload.html` | 2 | `clearFileSelection()` |

---

## 5. CSS

### 5.1 `!important` 使用

**Severity: LOW** | 1 處行內樣式

`expense/create.html:452` 使用 `!important` 覆蓋 Flatpickr 預設樣式：

```css
padding-left: 2.5rem !important;
```

**評估**：覆蓋第三方元件樣式時使用 `!important` 是常見做法，風險可接受。

---

### 5.2 Tailwind CSS 使用

**Severity: INFO** | 良好

- 全面使用 Tailwind utility classes
- Dark mode 支援完整（`dark:` prefix 一致使用）
- 響應式斷點使用 `sm:` prefix
- 自訂元件類別（`glass-card`、`btn-cta`、`btn-primary`、`bottom-nav` 等）定義於 `styles.css`
- 觸控友善（`min-h-[44px]`、`min-w-[44px]`、`touch-manipulation` 等）

---

## 6. SEO & Accessibility

### 6.1 Meta Tags

**Severity: INFO** | 良好

所有模板統一使用 head fragment，確保：
- `<meta name="viewport">` 含 `interactive-widget=resizes-content`
- `<meta name="description">`
- `<meta name="theme-color">`
- CSRF meta tags

---

### 6.2 Semantic HTML

**Severity: LOW** | 大致良好

正面發現：
- 正確使用 `<header>`、`<main>`、`<nav>`、`<footer>`、`<section>`
- 標題層級合理（`<h1>` - `<h4>`）
- 表單元素有 `<label>`
- 按鈕有 `aria-label`
- Modal 有 `role="dialog"` 和 `aria-modal="true"`
- `activity/list.html` 群組使用 `role="button"`、`tabindex="0"`、`aria-expanded`、`aria-controls`
- 鍵盤支援（`onkeydown` 處理 Enter/Space）

~~缺失~~：
- ~~無 skip-to-content 連結~~ → ✅ 已新增（`fragments/components.html` + 22+ 模板 `id="main-content"`）
- 部分行內腳本動態產生的按鈕缺少 `aria-label`

---

### 6.3 Image Alt Text

**Severity: LOW** | 良好

- Avatar images: `th:alt="${trip.title}"`、`alt="avatar"` 等
- Cover images: 有 alt text
- SVG icons: 使用 `aria-hidden="true"`
- Weather icons: `alt="${forecast.description || forecast.condition}"` -- 有意義

---

### 6.4 Focus Management

**Severity: ~~MEDIUM~~ → RESOLVED** | ✅ 2026-02-20 已修復

Modal 模組已實作完整 focus management：
- ✅ 開啟時 focus 至第一個可聚焦元素
- ✅ 關閉時恢復 focus 至觸發元素（`_previousFocus`）
- ✅ Tab/Shift+Tab 焦點陷阱（鍵盤使用者無法 tab 出 modal）
- ✅ 僅對可見元素進行 focus 循環（`offsetParent !== null` 過濾）

---

## 7. Google Maps

### 7.1 Maps 使用

`activity/create.html` 包含 Google Places Autocomplete 整合（行內腳本 ~200 行）。無客戶端地圖渲染 (Google Maps JS API)。Places 搜尋透過後端 proxy，前端僅處理搜尋結果選取。

**評估**：無客戶端 Google Maps 記憶體管理問題。

---

## Summary Statistics

| Category | High | Medium | Low | Info | Total |
|----------|------|--------|-----|------|-------|
| Thymeleaf Templates | 1 | 1 | 1 | 1 | 4 |
| JavaScript | 0 | 2 | 2 | 1 | 5 |
| AJAX & Fetch | 0 | 2 | 1 | 0 | 3 |
| Inline Event Handlers | 1 | 0 | 0 | 0 | 1 |
| CSS | 0 | 0 | 1 | 1 | 2 |
| SEO & Accessibility | 0 | 1 | 1 | 0 | 2 |
| Google Maps | 0 | 0 | 0 | 0 | 0 |
| **Total** | **2** (+1 overlap) | **6** | **6** | **3** | **17** |

### 前次 Review 已修復項目 (2026-02-12 → 2026-02-18)

| 原嚴重度 | 項目 | 狀態 |
|----------|------|------|
| CRITICAL | `members.html` controller data mismatch (`inviteLink`/`inviteLinkExpiry`) | 已修復 -- `MemberWebController` 已提供 |
| HIGH | 8+ 模板未使用 head fragment | 已修復 -- 全部模板已統一使用 |
| HIGH | 4 個重複 `escapeHtml` 實作 | 已修復 -- 全部委派至 `WeGo.escapeHtml()` |
| HIGH | ObjectURL 記憶體洩漏 (`CoverImagePreview`) | 已修復 -- 已加入 `revokeObjectURL` |
| MEDIUM | 4 個重複 CSRF token 取得模式 | 已修復 -- 全部委派至 `WeGo.getCsrfToken()`/`getCsrfHeader()` |
| MEDIUM | 7 個 console.log/warn/error (外部 JS) | 已修復 -- 已全數清除 |
| HIGH | 無 fetch timeout / AbortController | 已修復 -- `WeGo.fetchWithTimeout()` 已實作於 `common.js` |

### Top 5 Recommendations (by impact) — 更新 2026-02-20

1. ~~**抽取行內腳本為外部模組**~~ — ✅ 已完成（6 模組已抽取）
2. ~~**移除 inline onclick handlers**~~ — ✅ 已完成（6 主要模板）
3. ~~**統一使用 WeGo.fetchWithTimeout**~~ — ✅ 已在模組抽取時統一
4. ~~**完善 Modal focus trap**~~ — ✅ 已完成
5. ~~**清除行內腳本 console 語句**~~ — ✅ 已在模組抽取時清除

### 剩餘建議

1. **CSP 遷移至 nonce-based** — MEDIUM — 行內腳本已抽取，可進一步移除 `unsafe-inline`
2. **剩餘 onclick handler 清理** — LOW — `todo/list.html`、`fragments/components.html`、`error/*.html` 等

---

## Overall Score: ~~7.5~~ → **9.0 / 10** (updated 2026-02-20)

**評分依據**：
- (+) 共用工具模組整合度高（escapeHtml、CSRF、fetchWithTimeout 已統一）
- (+) XSS 防護完善（全面使用 escapeHtml，無 th:utext）
- (+) 暗黑模式支援完整
- (+) 觸控友善設計（44px touch targets、touch-manipulation）
- (+) 所有模板已統一使用 head fragment
- (+) Semantic HTML 與 ARIA 支援良好
- (+) **6 模板行內腳本已抽取為外部 JS 模組（~1900 行）**
- (+) **50+ onclick handler 改用 data-action 事件委派**
- (+) **Modal focus trap 完整（Tab/Shift+Tab + 關閉恢復）**
- (+) **skip-to-content 連結已新增**
- (-) 少數模板仍有 onclick（todo、error、components）
- (-) CSP 仍需 `unsafe-inline`（待 nonce-based 遷移）
