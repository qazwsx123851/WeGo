# WeGo 維運手冊 (Runbook)

> 最後更新: 2026-02-04 | 部署平台: Railway
>
> **變更日誌**:
> - 2026-02-04: Phase 4 完成 - 新增深色模式、E2E 測試、安全強化、無障礙支援
> - 2026-02-03: Phase 3 完成 - 新增匯率 API、統計功能問題排查
> - 2026-02-02: 遷移至 Google Routes API、新增 API 問題排查、部署流程暫停
> - 2026-02-01: 新增 Transport Mode 系統、統一錯誤處理、全域概覽頁面
> - 2026-01-28: 初始版本

## 部署架構

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   GitHub        │────▶│    Railway      │────▶│   Supabase      │
│   (main branch) │     │   (Java 17)     │     │   (PostgreSQL)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 部署流程

### 自動部署

> **⚠️ 目前狀態**: 自動部署已暫停 (2026-02-02)
>
> `.github/workflows/deploy.yml` 設定 `if: false`，需手動移除此行以恢復自動部署。

推送到 `main` 分支會觸發 CI 測試，部署需手動啟用：

```bash
git push origin main  # 只觸發 CI 測試
```

GitHub Actions 工作流程：
1. Checkout code
2. Setup JDK 17 + Node.js 20
3. Install frontend dependencies
4. Build Tailwind CSS
5. Build JAR (`./mvnw clean package -DskipTests`)
6. Deploy to Railway (目前暫停)

### 手動部署

```bash
# 1. 建置 JAR
./mvnw clean package -DskipTests

# 2. 使用 Railway CLI
railway login
railway up
```

---

## 本地開發環境

### 環境變數自動載入

專案使用 `spring-dotenv` 自動載入 `.env` 檔案：

```bash
# 1. 複製範本
cp .env.example .env

# 2. 填入實際值 (參考 .env.example 說明)

# 3. 啟動應用程式 (.env 自動載入)
./mvnw spring-boot:run
```

> **注意**: Railway 部署時直接設定環境變數，不使用 .env 檔案。

---

## 環境變數設定 (Railway)

在 Railway Dashboard 設定以下 Secrets：

### 必要變數

| 變數 | 說明 | 範例 |
|------|------|------|
| `DATABASE_URL` | PostgreSQL 連線 URL | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres` |
| `DATABASE_USERNAME` | 資料庫使用者 | `postgres` |
| `DATABASE_PASSWORD` | 資料庫密碼 | |
| `SUPABASE_URL` | Supabase 專案 URL | `https://xxx.supabase.co` |
| `SUPABASE_SERVICE_KEY` | Service Role Key (JWT 格式) | `eyJhbGciOiJIUzI1NiIs...` |
| `GOOGLE_CLIENT_ID` | OAuth Client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | OAuth Secret | `GOCSPX-xxx` |
| `PORT` | 伺服器埠號 | `8080` |

### 可選變數 (外部 API)

| 變數 | 說明 | 預設 |
|------|------|------|
| `GOOGLE_MAPS_API_KEY` | Google Maps API | (Mock) |
| `GOOGLE_MAPS_ENABLED` | 啟用 Google Maps | `false` |
| `GOOGLE_MAPS_USE_ROUTES_API` | 使用 Routes API | `true` |
| `OPENWEATHERMAP_API_KEY` | 天氣預報 API | (Mock) |
| `OPENWEATHERMAP_ENABLED` | 啟用天氣服務 | `false` |
| `EXCHANGERATE_API_KEY` | 匯率轉換 API | (Mock) |
| `EXCHANGERATE_ENABLED` | 啟用匯率服務 | `false` |

> **注意**: 可選 API 未設定時會使用 Mock 實作，功能正常但回傳模擬資料。

### GitHub Secrets (CI/CD)

| Secret | 說明 |
|--------|------|
| `RAILWAY_TOKEN` | Railway API Token (用於自動部署) |

---

## 監控與健康檢查

### 健康檢查端點

```bash
# 基本健康檢查
curl https://your-app.railway.app/api/health

# 預期回應
{
  "success": true,
  "data": {
    "application": "WeGo",
    "status": "healthy"
  },
  "timestamp": "2026-01-28T10:00:00Z"
}
```

