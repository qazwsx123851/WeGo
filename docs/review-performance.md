# WeGo 效能審查報告

> 審查日期：2026-02-18
> 審查員：perf-reviewer (Claude Opus 4.6)
> 審查範圍：Entity 關聯映射、資料庫索引、分頁處理、N+1 查詢、快取策略、外部 API 呼叫、非同步處理、前端靜態資源
> 基準：對照 2026-02-14 初審報告更新

---

## 摘要

| 分類 | 🔴 Critical | 🟡 Warning | 🔵 Suggestion | 小計 |
|------|:-----------:|:----------:|:-------------:|:----:|
| Entity 關聯映射 | 0 | 0 | 1 | 1 |
| 資料庫索引 | 0 | 1 | 1 | 2 |
| 分頁處理 | 0 | 2 | 1 | 3 |
| N+1 查詢 | 0 | 2 | 0 | 2 |
| 快取策略 | 0 | 0 | 2 | 2 |
| 外部 API 呼叫 | 0 | 1 | 1 | 2 |
| 非同步處理 | 0 | 1 | 1 | 2 |
| 前端靜態資源 | 0 | 1 | 1 | 2 |
| **合計** | **0** | **8** | **8** | **16** |

**自上次審查（2026-02-14）以來的變化：**
- 🔴 Critical: 3 → 0（全部已修復）
- 已修復：HTTP 壓縮已啟用（`application-prod.yml:9-16`）、ExpenseService N+1 已用批次查詢修復（`buildExpenseResponsesBatch` 方法）、TripMember 已新增 `idx_trip_member_user_id` 索引
- StatisticsService `@Cacheable` 代理問題已透過 `StatisticsCacheDelegate` 模式正確修復

---

## 1. Entity 關聯映射

### 設計選擇：扁平 UUID 引用（非 JPA 關聯）

所有 Entity 均使用扁平 UUID 欄位（如 `UUID tripId`）而非 JPA `@ManyToOne` / `@OneToMany` 關聯。

**優點：**
- 完全避免 EAGER fetch 造成的意外全量載入
- 無 Hibernate Proxy 序列化問題
- 無 LazyInitializationException 風險
- Entity 輕量且可預測

**代價：**
- 需要手動批次載入相關資料（已在多處正確實作）
- 無法使用 `@EntityGraph` 或 `JOIN FETCH` 語法

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 1 | 🔵 | 扁平 UUID 設計可接受但需注意手動批次載入 | 全部 Entity | 目前設計已避免 JPA 常見的 EAGER/LAZY 陷阱，是合理的架構選擇 |

---

## 2. 資料庫索引

### 已存在的索引（良好）

| Entity | 索引 |
|--------|------|
| Trip | `idx_trip_owner_id (owner_id)` |
| Activity | `idx_activity_trip_id`, `idx_activity_trip_day_sort (trip_id, day, sort_order)`, `idx_activity_place_id` |
| Expense | `idx_expense_trip_id`, `idx_expense_paid_by`, `idx_expense_activity_id`, `idx_expense_created_by`, `idx_expense_trip_created` |
| ExpenseSplit | `idx_split_expense_id`, `idx_split_user_id`, `idx_split_user_settled` |
| Document | `idx_documents_trip_id`, `idx_documents_uploaded_by`, `idx_documents_trip_created`, `idx_documents_related_activity_id` |
| Todo | `idx_todo_trip_id`, `idx_todo_assignee_id`, `idx_todo_created_by`, `idx_todo_trip_status_due` |
| Place | `idx_place_google_place_id` |
| InviteLink | `idx_invite_link_trip_id`, `idx_invite_link_created_by`, `idx_invite_link_token` |
| TripMember | `uk_trip_member (trip_id, user_id)` UNIQUE, `idx_trip_member_user_id (user_id)` |

### 上次審查已修復

