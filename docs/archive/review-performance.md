# WeGo 效能審查報告

> 審查日期：2026-02-21 (第四次更新)
> 審查員：perf-reviewer (Claude Opus 4.6)
> 審查範圍：Entity 關聯映射、FetchType、資料庫索引、分頁處理、N+1 查詢、快取策略、外部 API 呼叫、非同步處理、前端靜態資源、HikariCP 連線池
> 基準：對照 2026-02-14 初審、2026-02-20 二/三審更新

---

## 摘要

| 分類 | 🔴 Critical | 🟡 Warning | 🔵 Suggestion | 小計 |
|------|:-----------:|:----------:|:-------------:|:----:|
| Entity 關聯映射 / FetchType | 0 | 0 | 1 | 1 |
| 資料庫索引 | 0 | 1 | 0 | 1 |
| 分頁處理 | 0 | 2 | 1 | 3 |
| N+1 查詢 | 0 | 2 | 0 | 2 |
| 快取策略 | 0 | 0 | 1 | 1 |
| 外部 API 呼叫 | 0 | 1 | 0 | 1 |
| 非同步處理 | 0 | 1 | 0 | 1 |
| 前端靜態資源 | 0 | 0 | 1 | 1 |
| HikariCP 連線池 | 0 | 0 | 0 | 0 |
| **合計** | **0** | **7** | **4** | **11** |

**自上次審查（2026-02-20）以來的變化：**
- 問題總數 12 → 11（-1）
- 已確認修復（先前報告遺漏）：
  - ✅ 外部 API Client 共享連線池 RestTemplate（`HttpClientConfig.java`）— 問題 #9 已不存在
  - ✅ Google Maps 已有 Circuit Breaker（5 failures / 5min）— 問題 #10 已不存在
- 新增發現：
  - 🟡 SupabaseStorageClient 仍使用 `SimpleClientHttpRequestFactory`，未共享連線池
- 移除 CSS purge 建議（降為非效能範疇）

---

## 1. Entity 關聯映射 / FetchType

### 設計選擇：扁平 UUID 引用（非 JPA 關聯）

所有 Entity 均使用扁平 UUID 欄位（如 `UUID tripId`）而非 JPA `@ManyToOne` / `@OneToMany` 關聯。

**涉及 Entity（11 個）：**
- `src/main/java/com/wego/entity/Trip.java` — `ownerId: UUID`
- `src/main/java/com/wego/entity/Activity.java` — `tripId: UUID`, `placeId: UUID`
- `src/main/java/com/wego/entity/Expense.java` — `tripId: UUID`, `paidBy: UUID`, `activityId: UUID`, `createdBy: UUID`
- `src/main/java/com/wego/entity/ExpenseSplit.java` — `expenseId: UUID`, `userId: UUID`
- `src/main/java/com/wego/entity/TripMember.java` — `tripId: UUID`, `userId: UUID`
- `src/main/java/com/wego/entity/Document.java` — `tripId: UUID`, `uploadedBy: UUID`, `relatedActivityId: UUID`
- `src/main/java/com/wego/entity/Todo.java` — `tripId: UUID`, `assigneeId: UUID`, `createdBy: UUID`
- `src/main/java/com/wego/entity/Place.java` — 獨立 Entity
- `src/main/java/com/wego/entity/InviteLink.java` — `tripId: UUID`, `createdBy: UUID`
- `src/main/java/com/wego/entity/PersonalExpense.java` — `tripId: UUID`, `userId: UUID`
- `src/main/java/com/wego/entity/User.java` — 獨立 Entity

**優點：**
- 完全避免 EAGER fetch 造成的意外全量載入
- 無 Hibernate Proxy 序列化問題
- 無 `LazyInitializationException` 風險
- Entity 輕量且可預測

**代價：**
- 需要手動批次載入相關資料（已在多處正確實作）
- 無法使用 `@EntityGraph` 或 `JOIN FETCH` 語法

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 1 | 🔵 | 扁平 UUID 設計可接受但需注意手動批次載入 | 全部 Entity | 目前設計已避免 JPA 常見的 EAGER/LAZY 陷阱，是合理的架構選擇。未來若查詢複雜度增加，可考慮在熱路徑引入 DTO Projection 減少 SELECT 欄位 |

