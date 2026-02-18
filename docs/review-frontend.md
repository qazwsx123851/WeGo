# Frontend Review Report - WeGo

**Reviewer**: frontend-reviewer
**Date**: 2026-02-18
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

**主要問題**：
- 大量 inline `onclick` handler（50+ 處）違反 CSP 最佳實踐
- 多個模板含有大量行內 JavaScript（members.html ~240 行、expense/list.html ~300 行）
- 行內腳本仍有 4 處 `console.error`/`console.warn`
- 3 處使用 `alert()` 取代 Toast 元件

| Severity | Count |
|----------|-------|
| HIGH | 3 |
| MEDIUM | 6 |
| LOW | 5 |
| INFO | 3 |
| **Total** | **17** |

---

## 1. Thymeleaf Templates

### 1.1 Fragment 使用 -- 已全面改善

**Severity: INFO** | 已修復

所有模板（含 `error/*.html`、`login.html`、`index.html` 等）均已使用 `th:replace="~{fragments/head :: head(title=...)}"` 載入統一 head fragment。CSRF meta tags、字體載入、暗黑模式 FOUC 防止腳本已全面一致。

---

### 1.2 行內腳本過多

**Severity: HIGH** | 影響: 6+ 模板

多個模板包含大量行內 `<script>` 區塊，應抽取為外部 JS 檔案：

| 模板 | 行內 JS 行數 | 內容 |
|------|-------------|------|
| `trip/members.html` | ~240 行 | 成員管理 AJAX、邀請連結、角色變更 |
| `expense/list.html` | ~300 行 | 費用篩選、日期群組展開、費用詳情/刪除 modal |
| `document/list.html` | ~250 行 | 檔案上傳 modal、文件預覽、刪除確認 |
| `activity/list.html` | ~150 行 | 群組展開/收合、日期 tab 導航、交通重算 |
| `activity/create.html` | ~200 行 | Google Places 搜尋、交通模式切換 |
| `expense/settlement.html` | ~60 行 | 結算確認、Toast 通知 |

**建議**：將行內腳本逐步抽取為獨立 JS 模組（如 `member-management.js`、`expense-list.js`），遵循現有 `todo.js`、`route-optimizer.js` 的模組模式。

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

**Severity: LOW** | 檔案: `index.html:65`

```html
&copy; 2025 WeGo. Made with love for travelers.
```

版權年份硬編碼為 2025，已過期。

**建議**：使用 `${#temporals.year(#temporals.createNow())}` 或 server-side model attribute。

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

**Severity: MEDIUM** | 4 處行內腳本

外部 JS 檔案已全數清除 console 語句。但行內腳本仍有殘留：

| 檔案 | 行號 | 語句 |
|------|------|------|
| `expense/list.html` | 416 | `console.error('Error fetching expense:', error)` |
| `expense/list.html` | 667 | `console.error('Error deleting expense:', error)` |
| `activity/list.html` | 846 | `console.warn('Lottie animation failed to load:', e)` |
| `activity/create.html` | 715 | `console.error('Place search error:', error)` |

**建議**：移除或替換為 Toast 錯誤訊息，與外部 JS 檔案保持一致。

---

### 2.3 `var` 使用

**Severity: LOW** | 5 處

`var` 使用量已大幅減少：
- `fragments/head.html`: 2 處（FOUC 防止腳本，需最大相容性，**可接受**）
- `activity/list.html`: 3 處（行內腳本中的 `var response`、`var data`、`var msg`）

**建議**：行內腳本中的 `var` 在抽取為外部模組時一併轉換為 `const`/`let`。

---

### 2.4 `alert()` 使用

**Severity: MEDIUM** | 3 處

| 檔案 | 行號 | 內容 |
|------|------|------|
| `trip/create.html` | 266 | `alert('檔案大小不得超過 5MB')` |
| `activity/create.html` | 972 | `alert('選擇飛機或高鐵時，必須輸入預估交通時間')` |
| `activity/list.html` | 917 | `alert(msg)` -- 作為 Toast 不可用時的 fallback |