- **TripMember `user_id` 單欄索引已新增**（`TripMember.java:47`）- `findByUserId` 在 GlobalExpenseService、GlobalDocumentService、ProfileController 中使用，現已有索引支援。
- **TripMember `(trip_id, user_id)` 唯一約束** - PostgreSQL 自動為 unique constraint 建立索引，PermissionChecker 的 `findByTripIdAndUserId` 可有效利用。

### 剩餘問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 2 | 🟡 | **User 表缺少 `(provider, provider_id)` 複合索引** | `src/main/java/com/wego/entity/User.java:42-56` | `UserRepository.findByProviderAndProviderId` 是 OAuth 登入的核心查詢，每次登入都會呼叫。目前僅有 `email` 的 unique index，`provider + provider_id` 組合查詢需 full table scan。用戶量增長後會成為瓶頸。 |
| 3 | 🔵 | ExpenseSplit 缺少 `(expense_id, user_id)` 複合索引 | `src/main/java/com/wego/entity/ExpenseSplit.java:35-39` | `findUnsettledByTripIdAndUsers` 等查詢使用 JOIN + WHERE 條件，目前有 `expense_id` 和 `user_id` 的單欄索引，複合索引可進一步優化。但由於資料量通常有限，影響較低。 |

---

## 3. 分頁處理

### 正確使用分頁的查詢

- `TripRepository.findTripsByMemberId(userId, Pageable)` - 使用者行程列表
- `TripRepository.findByOwnerId(ownerId, Pageable)` - Owner 行程列表
- `ExpenseRepository.findByTripId(tripId, Pageable)` - 支出分頁
- `TodoRepository.findByTripId(tripId, Pageable)` - 待辦分頁
- `DocumentRepository.findByFilters(tripIds, search, mimeTypes, Pageable)` - 全域文件

### 問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 4 | 🟡 | **`getExpensesByTrip` 載入行程所有支出** | `src/main/java/com/wego/service/ExpenseService.java:155` | `findByTripIdOrderByCreatedAtDesc(tripId)` 載入行程所有支出到記憶體。N+1 問題已修復（見 `buildExpenseResponsesBatch`），但大量支出仍會一次性載入。單一行程支出通常 <100，但極端場景仍有風險。 |
| 5 | 🟡 | **`getDocumentsByTrip` 不帶分頁** | `src/main/java/com/wego/service/DocumentService.java:217` | `findByTripIdOrderByCreatedAtDesc(tripId)` 載入行程所有文件，無上限。 |
| 6 | 🔵 | `findByTripIdOrderedByDueDateAndStatus` 不帶分頁 | `src/main/java/com/wego/repository/TodoRepository.java:51` | 行程的待辦清單全量載入，但待辦數量通常有限，影響較低。 |

---

## 4. N+1 查詢問題

### 已修復的 N+1（良好實踐）

1. **ExpenseService.buildExpenseResponsesBatch**（`ExpenseService.java:578-624`）- 使用 `expenseSplitRepository.findByTripId` 批次載入所有 splits，再批次載入所有 users。單一行程的支出列表從 3N+1 次查詢降為 3 次。
2. **TripService.getUserTrips**（`TripService.java:186-226`）- 使用 `findByTripIdIn` 批次載入成員，再使用 `findAllById` 批次載入 users。
3. **ActivityService.mapActivitiesToResponses**（`ActivityService.java:573-585`）- 使用 `buildPlaceLookup` 批次載入 places。
4. **DocumentService.buildDocumentResponses**（`DocumentService.java:506-577`）- 批次載入 uploaders、activities、places，避免 N+1。
5. **ChatService.fetchPlaceMap**（`ChatService.java:262-273`）- 批次載入 places。
6. **DocumentRepository.countByTripIds** - 專用批次計數查詢。
7. **StatisticsCacheDelegate** - 將 `@Cacheable` 方法提取到獨立 Bean，正確解決 Spring AOP 代理繞過問題。