---

## 2. 資料庫索引

### 已存在的索引（良好）

| Entity | 索引 |
|--------|------|
| Trip | `idx_trip_owner_id (owner_id)` |
| Activity | `idx_activity_trip_id`, `idx_activity_trip_day_sort (trip_id, day, sort_order)`, `idx_activity_place_id` |
| Expense | `idx_expense_trip_id`, `idx_expense_paid_by`, `idx_expense_activity_id`, `idx_expense_created_by`, `idx_expense_trip_created (trip_id, created_at)` |
| ExpenseSplit | `idx_split_expense_id`, `idx_split_user_id`, `idx_split_user_settled (user_id, is_settled)`, `idx_split_expense_user (expense_id, user_id)` |
| Document | `idx_documents_trip_id`, `idx_documents_uploaded_by`, `idx_documents_trip_created`, `idx_documents_related_activity_id` |
| Todo | `idx_todo_trip_id`, `idx_todo_assignee_id`, `idx_todo_created_by`, `idx_todo_trip_status_due (trip_id, status, due_date)` |
| Place | `idx_place_google_place_id` |
| InviteLink | `idx_invite_link_trip_id`, `idx_invite_link_created_by`, `idx_invite_link_token` |
| TripMember | `uk_trip_member (trip_id, user_id)` UNIQUE, `idx_trip_member_user_id (user_id)` |

### 已修復（歷次審查）

- ✅ TripMember `user_id` 單欄索引（`TripMember.java:47`）
- ✅ TripMember `(trip_id, user_id)` 唯一約束
- ✅ ExpenseSplit `(expense_id, user_id)` 複合索引（`ExpenseSplit.java:39`）

### 剩餘問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 2 | 🟡 | **User 表缺少 `(provider, provider_id)` 複合索引** | `src/main/java/com/wego/entity/User.java:42-56` | `UserRepository.findByProviderAndProviderId` 是 OAuth 登入的核心查詢，每次登入都會呼叫。目前僅有 `email` 的 unique index，`provider + provider_id` 組合查詢需 full table scan。用戶量增長後會成為瓶頸。 |

**建議修復：**
```java
@Table(name = "users", indexes = {
    @Index(name = "idx_user_provider_provider_id", columnList = "provider, provider_id")
})
```

---

## 3. 分頁處理

### 正確使用分頁的查詢

- `TripRepository.findTripsByMemberId(userId, Pageable)` — 使用者行程列表
- `TripRepository.findByOwnerId(ownerId, Pageable)` — Owner 行程列表
- `ExpenseRepository.findByTripId(tripId, Pageable)` — 支出分頁
- `TodoRepository.findByTripId(tripId, Pageable)` — 待辦分頁
- `DocumentRepository.findByFilters(tripIds, search, mimeTypes, Pageable)` — 全域文件

### 問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 3 | 🟡 | **`getExpensesByTrip` 載入行程所有支出** | `src/main/java/com/wego/service/ExpenseService.java:155` | `findByTripIdOrderByCreatedAtDesc(tripId)` 載入行程所有支出到記憶體。N+1 問題已修復（`buildExpenseResponsesBatch`），但大量支出仍會一次性載入。單一行程支出通常 <100，但極端場景仍有風險。 |
| 4 | 🟡 | **`getDocumentsByTrip` 不帶分頁** | `src/main/java/com/wego/service/DocumentService.java:217` | `findByTripIdOrderByCreatedAtDesc(tripId)` 載入行程所有文件，無上限。 |
| 5 | 🔵 | `findByTripIdOrderedByDueDateAndStatus` 不帶分頁 | `src/main/java/com/wego/repository/TodoRepository.java:51` | 行程的待辦清單全量載入，但待辦數量通常有限，影響較低。 |

---

## 4. N+1 查詢問題

