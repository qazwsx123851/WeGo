# Phase 3: 分帳進階 - 開發任務清單

> 最後更新: 2026-02-03
> 版本: v2.0 (經 planner, architect, security-reviewer 審查)

## 專案狀態

| Phase | 狀態 | 完成日期 |
|-------|------|----------|
| Phase 0 | ✅ 完成 | 2026-01-28 |
| Phase 1 | ✅ 完成 | 2026-01-30 |
| Phase 2 | ✅ 完成 | 2026-02-01 |
| **Phase 3** | ✅ 完成 | 2026-02-03 |
| Phase 4 | ⏳ 待開始 | - |

---

## Phase 3 目標

1. **ExchangeRateService (P3-001~004)** - 動態匯率查詢與轉換
2. **多幣別結算 (P3-005~006)** - Convert-at-Display 策略
3. **支出統計圖表 (P3-007)** - 分類統計、趨勢分析

---

## 架構設計

### 新增模組結構

```
com.wego/
├── config/
│   └── ExchangeRateProperties.java       [NEW]
├── service/external/
│   ├── ExchangeRateClient.java           [NEW - Interface]
│   ├── ExchangeRateApiClient.java        [NEW - Real API]
│   ├── MockExchangeRateClient.java       [NEW - Mock]
│   └── ExchangeRateException.java        [NEW]
├── service/
│   ├── ExchangeRateService.java          [NEW - 快取+業務]
│   ├── StatisticsService.java            [NEW - 統計] (renamed from ExpenseStatisticsService)
│   ├── SettlementService.java            [MODIFY - 多幣別]
│   └── ExpenseService.java               [MODIFY - 快取失效]
├── domain/
│   ├── settlement/
│   │   └── DebtSimplifier.java           [NO CHANGE - 保持不變]
│   └── statistics/
│       └── ExpenseAggregator.java        [NEW - 純領域邏輯]
├── dto/
│   ├── request/
│   │   └── ExchangeRateRequest.java      [NEW - batch conversion]
│   └── response/
│       ├── ExchangeRateResponse.java     [NEW]
│       ├── CategoryBreakdownResponse.java [NEW]
│       ├── TrendResponse.java            [NEW]
│       └── MemberStatisticsResponse.java [NEW]
├── controller/api/
│   ├── ExchangeRateApiController.java    [NEW]
│   └── StatisticsApiController.java      [NEW]
├── repository/
│   └── ExpenseRepository.java            [MODIFY - 聚合查詢]
├── util/
│   └── CurrencyConverter.java            [NEW - BigDecimal 精度]
└── exception/
    └── GlobalExceptionHandler.java       [MODIFY - ExchangeRateException]
```

### 前端新增

```
src/main/resources/
├── templates/expense/
│   └── statistics.html                   [NEW - 統計頁面]
└── static/js/
    ├── expense-statistics.js             [NEW - Chart.js 整合]
    └── currency-converter.js             [NEW - 幣別轉換]
```

### 資料庫 Migration

```sql
-- P3-DB-001: 效能索引
CREATE INDEX idx_expenses_trip_category ON expenses(trip_id, category);
CREATE INDEX idx_expenses_trip_date ON expenses(trip_id, expense_date);
CREATE INDEX idx_expenses_trip_currency ON expenses(trip_id, currency);
```

---

## 技術決策

### 多幣別結算策略: Convert-at-Display (ADR-002)

```
Expense 儲存原始幣別 → 計算結算時轉換為基礎幣別 → DebtSimplifier 保持不變
```

**優點:**
- 保留原始資料完整性
- 最小化現有程式碼修改
- 允許以更新匯率重新計算

### 快取策略 (已修正)

| 資料類型 | Cache Key | 主 TTL | Fallback TTL | 失效策略 |
|----------|-----------|--------|--------------|----------|
| 匯率 | `exchange-rate:{base}:{date}` | **1 小時** | 24 小時 | TTL |
| 統計 | `statistics::{tripId}::{type}` | 5 分鐘 | - | TTL + 手動 |