### 剩餘問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 7 | 🟡 | **ExpenseService.buildExpenseResponse（單筆）仍有 N+1** | `src/main/java/com/wego/service/ExpenseService.java:541-572` | 單筆 expense 的 `buildExpenseResponse` 仍然執行 3 次獨立查詢（`userRepository.findById` + `expenseSplitRepository.findByExpenseId` + `getUserMap`）。被 `getExpense`、`createExpense`、`updateExpense` 呼叫。單筆操作影響有限，但如果被迴圈呼叫會退化為 N+1。 |
| 8 | 🟡 | **GlobalExpenseService.getUnsettledTrips 潛在 N+1** | `src/main/java/com/wego/service/GlobalExpenseService.java:110-123` | 對每個未結算行程呼叫 `calculateUserBalanceInTrip`（行 112），每次呼叫執行 2 次 DB 查詢。若有 M 個未結算行程，共 2M+2 次查詢。通常 M < 10，實際影響中等。可考慮用自定義 JPQL 一次性計算所有行程餘額。 |

---

## 5. 快取策略

### 現有快取配置（CacheConfig.java）

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
| `permission-check` | 5 sec | 500 | 權限檢查去重 |

### 已獨立管理的快取

| 快取 | 實作 | TTL | 用途 |
|------|------|-----|------|
| `signedUrlCache` | DocumentService 內建 Caffeine | signedUrlExpiry - 600s | Signed URL CDN 快取 |
| `RateLimitService` | 獨立 Caffeine sliding window | 2 min | AI Chat rate limiting |

### 已修復

- **StatisticsService `@Cacheable` 代理問題** - 已正確提取到 `StatisticsCacheDelegate` Bean，避免同類別內部呼叫繞過代理。

### 剩餘建議

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 9 | 🔵 | 可考慮行程詳情頁面 short-lived cache | `src/main/java/com/wego/service/TripService.java:145-158` | `getTrip` 每次呼叫執行多次 DB 查詢（findById + countByTripId + getRole + getMemberSummaries）。行程資訊變動頻率低，可加 short-lived cache（30 秒）。PermissionChecker 已有 5 秒快取，但 Trip 本身和成員列表未快取。 |
| 10 | 🔵 | SettlementService.calculateSettlement 無快取 | `src/main/java/com/wego/service/SettlementService.java:111-182` | 結算計算涉及載入所有 expenses + splits + currency conversion，計算密集。結果可快取 30-60 秒，因結算資料變動頻率低。 |

---

## 6. 外部 API 呼叫

### 現有配置

| API | Connect Timeout | Read Timeout | 快取 | Circuit Breaker |
|-----|-----------------|-------------|------|-----------------|
| Google Maps Routes API | 5s | 10s | directions: 10min | 無 |
| Google Maps Places API | 5s | 10s | places: 5min | 無 |
| OpenWeatherMap | 透過共用 RestTemplate | 同上 | weather: 6h | 無 |
| ExchangeRate API | 5s | 10s | 1h + 24h fallback | 5 failures / 5min |
| Gemini AI | 5s | 30s | 無 | 3 failures / 5min |
| Supabase Storage | 透過共用 RestTemplate | 同上 | Signed URL CDN | 無 |

### 問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 11 | 🟡 | **外部 API Client 各自建立獨立 RestTemplate，無連線池** | `src/main/java/com/wego/service/external/GeminiClientImpl.java:63-67`, `src/main/java/com/wego/service/external/ExchangeRateApiClient.java:97-101` | 使用 `SimpleClientHttpRequestFactory`（底層 `HttpURLConnection`），每次請求建新 TCP 連線，無連線復用。GeminiClientImpl、ExchangeRateApiClient 均各自建立 RestTemplate。建議使用 `HttpComponentsClientHttpRequestFactory`（Apache HttpClient 5）配合連線池，共享 connection pool。 |
| 12 | 🔵 | Google Maps API 缺少 Circuit Breaker | `src/main/java/com/wego/service/external/GoogleMapsClientImpl.java` | ExchangeRate 和 Gemini 都有 circuit breaker，但 Google Maps 沒有。API key 失效或配額耗盡時仍會嘗試呼叫。影響中等（通常由使用者主動觸發而非自動批次呼叫）。 |

---

