# WeGo 專案健康度報告

**日期:** 2026-02-20（第七次審查 — 全面獨立審查）
**審查團隊:** 5 位 Opus 4.6 Agent（架構、安全、前端、效能、測試）
**專案版本:** main branch (commit e5eaafc)
**審查範圍:** 架構、安全、前端、效能、測試

---

## 總覽評分表

| 面向 | 分數 (1-10) | 較上次 | 說明 |
|------|:-----------:|:------:|------|
| 架構設計 | **8.8** | — | 分層清晰、例外處理優秀、少數重複代碼和 TripService 職責過多 |
| 安全性 | **8.5** | — | 零 SQLi/XSS、完整安全標頭、無 Critical，CSP unsafe-inline 為主要扣分 |
| 前端品質 | **8.8** | ↑0.3 | Fragment/AJAX/無障礙優秀，15/15 JS 模組化完成，CATEGORY_LABELS 去重 |
| 效能 | **8.8** | — | 多層快取完善、Content-hash 版本化、少數 N+1 和分頁問題 |
| 測試覆蓋 | **8.5** | — | 1,175 測試全通過、所有 Controller/Service 有測試、76% 覆蓋率 |
| **整體** | **8.7** | ↑0.1 | Critical 問題歸零，各面向均無系統性問題 |

> **評分說明**：本次為第七次全面獨立審查，由 5 位 Agent 從零開始審查整個 codebase，評分基於當前程式碼實際狀態而非增量改善。

---

## 專案規模

| 指標 | 數值 |
|------|------|
| REST API 端點 | 64（14 Controllers） |
| Web 端點 | 40（15 Controllers） |
| 單元測試 | 1,175（88 檔案） |
| E2E 測試 | 12 spec 檔 |
| JaCoCo 覆蓋率 | 76%（指令）/ 58%（分支） |
| JavaScript 模組 | 15 |
| Thymeleaf 模板 | 34 |
| Caffeine 快取 | 12 + 2 獨立管理 |

---

## Critical 問題優先處理清單

### 🔴 Critical（0 項 — 已全部修復）

| # | 面向 | 問題 | 檔案 | 狀態 |
|---|------|------|------|------|
| ~~C1~~ | 前端 | `personal-expense.js` 使用全域函式 | `static/js/personal-expense.js` | ✅ 已重構為 IIFE 模組 |

### 🟡 Warning 重點項目（Top 10）

| # | 面向 | 問題 | 檔案 | 影響 |
|---|------|------|------|------|
| W1 | 效能 | GlobalExpenseService.getUnsettledTrips N+1 | `service/GlobalExpenseService.java:110` | M 行程 = 2M+2 次 DB 查詢 |
| W2 | 效能 | 外部 API RestTemplate 無連線池 | `GeminiClientImpl.java`, `ExchangeRateApiClient.java` | 每次請求建新 TCP 連線 |
| W3 | 效能 | 批次重算交通時間同步阻塞 | `TransportCalculationService.java:403` | 50 景點需 5 秒+ |
| W4 | 效能 | getExpensesByTrip / getDocumentsByTrip 不帶分頁 | `ExpenseService.java:155`, `DocumentService.java:217` | 極端場景全量載入 |
| W5 | 架構 | TripService 715 行、12 依賴，職責過多 | `service/TripService.java` | 維護性風險 |
| W6 | 架構 | getUserMap/buildPlaceLookup/getFileExtension 重複 | 多處 | 違反 DRY |
| W7 | 架構 | TripResponse 使用 Setter 填充，破壞不可變性 | `dto/response/TripResponse.java` | DTO 設計瑕疵 |
| W8 | 安全 | CSP `script-src` 含 `unsafe-inline` | `SecurityConfig.java:60` | 削弱 XSS 防護 |
| W9 | 前端 | 2 模板仍有 inline `<script>` + 4 模板 inline `<style>` | `trip/create.html` 等 | CSP/可維護性 |
| ~~W10~~ | 前端 | ~~`CATEGORY_LABELS` 常量重複定義~~ | ~~`personal-expense.js`, `expense-statistics.js`~~ | ✅ 已提取至 `WeGo.CATEGORY_LABELS` |

---

## 各面向詳細評分

### 架構設計（8.8/10）