**快取失效觸發點:**
- Expense 新增 → `evictByPrefix("statistics::" + tripId)`
- Expense 修改 → `evictByPrefix("statistics::" + tripId)`
- Expense 刪除 → `evictByPrefix("statistics::" + tripId)`

### 錯誤處理

| 情境 | HTTP Status | 處理方式 |
|------|-------------|----------|
| API 不可用 | 502 | 使用快取 (最多 24h 舊資料) |
| 快取過期 (>24h) | 503 | 拒絕請求，提示稍後重試 |
| 無效幣別 | 400 | 返回錯誤訊息 |
| 超過配額 | 429 | 使用快取 + 警告日誌 |

---

## API 設計

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/exchange-rates?from=USD&to=TWD` | 匯率查詢 |
| GET | `/api/exchange-rates/latest?base=USD` | 基礎幣別所有匯率 |
| GET | `/api/trips/{id}/statistics/category` | 分類統計 (餅圖) |
| GET | `/api/trips/{id}/statistics/trend?from=&to=` | 趨勢分析 (折線圖) |
| GET | `/api/trips/{id}/statistics/members` | 成員統計 |

### API Response 格式

#### 匯率查詢 (含快取狀態)
```json
{
    "success": true,
    "data": {
        "from": "USD",
        "to": "TWD",
        "rate": 31.5,
        "fetchedAt": "2026-02-03T00:00:00Z",
        "cached": true,
        "cacheAge": 3600000
    }
}
```

#### 分類統計
```json
{
    "success": true,
    "data": {
        "tripId": "uuid",
        "baseCurrency": "TWD",
        "totalAmount": 50000,
        "categories": [
            { "category": "FOOD", "amount": 20000, "percentage": 40.0, "count": 15 },
            { "category": "TRANSPORT", "amount": 15000, "percentage": 30.0, "count": 8 }
        ]
    }
}
```

---

## 任務清單

### Phase 3.1: 匯率基礎 (P3-001~004)

| 任務 ID | 任務 | 檔案 | 狀態 |
|---------|------|------|------|
| P3-001 | ExchangeRateClient 介面 | `service/external/ExchangeRateClient.java` | ✅ |
| P3-002 | MockExchangeRateClient | `service/external/MockExchangeRateClient.java` | ✅ |
| P3-003 | ExchangeRateApiClient (含 Circuit Breaker) | `service/external/ExchangeRateApiClient.java` | ✅ |
| P3-004a | ExchangeRateProperties (含 timeout) | `config/ExchangeRateProperties.java` | ✅ |
| P3-004b | ExchangeRateException | `service/external/ExchangeRateException.java` | ✅ |
| P3-004c | ExchangeRateService (1h 快取 + 24h fallback) | `service/ExchangeRateService.java` | ✅ |
| P3-004d | ExchangeRateApiController (含 rate limit) | `controller/api/ExchangeRateApiController.java` | ✅ |
| P3-004e | ExchangeRateResponse DTO | `dto/response/ExchangeRateResponse.java` | ✅ |
| P3-004f | ExchangeRateRequest DTO | `dto/request/ExchangeRateRequest.java` | ⏭️ 跳過 |
| P3-004g | GlobalExceptionHandler 更新 | `exception/GlobalExceptionHandler.java` | ✅ |
| P3-004h | CurrencyConverter 工具類 | `util/CurrencyConverter.java` | ✅ |

### Phase 3.2: 多幣別結算 (P3-005~006)

| 任務 ID | 任務 | 檔案 | 狀態 |
|---------|------|------|------|
| P3-005a | ExpenseService.convertToBaseCurrency() | `service/ExpenseService.java` | ⏭️ 合併至 P3-005b |
| P3-005b | SettlementService 多幣別支援 | `service/SettlementService.java` | ✅ |
| P3-005c | SettlementResponse 多幣別欄位 | `dto/response/SettlementResponse.java` | ✅ |
| P3-005d | 結算頁面顯示原始幣別 | `templates/expense/settlement.html` | ✅ |
| P3-006a | 分帳 UI - 幣別選擇器 | `templates/expense/create.html` | ✅ (已存在) |
| P3-006b | 分帳 UI - 即時匯率預覽 | `static/js/expense.js` | ✅ |
| P3-006c | 支出列表顯示幣別資訊 | `templates/expense/list.html` | ✅ (已存在) |

### Phase 3.3: 統計功能 (P3-007)

| 任務 ID | 任務 | 檔案 | 狀態 |
|---------|------|------|------|
| P3-007a | ExpenseRepository 聚合查詢 | `repository/ExpenseRepository.java` | ⏭️ 跳過 (使用 in-memory 聚合) |
| P3-007b | ExpenseAggregator (純領域邏輯) | `domain/statistics/ExpenseAggregator.java` | ✅ |
| P3-007c | StatisticsService (含授權檢查) | `service/StatisticsService.java` | ✅ |
| P3-007d | CategoryBreakdownResponse | `dto/response/CategoryBreakdownResponse.java` | ✅ |
| P3-007e | TrendResponse | `dto/response/TrendResponse.java` | ✅ |
| P3-007f | MemberStatisticsResponse | `dto/response/MemberStatisticsResponse.java` | ✅ |
| P3-007g | StatisticsApiController | `controller/api/StatisticsApiController.java` | ✅ |
| P3-007h | 統計頁面 | `templates/expense/statistics.html` | ✅ |
| P3-007i | Chart.js 整合 | `static/js/expense-statistics.js` | ✅ |
| P3-007j | SecurityConfig CSP 更新 | `config/SecurityConfig.java` | ✅ |
| P3-007k | ExpenseService 快取失效 | `service/StatisticsService.java` | ✅ (evictCaches 方法) |

### Phase 3.4: 資料庫 & 設定

| 任務 ID | 任務 | 狀態 |
|---------|------|------|
| P3-DB-001 | 資料庫索引 migration | ✅ |
| P3-CFG-001 | application.yml exchangerate 設定 | ✅ (已存在) |
| P3-CFG-002 | CacheConfig.java 快取設定 | ✅ |

### Phase 3.5: 測試 & 文件

| 任務 ID | 任務 | 狀態 |
|---------|------|------|
| P3-T-001 | ExchangeRateService 單元測試 | ✅ (30 tests) |
| P3-T-002 | CurrencyConverter 單元測試 | ✅ |
| P3-T-003 | StatisticsService 單元測試 | ✅ (17 tests) |
| P3-T-004 | ExpenseAggregator 單元測試 | ✅ (11 tests) |
| P3-T-005 | API 整合測試 (含授權) | ✅ (14 tests) |
| P3-T-006 | 文件更新 | ✅ |

---

## 風險評估

### 🔴 High Risk

| 風險 | 說明 | 緩解措施 |
|------|------|----------|
| **API Key 洩露** | 錯誤日誌可能包含 API Key | 實作 `sanitizeUrl()` 移除敏感資訊 |
| **ExchangeRate-API 配額** | 免費方案 1,500 req/month | 1h 快取 + 批次請求 + fallback |
| **授權繞過** | 統計 API 可能洩露行程資料 | 所有端點必須呼叫 `permissionChecker.canView()` |
| **幣別精度問題** | BigDecimal 多步驟轉換累積誤差 | 統一使用 `HALF_UP`, scale=2 (金額), scale=6 (匯率) |

### 🟡 Medium Risk

| 風險 | 說明 | 緩解措施 |
|------|------|----------|
| Chart.js CSP | CDN 被 CSP 阻擋 | 更新 SecurityConfig script-src |
| 快取失效遺漏 | 統計資料不同步 | 在 ExpenseService CRUD 方法加入 evict |
| SSRF 風險 | Currency code 注入 | Whitelist 驗證 + 正則 `^[A-Z]{3}$` |
| 資訊洩露 | 錯誤訊息暴露行程存在 | 統一錯誤訊息 "您沒有權限查看此行程" |

---

## 安全性檢查清單

### 外部 API 整合
- [x] API keys 存於環境變數
- [x] API keys 不記錄在日誌 (sanitizeUrl)
- [x] Currency code whitelist 驗證
- [x] Rate limiting (30 req/min per user)
- [x] Timeout 設定 (connect: 5s, read: 10s)
- [x] Circuit Breaker (5 failures → 5min cooldown)
- [x] Fallback 最大過期時間 24h

### 統計 API 授權
- [x] `permissionChecker.canView()` 在資料存取前呼叫
- [x] 錯誤訊息不洩露行程存在
- [x] 所有三個端點都有保護: category, trend, members

### 金額計算
- [x] BigDecimal 用於所有金錢運算
- [x] RoundingMode.HALF_UP, scale=2
- [x] 匯率範圍驗證 (0.0001 ~ 1,000,000)
- [x] CurrencyConverter 方法為 immutable

### 輸入驗證
- [x] UUID 格式驗證 (GlobalExceptionHandler)
- [x] Currency code 正則 `^[A-Z]{3}$`
- [x] Date range 參數驗證

---

## 時程估算

| 階段 | 任務數 | 預估工時 |
|------|--------|----------|
| Phase 3.1 (匯率基礎) | 11 | 3-4 天 |
| Phase 3.2 (多幣別結算) | 6 | 2-3 天 |
| Phase 3.3 (統計功能) | 11 | 4-5 天 |
| Phase 3.4 (資料庫設定) | 2 | 0.5 天 |
| Phase 3.5 (測試文件) | 6 | 3-4 天 |
| **總計** | **36** | **14-18 天 (3-4 週)** |

### 並行化建議

```
Week 1:
├── Developer A: P3-001 ~ P3-004h (匯率核心)
└── Developer B: P3-007a, P3-007d~f (Repository + DTOs)

