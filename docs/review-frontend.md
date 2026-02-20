# WeGo 前端審查報告

> 審查日期：2026-02-20
> 審查範圍：34 個 Thymeleaf 模板、15 個 JavaScript 模組、CSS 及 SEO/無障礙

---

## 目錄

1. [Thymeleaf 模板](#1-thymeleaf-模板)
2. [JavaScript 品質](#2-javascript-品質)
3. [AJAX / Fetch 呼叫](#3-ajax--fetch-呼叫)
4. [Google Maps 整合](#4-google-maps-整合)
5. [CSS](#5-css)
6. [SEO 與無障礙](#6-seo-與無障礙)
7. [總結與評分](#7-總結與評分)

---

## 1. Thymeleaf 模板

### 1.1 Fragment 重用

**優點：**
- 完善的 Fragment 系統：`head.html`（CSRF meta、CDN 資源）、`layout.html`（三種版面）、`components.html`（UI 元件庫）、`chat-widget.html`
- 所有頁面統一使用 `th:replace="~{fragments/head :: head(title='...')}"` 確保一致性
- Toast container、bottom-nav 等透過 Fragment 復用

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `dashboard.html` | 全檔 | Header 與 bottom-nav 採用 inline 寫法而非 fragment 復用 | 改用 `th:replace` 引入 `components.html` 的 header/bottom-nav fragment |
| Warning | `activity/detail.html` | 409-441 | Bottom-nav 為 inline HTML，未使用 `fragments/components :: bottom-nav` | 改為 `th:replace="~{fragments/components :: bottom-nav}"` |
| Warning | `activity/list.html` | 411-442 | 同上，bottom-nav inline 未復用 fragment | 同上 |
| Suggestion | `trip/create.html` | 10 | Toast container 為 inline `<div>` 而非使用 `th:replace` | 改為 `th:replace="~{fragments/components :: toast-container}"` |

### 1.2 商業邏輯洩漏到模板

**優點：**
- 大多數模板僅負責顯示，業務邏輯正確放在 Controller/ViewHelper 層
- 權限檢查統一使用 `canEdit`/`canInvite`/`isOwner` 等 boolean model 屬性

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Suggestion | `activity/list.html` | 119-131 | 時間範圍計算邏輯（取最後一個 activity 的 endTime）在模板中完成 | 移至 ViewHelper 提供 `dayTimeRange` 字串 |
| Suggestion | `activity/detail.html` | 177 | 停留時間格式化（分鐘轉小時+分鐘）在 SpEL 中完成，表達式複雜 | 在 ViewHelper 中提供格式化後的字串 |

### 1.3 Null-Safety

**優點：**
- 廣泛使用 `?.` safe navigation 和 `?:` elvis operator
- 必要的 `th:if` guard 保護集合操作

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `settlement.html` | 37-38 | `trip.startDate` / `trip.endDate` 無 null check 直接 format | 加入 `th:if` 條件或使用 `?.` |
| Suggestion | `activity/create.html` | 55 | `activity?.place?.category` 三層 null check 合理但略繁冗 | 考慮在 Controller 預處理 activityType 字串 |

### 1.4 表單綁定與 CSRF

**優點：**
- 所有 POST 表單包含 CSRF token（`th:name="${_csrf.parameterName}"`）
- `data-prevent-double-submit` 統一防重複提交
- 表單驗證使用 `required` + `maxlength` HTML5 屬性

**結論：** Thymeleaf 模板整體品質高，Fragment 系統完善但有少數頁面未完全復用。

---

## 2. JavaScript 品質

### 2.1 模組化與全域污染

**優點：**
- 14/15 個 JS 檔案使用 IIFE 或模組物件模式，避免全域污染
- `common.js` 提供統一的 `WeGo` 命名空間
- 大多數模組透過 `window.ModuleName` 有意識地暴露 API

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Critical | `personal-expense.js` | 全檔 | 使用全域函式（`switchTab`, `toggleAutoItem`, `showDeleteModal`, `trapFocus` 等）而非 IIFE/模組模式，是唯一未模組化的 JS 檔案 | 重構為 IIFE 或模組物件模式（如 `PersonalExpense.init()`），僅暴露必要 API |
| Warning | `personal-expense.js` & `expense-statistics.js` | 兩檔 | `CATEGORY_LABELS` 常量重複定義（7 個分類的中文對照表） | 提取至 `common.js` 的 `WeGo.CATEGORY_LABELS` 或獨立 `constants.js` |

### 2.2 `var` vs `let`/`const`

**優點：**
- `common.js` 在 IIFE 內部正確使用 `const` 宣告 WeGo 物件
- 較新的模組（`sortable-reorder.js`, `expense-statistics.js`, `settlement.js`）全面使用 `const`/`let`

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Suggestion | `personal-expense.js` | 全檔 | 大量使用 `var`，包括迴圈中的 `var i` | 全面改用 `const`/`let` |
| Suggestion | `common.js` | 22,36,51,65,67,70,93-94 | 函式內部使用 `var`（如 `var div`, `var meta`） | 改用 `const` |
| Suggestion | `todo.js` | 多處 | 混用 `var` 和 `const`/`let` | 統一使用 `const`/`let` |

### 2.3 未處理的 Promise Rejection

**優點：**
- 大部分 fetch 呼叫包含 `try/catch` 或 `.catch()` 處理
- `WeGo.fetchWithTimeout` 正確處理 AbortError

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `personal-expense.js` | 多處 | 多個 `fetch` 呼叫在 `.then()` 鏈中缺少 `.catch()` 全域錯誤處理 | 加入 `.catch()` 並顯示 Toast 錯誤訊息 |

### 2.4 記憶體洩漏

**優點：**
- `app.js` CoverImagePreview 正確呼叫 `URL.revokeObjectURL` 釋放 Blob URL
- Chart.js 實例在 `expense-statistics.js` 中正確銷毀後重建
- Event delegation 模式減少了事件監聽器數量

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `trip/create.html` | 273 | `FileReader.readAsDataURL` 產生的 base64 data URL 不會被釋放（不像 `createObjectURL` 需要手動釋放，但大檔案會佔用記憶體） | 考慮改用 `URL.createObjectURL` 並在替換時 revoke |
| Suggestion | `personal-expense.js` | 全檔 | Chart.js 實例存於區域變數，頁面切換 tab 時未銷毀前一個 chart | 在 `switchTab` 時銷毀已存在的 Chart 實例 |

### 2.5 Inline JavaScript

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `todo/list.html` | 46-47, 69-93, 111, 137, 238-239 | 多處使用 `onclick="TodoUI.xxx()"` 行內事件處理 | 改為 `data-action` + 事件委派模式（todo.js 中的 renderTodoItem 也有此問題） |
| Warning | `components.html` | 84 | Dark mode toggle 使用 inline `onclick="DarkMode.toggle(); ..."` | 改為 `data-action="toggle-dark-mode"` + 事件委派 |
| Warning | `error/404.html` | 50 | `onclick="history.back()"` 行內事件 | 改為 `data-action="go-back"` + JS 處理 |
| Warning | `trip/create.html` | 252-318 | 整段 inline `<script>` 處理封面預覽、日期驗證、字數計數 | 提取至獨立的 `trip-form.js` 外部模組 |
| Warning | `activity/detail.html` | 527-573 | 整段 inline `<script>` 處理 more options menu 與 delete dialog | 提取至外部 JS 或整合進 `activity-list.js` |

### 2.6 console 語句

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `personal-expense.js` | 618 | `console.error('[PersonalChart] init failed:', e)` 殘留 | 移除或改用 Toast 顯示錯誤 |

### 2.7 檔案大小

| 嚴重度 | 檔案 | 行數 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `app.js` | 1190 | 超過建議的 800 行上限，包含 12 個功能模組 | 考慮拆分 WeatherUI、TimePicker、DatePicker 為獨立模組 |
| Suggestion | `personal-expense.js` | 781 | 接近 800 行上限 | 重構模組化後自然會精簡 |

---

## 3. AJAX / Fetch 呼叫

### 3.1 錯誤處理

**優點：**
- 大多數 API 呼叫包含 try/catch 並顯示 Toast 錯誤訊息
- `sortable-reorder.js` 具備完整的 rollback 機制（失敗時恢復原始順序）
- `expense.js` 匯率查詢有 TTL cache（30 分鐘）避免重複請求

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `expense-list.js` | fetchExpenseDetail | 取得單一支出明細時，呼叫 `/api/trips/{tripId}/expenses` 取回全部支出再 filter | 建議後端提供 `GET /api/trips/{tripId}/expenses/{expenseId}` 單一查詢端點 |

### 3.2 Loading 狀態

**優點：**
- `document-list.js` 有完整的上傳進度條（XHR onprogress）
- `route-optimizer.js` 有 loading/result/error 多狀態 modal
- `sortable-reorder.js` 有 reorder loading overlay
- 表單提交有 `preventDoubleSubmit` + loading spinner

**結論：** Loading 狀態處理完善。

### 3.3 雙重提交防護

**優點：**
- `WeGo.preventDoubleSubmit` 自動掛載到所有 `data-prevent-double-submit` 表單
- 5 秒安全網自動解鎖
- `expense.js` 表單提交有全螢幕遮罩

**結論：** 防重複提交機制健全。

### 3.4 Timeout

**優點：**
- `WeGo.fetchWithTimeout` 提供 30 秒預設 timeout
- 使用 `AbortController` 正確實作

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Suggestion | 多個檔案 | - | 部分 fetch 呼叫直接使用 `fetch()` 而非 `WeGo.fetchWithTimeout()`（如 `sortable-reorder.js`, `settlement.js`, `chat.js`） | 統一使用 `WeGo.fetchWithTimeout` 確保所有請求有 timeout 保護 |

### 3.5 CSRF Token

**優點：**
- 所有 API 呼叫正確帶入 CSRF header（`WeGo.getCsrfToken()` + `WeGo.getCsrfHeader()`）
- E2E 測試工具也有 CSRF-aware helpers

---

## 4. Google Maps 整合

### 4.1 地圖嵌入

**優點：**
- `activity/detail.html` 使用 Google Maps Embed API（iframe），無需 JS SDK
- 無 API key 時有 fallback：clickable placeholder 連結至 Google Maps
- iframe 有 `loading="lazy"` 延遲載入

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Suggestion | `activity/detail.html` | 82 | Google Maps Embed iframe 使用 `lat,lng` 而非 place name，可能影響 pin 精確度 | 若有 `googlePlaceId` 可改用 `place_id:xxx` 查詢 |

### 4.2 地點搜尋

**優點：**
- `activity-form.js` 搜尋有 300ms debounce 防止連續請求
- 搜尋結果下拉選單外部點擊自動關閉
- hidden fields 正確儲存 placeId, latitude, longitude

### 4.3 Geolocation

**優點：**
- `app.js` WeatherUI 有 geolocation 拒絕處理（fallback 到預設城市）
- 使用 `navigator.permissions.query` 在支援的瀏覽器先檢查權限狀態

---

## 5. CSS

### 5.1 架構

**優點：**
- 使用 Tailwind CSS v3.4.19，utility-first 減少自訂 CSS
- `styles.css` 為 Tailwind 編譯產物，非手寫 CSS
- Dark mode 完整支援（使用 Tailwind `dark:` prefix）

### 5.2 Inline Styles

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `expense/list.html` | 365-406 | inline `<style>` 包含 animation keyframes（fade-in, slide-up 等） | 移至 Tailwind 配置或獨立 CSS 檔案 |
| Warning | `todo/list.html` | 440-456 | inline `<style>` 定義 `.filter-tab` 樣式與 `.line-clamp-2` | `.filter-tab` 改用 Tailwind classes；`.line-clamp-2` Tailwind v3.3+ 已內建 |
| Warning | `expense/create.html` | 401-483 | 大量 inline `<style>` 處理 split-tab 和 Flatpickr dark mode | 移至全域 CSS 或 Tailwind plugin |
| Warning | `document/list.html` | 430-467 | inline `<style>` 定義 scrollbar-hide、drawer animation | 移至全域 CSS |

### 5.3 `!important` 使用

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Suggestion | `expense/create.html` | 480 | `padding-left: 2.5rem !important` 覆蓋 Flatpickr altInput 樣式 | 改用更具體的 CSS selector 避免 `!important` |

### 5.4 RWD

**優點：**
- 全面使用 Tailwind 斷點（`sm:`, `md:`, `lg:`）
- 底部導航列適配移動端（固定底部、safe-area-inset-bottom）
- Chat widget 有 mobile bottom-sheet / desktop floating card 雙模式
- 所有互動元素保持 `min-w-[44px] min-h-[44px]`（觸控友好）
- 圖片使用 `aspect-[16/9]` 確保比例一致

**結論：** RWD 實作優秀。

---

## 6. SEO 與無障礙

### 6.1 Title 與 Meta

**優點：**
- 每頁使用 `th:replace="~{fragments/head :: head(title='...')}"` 確保獨立 title
- `head.html` 包含 viewport meta、charset

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Warning | `head.html` | - | 缺少 `<meta name="description">` 描述標籤 | 新增每頁 description meta（可透過 fragment 參數傳入） |
| Suggestion | `index.html` | - | Landing page 缺少 Open Graph / Twitter Card meta | 為公開頁面新增 OG 標籤提升社群分享效果 |

### 6.2 語意 HTML

**優點：**
- 正確使用 `<header>`, `<main>`, `<nav>`, `<section>` 語意標籤
- Dialogs 使用 `role="alertdialog"` + `aria-modal="true"`
- `<main id="main-content">` 搭配 skip-to-content link

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Suggestion | `activity/list.html` | 95-370 | `th:block` + `section` 的巢狀結構合理但 heading 層級不連續（h1 在 header, h2 在 day group, 缺 h4 直接在 activity card） | heading 層級改為 h1 > h2 > h3 連續 |

### 6.3 圖片 Alt 文字

**優點：**
- 裝飾性 SVG 皆有 `aria-hidden="true"`
- 使用者上傳圖片有 `th:alt="${doc.originalFileName}"` 或 `th:alt="${trip.title}"`
- Avatar 圖片有 `th:alt="${member.nickname}"`

**結論：** Alt 文字處理良好。

### 6.4 Heading 層級

**優點：**
- 大多數頁面維持 h1 > h2 > h3 正確層級
- Landing page (`index.html`) 有正確的 heading 結構

### 6.5 ARIA

**優點：**
- Modal 有 `aria-modal="true"`, `aria-labelledby`
- Chat widget 按鈕有 `aria-label`、搜尋 toggle 有 `aria-pressed`
- Skip-to-content link 包含在 toast-container fragment
- Activity list day groups 有 `aria-expanded`, `aria-controls`
- Filter tabs 有 keyboard support（tabindex）

**問題：**

| 嚴重度 | 檔案 | 行號 | 問題 | 建議 |
|--------|------|------|------|------|
| Suggestion | `todo/list.html` | 266-424 | Todo modal 有 `aria-modal` 和 `aria-labelledby` 但缺少 focus trap | 使用 `Modal.open()` 搭配已有的 focus trap 實作 |
| Suggestion | `trip/members.html` | 288-321 | Remove member dialog 有 `role="alertdialog"` 但缺少 `aria-describedby` | 加入 `aria-describedby="dialog-desc"` |

### 6.6 Reduced Motion

**優點：**
- `sortable-reorder.js` 檢測 `prefers-reduced-motion` 停用動畫
- `document/list.html` inline CSS 包含 `@media (prefers-reduced-motion: reduce)` 停用 spinner 和 drawer 動畫
- Reorder loading overlay 有 `motion-reduce:duration-0`

---

## 7. 總結與評分

### 各面向評分

| 面向 | 分數 | 說明 |
|------|:----:|------|
| Thymeleaf 模板 | 9.0 | Fragment 系統完善，null-safety 佳，少數頁面未復用 fragment |
| JS 模組化 | 7.5 | 14/15 檔案已模組化，`personal-expense.js` 為唯一例外且有重複常量 |
| AJAX 模式 | 9.0 | 錯誤處理、loading 狀態、雙重提交防護、CSRF 皆完善 |
| Google Maps | 9.0 | Embed API + fallback 設計良好，搜尋有 debounce |
| CSS | 8.5 | Tailwind utility-first 架構佳，但有 4 個模板包含 inline `<style>` |
| SEO | 7.5 | 基本 title 正確，缺 description meta 和 OG 標籤 |
| 無障礙 | 9.0 | ARIA 標記完善，focus trap、skip-to-content、reduced motion 支援 |

### 問題統計

| 嚴重度 | 數量 |
|--------|:----:|
| Critical | 1 |
| Warning | 18 |
| Suggestion | 14 |

### 優先修復建議

1. **Critical:** `personal-expense.js` 全域函式問題 -- 重構為 IIFE/模組物件模式
2. **Warning:** 消除 inline `<script>` -- `trip/create.html`、`activity/detail.html` 提取為外部 JS
3. **Warning:** 消除 inline `<style>` -- 4 個模板的 CSS 移至全域樣式
4. **Warning:** `CATEGORY_LABELS` 去重 -- 提取至共用常量
5. **Warning:** `todo/list.html` inline onclick -- 改為 data-action 事件委派
6. **Warning:** `expense-list.js` 全量查詢 -- 後端新增單一支出查詢端點
7. **Warning:** 新增 `<meta name="description">` 改善 SEO

### 整體前端評分

## 8.5 / 10

**評語：** 前端品質整體優秀。Tailwind CSS + Thymeleaf Fragment 架構成熟，AJAX 模式健全（CSRF、timeout、double-submit 防護完備），無障礙支援超越一般專案水準。主要改進空間在於 `personal-expense.js` 模組化、消除殘留的 inline JS/CSS、以及補齊 SEO meta 標籤。相較前次評分（健康分數 9.0），本次發現的問題多為 Suggestion 等級，整體趨勢正向。