### 已修復的 N+1（良好實踐）

1. **ExpenseService.buildExpenseResponsesBatch**（`ExpenseService.java:578-624`）— 使用 `findByTripId` 批次載入所有 splits，再批次載入所有 users。3N+1 → 3 次查詢。
2. **TripService.getUserTrips**（`TripService.java:186-226`）— `findByTripIdIn` 批次載入成員 + `findAllById` 批次載入 users。
3. **ActivityService.mapActivitiesToResponses**（`ActivityService.java:573-585`）— `buildPlaceLookup` 批次載入 places。
4. **DocumentService.buildDocumentResponses**（`DocumentService.java:506-577`）— 批次載入 uploaders、activities、places。
5. **ChatService.fetchPlaceMap**（`ChatService.java:262-273`）— 批次載入 places。
6. **DocumentRepository.countByTripIds** — 專用批次計數查詢。
7. **StatisticsCacheDelegate** — 將 `@Cacheable` 方法提取到獨立 Bean，正確解決 Spring AOP self-invocation 問題。
8. **PersonalExpenseService** — 3-query 批次模式（expenses + splits + merge），避免 N+1。

### 剩餘問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 6 | 🟡 | **ExpenseService.buildExpenseResponse（單筆）仍有 N+1 風險** | `src/main/java/com/wego/service/ExpenseService.java:541-572` | 單筆 expense 的 `buildExpenseResponse` 執行 3 次獨立查詢（`userRepository.findById` + `expenseSplitRepository.findByExpenseId` + `getUserMap`）。被 `getExpense`、`createExpense`、`updateExpense` 呼叫。單筆操作影響有限，但若被迴圈呼叫會退化為 N+1。 |
| 7 | 🟡 | **GlobalExpenseService.getUnsettledTrips 潛在 N+1** | `src/main/java/com/wego/service/GlobalExpenseService.java:110-123` | 對每個未結算行程呼叫 `calculateUserBalanceInTrip`（行 112），每次呼叫 2 次 DB 查詢。M 個未結算行程 = 2M+2 次查詢。可用自定義 JPQL 一次性計算所有行程餘額。 |

---

## 5. 快取策略

### 現有快取配置（`src/main/java/com/wego/config/CacheConfig.java`）

| 快取名稱 | TTL | 最大條目 | 用途 |
|----------|-----|---------|------|
| `statistics-category` | 5 min | 500 | 分類統計 |
| `statistics-trend` | 5 min | 500 | 趨勢統計 |
| `statistics-members` | 5 min | 500 | 成員統計 |
| `exchange-rate` | 1 hour | 200 | 匯率主快取 |
| `exchange-rate-fallback` | 24 hours | 200 | 匯率降級快取 |
| `exchange-rate-all` | 1 hour | 50 | 全部匯率 |
| `exchange-rate-all-fallback` | 24 hours | 50 | 全部匯率降級 |
| `weather` | 6 hours | 200 | 天氣預報 |
| `places` | 5 min | 500 | 地點搜尋 |
| `directions` | 10 min | 200 | 路線規劃 |
| `settlement` | 1 min | 200 | 結算計算（費用變動時 evict） |
| `permission-check` | 5 sec | 500 | 權限檢查去重 |

### 已獨立管理的快取

| 快取 | 實作 | TTL | 用途 |
|------|------|-----|------|
| `signedUrlCache` | DocumentService 內建 Caffeine | signedUrlExpiry - 600s | Signed URL CDN 快取 |
| `RateLimitService` | 獨立 Caffeine sliding window | 2 min | AI Chat rate limiting |

### 已修復

- ✅ StatisticsService `@Cacheable` 代理問題 → `StatisticsCacheDelegate` Bean
- ✅ SettlementService `@Cacheable("settlement")` — 1min TTL + settle/unsettle eviction

### 剩餘建議

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 8 | 🔵 | 可考慮行程詳情頁面 short-lived cache | `src/main/java/com/wego/service/TripService.java:145-158` | `getTrip` 每次呼叫執行多次 DB 查詢（findById + countByTripId + getRole + getMemberSummaries）。行程資訊變動頻率低，可加 short-lived cache（30 秒）。 |

