# WeGo 專案健康度報告

**日期:** 2026-02-13
**審查範圍:** 架構、安全、前端、效能、測試
**審查團隊:** 5 位自動化審查員 (Claude Opus 4.6)
**分支:** main

---

## 總覽評分表

| 面向 | 分數 (1-10) | 說明 |
|------|:-----------:|------|
| 架構設計 | **8.5** | 分層清晰，Auth 統一為 `@CurrentUser UserPrincipal` (零 DB 查詢)，業務邏輯提取至 ViewHelper，PermissionChecker 含請求級快取，TripConstants 共用常數 |
| 安全性 | **8.5** | 基礎扎實 (OAuth2、CSRF、參數化查詢、無 XSS)，max-request-size 已降至 30MB；生產環境錯誤訊息曝露仍為 Critical |
| 前端品質 | **8.0** | 模組化 JS 良好，共用 `common.js` (escapeHtml/CSRF/fetchWithTimeout/preventDoubleSubmit)，**27/27 模板統一 head fragment**，表單防重複提交 |
| 效能 | **8.5** | 資料庫索引、Cache-Control、統一快取系統、N+1 修復（Trip/Document 批次查詢）、Signed URL 快取、PermissionChecker 快取 (5s TTL)、Web Auth 零 DB 查詢、分離 RestTemplate timeout |
| 測試覆蓋 | **9.0** | 1060 單元測試 (79 個測試檔案) + 11 個 E2E spec 全數通過，12/12 Web Controller 皆有 WebMvcTest，REST API 100% 覆蓋，ViewHelper 單元測試 |
| **整體** | **8.7** | 功能完整的 MVP，Auth 統一、業務邏輯分層、前端整合、例外處理收緊、模板全統一、Web Controller 測試全覆蓋、檔案預覽效能優化 |

---

## Critical 問題優先處理清單

以下問題應**立即修復**，依影響程度排序：

| # | 嚴重度 | 面向 | 問題 | 檔案 | 影響 |
|---|--------|------|------|------|------|
| 1 | 🔴 | 安全 | `server.error.include-message: always` 洩漏內部細節 | `application.yml:83-84` | 生產環境曝露類別名、Schema 資訊 | ✅ 已修復 |
| 2 | 🔴 | 效能 | `getUserTrips` N+1 查詢 | `TripService.java:185-195` | 每頁 20 筆行程 = 40+ 額外查詢 | ✅ 已修復 — batch loading |
| 3 | 🔴 | 架構 | TripController 1664 行，God Controller | `TripController.java` | 可維護性極差，違反 SRP | ✅ 已修復 — 拆分至 535 行，新增 DocumentWebController、MemberWebController |
| 4 | 🔴 | 架構 | 業務邏輯洩漏到 Controller 層 | `TripController.java` 多處 | 無法測試、無法復用 | ✅ 部分修復 — Place find-or-create 已提取至 PlaceService，BaseWebController helpers 消除重複 |
| 5 | 🔴 | 架構 | Place find-or-create 邏輯重複 77 行 | `TripController.java:1081-1158, 1304-1380` | DRY 違反 | ✅ 已修復 — 提取至 PlaceService |
| 6 | 🔴 | 前端 | `members.html` 引用 `inviteLink`/`inviteLinkExpiry` 但 Controller 未提供 | `trip/members.html` + `TripController` | 邀請連結功能完全損壞 | ✅ 已修復 — Controller 已提供 |

---

## 近期已修復項目