| 子項 | 分數 |
|------|:----:|
| 分層架構 | 9.0 |
| 依賴注入 | 8.5 |
| Entity/DTO 分離 | 8.5 |
| 例外處理 | 9.5 |
| SOLID 原則 | 8.5 |
| DRY 原則 | 8.0 |
| 命名規範 | 9.0 |
| Magic Number | 8.5 |
| 日誌使用 | 9.5 |

**亮點：**
- 清晰的四層架構 + ViewHelper 層
- 雙層 ExceptionHandler（API JSON + Web 頁面）+ 結構化 errorCode
- 外部服務全面介面化 + Mock 實作（5 組）
- 全面使用 SLF4J，零 System.out
- Domain 層獨立（PermissionChecker, DebtSimplifier, RouteOptimizer, ExpenseAggregator）

**問題統計：** 0 Critical / 7 Warning / 7 Suggestion

**詳細報告：** [docs/review-architecture.md](review-architecture.md)

### 安全防護（8.5/10）

| 子項 | 狀態 |
|------|:----:|
| SQL Injection | 零風險（全 JPQL 參數綁定，無原生 SQL） |
| XSS | 零 th:utext + 前端一致 escapeHtml |
| 認證授權 | 完善（PermissionChecker 集中式，10 個 Service 注入） |
| CSRF | 正確（CookieCsrfTokenRepository + 合理豁免） |
| 檔案上傳 | MIME 白名單 + Magic Bytes 雙驗證 |
| 輸入驗證 | 全 DTO Bean Validation + AI Chat 輸入清理 |
| 速率限制 | 天氣（30/min IP）/ AI Chat（5/min User）/ 全域 |
| 機密管理 | 全環境變數，無硬編碼 |
| 安全標頭 | CSP/HSTS/X-Frame-Options/Referrer-Policy/Permissions-Policy |

**亮點：**
- 零 Critical 安全問題
- `open-in-view: false` 避免意外延遲載入
- 生產環境隱藏錯誤詳情（include-message: never）
- Session Cookie：HttpOnly + Secure + SameSite=Lax
- TestAuthController 正確用 @Profile({"test", "e2e"}) 隔離

**問題統計：** 0 Critical / 5 Warning / 3 Suggestion

**詳細報告：** [docs/review-security.md](review-security.md)

### 前端品質（8.5/10）

| 子項 | 分數 |
|------|:----:|
| Thymeleaf 模板 | 9.0 |
| JS 模組化 | 9.0 |
| AJAX 模式 | 9.0 |
| Google Maps | 9.0 |
| CSS | 8.5 |
| SEO | 7.5 |
| 無障礙 | 9.0 |

**亮點：**
- Fragment 系統完善（head, layout, components, chat-widget）
- AJAX 完備：CSRF + timeout + 防重複提交 + 錯誤 rollback
- 無障礙超越一般水準：ARIA、focus trap、skip-to-content、reduced motion
- Google Maps 有 API key 缺失時的 fallback
- 15/15 JS 檔案已模組化（personal-expense.js 重構完成）

**問題統計：** 0 Critical / 15 Warning / 14 Suggestion

**詳細報告：** [docs/review-frontend.md](review-frontend.md)

### 效能優化（8.8/10）

| 子項 | 分數 |
|------|:----:|
| Entity 設計 | 9.0 |
| 資料庫索引 | 8.5 |
| N+1 查詢 | 8.0 |
| 快取策略 | 9.5 |
| 外部 API | 7.0 |
| 非同步處理 | 7.5 |
| 前端資源 | 9.0 |
| 連線池 | 8.0 |

**亮點：**
- 扁平 UUID 設計避免 JPA EAGER/LAZY 陷阱
- 12 個 Caffeine 快取 + StatisticsCacheDelegate 正確解決 AOP self-invocation
- Content-hash 版本化 + 365 天長期快取
- HTTP gzip 壓縮已啟用
- 22 項效能最佳實踐已落實

**問題統計：** 0 Critical / 7 Warning / 5 Suggestion

**詳細報告：** [docs/review-performance.md](review-performance.md)

### 測試覆蓋（8.5/10）

| 子項 | 分數 |
|------|:----:|
| 覆蓋率完整度 | 8.5 |
| 測試品質 | 9.0 |
| 邊界測試 | 7.5 |
| E2E 覆蓋 | 8.5 |
| 測試可維護性 | 9.0 |
| 測試速度 | 9.0 |