---

## 6. 外部 API 呼叫

### 現有配置

**共享連線池（`src/main/java/com/wego/config/HttpClientConfig.java`）：**
- Apache HttpClient 5 + `PoolingHttpClientConnectionManager`（maxTotal=50, maxPerRoute=10）
- `externalApiRestTemplate`：connect 5s / read 10s → Google Maps, ExchangeRate, OpenWeatherMap
- `geminiRestTemplate`：connect 5s / read 30s → Gemini AI

| API | RestTemplate | 快取 | Circuit Breaker | Rate Limit |
|-----|-------------|------|-----------------|------------|
| Google Maps Routes API | `externalApiRestTemplate` (pooled) | directions: 10min | ✅ 5 failures / 5min | Per-user Bucket |
| Google Maps Places API | `externalApiRestTemplate` (pooled) | places: 5min | ✅ 5 failures / 5min | Per-user Bucket |
| OpenWeatherMap | `externalApiRestTemplate` (pooled) | weather: 6h | ❌ 無 | 無 |
| ExchangeRate API | `externalApiRestTemplate` (pooled) | 1h + 24h fallback | ✅ 可配置 | 無 |
| Gemini AI | `geminiRestTemplate` (pooled) | 無 | ✅ 3 failures / 5min | Per-user Caffeine |
| Supabase Storage | **獨立** `SimpleClientHttpRequestFactory` | Signed URL CDN | ❌ 無 | 無 |

### 已修復（先前報告遺漏，本次確認已到位）

- ✅ **外部 API Client 共享連線池 RestTemplate**（`HttpClientConfig.java`）— Google Maps、ExchangeRate、OpenWeatherMap、Gemini 均使用 `@Qualifier` 注入共享 RestTemplate
- ✅ **Google Maps Circuit Breaker**（`GoogleMapsClientImpl.java:75-76`）— `CIRCUIT_BREAKER_THRESHOLD = 5`, `CIRCUIT_BREAKER_COOLDOWN_MS = 5min`

### 問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 9 | 🟡 | **SupabaseStorageClient 仍使用獨立 `SimpleClientHttpRequestFactory`，未共享連線池** | `src/main/java/com/wego/service/external/SupabaseStorageClient.java:56-64` | 建立了 `apiRestTemplate` 和 `fileRestTemplate` 兩個獨立 RestTemplate，底層為 `HttpURLConnection`，每次請求建新 TCP 連線。檔案上傳/下載場景連線重用效益較大。建議改用共享連線池或建立專用 `supabaseRestTemplate` Bean。同時缺少 Circuit Breaker。 |

---

## 7. 非同步處理

### 已配置（`src/main/java/com/wego/config/AsyncConfig.java`）

- `@EnableAsync` 已啟用
- `transportExecutor` 執行緒池：core=2, max=4, queue=10, `CallerRunsPolicy`
- 已用於：`ActivityService.recalculateTransportTimesAsync`、`TripService.deleteStorageFilesAsync`

### 問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 10 | 🟡 | **批次重算交通時間 `batchRecalculateWithRateLimit` 同步阻塞** | `src/main/java/com/wego/service/TransportCalculationService.java:403-568` | 逐一同步呼叫 Google Maps API（每次間隔 100ms）。50 個景點需至少 5 秒 + API 回應時間。雖有 `@Async` 基礎設施，此方法仍為同步執行。建議返回 `CompletableFuture` 或提供 polling-based 進度更新。 |

---

## 8. 前端靜態資源

### 已修復

- ✅ HTTP 壓縮已啟用（`application-prod.yml:8-16`）— gzip for text/html, css, js, json, svg，最小 1024 bytes
- ✅ **Content-hash 版本化 + 365 天長期快取**（`src/main/java/com/wego/config/WebConfig.java:60-83`）
  - `VersionResourceResolver.addContentVersionStrategy("/**")` — 檔案內容雜湊版本號
  - `CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic()` — 瀏覽器長期快取
  - `ResourceUrlEncodingFilter` — Thymeleaf `@{/css/output.css}` 自動轉換為 `/css/output-<hash>.css`
  - 覆蓋 `/css/**`、`/js/**`、`/images/**` 三個路徑