### Railway 監控

1. 進入 Railway Dashboard
2. 選擇 wego 服務
3. 檢視 Deployments / Logs / Metrics

### 日誌查看

```bash
# Railway CLI
railway logs

# 或在 Railway Dashboard 查看
```

---

## 常見問題與修復

### 1. 資料庫連線失敗

**症狀**: `FATAL: Tenant or user not found` 或 `Connection refused`

**原因**: 連線字串格式錯誤

**修復**:
```bash
# Direct Connection (開發環境)
DATABASE_URL=jdbc:postgresql://db.[project-ref].supabase.co:5432/postgres
DATABASE_USERNAME=postgres

# Pooler Mode (生產環境，IPv4/IPv6 相容)
DATABASE_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres
DATABASE_USERNAME=postgres.[project-ref]
```

### 2. Port 8080 被佔用

**症狀**: `Port 8080 was already in use`

**修復**:
```bash
# macOS/Linux
lsof -ti:8080 | xargs kill -9

# 或使用其他 port
PORT=8081 ./mvnw spring-boot:run
```

### 3. OAuth 登入失敗

**症狀**: Google 登入後重導向錯誤

**檢查**:
1. Google Cloud Console → OAuth 2.0 Client IDs
2. 確認 Authorized redirect URIs 包含:
   - `http://localhost:8080/login/oauth2/code/google` (開發)
   - `https://your-app.railway.app/login/oauth2/code/google` (生產)

**症狀**: `Error 400: redirect_uri_mismatch`

**修復**: Authorized redirect URI 必須完全符合，包括協定和路徑

**症狀**: `Access blocked: This app's request is invalid`

**修復**:
1. OAuth consent screen 需完成設定
2. 在 Test users 加入測試用的 Gmail 帳號

### 3a. 使用者建立失敗

**症狀**: OAuth 登入成功但 redirect 到錯誤頁面

**原因**: Google 未回傳 email (罕見)

**檢查**:
```bash
# 確認 OAuth scope 包含 email
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            scope:
              - email
              - profile
```

### 4. Tailwind CSS 未載入

**症狀**: 頁面無樣式

**修復**:
```bash
cd src/main/frontend
npm install
npm run build
# 確認 dist/styles.css 存在
```

### 5. 測試覆蓋率不足

**症狀**: CI 警告覆蓋率低於 80%

**檢查**:
```bash
./mvnw jacoco:report
open target/site/jacoco/index.html
```

### 6. 天氣 API 失敗

**症狀**: 天氣卡片顯示「載入中...」或「無法取得天氣資料」

**可能原因**:
- OpenWeatherMap API Key 未設定或無效
- 日期超過 5 天預報範圍
- 座標無效

**檢查**:
```bash
# 驗證 API Key
curl "https://api.openweathermap.org/data/2.5/forecast?lat=25.0&lon=121.5&appid=$OPENWEATHERMAP_API_KEY"

# 檢查應用程式日誌
railway logs | grep -i weather
```

**修復**:
1. 確認 `OPENWEATHERMAP_API_KEY` 已正確設定
2. 確認 `OPENWEATHERMAP_ENABLED=true`
3. 若 API 配額用盡，系統會自動 fallback 到 MockWeatherClient

### 7. CSP 阻擋資源載入

**症狀**: Console 顯示 `violates the following Content Security Policy directive`

**檢查位置**: `src/main/java/com/wego/config/SecurityConfig.java`

**常見情境**:

| 資源類型 | CSP 指令 | 解決方法 |
|----------|----------|----------|
| Inline script | script-src | 移至外部 .js 檔案 |
| Blob URL (圖片預覽) | img-src | 添加 `blob:` |
| Google Fonts | font-src, style-src | 添加 `https://fonts.googleapis.com` |
| Lottie 動畫 | script-src | 添加 `https://unpkg.com` |

### 8. 圖片上傳失敗

**症狀**: 封面圖片上傳後無反應或錯誤

**可能原因**:
1. Supabase 使用了錯誤的 Key (anon key 而非 service_role key)
2. Storage bucket 未建立
3. 檔案大小超過限制 (10MB)