## 7. 非同步處理

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 13 | 🟡 | **批次重算交通時間完全同步阻塞** | `src/main/java/com/wego/service/TransportCalculationService.java:403-568` | `batchRecalculateWithRateLimit` 逐一同步呼叫 Google Maps API（每次間隔 100ms）。50 個景點需至少等待 5 秒 + API 回應時間。專案未配置 `@EnableAsync`，無非同步基礎設施。建議對批次重算加入 `@Async` 或返回 polling-based 進度更新。 |
| 14 | 🔵 | `TripService.deleteTrip` 同步刪除 Storage 檔案 | `src/main/java/com/wego/service/TripService.java:318-326` | 刪除行程時逐一同步刪除 Supabase Storage 檔案。若有大量文件，使用者需等待所有刪除完成。可考慮非同步刪除 Storage 檔案（DB 記錄已刪除即可回應）。 |

---

## 8. 前端靜態資源

### 已修復

- **HTTP 壓縮已啟用**（`application-prod.yml:8-16`）- 正確設定 `server.compression.enabled: true`，支援 text/html, text/css, application/javascript, application/json, image/svg+xml，最小回應大小 1024 bytes。預估靜態資源傳輸量減少 70-80%。

### 剩餘問題

| # | 嚴重度 | 問題 | 檔案 | 說明 |
|---|--------|------|------|------|
| 15 | 🟡 | **靜態資源 Cache-Control 僅 1 天** | `src/main/java/com/wego/config/WebConfig.java:44` | 靜態 CSS/JS 使用 `maxAge(1, TimeUnit.DAYS)`。每天瀏覽器都需重新驗證所有靜態資源。建議：使用 Spring `ResourceUrlProvider` 或 Webpack 內容雜湊版本號（如 `output.css?v=abc123`）搭配長期快取（30-365 天）。或至少在 prod 中延長至 7 天。 |
| 16 | 🔵 | CSS 可能有未使用的樣式 | `src/main/resources/static/css/` | `output.css`（Tailwind 產出）和 `styles.css` 共存。建議確認 Tailwind purge/content 設定是否正確，移除未使用的樣式可減少 CSS 體積。 |

---

## 9. 正面發現（效能最佳實踐）

1. **`open-in-view: false`**（`application.yml:36`）- 正確關閉 OSIV，避免 View 層觸發延遲載入
2. **HikariCP 連線池配置**（`application.yml:19-23`）- pool-size 10, minimum-idle 2, 合理配置
3. **`@Transactional(readOnly = true)`** - 讀取操作正確標記為唯讀，允許 DB 層級優化
4. **TripService.getUserTrips 批次載入** - 使用 `findByTripIdIn` 避免 N+1，再批次載入 users
5. **ActivityService 批次載入 Places** - `buildPlaceLookup` 單次查詢載入所有 Place
6. **ExpenseService.buildExpenseResponsesBatch** - 使用 `findByTripId` 批次載入 splits，再批次載入 users（3 次查詢）
7. **DocumentService.buildDocumentResponses** - 批次載入 uploaders、activities、places
8. **DocumentRepository.countByTripIds** - 專用批次計數查詢避免 N+1
9. **PermissionChecker 5 秒 Caffeine 快取** - 有效去重同一請求內的多次權限檢查
10. **ExchangeRateService 雙層快取** - Primary（1h）+ Fallback（24h）策略，API 故障時有降級方案
11. **Gemini + ExchangeRate Circuit Breaker** - 外部 API 熔斷保護，避免級聯失敗
12. **StatisticsCacheDelegate 模式** - 正確解決 Spring AOP self-invocation 繞過快取問題
13. **DocumentService signedUrlCache** - Signed URL CDN 快取，減少 Supabase API 呼叫
14. **JPA `ddl-auto: none`**（production: `validate`）- 不自動變更 schema
15. **UUID 主鍵 + 應用層生成** - 避免序列瓶頸
16. **Thymeleaf cache: true（prod）** - 生產環境啟用模板快取
17. **HTTP 壓縮（prod）** - gzip 壓縮已啟用
18. **ChatService prompt 5000 char 截斷 + 2KB byte limit** - 防止 OOM 和超大請求