Week 2:
├── Developer A: P3-005a~c (多幣別結算)
└── Developer B: P3-007b~c (Aggregator + Service)

Week 3:
├── Developer A: P3-006a~c (分帳 UI)
└── Developer B: P3-007g~k (統計 UI + CSP)

Week 4:
├── Developer A: P3-T-001~002 (匯率測試)
└── Developer B: P3-T-003~006 (統計測試 + 文件)
```

---

## 開發規範提醒

1. **遵循現有模式** - 參考 `WeatherClient`/`OpenWeatherMapClient`/`MockWeatherClient`
2. **TDD 流程** - 先寫測試 (RED)，再寫實作 (GREEN)，最後重構 (REFACTOR)
3. **契約註解** - 所有 public 方法須包含 `@contract` 註解
4. **授權檢查** - 統計 API 必須在資料存取前檢查權限
5. **覆蓋率目標** - Service >= 80%, Domain >= 90%
6. **BigDecimal** - 金額計算禁止使用 double/float

---

## 進度追蹤

| 日期 | 完成項目 | 備註 |
|------|----------|------|
| 2026-02-03 | 建立 task.md v1.0 | 開始 Phase 3 規劃 |
| 2026-02-03 | 更新 task.md v2.0 | 經 agent 審查，細化任務、加入安全檢查清單 |
| 2026-02-03 | Phase 3.1 完成 (10/11) | ExchangeRate 基礎建設完成，P3-004f 跳過 |
| 2026-02-03 | Phase 3.2 完成 | 多幣別結算、即時匯率預覽，全部 714 tests 通過 |
| 2026-02-03 | Phase 3.3 完成 | 統計功能完成，分類/趨勢/成員統計，Chart.js 整合，725 tests 通過 |
| 2026-02-03 | Phase 3.4 完成 | CacheConfig + Caffeine 設定，資料庫索引 migration，725 tests 通過 |
| 2026-02-03 | Phase 3.5 完成 | StatisticsService + API Controller 測試，756 tests 通過，**Phase 3 全部完成** |
| 2026-02-03 | P3-T-001 補齊 | 新增 ExchangeRateServiceTest.java (30 tests)，總共 786 tests 通過 |
| - | - | - |
