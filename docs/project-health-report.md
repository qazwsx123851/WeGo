# WeGo 專案健康度報告

**日期:** 2026-02-20（第六次審查 — 前端品質 + 效能改善後）
**審查範圍:** 架構、安全、前端、效能、測試
**分支:** main

---

## 總覽評分表

| 面向 | 分數 (1-10) | 較上次 | 說明 |
|------|:-----------:|:------:|------|
| 架構設計 | **9.2** | — | 維持上次水準 |
| 安全性 | **9.2** | — | 維持上次水準 |
| 前端品質 | **9.0** | ↑ 1.2 | 6 模板行內腳本抽取為外部 JS 模組、50+ onclick→事件委派、Modal focus trap 完善、skip-to-content、版權年份動態化 |
| 效能 | **8.8** | ↑ 0.3 | Settlement 快取 (1min TTL + eviction)、deleteTrip Storage 非同步刪除、ExpenseSplit 複合索引 |
| 測試覆蓋 | **9.0** | — | 1152 測試全數通過（+52 新增） |
| **整體** | **9.0** | ↑ 0.3 | 前端品質從 7.8 躍升至 9.0，效能從 8.5 提升至 8.8，整體突破 9.0 |

> **評分說明**：本次改善聚焦前端品質和效能兩大面向。前端面提升最大（+1.2），6 個模板的 ~1900 行行內 JavaScript 全數抽取為獨立模組，50+ onclick handler 改用 data-action 事件委派，Modal focus trap 和 skip-to-content 完善無障礙支援。效能面新增 Settlement 結算快取和 Storage 非同步刪除。

---

## Critical 問題優先處理清單

**🔴 Critical: 0 項** — 所有 Critical 問題已修復。

---

## 本次改善項目（前端品質 + 效能）

### 前端品質改善 — 行內腳本抽取
| 項目 | 面向 | 檔案 |
|------|------|------|
| ✅ `settlement.js` 抽取 (~60 行) | 前端 | `settlement.js`（新增）, `expense/settlement.html` |
| ✅ `activity-list.js` 抽取 (~260 行) | 前端 | `activity-list.js`（新增）, `activity/list.html` |
| ✅ `activity-form.js` 抽取 (~370 行) | 前端 | `activity-form.js`（新增）, `activity/create.html` |
| ✅ `member-management.js` 抽取 (~240 行) | 前端 | `member-management.js`（新增）, `trip/members.html` |
| ✅ `expense-list.js` 抽取 (~480 行) | 前端 | `expense-list.js`（新增）, `expense/list.html` |
| ✅ `document-list.js` 抽取 (~555 行) | 前端 | `document-list.js`（新增）, `document/list.html` |

### 前端品質改善 — 無障礙與修補
| 項目 | 面向 | 檔案 |
|------|------|------|
| ✅ Modal focus trap (Tab/Shift+Tab 循環 + 關閉恢復 focus) | 前端 | `app.js` |
| ✅ skip-to-content 連結 | 前端 | `fragments/components.html`, 22+ 模板 `id="main-content"` |
| ✅ 版權年份動態化 (`th:text` 取代硬編碼) | 前端 | `index.html` |

### 效能改善
| 項目 | 面向 | 檔案 |
|------|------|------|
| ✅ Settlement 結算快取 (1min TTL + 費用變動 eviction) | 效能 | `SettlementService.java`, `CacheConfig.java` |
| ✅ deleteTrip Storage 非同步刪除 (`@Async`) | 效能 | `TripService.java` |
| ✅ ExpenseSplit `(expense_id, user_id)` 複合索引 | 效能 | `ExpenseSplit.java` |

### 上一輪改善項目（Batch 1-4, 2026-02-18）

<details>
<summary>展開查看上一輪改善項目</summary>

#### Batch 1 — 立即修復
| 項目 | 面向 | 檔案 |
|------|------|------|
| ✅ User 表加 `(provider, provider_id)` 複合索引 | 效能 | `User.java` |
| ✅ TodoWebController 改用 BaseWebController 方法 | 架構 | `TodoWebController.java` |
| ✅ WebExceptionHandler 隱藏內部錯誤訊息 | 安全 | `WebExceptionHandler.java` |
| ✅ 前端 console 語句 + alert() 清理 | 前端 | 5 個 HTML 模板 |

#### Batch 2 — 架構收尾
| 項目 | 面向 | 檔案 |
|------|------|------|
| ✅ DocumentService.getDocumentForPreview() 改回傳 DTO | 架構 | `DocumentService.java`, `DocumentPreviewInfo.java`, `DocumentApiController.java` |
| ✅ 消除 requireUserId() 重複 | 架構 | `ActivityApiController.java`, `ChatApiController.java` |
| ✅ 業務例外日誌等級調整 | 架構 | `ActivityWebController.java` |