**檢查**:
```bash
# 驗證 Supabase 連線
curl -X GET "$SUPABASE_URL/storage/v1/bucket" \
  -H "Authorization: Bearer $SUPABASE_SERVICE_KEY"
```

**修復**:
1. 確認使用 `service_role` key (JWT 格式，以 `eyJ` 開頭)
2. 在 Supabase Dashboard 建立 `trip-covers` bucket
3. 設定 bucket 為 public 或配置適當的 RLS

### 9. 路線優化無效果

**症狀**: 點擊「智慧排序」後順序未改變

**可能原因**:
- 活動少於 3 個 (無需優化)
- 活動沒有地點座標
- 原本順序已是最佳

**檢查**:
```bash
# 查看優化日誌
railway logs | grep -i "RouteOptimizer"
```

**注意**:
- RouteOptimizer 使用 Greedy Nearest Neighbor 演算法
- 若活動超過 15 個會顯示警告
- 第一個活動的位置會被保留為起點

### 10. 批次重算交通時間問題

**症狀**: 重算功能無反應或顯示錯誤

**可能原因**:
- Google Maps API 配額用盡
- 網路連線問題
- 景點缺少座標

**檢查**:
```bash
# 查看 TransportCalculationService 日誌
railway logs | grep -i "TransportCalculation"

# 查看 API 呼叫狀態
railway logs | grep -i "Google Maps"
```

**Rate Limiting 機制**:
- API 呼叫間隔: 100ms
- 預設最大呼叫次數: 無限制 (可透過參數設定)
- 超過限額自動切換 Haversine 估算

**回傳統計說明**:
| 欄位 | 說明 |
|------|------|
| `apiSuccessCount` | Google API 成功呼叫次數 |
| `fallbackCount` | 降級為 Haversine 估算次數 |
| `skippedCount` | 跳過的景點 (首個景點、NOT_CALCULATED 模式) |
| `manualCount` | 保留手動輸入的景點 (FLIGHT/HIGH_SPEED_RAIL) |

### 11. Transport Source Badge 不顯示

**症狀**: 交通資訊顯示但沒有來源 Badge (精確/估算/手動)

**原因**: 舊資料可能沒有 `transportSource` 欄位值

**修復**:
1. 執行批次重算功能更新所有景點
2. 或手動編輯景點觸發重新計算

**Thymeleaf 顯示邏輯**:
```html
<span th:if="${activity.transportSource != null}"
      th:class="${activity.transportSource.badgeClass}"
      th:text="${activity.transportSource.displayName}">
</span>
```

### 12. 全域概覽頁面無資料

**症狀**: `/expenses` 或 `/documents` 頁面顯示空白

**可能原因**:
- 使用者尚未加入任何行程
- 行程中沒有支出/文件
- 權限問題 (非成員無法查看)

**檢查**:
```bash
# 確認使用者的行程成員關係
railway logs | grep -i "GlobalExpenseService\|GlobalDocumentService"
```

**注意**: 全域概覽只顯示使用者有權限存取的行程資料

### 13. 錯誤頁面顯示不正確

**症狀**: 錯誤發生時顯示白屏或預設錯誤頁面

**可能原因**:
- `WebExceptionHandler` 未正確載入
- 錯誤模板不存在
- CSP 阻擋樣式載入

**檢查**:
1. 確認 `error/error.html` 存在於 templates 目錄
2. 確認 `WebExceptionHandler` 標註 `@Order(1)`
3. 確認 CSS 檔案正確部署

**錯誤頁面位置**:
```
src/main/resources/templates/error/
├── error.html    # 統一錯誤頁面 (動態內容)
├── 403.html      # 舊版 403 頁面 (備用)
├── 404.html      # 舊版 404 頁面 (備用)
└── 500.html      # 舊版 500 頁面 (備用)
```

### 14. 拖曳重排無法儲存

**症狀**: 拖曳景點後刷新頁面順序恢復原狀

**可能原因**:
- JavaScript 錯誤
- API 呼叫失敗
- 權限不足 (Viewer 角色)

**檢查**:
```javascript
// 瀏覽器 Console 檢查
console.log('Drag event listeners attached');
```

```bash
# 後端日誌
railway logs | grep -i "reorder"
```

**權限需求**: 需要 OWNER 或 EDITOR 角色才能重排景點

