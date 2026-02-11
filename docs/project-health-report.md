# WeGo 專案健康度報告

**日期:** 2026-02-10
**審查範圍:** 架構、安全、前端、效能、測試
**審查團隊:** 5 位自動化審查員 (Claude Opus 4.6)
**分支:** fixActivities

---

## 總覽評分表

| 面向 | 分數 (1-10) | 說明 |
|------|:-----------:|------|
| 架構設計 | **6** | 分層清晰但 TripController (1664行) 違反 SRP，業務邏輯洩漏到 Controller，大量重複程式碼 |
| 安全性 | **8** | 基礎扎實 (OAuth2、CSRF、參數化查詢、無 XSS)，但生產環境錯誤訊息曝露為 Critical |
| 前端品質 | **6** | 模組化 JS 良好，但 8+ 模板未復用 Fragment、AJAX 無 timeout、記憶體洩漏、程式碼大量重複 |
| 效能 | **5** | N+1 查詢、缺少索引、無快取策略、靜態資源無 Cache-Control、外部 API 全同步阻塞 |
| 測試覆蓋 | **7** | 862 測試全數通過 (+74 新測試)，核心 Service 皆有測試，但 Web Controller 零測試、缺整合測試 |
| **整體** | **6.4** | 功能完整的 MVP，核心安全無虞，但需重構架構與優化效能才適合生產部署 |

---

## Critical 問題優先處理清單

以下問題應**立即修復**，依影響程度排序：

| # | 嚴重度 | 面向 | 問題 | 檔案 | 影響 |
|---|--------|------|------|------|------|
| 1 | 🔴 | 安全 | `server.error.include-message: always` 洩漏內部細節 | `application.yml:83-84` | 生產環境曝露類別名、Schema 資訊 |
| 2 | 🔴 | 效能 | Entity 缺少 `@Table(indexes=...)` 註解 | 所有 Entity 類別 | 所有列表查詢無索引，全表掃描 |
| 3 | 🔴 | 效能 | `getUserTrips` N+1 查詢 | `TripService.java:185-195` | 每頁 20 筆行程 = 40+ 額外查詢 |
| 4 | 🔴 | 架構 | TripController 1664 行，God Controller | `TripController.java` | 可維護性極差，違反 SRP |
| 5 | 🔴 | 架構 | 業務邏輯洩漏到 Controller 層 | `TripController.java` 多處 | 無法測試、無法復用 |
| 6 | 🔴 | 架構 | Place find-or-create 邏輯重複 77 行 | `TripController.java:1081-1158, 1304-1380` | DRY 違反 |
| 7 | 🔴 | 前端 | `members.html` 引用 `inviteLink`/`inviteLinkExpiry` 但 Controller 未提供 | `trip/members.html` + `TripController` | 邀請連結功能完全損壞 |

---

## 各面向詳細發現統計

### 架構 (18 issues)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 3 | God Controller、業務邏輯洩漏、Place 邏輯重複 |
| 🟡 Warning | 8 | 5 個 Controller 直接注入 Repository、`getCurrentUser()` 重複 8 處、權限檢查樣板重複 11 處、auth 模式不一致 |
| 🔵 Suggestion | 4 | Magic Number、命名不一致、常數重複 |

### 安全 (13 issues)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 1 | 生產環境錯誤訊息曝露 |
| 🟡 Warning | 7 | `ddl-auto: update`、SQL 日誌 DEBUG、CSP `unsafe-inline`、Google Maps Key 前端曝露、Weather API 無認證、`max-request-size: 100MB`、無 `@PreAuthorize` |
| 🔵 Suggestion | 5 | CORS 確認、`img-src` 收窄、TestAuth Profile 保護測試 |

**通過項目:** SQL Injection、XSS、CSRF、Session 管理、Secrets 管理、CORS、Invite Token

### 前端 (21 issues)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 1 | `members.html` Controller 資料缺失 |
| 🟡 High | 7 | 8+ 模板重複 head、無 fetch timeout、ObjectURL 記憶體洩漏、`escapeHtml` 重複 4 處、CSRF 取得重複 4 處、無統一錯誤處理、缺少 CSRF meta tag |
| 🟠 Medium | 8 | ~80 個 `var`、7 個 console 語句、20+ innerHTML、部分頁面缺防重複提交、Modal focus trap 不完整 |
| 🔵 Low | 5 | 版權年份寫死、可能未用的 CSS、缺 skip-to-content、部分按鈕缺 aria-label |

### 效能 (16 issues)

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 2 | 缺少資料庫索引、N+1 查詢 |
| 🟡 Warning | 8 | Settlement N+1、無界列表查詢、Trip 刪除 9 次 DB 操作、`getTrip` 無快取、`PermissionChecker` 無快取、CacheService 無上限、全同步外部 API、靜態資源無 Cache-Control |
| 🔵 Suggestion | 6 | 統一快取系統、共享 RestTemplate、Maps API 快取、Circuit Breaker 擴展、生產環境設定分離 |

**正面發現:** 扁平 Entity 設計、`open-in-view: false`、批次 Place 查詢、Caffeine 統計快取、匯率兩層快取、Bucket4j 限流

### 測試 (4 warnings + 5 suggestions)