---

## 10. 改善建議優先順序

### 第一優先（高影響、低成本）

1. **User 表加 `(provider, provider_id)` 複合索引**（問題 #2）

```java
@Table(name = "users", indexes = {
    @Index(name = "idx_user_provider_provider_id", columnList = "provider, provider_id")
})
```

影響：每次 OAuth 登入都會查詢此欄位組合，建立索引後從 full table scan 降為 index lookup。

2. **GlobalExpenseService.getUnsettledTrips 批次計算餘額**（問題 #8）

```java
// 用自定義 JPQL 一次計算所有行程的 owed / owedTo
@Query("SELECT e.tripId, " +
       "SUM(CASE WHEN e.paidBy = :userId AND es.userId != :userId AND es.isSettled = false THEN es.amount ELSE 0 END), " +
       "SUM(CASE WHEN es.userId = :userId AND es.userId != e.paidBy AND es.isSettled = false THEN es.amount ELSE 0 END) " +
       "FROM Expense e JOIN ExpenseSplit es ON es.expenseId = e.id " +
       "WHERE e.tripId IN :tripIds " +
       "GROUP BY e.tripId")
```

影響：M 個未結算行程從 2M+2 次查詢降為 1-2 次。

### 第二優先（中等影響）

3. **共享 RestTemplate + 連線池**（問題 #11）

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        // Apache HttpClient 5 自帶連線池
        return new RestTemplate(factory);
    }
}
```

4. **靜態資源版本號 + 長期快取**（問題 #15）- 使用 Spring `ResourceUrlProvider` 或手動版本號搭配 7+ 天快取

5. **@Async 配置 + 批次重算非同步化**（問題 #13）- 加入 `@EnableAsync` 和自訂執行緒池，對 `batchRecalculateWithRateLimit` 返回 `CompletableFuture`

### 第三優先（低影響 / 未來優化）

6. SettlementService 計算結果快取（問題 #10）
7. TripService.getTrip short-lived cache（問題 #9）
8. CSS 清理與 Tailwind purge 驗證（問題 #16）
9. Expense / Document 列表加分頁上限（問題 #4, #5）

---

## 效能評分

| 維度 | 分數 | 說明 |
|------|:----:|------|
| Entity 設計 | 9/10 | 扁平 UUID 設計乾淨，避免 JPA 關聯陷阱 |
| 資料庫索引 | 8/10 | 索引覆蓋良好，TripMember 已補充，User 表需加索引 |
| N+1 查詢 | 8/10 | 主要 N+1 已修復（Expense 批次、Trip 批次、Document 批次），僅餘單筆和 Global 小問題 |
| 快取策略 | 9/10 | 多層快取設計優秀，StatisticsDelegate 正確解決代理問題，signed URL 快取到位 |
| 外部 API | 7/10 | Timeout + Circuit Breaker 到位，連線池仍需改善 |
| 非同步處理 | 5/10 | 完全同步設計，批次操作有阻塞風險 |
| 前端資源 | 7/10 | gzip 壓縮已啟用，Cache-Control 策略可改善 |
| 連線池/並行 | 8/10 | HikariCP 配置合理，HTTP 連線未池化 |

### **綜合效能評分：7.8 / 10**

**自上次審查提升：7.0 → 7.8（+0.8）**

主要提升項：
- HTTP 壓縮已啟用（+0.4）
- ExpenseService N+1 已修復（+0.3）
- TripMember 索引已補充（+0.1）

主要扣分項：
- 無 @Async / 執行緒池（-1.0）
- 外部 API 無連線池（-0.5）
- 靜態資源 Cache-Control 僅 1 天（-0.3）
- GlobalExpenseService N+1 未修（-0.2）
- User 表缺少 OAuth 查詢索引（-0.2）

**下一步建議：配置連線池 + @Async 即可提升至 8.5+**