### 15. 匯率 API 失敗

**症狀**: 支出頁面匯率顯示錯誤或無法轉換

**可能原因**:
- ExchangeRate API Key 未設定
- API 配額用盡
- 不支援的貨幣代碼

**檢查**:
```bash
# 驗證 API Key
curl "https://v6.exchangerate-api.com/v6/$EXCHANGERATE_API_KEY/latest/TWD"

# 查看應用程式日誌
railway logs | grep -i "ExchangeRate"
```

**修復**:
1. 確認 `EXCHANGERATE_API_KEY` 已設定
2. 確認 `EXCHANGERATE_ENABLED=true`
3. 若 API 配額用盡，系統會 fallback 到 MockExchangeRateClient (固定匯率)

**支援貨幣**: TWD, USD, JPY, EUR, GBP, CNY, KRW, HKD

### 16. 深色模式問題

**症狀**: 切換深色模式後頁面閃爍 (FOUC)

**可能原因**:
- head 內嵌腳本未載入
- localStorage 被清除
- CSP 阻擋 inline script

**檢查**:
```javascript
// 瀏覽器 Console
localStorage.getItem('theme')
document.documentElement.classList.contains('dark')
```

**修復**:
1. 確認 `fragments/head.html` 包含 FOUC 防護腳本
2. 確認 CSP 允許 `'unsafe-inline'` for script-src (必要)
3. 清除 localStorage 後重新選擇主題

**症狀**: Chart.js 圖表顏色未隨主題變化

**修復**:
1. 確認 `expense-statistics.js` 監聽 `themechange` 事件
2. 手動觸發: `window.dispatchEvent(new Event('themechange'))`

### 17. E2E 測試失敗

**症狀**: Playwright 測試無法通過

**可能原因**:
- 應用程式未啟動
- OAuth Mock 未正確設定
- 瀏覽器驅動過期

**檢查**:
```bash
# 確認應用程式在 8080 運行
curl http://localhost:8080/api/health

# 更新 Playwright 瀏覽器
npx playwright install
```

**修復**:
```bash
cd e2e
npm install
npx playwright install chromium
npx playwright test --debug  # 除錯模式
```

**OAuth Mock 機制**:
- E2E 測試使用 `/test/auth/mock-login` 端點
- 需在 `application-test.yml` 啟用測試 profile
- 測試用戶: `testuser@wego.test`

### 18. Rate Limiting 觸發

**症狀**: API 回傳 `429 Too Many Requests`

**預設限制**:
- 100 requests/minute per IP
- Bucket 使用 Caffeine cache (TTL: 5 分鐘)
- 最大追蹤 IP 數: 100,000

**檢查**:
```bash
railway logs | grep -i "rate limit\|too many"
```

**調整** (需修改 `RateLimitConfig.java`):
```java
private static final int REQUESTS_PER_MINUTE = 100;  // 調整此值
private static final int MAX_CACHE_SIZE = 100_000;
private static final int CACHE_TTL_MINUTES = 5;
```

---

## 回滾程序

### Railway 回滾

1. 進入 Railway Dashboard → Deployments
2. 找到上一個成功的部署
3. 點擊 "Redeploy"

### Git 回滾

```bash
# 查看歷史
git log --oneline -10

# 回滾到指定 commit
git revert HEAD
git push origin main

# 或 reset (謹慎使用)
git reset --hard <commit-hash>
git push origin main --force  # 需要權限
```

---

## 資料庫維運

### Supabase 備份

Supabase Pro 方案自動備份。手動備份：

```bash
# 使用 pg_dump
pg_dump -h db.[project-ref].supabase.co -U postgres -d postgres > backup.sql
```

### 執行 Migration

Migration 透過 Hibernate `ddl-auto: update` 自動執行。

手動執行 SQL：
```bash
# 透過 Supabase Dashboard → SQL Editor
# 或使用 psql
psql -h db.[project-ref].supabase.co -U postgres -d postgres -f migration.sql
```

---

## 效能調校

### JVM 參數 (Railway)

在 Railway 設定 Start Command：
```bash
java -Xmx512m -Xms256m -jar target/wego-1.0.0-SNAPSHOT.jar
```

### 連線池設定

