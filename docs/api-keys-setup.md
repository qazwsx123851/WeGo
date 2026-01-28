# WeGo API Keys 設定指南

## 概述

本專案使用多個外部 API，本文件說明各 API 的申請方式、開發階段的替代方案，以及安全性注意事項。

---

## 1. API 需求優先級

| 優先級 | API | 階段 | 費用 | 說明 |
|--------|-----|------|------|------|
| 🔴 P0 | Supabase | MVP | 免費方案可用 | 資料庫，核心必須 |
| 🔴 P0 | Google OAuth | MVP | 免費 | 用戶登入 |
| 🟡 P1 | Google Maps | MVP | 免費額度 $200/月 | 可先 mock 開發 |
| 🟢 P2 | OpenWeatherMap | Phase 2 | 免費方案可用 | 天氣預報 |
| 🟢 P3 | ExchangeRate-API | Phase 3 | 免費方案可用 | 匯率轉換 |

---

## 2. Supabase 設定（必須）

### 2.1 申請步驟

1. 前往 [supabase.com](https://supabase.com)
2. 使用 GitHub 帳號登入
3. 點擊「New Project」
4. 設定：
   - Project name: `wego`
   - Database password: （記下來）
   - Region: `Northeast Asia (Tokyo)` 或最近的區域
5. 等待專案建立完成（約 2 分鐘）

### 2.2 取得連線資訊

1. 進入專案 Dashboard
2. 點擊左側「Settings」→「Database」
3. 複製「Connection string」→「URI」
4. 格式：`postgresql://postgres:[PASSWORD]@db.[REF].supabase.co:5432/postgres`

### 2.3 取得 Storage 金鑰

1. 點擊「Settings」→「API」
2. 複製：
   - `Project URL` → `SUPABASE_URL`
   - `service_role` (secret) → `SUPABASE_SERVICE_KEY`

### 2.4 環境變數

```bash
DATABASE_URL=postgresql://postgres:[PASSWORD]@db.[REF].supabase.co:5432/postgres
SUPABASE_URL=https://[REF].supabase.co
SUPABASE_SERVICE_KEY=eyJhbGciOiJIUzI1NiIs...
```

---

## 3. Google OAuth 設定（必須）

### 3.1 申請步驟

1. 前往 [Google Cloud Console](https://console.cloud.google.com)
2. 建立新專案（或選擇現有專案）
3. 左側選單 →「APIs & Services」→「Credentials」
4. 點擊「Create Credentials」→「OAuth client ID」
5. 設定：
   - Application type: `Web application`
   - Name: `WeGo Web Client`
   - Authorized JavaScript origins:
     - `http://localhost:8080`
     - `https://your-domain.com`（部署後）
   - Authorized redirect URIs:
     - `http://localhost:8080/login/oauth2/code/google`
     - `https://your-domain.com/login/oauth2/code/google`
6. 複製 Client ID 和 Client Secret

### 3.2 設定 OAuth 同意畫面

1. 「OAuth consent screen」→ 選擇「External」
2. 填寫 App name、User support email
3. 新增 Scopes：`email`, `profile`, `openid`
4. 新增 Test users（開發階段）

### 3.3 環境變數

```bash
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxx
```

---

## 4. Google Maps API（可延後）

### 4.1 申請步驟

1. 前往 [Google Cloud Console](https://console.cloud.google.com)
2. 「APIs & Services」→「Library」
3. 啟用以下 API：
   - Maps JavaScript API
   - Distance Matrix API
   - Places API
4. 「Credentials」→「Create Credentials」→「API Key」
5. 建議設定 API Key 限制：
   - Application restrictions: HTTP referrers
   - API restrictions: 僅限上述 3 個 API

### 4.2 費用說明

| API | 免費額度 | 超出費用 |
|-----|---------|---------|
| Maps JavaScript | 無限制（地圖顯示） | - |
| Distance Matrix | $200/月（約 40,000 次） | $5/1000 次 |
| Places | $200/月 | $17-40/1000 次 |

### 4.3 開發階段替代方案

在沒有 API Key 時，可使用 Mock 服務：

```java
@Profile("dev")
@Service
public class MockGoogleMapsService implements GoogleMapsService {

    @Override
    public DirectionResult getDirections(String origin, String destination, TravelMode mode) {
        // 回傳模擬資料
        return DirectionResult.builder()
            .distance("5.2 km")
            .duration("15 mins")
            .durationSeconds(900)
            .build();
    }

    @Override
    public List<PlaceResult> searchPlaces(String query, double lat, double lng) {
        // 回傳預設景點
        return List.of(
            PlaceResult.builder()
                .placeId("mock-place-1")
                .name(query)
                .address("模擬地址")
                .lat(lat + 0.01)
                .lng(lng + 0.01)
                .build()
        );
    }
}
```

### 4.4 環境變數

```bash
GOOGLE_MAPS_API_KEY=AIzaSy...
```

---

## 5. OpenWeatherMap API（Phase 2）

### 5.1 申請步驟

1. 前往 [OpenWeatherMap](https://openweathermap.org/api)
2. 註冊帳號
3. 「API Keys」→「Generate」
4. 免費方案限制：60 calls/minute, 1,000,000 calls/month

### 5.2 開發階段替代方案

```java
@Profile("dev")
@Service
public class MockWeatherService implements WeatherService {

    @Override
    public WeatherForecast getForecast(double lat, double lng, LocalDate date) {
        return WeatherForecast.builder()
            .date(date)
            .tempHigh(25)
            .tempLow(18)
            .condition("晴")
            .icon("01d")
            .rainProbability(10)
            .build();
    }
}
```

### 5.3 環境變數

```bash
OPENWEATHERMAP_API_KEY=xxx
```

---

## 6. ExchangeRate-API（Phase 3）

### 6.1 申請步驟

1. 前往 [ExchangeRate-API](https://www.exchangerate-api.com)
2. 註冊免費帳號
3. 取得 API Key
4. 免費方案：1,500 requests/month

### 6.2 開發階段替代方案

使用固定匯率表：

```java
@Profile("dev")
@Service
public class MockExchangeRateService implements ExchangeRateService {

    private static final Map<String, BigDecimal> RATES_TO_TWD = Map.of(
        "USD", new BigDecimal("31.5"),
        "JPY", new BigDecimal("0.21"),
        "EUR", new BigDecimal("34.2"),
        "KRW", new BigDecimal("0.024"),
        "TWD", BigDecimal.ONE
    );

    @Override
    public BigDecimal getRate(String from, String to) {
        BigDecimal fromRate = RATES_TO_TWD.getOrDefault(from, BigDecimal.ONE);
        BigDecimal toRate = RATES_TO_TWD.getOrDefault(to, BigDecimal.ONE);
        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
    }
}
```

### 6.3 環境變數

```bash
EXCHANGERATE_API_KEY=xxx
```

---

## 7. 本地開發設定

### 7.1 環境變數檔案

建立 `.env.local`（已加入 .gitignore）：

```bash
# === 必須 ===
DATABASE_URL=postgresql://postgres:password@db.xxx.supabase.co:5432/postgres
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_SERVICE_KEY=eyJ...

GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxx

# === 可選（沒有則使用 Mock）===
GOOGLE_MAPS_API_KEY=
OPENWEATHERMAP_API_KEY=
EXCHANGERATE_API_KEY=
```

### 7.2 Spring Profile 設定

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILE:dev}

---
# application-dev.yml（開發環境，使用 Mock）
spring:
  config:
    activate:
      on-profile: dev

wego:
  external-api:
    google-maps:
      enabled: ${GOOGLE_MAPS_API_KEY:}
      mock-enabled: true  # 沒有 key 時使用 mock
    weather:
      enabled: ${OPENWEATHERMAP_API_KEY:}
      mock-enabled: true
    exchange-rate:
      enabled: ${EXCHANGERATE_API_KEY:}
      mock-enabled: true

---
# application-prod.yml（生產環境，必須有真實 key）
spring:
  config:
    activate:
      on-profile: prod

wego:
  external-api:
    google-maps:
      enabled: true
      mock-enabled: false
    weather:
      enabled: true
      mock-enabled: false
    exchange-rate:
      enabled: true
      mock-enabled: false
```

### 7.3 條件式 Bean 載入

```java
@Configuration
public class ExternalApiConfig {

    @Bean
    @ConditionalOnProperty(name = "wego.external-api.google-maps.mock-enabled", havingValue = "true")
    public GoogleMapsService mockGoogleMapsService() {
        return new MockGoogleMapsService();
    }

    @Bean
    @ConditionalOnProperty(name = "wego.external-api.google-maps.mock-enabled", havingValue = "false")
    public GoogleMapsService realGoogleMapsService(
            @Value("${GOOGLE_MAPS_API_KEY}") String apiKey) {
        return new GoogleMapsServiceImpl(apiKey);
    }
}
```

---

## 8. 安全性注意事項

### 8.1 絕對不要做的事

- ❌ 將 API Key 寫死在程式碼中
- ❌ 將 `.env` 檔案提交到 Git
- ❌ 在前端 JavaScript 中暴露 Secret Key
- ❌ 使用無限制的 API Key

### 8.2 應該做的事

- ✅ 使用環境變數
- ✅ 將 `.env*` 加入 `.gitignore`
- ✅ 設定 API Key 使用限制（IP、Referrer、API 範圍）
- ✅ 定期輪換 Key
- ✅ 監控 API 使用量

### 8.3 .gitignore 設定

```gitignore
# Environment files
.env
.env.local
.env.*.local
*.env

# IDE
.idea/
*.iml
.vscode/

# Build
target/
build/
```

---

## 9. 開發啟動 Checklist

### 最小可開發狀態（僅需 2 個 API）

- [ ] Supabase 專案已建立
- [ ] `DATABASE_URL` 已設定
- [ ] `SUPABASE_URL` 已設定
- [ ] `SUPABASE_SERVICE_KEY` 已設定
- [ ] Google OAuth Client 已建立
- [ ] `GOOGLE_CLIENT_ID` 已設定
- [ ] `GOOGLE_CLIENT_SECRET` 已設定
- [ ] `.env.local` 已建立且在 `.gitignore` 中

### 完整功能狀態

- [ ] 以上全部完成
- [ ] `GOOGLE_MAPS_API_KEY` 已設定
- [ ] `OPENWEATHERMAP_API_KEY` 已設定
- [ ] `EXCHANGERATE_API_KEY` 已設定