| 項目 | 數據 |
|------|------|
| 測試總數 | 862 (新增 74) |
| 通過率 | 100% |
| 新增測試檔 | `ActivityServiceTest` (30)、`ActivityApiControllerTest` (17)、`TransportCalculationServiceTest` (27) |
| 已覆蓋 Service | 12/12 核心 Service |
| REST API 覆蓋 | 大部分有 WebMvcTest，缺 TodoApiController (6 端點)、ExchangeRateApiController (4 端點) |
| Web Controller 覆蓋 | 0/30+ 端點 |
| 整合測試 | 無 `@SpringBootTest` |

---

## 改善 Roadmap

### 立即 (Sprint 0 - 生產部署前)

| # | 任務 | 面向 | 預期效果 |
|---|------|------|----------|
| 1 | `application.yml` 設定 `include-message: never`, `include-binding-errors: never` | 安全 | 阻止內部資訊洩漏 |
| 2 | 建立 `application-prod.yml`: `ddl-auto: validate`, SQL 日誌 `WARN`, `thymeleaf.cache: true` | 安全+效能 | 生產環境安全設定 |
| 3 | 修復 `showMembersPage()` 提供 `inviteLink` / `inviteLinkExpiry` 給模板 | 前端 | 邀請連結功能恢復 |
| 4 | 所有 Entity 加上 `@Table(indexes=...)` 註解 | 效能 | 列表查詢效能大幅提升 |

### 短期 (1-2 Sprint)

| # | 任務 | 面向 | 預期效果 |
|---|------|------|----------|
| 5 | 修復 `getUserTrips` N+1：批次查詢 TripMember + User | 效能 | Dashboard 查詢數從 40+ 降至 3 |
| 6 | 靜態資源加 `Cache-Control` (`setCachePeriod(604800)`) | 效能 | 頁面載入速度顯著提升 |
| 7 | `PermissionChecker` 加 Request-scoped 快取 | 效能 | 每次請求減少 3+ 重複查詢 |
| 8 | 提取 `BaseWebController`：`getCurrentUser()` + `findCurrentMember()` + `canEdit()` | 架構 | 消除 8+ Controller 重複程式碼 |
| 9 | 提取共享 JS 工具：`escapeHtml`、CSRF、`fetchWithTimeout` | 前端 | 消除 4x 重複、AJAX 不再無限等待 |
| 10 | 所有模板改用 `fragments/head` Fragment | 前端 | 統一 meta tag、減少維護負擔 |
| 11 | 修復 `CoverImagePreview` ObjectURL 記憶體洩漏 | 前端 | `URL.revokeObjectURL()` |

### 中期 (3-4 Sprint)

| # | 任務 | 面向 | 預期效果 |
|---|------|------|----------|
| 12 | 拆分 TripController 為 4-5 個 focused Controller | 架構 | SRP 合規、可維護性大幅提升 |
| 13 | 業務邏輯從 Controller 移至 Service 層 | 架構 | 邏輯可測試、可復用 |
| 14 | Place find-or-create 提取至 `PlaceService` | 架構 | 消除 77 行重複 |
| 15 | 統一快取系統至 Spring Cache + Caffeine | 效能 | 消除記憶體洩漏風險、統一管理 |
| 16 | CSP 從 `unsafe-inline` 遷移至 nonce-based | 安全 | XSS 防護等級提升 |
| 17 | 補寫 TodoApiController + ExchangeRateApiController WebMvcTest | 測試 | REST API 100% Controller 測試覆蓋 |
| 18 | 外部 API 呼叫加 `@Async` + 專用執行緒池 | 效能 | 釋放請求執行緒、避免阻塞 |
| 19 | 引入 Flyway 或 Liquibase 管理 Schema 遷移 | 安全+效能 | 取代 `ddl-auto: update` |

### 長期 (5+ Sprint)

| # | 任務 | 面向 | 預期效果 |
|---|------|------|----------|
| 20 | 所有 Web Controller 遷移至 `@CurrentUser UserPrincipal` | 架構 | 統一認證模式、消除每次請求的 email 查詢 |
| 21 | 補寫 Web Controller 測試 (30+ 端點) | 測試 | Thymeleaf 渲染正確性驗證 |
| 22 | 加入 `@SpringBootTest` 整合測試 | 測試 | 驗證完整 Spring Wiring |
| 23 | 引入 Resilience4j：全外部 API 加 Circuit Breaker | 效能 | 防止級聯失敗 |
| 24 | 前端 inline script 提取為外部 JS 模組 | 前端 | 消除 ~80 個 `var`、支援 CSP nonce |
| 25 | Google Maps API Key 限制 HTTP Referrer + 僅 Embed API | 安全 | 降低 Key 被濫用風險 |

---

## 詳細報告索引

| 報告 | 路徑 | Issues |
|------|------|--------|
| 架構審查 | `docs/review-architecture.md` | 3 Critical, 8 Warning, 4 Suggestion |
| 安全審查 | `docs/review-security.md` | 1 Critical, 7 Warning, 5 Suggestion |
| 前端審查 | `docs/review-frontend.md` | 1 Critical, 7 High, 8 Medium, 5 Low |
| 效能審查 | `docs/review-performance.md` | 2 Critical, 8 Warning, 6 Suggestion |
| 測試審查 | `docs/review-testing.md` | 0 Critical, 4 Warning, 5 Suggestion |

---

*本報告由 5 位 Claude Opus 4.6 自動化審查員並行產出，由 Team Lead 彙整。*