`application.yml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10  # 根據 Railway 資源調整
      minimum-idle: 2
      connection-timeout: 30000
```

---

## 快速診斷清單

當系統出現問題時，依序檢查：

### 啟動失敗
- [ ] `.env` 檔案存在且包含所有必要變數
- [ ] `DATABASE_URL` 格式正確 (`jdbc:postgresql://...`)
- [ ] `GOOGLE_CLIENT_ID` 和 `GOOGLE_CLIENT_SECRET` 已設定
- [ ] Port 8080 未被佔用

### 登入失敗
- [ ] Google OAuth redirect URI 設定正確
- [ ] OAuth consent screen 已配置
- [ ] 測試使用者已加入 (開發模式)

### 功能異常
- [ ] 檢查 browser console 是否有 CSP 錯誤
- [ ] 檢查 network tab 是否有 4xx/5xx 錯誤
- [ ] 查看 Railway/應用程式日誌

### 效能問題
- [ ] 檢查 HikariCP 連線池狀態
- [ ] 檢查外部 API 回應時間 (Google Maps, OpenWeatherMap)
- [ ] 檢查是否有 N+1 查詢問題

### Transport 相關問題
- [ ] 確認 Google Maps API Key 已設定且有效
- [ ] 確認 `GOOGLE_MAPS_ENABLED=true`
- [ ] 檢查景點是否有有效座標
- [ ] 檢查 TransportMode 是否支援自動計算

### 全域概覽問題
- [ ] 確認使用者已登入
- [ ] 確認使用者是行程成員
- [ ] 檢查 GlobalExpenseService/GlobalDocumentService 日誌

### 深色模式問題
- [ ] 檢查 localStorage 中 `theme` 值
- [ ] 確認 `<html>` 標籤有 `dark` class
- [ ] 檢查 CSP 是否允許 inline script
- [ ] 驗證 Chart.js 是否監聽 `themechange` 事件

### E2E 測試問題
- [ ] 確認應用程式在 localhost:8080 運行
- [ ] 確認 Playwright 瀏覽器已安裝
- [ ] 檢查 OAuth Mock 端點是否可存取
- [ ] 執行 `npx playwright test --debug` 除錯

---

## 緊急聯絡

| 角色 | 聯絡方式 |
|------|----------|
| 維運 | (待設定) |
| 資料庫 | Supabase Support |
| 部署 | Railway Support |
| 地圖 API | Google Cloud Support |
| 天氣 API | OpenWeatherMap Support |
| 匯率 API | ExchangeRate-API Support |

---

## 新功能維運指南

### Transport Mode 系統監控

**關鍵日誌**:
```bash
# 查看交通計算
railway logs | grep -i "TransportCalculation"

# 查看 Google Maps API 呼叫 (Routes API)
railway logs | grep -i "GoogleMapsClient\|Routes API"

# 查看 Haversine fallback
railway logs | grep -i "Haversine"

# 查看 TRANSIT → DRIVING fallback
railway logs | grep -i "fallback\|fromFallback"
```

**監控指標**:
- Google API 成功率
- Haversine fallback 頻率
- TRANSIT → DRIVING fallback 頻率
- 平均計算時間

### Google Routes API 問題排查

**症狀**: 交通時間計算失敗

**可能原因**:
1. API Key 未啟用 Routes API
2. TRANSIT 模式在日本部分地區無資料
3. API 配額用盡

**檢查**:
```bash
# 確認 API 回應
railway logs | grep -i "Routes API\|computeRouteMatrix"

# 確認 fallback 狀態
railway logs | grep -i "fromFallback"
```

**Fallback 機制**:
```
Routes API (TRANSIT)
    ↓ 失敗
Routes API (DRIVING)  ← fromFallback=true
    ↓ 失敗
GoogleMapsException
```

**環境變數切換**:
```bash
# 使用新 Routes API (推薦)
GOOGLE_MAPS_USE_ROUTES_API=true

# 回退到舊 Distance Matrix API
GOOGLE_MAPS_USE_ROUTES_API=false
```

### 全域概覽頁面監控

**關鍵端點**:
| 端點 | 說明 |
|------|------|
| `/expenses` | 全域支出概覽 |
| `/documents` | 全域文件概覽 |
| `/profile` | 使用者個人檔案 |