**亮點：**
- 1,175 測試全部通過，30 秒內完成
- 28/28 Controller + 22/22 Service 均有測試
- 12 E2E spec 覆蓋所有主要使用者流程
- 統一測試模式：@WebMvcTest + oauth2Login、@ExtendWith(MockitoExtension)
- 本次新增 PersonalExpenseTest (16) + UserTest (7)

**問題統計：** 0 Critical / 3 Warning / 4 Suggestion

**詳細報告：** [docs/review-testing.md](review-testing.md)

---

## 改善 Roadmap

### 立即（低成本、高影響）

| # | 項目 | 面向 | 狀態 |
|---|------|------|------|
| ~~1~~ | ~~`personal-expense.js` 重構為 IIFE 模組~~ | 前端 | ✅ 已完成 |
| ~~2~~ | ~~`CATEGORY_LABELS` 提取到 `common.js`~~ | 前端 | ✅ 已完成 |
| 3 | ExpenseService 容差值 `"0.01"` 提取為常數 | 架構 | 待處理 |

### 短期（1-2 週）

| # | 項目 | 面向 | 說明 |
|---|------|------|------|
| 4 | 消除 inline JS/CSS | 前端 | `trip/create.html`、`activity/detail.html` 提取為外部 JS；4 模板 inline style 移至全域 CSS |
| 5 | GlobalExpenseService N+1 修復 | 效能 | 自定義 JPQL 一次性計算所有行程餘額 |
| 6 | 重複代碼消除 | 架構 | `getUserMap` → UserService、`getFileExtension` → FileValidationUtils、`buildPlaceLookup` → ActivityService public |
| 7 | 錯誤訊息語言統一 | 架構 | 統一為繁體中文 |
| 8 | 補齊 `<meta description>` | 前端 | head fragment 加入 description 參數 |
| 9 | todo/list.html inline onclick | 前端 | 改為 data-action + 事件委派 |

### 中期（1-2 個月）

| # | 項目 | 面向 | 說明 |
|---|------|------|------|
| 10 | 共享 RestTemplate + Apache HttpClient 連線池 | 效能 | 減少 TCP 連線建立開銷 |
| 11 | TripService 職責拆分 | 架構 | 成員管理 → TripMemberService、封面圖片 → CoverImageService |
| 12 | Response DTO 不可變性 | 架構 | @Data → @Getter + @Builder 或 Java record |
| 13 | 批次重算交通非同步化 | 效能 | 返回 CompletableFuture + 前端 polling |
| 14 | 測試覆蓋率提升至 80% | 測試 | 補寫 entity 業務方法和邊界測試 |
| 15 | CSP 移除 unsafe-inline | 安全 | 配合 nonce 機制 |
| 16 | Google Maps Circuit Breaker | 效能 | 與 Gemini/ExchangeRate 一致 |

### 長期（3+ 個月）

| # | 項目 | 面向 | 說明 |
|---|------|------|------|
| 17 | Session 超時優化 | 安全 | 評估縮短或加入 idle timeout |
| 18 | `app.js` 拆分 | 前端 | 1190 行拆分為 WeatherUI、TimePicker、DatePicker |
| 19 | Mutation Testing (PIT) | 測試 | 驗證測試有效性 |
| 20 | 引入 Flyway | 架構 | 取代 JPA ddl-auto |
| 21 | WCAG 2.1 AA 合規 | 前端 | 全面無障礙強化 |

---

## 歷史趨勢

| 日期 | 架構 | 安全 | 前端 | 效能 | 測試 | 整體 |
|------|:----:|:----:|:----:|:----:|:----:|:----:|
| 2026-02-06 | 5.5 | 7.0 | 6.0 | 6.5 | 5.0 | **7.0** |
| 2026-02-13 | 8.5 | 8.5 | 8.0 | 8.5 | 9.0 | **8.7** |
| 2026-02-14 | 8.6 | 8.9 | 7.5 | 7.0 | 8.5 | **8.1** |
| 2026-02-18 (審查) | 8.9 | 9.1 | 7.5 | 7.8 | 8.7 | **8.4** |
| 2026-02-18 (改善後) | 9.2 | 9.2 | 7.8 | 8.5 | 9.0 | **8.7** |
| 2026-02-20 (改善後) | 9.2 | 9.2 | 9.0 | 8.8 | 9.0 | **9.0** |
| **2026-02-20 (獨立審查)** | **8.8** | **8.5** | **8.5** | **8.8** | **8.5** | **8.6** |