#### Batch 3 — 效能進階
| 項目 | 面向 | 檔案 |
|------|------|------|
| ✅ GlobalExpenseService N+1 批次修復（2N+2 → 3 次查詢） | 效能 | `ExpenseSplitRepository.java`, `GlobalExpenseService.java` |
| ✅ 共享 RestTemplate + Apache HttpClient 5 連線池 | 效能 | `HttpClientConfig.java`（新增）, 4 個 Client |
| ✅ Google Maps Circuit Breaker | 效能 | `GoogleMapsClientImpl.java` |

#### Batch 4 — 測試補強
| 項目 | 面向 | 檔案 |
|------|------|------|
| ✅ ExchangeRateApiClient 單元測試（13 tests） | 測試 | `ExchangeRateApiClientTest.java`（新增） |
| ✅ SupabaseStorageClient 單元測試（18 tests） | 測試 | `SupabaseStorageClientTest.java`（新增） |

</details>

---

## 各面向詳細發現

### 架構 — 9.2/10

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 0 | — |
| 🟡 Warning | 0 | **全部已修復** |
| 🔵 Suggestion | 6 | BaseWebController field injection、TripService 12 依賴、GlobalExceptionHandler 缺 ValidationException handler、Controller magic number、ExchangeRateApiController 內嵌 DTO、inline styles in templates |

### 安全 — 9.2/10

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 0 | — |
| 🟡 Warning | 4 | Chat AI innerHTML（有 escapeHtml）、CSP script-src unsafe-inline、CSP style-src unsafe-inline、Spring Boot 3.2.2 版本偏舊 |
| 🔵 Suggestion | 4 | TestAuth prod 隔離測試、Method-Level Security、Session timeout、DOMPurify |

### 前端 — 9.0/10

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 0 | — |
| HIGH | 0 | **全部已修復** — 6 模板行內 JS 已抽取為外部模組、onclick 改事件委派、fetchWithTimeout 統一使用 |
| MEDIUM | 1 | SpEL null-safety 少數可改善 |
| LOW | 1 | !important 1 處（覆蓋 Flatpickr 樣式，可接受） |

### 效能 — 8.8/10

| 嚴重度 | 數量 | 主要問題 |
|--------|:----:|----------|
| 🔴 Critical | 0 | — |
| 🟡 Warning | 3 | getExpensesByTrip 不帶分頁、getDocumentsByTrip 不帶分頁、單筆 buildExpenseResponse 仍 3 次查詢 |
| 🔵 Suggestion | 3 | 行程詳情 short-lived cache、CSS 未使用樣式、Todo 不帶分頁 |

### 測試 — 9.0/10

| 指標 | 數值 |
|------|------|
| 總測試數 | **1152**（從 1100 增至 1152） |
| 通過率 | 100%（0 失敗、0 跳過） |
| 測試檔案數 | 86 |
| 執行時間 | ~21 秒 |
| API Controller 覆蓋 | 14/14 (100%) |
| Web Controller 覆蓋 | 13/13 (100%) |
| Service 覆蓋 | 22/22 (100%) |
| 外部 Client 覆蓋 | 6/6 (100%) |
| E2E Spec | 12 個 |

---

## 改善 Roadmap

### 📋 短期（1-2 Sprint）

| # | 任務 | 面向 | 預期效果 | 工時 |
|---|------|------|----------|------|
| 1 | 升級 Spring Boot 至最新 3.2.x patch 或 3.3.x | 安全 | 安全修復 | 半天-1天 |
| 2 | CI 啟用 JaCoCo 覆蓋率門檻 80% | 測試 | 防止覆蓋率退化 | 2 小時 |

### 🔧 中期（3-4 Sprint）

| # | 任務 | 面向 | 預期效果 | 工時 | 狀態 |
|---|------|------|----------|------|:----:|
| 3 | CSP 從 `unsafe-inline` 遷移至 nonce-based | 安全 | XSS 防護等級大幅提升 | 2-3 天 | |
| 4 | ~~行內腳本抽取為外部 JS 模組（6 模板）~~ | 前端 | ~~維護性提升、CSP 相容、消除 onclick~~ | ~~3-5 天~~ | ✅ |
| 5 | ~~@EnableAsync + 自訂執行緒池~~ (已有) + deleteTrip 非同步化 | 效能 | 釋放請求執行緒，避免阻塞 | ~~1 天~~ | ✅ |
| 6 | ~~靜態資源版本號 + 長期快取~~ (已有 VersionResourceResolver) | 效能 | ~~減少瀏覽器重驗證~~ | ~~半天~~ | ✅ 已存在 |
| 7 | 成員管理 + 費用結算 E2E 測試 | 測試 | 關鍵流程端到端驗證 | 1-2 天 | |
| 8 | ~~Modal focus trap 完善 + skip-to-content~~ | 前端 | ~~無障礙合規~~ | ~~1 天~~ | ✅ |