| 項目 | 面向 | 原嚴重度 | 狀態 |
|------|------|----------|------|
| AI 聊天安全強化（prompt injection、circuit breaker、Unicode 驗證、OOM 修復） | 安全 | 🔴 Critical | ✅ 已修復 |
| Entity 加上 `@Table(indexes=...)` 註解 | 效能 | 🔴 Critical | 已修復 |
| 靜態資源加上 Cache-Control (`setCachePeriod`) | 效能 | 🟡 Warning | 已修復 |
| Thymeleaf cache 生產環境啟用 | 效能 | 🔵 Suggestion | 已修復 |
| `max-request-size` 從 100MB 降至 30MB | 安全 | 🟡 Warning | 已修復 |
| ObjectURL 記憶體洩漏修復 (`revokeObjectURL`) | 前端 | 🟡 High | 已修復 |
| console.log/warn/error 語句清除 | 前端 | 🟠 Medium | 已修復 |
| 提取 `BaseWebController` 基礎類別 | 架構 | 🟡 Warning | ✅ 已修復 |
| Repository 旁路修復 (InviteController, ProfileController, ExpenseWebController) | 架構 | 🔴 Critical | ✅ 已修復 |
| TripController 拆分 (1664→535 行) | 架構 | 🔴 Critical | ✅ 已修復 |
| Place find-or-create 提取至 PlaceService | 架構 | 🔴 Critical | ✅ 已修復 |
| BaseWebController 全面繼承 (9 個 Web Controller) | 架構 | 🟡 Warning | ✅ 已修復 |
| trip-fetch-and-null-check 重複模板提取 | 架構 | 🟡 Warning | ✅ 已修復 |
| 統一快取系統至 Spring Cache + Caffeine | 效能 | 🟡 Warning | ✅ 已修復 |
| CacheService 記憶體洩漏風險消除 | 效能 | 🟡 Warning | ✅ 已修復 |
| TodoApiController WebMvcTest (20 tests) | 測試 | 🟡 Warning | ✅ 已修復 |
| ExchangeRateApiController WebMvcTest (16 tests) | 測試 | 🟡 Warning | ✅ 已修復 |
| TripController WebMvcTest (16 tests) | 測試 | 🟡 Warning | ✅ 已修復 |
| N+1 查詢修復 (getUserTrips batch loading) | 效能 | 🔴 Critical | ✅ 已修復 |
| inviteLink/inviteLinkExpiry Controller 提供 | 前端 | 🔴 Critical | ✅ 已修復 |
| `@CurrentUser UserPrincipal` 全面遷移 (11 個 Controller) | 架構 | 🟡 Warning | ✅ 已修復 |
| ActivityViewHelper / ExpenseViewHelper 業務邏輯提取 | 架構 | 🟡 Warning | ✅ 已修復 |
| PermissionChecker 請求級 Caffeine 快取 (5s TTL) | 效能 | 🟡 Warning | ✅ 已修復 |
| TripConstants 共用常數 (`MAX_MEMBERS_PER_TRIP`) | 架構 | 🔵 Suggestion | ✅ 已修復 |
| HomeController DTO mutation 修復 (不可變 Map) | 架構 | 🟡 Warning | ✅ 已修復 |
| `common.js` 共用工具 (escapeHtml/CSRF/fetchWithTimeout) | 前端 | 🟡 High | ✅ 已修復 |
| 模板 `var` → `const/let` (95+5 處) | 前端 | 🟠 Medium | ✅ 已修復 |
| Web Controller 測試全覆蓋 (12/12, +54 tests) | 測試 | 🟡 Warning | ✅ 已修復 |
| 例外處理收緊 (Activity/Expense Controller) | 架構 | 🟡 Warning | ✅ 已修復 (24→19 個 broad catch) |
| 27/27 模板統一 `fragments/head` | 前端 | 🟡 High | ✅ 已修復 (含 error 頁面 fallback) |
| ViewHelper 單元測試 (32 tests) | 測試 | 🟠 Medium | ✅ 已修復 |
| 表單防重複提交 (`WeGo.preventDoubleSubmit`) | 前端 | 🟠 Medium | ✅ 已修復 (4 個表單) |
| Document Signed URL 快取 (Caffeine, 動態 TTL) | 效能 | 🟡 Warning | ✅ 已修復 |
| Document N+1 批次查詢 (`buildDocumentResponses`) | 效能 | 🟡 Warning | ✅ 已修復 (~23→~4 queries) |
| SupabaseStorageClient 雙 RestTemplate (API/File timeout 分離) | 效能 | 🔵 Suggestion | ✅ 已修復 |

---

## 各面向詳細發現統計

### 架構 (18 issues, 大部分已修復)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 0 | (全部已修復) |
| 🟡 Warning | 1 | broad exception catching (19 個 `catch (Exception e)`，已從 24 個收緊) |
| 🔵 Suggestion | 1 | 命名不一致 |