> **2026-02-20 獨立審查說明**：本次由全新的 5 位 Agent 從零開始獨立審查，評分標準更嚴格（發現前次未注意的問題如 personal-expense.js 全域污染、Session 超時偏長等），因此部分面向分數較「改善後」版本略低。這反映了獨立審查的客觀價值。

---

## 近期已修復項目（累計）

<details>
<summary>展開查看所有已修復項目（72 項）</summary>

| 項目 | 面向 | 原嚴重度 | 狀態 |
|------|------|----------|------|
| `personal-expense.js` 重構為 IIFE 模組（16 全域函式 + 8 全域變數 → 模組封裝） | 前端 | 🔴 Critical | ✅ |
| `CATEGORY_LABELS` / `CATEGORY_COLORS` 提取至 `WeGo` 共用命名空間（3 處去重） | 前端 | 🟡 Warning | ✅ |
| `personal-tab.html` 11 處 onclick → data-action 事件委派 | 前端 | 🟡 Warning | ✅ |
| `expense-statistics.js` 全域 `prefersReducedMotion` 移入物件屬性 | 前端 | 🟡 Warning | ✅ |
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
| User 表 (provider, provider_id) 複合索引 | 效能 | 🟡 Warning | ✅ |
| TodoWebController 改用 BaseWebController 方法 | 架構 | 🟡 Warning | ✅ |
| WebExceptionHandler 隱藏內部錯誤訊息 | 安全 | 🔵 Suggestion | ✅ |
| 前端 console 語句 + alert() 清理 | 前端 | 🟠 Medium | ✅ |
| DocumentService.getDocumentForPreview() 改回傳 DTO | 架構 | 🟡 Warning | ✅ |
| requireUserId() 重複消除 | 架構 | 🟡 Warning | ✅ |
| 業務例外日誌等級調整 | 架構 | 🔵 Suggestion | ✅ |
| GlobalExpenseService N+1 批次修復 | 效能 | 🟡 Warning | ✅ |
| 共享 RestTemplate + Apache HttpClient 5 連線池 | 效能 | 🟡 Warning | ✅ |
| Google Maps Circuit Breaker | 效能 | 🔵 Suggestion | ✅ |
| ExchangeRateApiClient 單元測試 | 測試 | 🟡 Warning | ✅ |
| SupabaseStorageClient 單元測試 | 測試 | 🟡 Warning | ✅ |
| settlement.js 行內腳本抽取 | 前端 | HIGH | ✅ |
| activity-list.js 行內腳本抽取 | 前端 | HIGH | ✅ |
| activity-form.js 行內腳本抽取 | 前端 | HIGH | ✅ |
| member-management.js 行內腳本抽取 | 前端 | HIGH | ✅ |
| expense-list.js 行內腳本抽取 | 前端 | HIGH | ✅ |
| document-list.js 行內腳本抽取 | 前端 | HIGH | ✅ |
| 50+ onclick handler → data-action 事件委派 | 前端 | HIGH | ✅ |
| Modal focus trap (Tab/Shift+Tab 循環) | 前端 | 🟠 Medium | ✅ |
| skip-to-content 連結 | 前端 | LOW | ✅ |
| 版權年份動態化 | 前端 | LOW | ✅ |
| Settlement 結算快取 (1min TTL + eviction) | 效能 | 🔵 Suggestion | ✅ |
| deleteTrip Storage 非同步刪除 | 效能 | 🔵 Suggestion | ✅ |
| ExpenseSplit (expense_id, user_id) 複合索引 | 效能 | 🔵 Suggestion | ✅ |

</details>

---

## 結論

WeGo 專案整體品質優秀，綜合健康度 **8.6/10**。五個審查面向均無系統性問題：

- **零 SQL Injection、零 XSS（th:utext）、零 System.out**
- **1,175 測試全部通過**，覆蓋所有 Controller 和 Service
- **多層快取架構**完善（12 Caffeine 快取 + StatisticsCacheDelegate）
- **集中式權限檢查**（PermissionChecker）確保授權一致性
- **外部服務全面介面化**（5 組 Client + Mock），支援 fallback

唯一的 Critical 問題是 `personal-expense.js` 未模組化，修復成本低。其餘問題以 Warning 和 Suggestion 為主，可按 Roadmap 逐步改善。

專案架構設計成熟穩定，適合持續迭代開發。

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

*本報告由 5 位 Opus 4.6 Agent 獨立審查後由 Lead 彙整（2026-02-20）。*