### 🏗️ 長期（5+ Sprint）

| # | 任務 | 面向 | 預期效果 |
|---|------|------|----------|
| 9 | 引入 Flyway 管理 Schema 遷移 | 安全+架構 | 取代 ddl-auto |
| 10 | 引入 Resilience4j 全外部 API Circuit Breaker | 效能 | 統一熔斷框架 |
| 11 | @SpringBootTest 整合測試 | 測試 | 驗證完整 Spring Wiring |
| 12 | Mutation Testing (PIT) | 測試 | 驗證測試有效性 |
| 13 | 前端無障礙強化（WCAG 2.1 AA） | 前端 | 合規與包容性 |

---

## 歷史趨勢

| 日期 | 架構 | 安全 | 前端 | 效能 | 測試 | 整體 |
|------|:----:|:----:|:----:|:----:|:----:|:----:|
| 2026-02-06 | 5.5 | 7.0 | 6.0 | 6.5 | 5.0 | **7.0** |
| 2026-02-13 | 8.5 | 8.5 | 8.0 | 8.5 | 9.0 | **8.7** |
| 2026-02-14 | 8.6 | 8.9 | 7.5 | 7.0 | 8.5 | **8.1** |
| 2026-02-18 (審查) | 8.9 | 9.1 | 7.5 | 7.8 | 8.7 | **8.4** |
| 2026-02-18 (改善後) | 9.2 | 9.2 | 7.8 | 8.5 | 9.0 | **8.7** |
| **2026-02-20 (改善後)** | **9.2** | **9.2** | **9.0** | **8.8** | **9.0** | **9.0** |

> **2026-02-20 改善後**：前端品質大幅提升（7.8 → 9.0），6 模板行內 JS 抽取為外部模組（~1900 行）、50+ onclick 改事件委派、Modal focus trap + skip-to-content 完善。效能新增 Settlement 快取和 Storage 非同步刪除（8.5 → 8.8）。整體首次突破 9.0。

---

## 近期已修復項目（累計）

<details>
<summary>展開查看所有已修復項目（68 項）</summary>