**建議**：統一使用 `Toast.error()` 顯示錯誤訊息。`activity/list.html` 的 fallback 可保留但應加註解說明。

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

**Severity: HIGH** | 50+ 處，分布於 12+ 模板

以下模板使用行內 `onclick` handler：

| 模板 | onclick 數量 | 範例函數 |
|------|:-----------:|---------|
| `document/list.html` | 12 | `openUploadModal()`, `closeDetailModal()`, `executeDelete()` |
| `trip/members.html` | 8 | `generateInviteLink()`, `changeRole()`, `removeMember()` |
| `expense/list.html` | 7 | `toggleDateGroup()`, `closeExpenseModal()`, `deleteExpense()` |
| `todo/list.html` | 8 | `TodoUI.showCreateModal()`, `TodoUI.filterTodos()` |
| `fragments/components.html` | 4 | `DarkMode.toggle()`, toast dismiss |
| `error/*.html` | 5 | `history.back()`, `location.reload()` |
| `document/global-overview.html` | 3 | `openDocumentPreview()`, `closePreviewModal()` |
| `expense/settlement.html` | 1 | `settleItem()` |
| `document/upload.html` | 2 | `clearFileSelection()` |
| `activity/list.html` | 1 | `toggleActivityGroup()` |

**問題**：
1. 違反 Content Security Policy (CSP) 最佳實踐 -- 需要 `unsafe-inline` 才能執行
2. 全域命名空間污染 -- onclick 引用的函數必須為全域函數
3. 維護困難 -- HTML 與 JS 邏輯緊耦合

**建議**：
- 使用 `data-action` attribute + 事件委派模式取代 onclick
- 參考 `drag-reorder.js` 的事件委派實作
- 短期可先處理高互動頁面（`document/list.html`、`trip/members.html`、`expense/list.html`）

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

缺失：
- 無 skip-to-content 連結
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

**Severity: MEDIUM** | 檔案: `app.js` Modal module

Modal 模組已實作：
- 開啟時 focus 至第一個可聚焦元素

尚缺：
- 關閉時未返回 focus 至觸發元素
- 未實作 Tab/Shift+Tab 焦點陷阱（鍵盤使用者可 tab 出 modal）

**建議**：實作完整 focus trap（攔截 Tab/Shift+Tab）並於關閉時恢復 focus。

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

### Top 5 Recommendations (by impact)

1. **抽取行內腳本為外部模組** -- HIGH -- 6+ 模板含 150-300 行行內 JS，增加維護成本且阻礙 CSP 政策
2. **移除 inline onclick handlers** -- HIGH -- 50+ 處 onclick 違反 CSP 最佳實踐，應改用事件委派
3. **統一使用 WeGo.fetchWithTimeout** -- MEDIUM -- 行內腳本的 AJAX 呼叫仍可能缺少 timeout
4. **完善 Modal focus trap** -- MEDIUM -- 鍵盤使用者可 tab 離開 modal
5. **清除行內腳本 console 語句** -- MEDIUM -- 4 處 console.error/warn 殘留

---

## Overall Score: 7.5 / 10

**評分依據**：
- (+) 共用工具模組整合度高（escapeHtml、CSRF、fetchWithTimeout 已統一）
- (+) XSS 防護完善（全面使用 escapeHtml，無 th:utext）
- (+) 暗黑模式支援完整
- (+) 觸控友善設計（44px touch targets、touch-manipulation）
- (+) 所有模板已統一使用 head fragment
- (+) Semantic HTML 與 ARIA 支援良好
- (-) 大量行內腳本與 onclick handlers（CSP 風險、維護成本）
- (-) 行內腳本品質不一致（console、alert、var 殘留）
- (-) Modal focus trap 不完整