### 安全 (12 issues)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 1 | 生產環境錯誤訊息曝露 |
| 🟡 Warning | 6 | `ddl-auto: update`、SQL 日誌 DEBUG、CSP `unsafe-inline`、Google Maps Key 前端曝露、Weather API 無認證 |
| 🔵 Suggestion | 5 | CORS 確認、`img-src` 收窄、TestAuth Profile 保護測試 |

**通過項目:** SQL Injection、XSS、CSRF、Session 管理、Secrets 管理、CORS、Invite Token、File Upload（max-request-size 已修復）

### 前端 (21 issues, 含 2 已修復)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 0 | (已修復) |
| 🟡 High | 0 | (已修復：27/27 模板皆使用 head fragment) |
| 🟠 Medium | 4 | 20+ innerHTML、Modal focus trap 不完整 |
| 🔵 Low | 5 | 版權年份寫死、可能未用的 CSS、缺 skip-to-content、部分按鈕缺 aria-label |

### 效能 (13 issues, 含 3 已修復)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 0 | (已修復) |
| 🟡 Warning | 4 | Settlement N+1、無界列表查詢、Trip 刪除 9 次 DB 操作、`getTrip` 無快取 |
| 🔵 Suggestion | 6 | 統一快取系統、共享 RestTemplate、Maps API 快取、Circuit Breaker 擴展、生產環境設定分離、全同步外部 API |

**正面發現:** 扁平 Entity 設計、`open-in-view: false`、批次 Place 查詢、Caffeine 統計快取、匯率兩層快取、Bucket4j 限流

### 測試 (4 warnings + 5 suggestions)

| 項目 | 數據 |
|------|------|
| 單元測試總數 | 1060 (79 個測試檔案) |
| E2E 測試總數 | 11 個 spec |
| 通過率 | 100% |
| 新增測試檔 | ActivityWebControllerTest (16)、ExpenseWebControllerTest (14)、ProfileControllerTest (6)、InviteControllerTest (7)、TodoWebControllerTest (3)、SettlementWebControllerTest (4)、GlobalExpenseControllerTest (2)、GlobalDocumentControllerTest (2)、TripControllerTest (16)、TodoApiControllerTest (20)、ExchangeRateApiControllerTest (16) |
| 已覆蓋 Service | 12/12 核心 Service |
| REST API 覆蓋 | 全部 REST API Controller 皆有 WebMvcTest |
| Web Controller 覆蓋 | **12/12** — 全部 Web Controller 皆有 WebMvcTest |
| 整合測試 | 無 `@SpringBootTest` |

---

## 改善 Roadmap

### 立即 (Sprint 0 - 生產部署前)

| # | 任務 | 面向 | 預期效果 | 狀態 |
|---|------|------|----------|------|
| 1 | `application.yml` 設定 `include-message: never`, `include-binding-errors: never` | 安全 | 阻止內部資訊洩漏 | ✅ 已完成 |
| 2 | 建立 `application-prod.yml`: `ddl-auto: validate`, SQL 日誌 `WARN`, `thymeleaf.cache: true` | 安全+效能 | 生產環境安全設定 | ✅ 已完成 |
| 3 | 修復 `showMembersPage()` 提供 `inviteLink` / `inviteLinkExpiry` 給模板 | 前端 | 邀請連結功能恢復 | ✅ 已完成 |
| 4 | 所有 Entity 加上 `@Table(indexes=...)` 註解 | 效能 | 列表查詢效能大幅提升 | 已完成 |

### 短期 (1-2 Sprint)