| 項目 | 面向 | 原嚴重度 | 狀態 |
|------|------|----------|------|
| AI 聊天安全強化（prompt injection、circuit breaker、Unicode 驗證、OOM 修復） | 安全 | 🔴 Critical | ✅ |
| `server.error.include-message: always` 洩漏內部細節 | 安全 | 🔴 Critical | ✅ |
| `getUserTrips` N+1 查詢 | 效能 | 🔴 Critical | ✅ |
| TripController 1664→535 行拆分 | 架構 | 🔴 Critical | ✅ |
| 業務邏輯洩漏到 Controller 層 | 架構 | 🔴 Critical | ✅ 部分 |
| Place find-or-create 77 行重複 | 架構 | 🔴 Critical | ✅ |
| `members.html` inviteLink 功能損壞 | 前端 | 🔴 Critical | ✅ |
| Repository 旁路修復 | 架構 | 🔴 Critical | ✅ |
| Entity 加上 `@Table(indexes=...)` | 效能 | 🔴 Critical | ✅ |
| ExpenseService N+1 查詢（3N+1 → 3） | 效能 | 🔴 Critical | ✅ |
| TripMember 補 user_id 索引 | 效能 | 🔴 Critical | ✅ |
| HTTP 壓縮啟用 | 效能 | 🔴 Critical | ✅ |
| `@CurrentUser UserPrincipal` 全面遷移 | 架構 | 🟡 Warning | ✅ |
| ActivityViewHelper / ExpenseViewHelper 提取 | 架構 | 🟡 Warning | ✅ |
| BaseWebController 全面繼承 | 架構 | 🟡 Warning | ✅ |
| 統一快取系統至 Spring Cache + Caffeine | 效能 | 🟡 Warning | ✅ |
| CacheService 記憶體洩漏風險消除 | 效能 | 🟡 Warning | ✅ |
| PermissionChecker Caffeine 快取 5s TTL | 效能 | 🟡 Warning | ✅ |
| 靜態資源 Cache-Control | 效能 | 🟡 Warning | ✅ |
| Document N+1 批次查詢 | 效能 | 🟡 Warning | ✅ |
| Document Signed URL 快取 | 效能 | 🟡 Warning | ✅ |
| max-request-size 100→30MB | 安全 | 🟡 Warning | ✅ |
| production profile 安全設定 | 安全 | 🟡 Warning | ✅ |
| ObjectURL 記憶體洩漏 | 前端 | 🟡 High | ✅ |
| `common.js` 共用工具 | 前端 | 🟡 High | ✅ |
| 27/27 模板統一 head fragment | 前端 | 🟡 High | ✅ |
| 例外處理收緊 24→19 broad catch | 架構 | 🟡 Warning | ✅ |
| Web Controller 測試 12/12 | 測試 | 🟡 Warning | ✅ |
| ViewHelper 單元測試 32 tests | 測試 | 🟠 Medium | ✅ |
| TodoApiController WebMvcTest | 測試 | 🟡 Warning | ✅ |
| ExchangeRateApiController WebMvcTest | 測試 | 🟡 Warning | ✅ |
| TripController WebMvcTest | 測試 | 🟡 Warning | ✅ |
| SupabaseStorageClient 雙 RestTemplate | 效能 | 🔵 Suggestion | ✅ |
| 表單防重複提交 | 前端 | 🟠 Medium | ✅ |
| console.log 語句清除 | 前端 | 🟠 Medium | ✅ |
| `var` → `const/let` 轉換 | 前端 | 🟠 Medium | ✅ |
| HSTS header 設定 | 安全 | 🟡 Warning | ✅ |
| Session Fixation 明確設定 | 安全 | 🔵 Suggestion | ✅ |
| 天氣 API IP-based 速率限制 | 安全 | 🟡 Warning | ✅ |
| GeoUtils 統一 Haversine 計算 | 架構 | 🟡 Warning | ✅ |
| FileValidationUtils 統一 Magic Bytes | 架構 | 🟡 Warning | ✅ |
| TripConstants.UNKNOWN_USER_NAME 統一 | 架構 | 🟡 Warning | ✅ |
| TripViewHelper 提取 + HomeController 委派 | 架構 | 🟡 Warning | ✅ |
| PlaceService.findOrCreate() 改回傳 UUID | 架構 | 🟡 Warning | ✅ |
| **User 表 (provider, provider_id) 複合索引** | 效能 | 🟡 Warning | ✅ 新增 |
| **TodoWebController 改用 BaseWebController 方法** | 架構 | 🟡 Warning | ✅ 新增 |
| **WebExceptionHandler 隱藏內部錯誤訊息** | 安全 | 🔵 Suggestion | ✅ 新增 |
| **前端 console 語句 + alert() 清理** | 前端 | 🟠 Medium | ✅ 新增 |
| **DocumentService.getDocumentForPreview() 改回傳 DTO** | 架構 | 🟡 Warning | ✅ 新增 |
| **requireUserId() 重複消除** | 架構 | 🟡 Warning | ✅ 新增 |
| **業務例外日誌等級調整** | 架構 | 🔵 Suggestion | ✅ 新增 |
| **GlobalExpenseService N+1 批次修復** | 效能 | 🟡 Warning | ✅ 新增 |
| **共享 RestTemplate + Apache HttpClient 5 連線池** | 效能 | 🟡 Warning | ✅ 新增 |
| **Google Maps Circuit Breaker** | 效能 | 🔵 Suggestion | ✅ 新增 |
| **ExchangeRateApiClient 單元測試** | 測試 | 🟡 Warning | ✅ 新增 |
| **SupabaseStorageClient 單元測試** | 測試 | 🟡 Warning | ✅ 新增 |
| **settlement.js 行內腳本抽取** | 前端 | HIGH | ✅ 新增 |
| **activity-list.js 行內腳本抽取** | 前端 | HIGH | ✅ 新增 |
| **activity-form.js 行內腳本抽取** | 前端 | HIGH | ✅ 新增 |
| **member-management.js 行內腳本抽取** | 前端 | HIGH | ✅ 新增 |
| **expense-list.js 行內腳本抽取** | 前端 | HIGH | ✅ 新增 |
| **document-list.js 行內腳本抽取** | 前端 | HIGH | ✅ 新增 |
| **50+ onclick handler → data-action 事件委派** | 前端 | HIGH | ✅ 新增 |
| **Modal focus trap (Tab/Shift+Tab 循環)** | 前端 | 🟠 Medium | ✅ 新增 |
| **skip-to-content 連結** | 前端 | LOW | ✅ 新增 |
| **版權年份動態化** | 前端 | LOW | ✅ 新增 |
| **Settlement 結算快取 (1min TTL + eviction)** | 效能 | 🔵 Suggestion | ✅ 新增 |
| **deleteTrip Storage 非同步刪除** | 效能 | 🔵 Suggestion | ✅ 新增 |
| **ExpenseSplit (expense_id, user_id) 複合索引** | 效能 | 🔵 Suggestion | ✅ 新增 |

</details>

---

## 詳細報告索引

| 報告 | 路徑 |
|------|------|
| 架構審查 | `docs/review-architecture.md` |
| 安全審查 | `docs/review-security.md` |
| 前端審查 | `docs/review-frontend.md` |
| 效能審查 | `docs/review-performance.md` |
| 測試審查 | `docs/review-testing.md` |

---

*本報告基於第五次審查結果 + 前端品質 / 效能改善成果更新（2026-02-20）。*