### 剩餘建議

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 11 | 🔵 | **`common.js` 缺少 `defer` 屬性，阻塞頁面渲染** | `src/main/resources/templates/fragments/head.html:55` | `<script th:src="@{/js/common.js}">` 無 `defer`，會阻塞 HTML 解析。同檔案中 Lottie 和 Flatpickr 已正確使用 `defer`。建議加上 `defer` 屬性，確保 DOM 解析不被阻塞。 |

---

## 9. HikariCP 連線池

### 現有配置（`src/main/resources/application.yml:19-23`）

| 參數 | 值 | 評估 |
|------|-----|------|
| `maximum-pool-size` | 10 | 適合小型應用，搭配 Supabase 免費/Pro 方案的連線上限 |
| `minimum-idle` | 2 | 合理，避免冷啟動延遲 |
| `idle-timeout` | 600000 (10 min) | Spring Boot 預設值，合理 |
| `connection-timeout` | 30000 (30s) | Spring Boot 預設值，合理 |
| `max-lifetime` | 1800000 (30 min) | Spring Boot 預設值，合理 |
| `open-in-view` | `false` | 正確關閉 OSIV |

**評估：無需調整。** HikariCP 預設配置對目前規模（10 連線、低並發）已足夠。`open-in-view: false` 確保 DB 連線在 Service 層就釋放，不會延伸到 View 層。

---

## 10. 正面發現（效能最佳實踐）

1. **`open-in-view: false`**（`application.yml:36`）— 正確關閉 OSIV
2. **HikariCP 連線池配置**（`application.yml:19-23`）— pool-size 10, minimum-idle 2
3. **`@Transactional(readOnly = true)`** — 讀取操作正確標記為唯讀
4. **TripService.getUserTrips 批次載入** — `findByTripIdIn` + `findAllById` 避免 N+1
5. **ActivityService 批次載入 Places** — `buildPlaceLookup` 單次查詢
6. **ExpenseService.buildExpenseResponsesBatch** — 批次 splits + users（3 次查詢）
7. **DocumentService.buildDocumentResponses** — 批次 uploaders、activities、places
8. **DocumentRepository.countByTripIds** — 專用批次計數查詢
9. **PersonalExpenseService 3-query 批次模式** — expenses + splits + merge sorted
10. **PermissionChecker 5 秒 Caffeine 快取** — 請求級去重
11. **ExchangeRateService 雙層快取** — Primary（1h）+ Fallback（24h）
12. **4/5 外部 API Client Circuit Breaker** — Gemini、ExchangeRate、Google Maps Routes、Google Maps Places
13. **StatisticsCacheDelegate 模式** — 正確解決 AOP self-invocation 問題
14. **DocumentService signedUrlCache** — Signed URL CDN 快取
15. **JPA `ddl-auto: none`**（prod: `validate`）— 不自動變更 schema
16. **UUID 主鍵 + 應用層生成** — 避免序列瓶頸
17. **Thymeleaf cache: true（prod）** — 生產環境啟用模板快取
18. **HTTP 壓縮（prod）** — gzip 壓縮
19. **ChatService prompt 5000 char 截斷 + 2KB byte limit** — 防止 OOM
20. **SettlementService @Cacheable("settlement")** — 1min TTL + eviction
21. **TripService.deleteStorageFilesAsync @Async** — Storage 非同步刪除
22. **ExpenseSplit (expense_id, user_id) 複合索引** — 優化分帳查詢
23. **Content-hash 版本化靜態資源 + 365 天快取**（`WebConfig.java:60-83`）— 最佳化瀏覽器快取
24. **HttpClientConfig 共享連線池**（`HttpClientConfig.java`）— Apache HttpClient 5, maxTotal=50, maxPerRoute=10
25. **Bucket4j per-IP Rate Limiting**（`RateLimitConfig.java`）— 100 req/min