| # | 任務 | 面向 | 預期效果 | 狀態 |
|---|------|------|----------|------|
| 5 | 修復 `getUserTrips` N+1：批次查詢 TripMember + User | 效能 | Dashboard 查詢數從 40+ 降至 3 | ✅ 已完成 |
| 6 | 靜態資源加 `Cache-Control` | 效能 | 頁面載入速度顯著提升 | 已完成 |
| 7 | `PermissionChecker` 加 Request-scoped 快取 | 效能 | 每次請求減少 3+ 重複查詢 | ✅ 已完成 (Caffeine 5s TTL) |
| 8 | 提取 `BaseWebController`：`getCurrentUser()` + `findCurrentMember()` + `canEdit()` | 架構 | 消除 8+ Controller 重複程式碼 | ✅ 已完成 |
| 9 | 提取共享 JS 工具：`escapeHtml`、CSRF、`fetchWithTimeout` | 前端 | 消除 4x 重複、AJAX 不再無限等待 | ✅ 已完成 (common.js) |
| 10 | 所有模板改用 `fragments/head` Fragment | 前端 | 統一 meta tag、減少維護負擔 | ✅ 已完成 (27/27 模板，含 error 頁面 fallback) |
| 11 | 修復 `CoverImagePreview` ObjectURL 記憶體洩漏 | 前端 | `URL.revokeObjectURL()` | 已完成 |

### 中期 (3-4 Sprint)

| # | 任務 | 面向 | 預期效果 | 狀態 |
|---|------|------|----------|------|
| 12 | 拆分 TripController 為 4-5 個 focused Controller | 架構 | SRP 合規、可維護性大幅提升 | ✅ 已完成 |
| 13 | 業務邏輯從 Controller 移至 Service 層 | 架構 | 邏輯可測試、可復用 | ✅ 已完成 (ActivityViewHelper + ExpenseViewHelper) |
| 14 | Place find-or-create 提取至 `PlaceService` | 架構 | 消除 77 行重複 | ✅ 已完成 |
| 15 | 統一快取系統至 Spring Cache + Caffeine | 效能 | 消除記憶體洩漏風險、統一管理 | ✅ 已完成 |
| 16 | CSP 從 `unsafe-inline` 遷移至 nonce-based | 安全 | XSS 防護等級提升 | 待處理 |
| 17 | 補寫 TodoApiController + ExchangeRateApiController WebMvcTest | 測試 | REST API 100% Controller 測試覆蓋 | ✅ 已完成 |
| 18 | 外部 API 呼叫加 `@Async` + 專用執行緒池 | 效能 | 釋放請求執行緒、避免阻塞 | 待處理 |
| 19 | 引入 Flyway 或 Liquibase 管理 Schema 遷移 | 安全+效能 | 取代 `ddl-auto: update` | 待處理 |

### 長期 (5+ Sprint)

| # | 任務 | 面向 | 預期效果 | 狀態 |
|---|------|------|----------|------|
| 20 | 所有 Web Controller 遷移至 `@CurrentUser UserPrincipal` | 架構 | 統一認證模式、消除每次請求的 email 查詢 | ✅ 已完成 (11 個 Controller) |
| 21 | 補寫 Web Controller 測試 (30+ 端點) | 測試 | Thymeleaf 渲染正確性驗證 | ✅ 已完成 (12/12 Controller, 70 tests) |
| 22 | 加入 `@SpringBootTest` 整合測試 | 測試 | 驗證完整 Spring Wiring | 待處理 |
| 23 | 引入 Resilience4j：全外部 API 加 Circuit Breaker | 效能 | 防止級聯失敗 | 待處理 |
| 24 | 前端 inline script `var` → `const/let` | 前端 | 95 個 `var` → `const`、5 個 → `let` | ✅ 已完成 |
| 25 | Google Maps API Key 限制 HTTP Referrer + 僅 Embed API | 安全 | 降低 Key 被濫用風險 | 待處理 |

---

## 詳細報告索引

| 報告 | 路徑 | Issues |
|------|------|--------|
| 架構審查 | `docs/review-architecture.md` | 3 Critical, 8 Warning, 4 Suggestion |
| 安全審查 | `docs/review-security.md` | 1 Critical, 6 Warning, 5 Suggestion |
| 前端審查 | `docs/review-frontend.md` | 1 Critical, 6 High, 7 Medium, 5 Low |
| 效能審查 | `docs/review-performance.md` | 1 Critical, 6 Warning, 6 Suggestion |
| 測試審查 | `docs/review-testing.md` | 0 Critical, 4 Warning, 5 Suggestion |

---

*本報告由 5 位 Claude Opus 4.6 自動化審查員並行產出，由 Team Lead 彙整。*
