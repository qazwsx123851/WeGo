# WeGo 前端程式碼審查報告

> **審查日期**: 2026-02-21 (第二次審查)
> **審查範圍**: 34 個 Thymeleaf 模板、15 個 JavaScript 模組、2 個 CSS 檔案
> **審查版本**: `6141afd` (main branch)
> **前次審查**: 2026-02-20 (整體 8.5/10)

---

## 目錄

1. [總覽與評分](#1-總覽與評分)
2. [前次 Critical 問題追蹤](#2-前次-critical-問題追蹤)
3. [Thymeleaf 模板審查](#3-thymeleaf-模板審查)
4. [JavaScript 品質審查](#4-javascript-品質審查)
5. [AJAX 與錯誤處理審查](#5-ajax-與錯誤處理審查)
6. [Google Maps 功能審查](#6-google-maps-功能審查)
7. [CSS 品質審查](#7-css-品質審查)
8. [SEO 與無障礙審查](#8-seo-與無障礙審查)
9. [問題統計](#9-問題統計)

---

## 1. 總覽與評分

| 維度 | 分數 (1-10) | 前次 | 變化 | 說明 |
|------|:-----------:|:----:|:----:|------|
| Thymeleaf 模板 | 8.5 | 9.0 | -0.5 | Fragment 復用良好，dashboard 重複代碼仍未修復 |
| JavaScript 品質 | 8.5 | 7.5 | +1.0 | 15/15 模組化完成 (前次 14/15)，app.js 仍過大 |
| AJAX / 錯誤處理 | 9.0 | 9.0 | = | 統一 fetchWithTimeout，CSRF、超時處理完善 |
| Google Maps | 9.0 | 9.0 | = | debounce 搜尋、伺服器代理、錯誤回退完備 |
| CSS 品質 | 8.0 | 8.5 | -0.5 | inline `<style>` 問題仍存在於 4 個模板 |
| SEO / 無障礙 | 7.5 | 7.5+9.0 | = | 有 aria-label、focus trap、skip-to-content，缺 meta description |
| **綜合** | **8.4** | **8.5** | **-0.1** | 模組化改善但仍有技術債 |

---

## 2. 前次 Critical 問題追蹤

| 問題 | 狀態 | 說明 |
|------|:----:|------|
| `personal-expense.js` 全域函式 (Critical) | **已修復** | 已重構為 IIFE `PersonalExpense` 模組，`switchTab` 等函式不再汙染全域 |
| `CATEGORY_LABELS` 重複定義 (Warning) | **已修復** | 已提取至 `common.js` → `WeGo.CATEGORY_LABELS`，三個檔案共用 |
| `personal-expense.js` inline onclick (Warning) | **已修復** | 已改為 `data-action` 事件委派模式 |

---

## 3. Thymeleaf 模板審查

### 3.1 Fragment 復用

**優點**:
- 完善的 Fragment 系統：`head.html` (CSRF meta、CDN 資源)、`layout.html` (三種版面)、`components.html` (UI 元件庫)、`chat-widget.html`
- 所有頁面統一使用 `th:replace="~{fragments/head :: head(title='...')}"` 確保一致性
- Toast container、bottom-nav 等透過 Fragment 復用

**問題**:

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `fragments/layout.html` | dashboard-layout 完整重複 app-layout 的 header HTML，修改需同步兩處 | 將 header 抽成獨立 fragment `~{fragments/layout :: app-header}`，兩個 layout 共用 |
| 🟡 Warning | `dashboard.html` | bottom-nav 為 inline HTML，未使用 `th:replace="~{fragments/components :: bottom-nav}"` | 改用 fragment 引入 |
| 🟡 Warning | `activity/detail.html` (行 409-441) | bottom-nav inline HTML，與 `components.html` 定義重複 | 改為 `th:replace` |
| 🟡 Warning | `activity/list.html` (行 411-442) | 同上 | 同上 |
| 🔵 Suggestion | `trip/create.html` (行 10) | Toast container 為 inline `<div>` | 改為 `th:replace="~{fragments/components :: toast-container}"` |

### 3.2 模板中的業務邏輯

**優點**: 大多數模板僅負責顯示，業務邏輯正確放在 Controller/ViewHelper 層。權限檢查統一使用 `canEdit`/`canInvite`/`isOwner` 等 boolean model 屬性。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `activity/list.html` (行 119-131) | 時間範圍計算邏輯在模板中完成 | 移至 ViewHelper 提供 `dayTimeRange` 字串 |
| 🔵 Suggestion | `activity/detail.html` (行 177) | 停留時間格式化 (分鐘轉小時+分鐘) 在 SpEL 中完成，表達式複雜 | 在 ViewHelper 中提供格式化後字串 |

### 3.3 Null-Safety

**優點**: 廣泛使用 `?.` safe navigation 和 `?:` elvis operator，必要的 `th:if` guard 保護集合操作。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `settlement.html` (行 37-38) | `trip.startDate`/`trip.endDate` 無 null check 直接 format | 加入 `th:if` 條件或使用 `?.` |

### 3.4 XSS 防護

**結論**: XSS 防護完備。全部使用 `th:text` (無 `th:utext`)，無 `th:onclick` 拼接，動態內容均使用 `th:data-*` + JS 讀取。

### 3.5 表單綁定與 CSRF

**優點**: 所有 POST 表單包含 CSRF token，`data-prevent-double-submit` 統一防重複提交，表單驗證使用 `required` + `maxlength` HTML5 屬性。

---

## 4. JavaScript 品質審查

### 4.1 模組化與全域污染

**優點**: **15/15** 個 JS 檔案全部模組化完成 (前次 14/15)。

| 模式 | 檔案 |
|------|------|
| IIFE | `common.js`, `chat.js`, `activity-list.js`, `member-management.js`, `activity-form.js`, `document-list.js`, `expense-list.js`, `personal-expense.js`, `settlement.js` |
| `const` 模組 | `app.js`, `todo.js`, `expense.js`, `expense-statistics.js`, `route-optimizer.js`, `sortable-reorder.js` |

已知全域物件 (有意識暴露):
`window.WeGo`, `window.Toast`, `window.DarkMode`, `window.Loading`, `window.Modal`, `window.PersonalExpense`, `window.ExpenseStatistics`, `window.TodoApi`, `window.TodoUI` 等

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `app.js` 多模組 | 10+ 個模組直接掛載至 `window` | 考慮統一收歸至 `WeGo` 命名空間 (如 `WeGo.Toast`) |

### 4.2 `var` vs `let`/`const`

**優點**: 新模組全面使用 `const`/`let`。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `common.js` (行 22,36,51,65,67,70,93) | 函式內部使用 `var` (如 `var div`, `var meta`) | 改用 `const` |
| 🔵 Suggestion | `todo.js` 多處 | 混用 `var` 和 `const`/`let` | 統一使用 `const`/`let` |

### 4.3 事件處理

**優點**: 大多數模組使用 `data-action` 事件委派模式。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `todo.js` (renderTodoItem) | 使用字串拼接建構 HTML + `onclick="TodoUI.xxx()"` 內聯事件 | 改為 `data-action` + 事件委派，與其他模組一致 |
| 🟡 Warning | `todo/list.html` (行 46-47, 69-93, 111, 137, 238-239) | 多處 `onclick="TodoUI.xxx()"` 行內事件 | 改為 `data-action` 事件委派 |
| 🟡 Warning | `components.html` (行 84) | Dark mode toggle 使用 inline `onclick="DarkMode.toggle()"` | 改為 `data-action="toggle-dark-mode"` |
| 🟡 Warning | `error/404.html` (行 50) | `onclick="history.back()"` | 改為 `data-action="go-back"` |

### 4.4 Inline JavaScript (模板中的 `<script>`)

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `trip/create.html` (行 252-318) | 整段 inline `<script>` 處理封面預覽、日期驗證、字數計數 | 提取至獨立 `trip-form.js` |
| 🟡 Warning | `activity/detail.html` (行 527-573) | inline `<script>` 處理 more options menu 與 delete dialog | 提取至外部 JS |

### 4.5 檔案大小

| 嚴重度 | 檔案 | 行數 | 問題 | 建議 |
|--------|------|------|------|------|
| 🟡 Warning | `app.js` | ~1190 | 超過建議的 800 行上限，包含 12 個功能模組 | 拆分 WeatherUI、TimePicker、DatePicker 為獨立模組 |

### 4.6 記憶體管理

**優點**:
- `expense-statistics.js` 主題切換時正確銷毀舊 Chart 實例再建立新的
- `personal-expense.js` Chart 實例有 `destroy()` 呼叫
- `app.js` CoverImagePreview 正確呼叫 `URL.revokeObjectURL`
- Event delegation 模式減少事件監聽器數量

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `chat.js` | chat widget 關閉時未移除 scroll/resize 事件監聽器 | 關閉時清理事件 |

### 4.7 console 語句

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| 🟡 Warning | `personal-expense.js` | 618 | `console.error('[PersonalChart] init failed:', e)` 殘留 | 移除或改用 Toast 顯示錯誤 |

---

## 5. AJAX 與錯誤處理審查

### 5.1 統一 Fetch 封裝

**優點**: 大部分 AJAX 呼叫使用 `WeGo.fetchWithTimeout()` (預設 30 秒超時，AbortController)。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `sortable-reorder.js`, `settlement.js`, `chat.js` | 部分呼叫直接使用 `fetch()` 而非 `WeGo.fetchWithTimeout()` | 統一使用確保 timeout 保護 |

### 5.2 CSRF 處理

**結論**: 完備。所有 POST/PUT/DELETE 請求均包含 CSRF header (`WeGo.getCsrfToken()` + `WeGo.getCsrfHeader()`)。

### 5.3 錯誤處理

**優點**:
- 所有 fetch 鏈結包含 `.catch()` 處理
- 錯誤訊息透過 `Toast.error()` 統一顯示
- `expense-list.js` API 失敗時使用卡片快取資料作為 fallback
- `sortable-reorder.js` 失敗時完整 rollback 恢復原始順序

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `expense-list.js` (fetchExpenseDetail) | 取得單一支出時呼叫全量 API `/api/trips/{tripId}/expenses` 再 filter | 後端新增 `GET /api/trips/{tripId}/expenses/{expenseId}` 端點 |

### 5.4 Loading 狀態

**結論**: 完善。`document-list.js` 有上傳進度條、`expense-list.js` modal 有 loading skeleton、表單有 preventDoubleSubmit + loading spinner。

### 5.5 防止重複提交

**結論**: 健全。表單層 `WeGo.preventDoubleSubmit` + API 層按鈕 disable + `member-management.js` isProcessing 旗標。

---

## 6. Google Maps 功能審查

### 6.1 地點搜尋

檔案: `static/js/activity-form.js`

**優點**:
- 搜尋使用 300ms debounce 防止連續 API 呼叫
- 透過伺服器端代理 `/api/trips/{tripId}/places/search` 呼叫 Google Maps API，不在前端暴露 API Key
- hidden fields 正確儲存 placeId, latitude, longitude
- 搜尋結果下拉選單外部點擊自動關閉

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `activity-form.js` | 搜尋結果為空陣列時下拉選單直接隱藏 | 顯示「未找到相關地點」提示 |

### 6.2 地圖顯示

**優點**: Google Maps Embed API (iframe) + `loading="lazy"`，無 API key 時有 fallback placeholder。

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `activity/detail.html` (行 82) | iframe 使用 `lat,lng` 而非 place name | 若有 `googlePlaceId` 改用 `place_id:xxx` 查詢 |

### 6.3 Geolocation

**優點**: WeatherUI 有 geolocation 拒絕處理 (fallback 預設城市)，使用 `navigator.permissions.query` 先檢查權限。

---

## 7. CSS 品質審查

### 7.1 架構

**優點**: Tailwind CSS v3.4.19 utility-first，暗色模式完整 (`dark:` prefix)，RWD 全面使用斷點。

### 7.2 Inline `<style>` 問題

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `expense/list.html` (行 365-406) | inline `<style>` 含 animation keyframes (fade-in, slide-up) | 移至 Tailwind 配置或獨立 CSS |
| 🟡 Warning | `todo/list.html` (行 440-456) | inline `<style>` 定義 `.filter-tab` 與 `.line-clamp-2` | `.line-clamp-2` Tailwind v3.3+ 已內建 |
| 🟡 Warning | `expense/create.html` (行 401-483) | 大量 inline `<style>` 處理 split-tab 和 Flatpickr dark mode | 移至全域 CSS 或 Tailwind plugin |
| 🟡 Warning | `document/list.html` (行 430-467) | inline `<style>` 定義 scrollbar-hide、drawer animation | 移至全域 CSS |

### 7.3 `!important` 使用

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `expense/create.html` (行 480) | `padding-left: 2.5rem !important` 覆蓋 Flatpickr | 改用更具體 CSS selector |

### 7.4 響應式設計

**結論**: 優秀。Modal 手機 bottom-sheet + 桌面居中、bottom-nav + sidebar 切換、Chat widget 雙模式、觸控友好尺寸 (`min-w-[44px]`)。

---

## 8. SEO 與無障礙審查

### 8.1 Meta 標籤

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `fragments/head.html` | 缺少 `<meta name="description">` | 新增每頁 description meta (透過 fragment 參數傳入) |
| 🔵 Suggestion | `index.html` | Landing page 缺少 Open Graph / Twitter Card meta | 為公開頁面新增 OG 標籤 |

### 8.2 語意化 HTML

**優點**: 正確使用 `<header>`, `<main>`, `<nav>`, `<section>` 語意標籤。Dialogs 使用 `role="alertdialog"` + `aria-modal="true"`。`<main id="main-content">` 搭配 skip-to-content link。

### 8.3 Heading 層級

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🟡 Warning | `dashboard.html` | 從 `<h2>` 開始，缺少 `<h1>` | 每頁應有且僅有一個 `<h1>` |
| 🟡 Warning | `expense/list.html` | 頁面標題為 `<h2>`，缺少 `<h1>` | 同上 |
| 🔵 Suggestion | `activity/list.html` (行 95-370) | heading 層級不連續 (h1 > h2，缺 h3 直接到 card) | 按 h1 > h2 > h3 連續遞降 |

### 8.4 無障礙功能

**優點**:
- aria-label: 按鈕、連結、表單元素均有適當的 aria-label
- Focus trap: `app.js` Modal 模組 + `personal-expense.js` 刪除確認 modal
- `role="alert"`: 登入頁錯誤訊息
- `prefers-reduced-motion`: `expense-list.js`, `activity-list.js`, `personal-expense.js`, `sortable-reorder.js`, `document/list.html` 均支援
- 鍵盤導覽: Escape 鍵關閉 modal (多模組)
- 裝飾性 SVG 有 `aria-hidden="true"`

| 嚴重度 | 檔案 | 問題 | 建議 |
|--------|------|------|------|
| 🔵 Suggestion | `app.js` (Toast 模組) | Toast 通知未設定 `role="status"` 或 `aria-live="polite"` | 螢幕閱讀器可能不會朗讀通知 |
| 🔵 Suggestion | `todo/list.html` (行 266-424) | Todo modal 有 `aria-modal` 但缺少 focus trap | 使用已有的 `Modal.open()` focus trap |
| 🔵 Suggestion | `trip/members.html` (行 288-321) | Remove member dialog 缺少 `aria-describedby` | 加入 `aria-describedby` |

---

## 9. 問題統計

### 依嚴重度

| 嚴重度 | 數量 | 說明 |
|:------:|:----:|------|
| 🔴 Critical | 0 | 無重大安全或功能問題 (前次 1 個已修復) |
| 🟡 Warning | 19 | 模板重複代碼、inline JS/CSS、heading 層級等 |
| 🔵 Suggestion | 13 | 命名空間統一、var 改 const、aria 補齊等 |
| **合計** | **32** | |

### 依維度分佈

| 維度 | 🔴 | 🟡 | 🔵 | 小計 |
|------|:--:|:--:|:--:|:----:|
| Thymeleaf 模板 | 0 | 5 | 3 | 8 |
| JavaScript 品質 | 0 | 7 | 3 | 10 |
| AJAX / 錯誤處理 | 0 | 1 | 1 | 2 |
| Google Maps | 0 | 0 | 2 | 2 |
| CSS 品質 | 0 | 4 | 1 | 5 |
| SEO / 無障礙 | 0 | 2 | 3 | 5 |

### 與前次審查比較

| 指標 | 前次 (02-20) | 本次 (02-21) | 變化 |
|------|:----------:|:----------:|:----:|
| Critical | 1 | 0 | -1 |
| Warning | 18 | 19 | +1 |
| Suggestion | 14 | 13 | -1 |
| 綜合分數 | 8.5 | 8.4 | -0.1 |

### 優先修復建議

1. **消除 inline `<script>`** — `trip/create.html`、`activity/detail.html` 提取為外部 JS
2. **消除 inline `<style>`** — 4 個模板的 CSS 移至全域樣式或 Tailwind plugin
3. **拆分 `app.js`** (~1190 行) — WeatherUI、TimePicker、DatePicker 獨立為模組
4. **`todo.js` inline onclick** — 改為 `data-action` 事件委派，與其他 14 個模組一致
5. **dashboard/activity 模板復用** — header、bottom-nav 改用 fragment 引入
6. **`expense-list.js` 全量查詢** — 後端新增單一支出查詢端點
7. **補齊 SEO meta** — 新增 `<meta name="description">`，公開頁面加 OG 標籤
8. **Heading 層級修正** — dashboard、expense/list 頁面補齊 `<h1>`

---

## 附錄：已完成的良好實踐

以下列出專案中值得肯定的前端工程實踐：

1. **模組化 15/15**: 所有 JS 檔案均使用 IIFE 或 const 模組模式，零全域汙染
2. **XSS 防護完備**: 全部使用 `th:text`，JS 端使用 `WeGo.escapeHtml()`
3. **CSRF 統一處理**: `common.js` 集中管理，所有 API 呼叫自動附加
4. **暗色模式**: 全站 dark mode 支援，含 FOUC 防閃同步腳本
5. **事件委派**: 大多數模組使用 `data-action` 模式，減少事件綁定
6. **fetchWithTimeout**: 統一 30 秒超時 + AbortController
7. **防止重複提交**: 表單層 + API 層雙重防護
8. **prefers-reduced-motion**: 5+ 個模組/模板尊重使用者動畫偏好
9. **Focus trap**: Modal 元件正確實作焦點陷阱
10. **Chart.js 記憶體管理**: 主題切換時正確銷毀舊圖表實例
11. **共用常量去重**: `CATEGORY_LABELS`/`CATEGORY_COLORS` 集中於 `WeGo` 命名空間
12. **伺服器端代理**: Google Maps API 不在前端暴露 Key