---

## 11. 改善建議優先順序

### 第一優先（高影響、低成本）

1. **User 表加 `(provider, provider_id)` 複合索引**（問題 #2）

```java
@Table(name = "users", indexes = {
    @Index(name = "idx_user_provider_provider_id", columnList = "provider, provider_id")
})
```

影響：每次 OAuth 登入都會查詢此欄位組合，建立索引後從 full table scan 降為 index lookup。

2. **GlobalExpenseService.getUnsettledTrips 批次計算餘額**（問題 #7）

```java
@Query("SELECT e.tripId, " +
       "SUM(CASE WHEN e.paidBy = :userId AND es.userId != :userId AND es.isSettled = false THEN es.amount ELSE 0 END), " +
       "SUM(CASE WHEN es.userId = :userId AND es.userId != e.paidBy AND es.isSettled = false THEN es.amount ELSE 0 END) " +
       "FROM Expense e JOIN ExpenseSplit es ON es.expenseId = e.id " +
       "WHERE e.tripId IN :tripIds " +
       "GROUP BY e.tripId")
```

影響：M 個未結算行程從 2M+2 次查詢降為 1-2 次。

3. **`common.js` 加 `defer` 屬性**（問題 #11）

```html
<script defer th:src="@{/js/common.js}"></script>
```

影響：消除渲染阻塞，改善首屏載入速度。

### 第二優先（中等影響）

4. **SupabaseStorageClient 改用共享連線池**（問題 #9）— 注入 `externalApiRestTemplate` 或建立專用 `supabaseRestTemplate`，加入 Circuit Breaker

5. **@Async 批次重算非同步化**（問題 #10）— 對 `batchRecalculateWithRateLimit` 返回 `CompletableFuture`，搭配前端 polling 顯示進度

### 第三優先（低影響 / 未來優化）

6. TripService.getTrip short-lived cache（問題 #8）
7. Expense / Document 列表加分頁上限（問題 #3, #4）

---

## 效能評分

| 維度 | 分數 | 說明 |
|------|:----:|------|
| Entity 設計 | 9/10 | 扁平 UUID 設計乾淨，避免 JPA 關聯陷阱 |
| 資料庫索引 | 8.5/10 | 索引覆蓋良好，僅 User 表需加索引 |
| N+1 查詢 | 8/10 | 主要 N+1 已修復，僅餘單筆和 Global 小問題 |
| 快取策略 | 9.5/10 | 多層快取設計優秀，StatisticsDelegate + Settlement 快取到位 |
| 外部 API | 8.5/10 | 共享連線池 + Timeout + Circuit Breaker 到位，僅 Supabase 未池化 |
| 非同步處理 | 7.5/10 | AsyncConfig 已配置，deleteTrip 非同步化完成，批次重算仍同步 |
| 前端資源 | 9/10 | gzip 壓縮 + Content-hash 版本化 + 365 天快取，common.js 缺 defer |
| 連線池/並行 | 9/10 | HikariCP + Apache HttpClient 5 連線池配置合理 |

### **綜合效能評分：8.8 → 9.0 / 10** (updated 2026-02-21)

**歷史提升：7.0 → 7.8 → 8.5 → 8.8 → 9.0**

2026-02-21 提升項（先前報告遺漏修正，本次確認已到位）：
- 外部 API 共享連線池 RestTemplate 已實作（+0.1 外部 API 維度）
- Google Maps Circuit Breaker 已實作（+0.1 外部 API 維度）

主要扣分項：
- 批次重算交通仍同步阻塞（-0.5）
- getExpensesByTrip / getDocumentsByTrip 不帶分頁（-0.3）
- 單筆 buildExpenseResponse 仍 3 次查詢（-0.2）
- GlobalExpenseService N+1 未修（-0.2）

**下一步建議：批次重算非同步化 + GlobalExpenseService 批次查詢即可穩固維持 9.0+**