**效能注意事項**:
- 全域概覽需要聚合多行程資料
- 文件頁面支援分頁 (每頁 20 筆)
- 建議監控這些端點的回應時間

### 錯誤處理架構

**錯誤處理流程**:
```
Exception 發生
    │
    ▼
是 API Controller? ──Yes──▶ GlobalExceptionHandler ──▶ JSON Response
    │
    No
    │
    ▼
WebExceptionHandler ──▶ error/error.html ──▶ HTML Response
```

**自訂錯誤碼**:
| ErrorCode | HTTP Status | 說明 |
|-----------|-------------|------|
| `VALIDATION_ERROR` | 400 | 驗證失敗 |
| `AUTH_REQUIRED` | 401 | 需要登入 |
| `ACCESS_DENIED` | 403 | 無權限 |
| `TRIP_NOT_FOUND` | 404 | 行程不存在 |
| `ACTIVITY_NOT_FOUND` | 404 | 景點不存在 |
| `INTERNAL_ERROR` | 500 | 內部錯誤 |

---

## 相關文件

| 文件 | 說明 |
|------|------|
| [CONTRIB.md](./CONTRIB.md) | 開發貢獻指南 |
| [api-keys-setup.md](./api-keys-setup.md) | API Keys 設定指南 |
| [software-design-document.md](./software-design-document.md) | 軟體設計文件 |
| [ui-design-guide.md](./ui-design-guide.md) | UI 設計指南 |
| [../CLAUDE.md](../CLAUDE.md) | AI 開發指南與錯誤模式 |

---

## Phase 4 新增維運項目

### 深色模式監控

**關鍵檔案**:
| 檔案 | 說明 |
|------|------|
| `app.js` | DarkMode 控制器 |
| `fragments/head.html` | FOUC 防護腳本 |
| `expense-statistics.js` | Chart.js 主題響應 |

**localStorage 鍵**:
- `theme`: 使用者偏好 (`light` / `dark` / `system`)

### E2E 測試維運

**測試檔案位置**: `e2e/tests/`

| 測試 | 說明 | 執行時間 |
|------|------|----------|
| `auth.spec.ts` | 登入/登出流程 | ~30s |
| `trip.spec.ts` | 行程 CRUD | ~45s |
| `activity.spec.ts` | 景點管理 | ~40s |
| `expense.spec.ts` | 分帳功能 | ~35s |
| `document.spec.ts` | 文件上傳 | ~30s |
| `todo.spec.ts` | 代辦事項 | ~25s |
| `dark-mode.spec.ts` | 深色模式 | ~40s |

**執行 E2E 測試**:
```bash
# 本地執行 (需先啟動應用程式)
cd e2e
npm install
npx playwright test

# 僅執行特定測試
npx playwright test auth.spec.ts

# 除錯模式
npx playwright test --debug

# 查看報告
npx playwright show-report
```

**CI 整合** (待完成):
```yaml
# .github/workflows/e2e.yml
- name: Run E2E tests
  run: |
    cd e2e
    npm ci
    npx playwright install chromium
    npx playwright test
```

### 安全監控

**Rate Limiting 監控**:
```bash
# 查看 Rate Limit 觸發
railway logs | grep -i "rate limit\|429"

# 查看 IP bucket 狀態 (需在程式碼中加入 logging)
railway logs | grep -i "RateLimitConfig"
```

**安全相關日誌**:
```bash
# 認證失敗
railway logs | grep -i "unauthorized\|authentication"

# 授權失敗 (IDOR 嘗試)
railway logs | grep -i "forbidden\|access denied"

# 驗證失敗
railway logs | grep -i "validation\|invalid"
```

### 效能監控 (Phase 4 優化)

**N+1 查詢優化**:
- `DocumentRepository.findByTripIdWithUser()` - Batch fetch
- `ExpenseSplitRepository.findByExpenseIds()` - Batch fetch
- `TripService.getTripWithMemberNames()` - Join fetch

**快取設定** (Caffeine):
| 快取名稱 | TTL | 最大條目 |
|----------|-----|---------|
| `exchangeRates` | 1 小時 | 100 |
| `weather` | 30 分鐘 | 500 |
| `rateLimitBuckets` | 5 分鐘 | 100,000 |
