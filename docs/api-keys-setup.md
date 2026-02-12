# WeGo API Keys 設定指南

> 更新日期：2026-02-12

## 概述

本專案使用多個外部 API，本文件說明各 API 的申請方式、開發階段的替代方案，以及安全性注意事項。

---

## 1. API 需求優先級

| 優先級 | API | 階段 | 費用 | 說明 |
|--------|-----|------|------|------|
| P0 | Supabase | MVP | 免費方案可用 | 資料庫，核心必須 |
| P0 | Google OAuth | MVP | 免費 | 用戶登入 |
| P1 | Google Maps | MVP | 免費額度 $200/月 | 可先 mock 開發 |
| P2 | OpenWeatherMap | Phase 2 | 免費方案可用 | 天氣預報 |
| P3 | ExchangeRate-API | Phase 3 | 免費方案可用 | 匯率轉換 |

---

## 2. Supabase 設定（必須）

### 2.1 申請步驟

1. 前往 [supabase.com](https://supabase.com)
2. 使用 GitHub 帳號登入
3. 點擊「New Project」
4. 設定：
   - Project name: `wego`
   - Database password:（記下來）
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

| 變數名稱 | 說明 | 範例 |
|----------|------|------|
| `DATABASE_URL` | PostgreSQL 連線字串 | `postgresql://postgres:[PASSWORD]@db.[REF].supabase.co:5432/postgres` |
| `SUPABASE_URL` | Supabase 專案 URL | `https://[REF].supabase.co` |
| `SUPABASE_SERVICE_KEY` | Service Role 金鑰（JWT 格式） | `eyJhbGciOiJIUzI1NiIs...` |

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
   - Authorized JavaScript origins: `http://localhost:8080`（部署後加上正式網域）
   - Authorized redirect URIs: `http://localhost:8080/login/oauth2/code/google`（部署後加上正式網域）
6. 複製 Client ID 和 Client Secret

### 3.2 設定 OAuth 同意畫面

1. 「OAuth consent screen」→ 選擇「External」
2. 填寫 App name、User support email
3. 新增 Scopes：`email`, `profile`, `openid`
4. 新增 Test users（開發階段）

### 3.3 環境變數

| 變數名稱 | 說明 | 範例 |
|----------|------|------|
| `GOOGLE_CLIENT_ID` | OAuth Client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | OAuth Client Secret | `GOCSPX-xxx` |

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

在沒有 API Key 時，系統自動載入 Mock 服務（透過 Spring Profile `dev`）。Mock 服務的行為：

- **地圖方向**：回傳固定模擬資料（距離 5.2 km、時間 15 分鐘）
- **地點搜尋**：回傳以查詢字串為名稱的模擬地點，座標為輸入座標的微偏移

### 4.4 環境變數

| 變數名稱 | 說明 | 範例 |
|----------|------|------|
| `GOOGLE_MAPS_API_KEY` | Google Maps API 金鑰 | `AIzaSy...` |

---

## 5. OpenWeatherMap API（Phase 2）

### 5.1 申請步驟

1. 前往 [OpenWeatherMap](https://openweathermap.org/api)
2. 註冊帳號
3. 「API Keys」→「Generate」
4. 免費方案限制：60 calls/minute, 1,000,000 calls/month

### 5.2 開發階段替代方案

在沒有 API Key 時，Mock 服務回傳固定的天氣預報資料（晴天、最高溫 25 度、最低溫 18 度、降雨機率 10%）。

### 5.3 環境變數

| 變數名稱 | 說明 | 範例 |
|----------|------|------|
| `OPENWEATHERMAP_API_KEY` | 天氣 API 金鑰 | `xxx` |

---

## 6. ExchangeRate-API（Phase 3）

### 6.1 申請步驟

1. 前往 [ExchangeRate-API](https://www.exchangerate-api.com)
2. 註冊免費帳號
3. 取得 API Key
4. 免費方案：1,500 requests/month

### 6.2 開發階段替代方案

在沒有 API Key 時，Mock 服務使用固定匯率表進行換算：

| 幣別 | 對 TWD 匯率 |
|------|-------------|
| USD | 31.5 |
| JPY | 0.21 |
| EUR | 34.2 |
| KRW | 0.024 |
| TWD | 1 |

### 6.3 環境變數

| 變數名稱 | 說明 | 範例 |
|----------|------|------|
| `EXCHANGERATE_API_KEY` | 匯率 API 金鑰 | `xxx` |

---

## 7. 本地開發設定

### 7.1 環境變數檔案

建立 `.env.local`（已加入 .gitignore），包含以下變數：

**必須變數：**

| 變數名稱 | 說明 |
|----------|------|
| `DATABASE_URL` | Supabase PostgreSQL 連線字串 |
| `SUPABASE_URL` | Supabase 專案 URL |
| `SUPABASE_SERVICE_KEY` | Service Role 金鑰 |
| `GOOGLE_CLIENT_ID` | OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | OAuth Client Secret |

**可選變數（無則使用 Mock）：**

| 變數名稱 | 說明 |
|----------|------|
| `GOOGLE_MAPS_API_KEY` | Google Maps API 金鑰 |
| `OPENWEATHERMAP_API_KEY` | 天氣 API 金鑰 |
| `EXCHANGERATE_API_KEY` | 匯率 API 金鑰 |

### 7.2 Spring Profile 設定

專案使用 Spring Profile 控制外部 API 的載入行為：

- **dev Profile**：預設使用 Mock 服務，當環境變數未設定時自動啟用 mock-enabled
- **prod Profile**：必須提供所有真實 API Key，mock-enabled 為 false

### 7.3 條件式 Bean 載入

外部 API 服務使用 `@ConditionalOnProperty` 依據 `mock-enabled` 屬性決定載入真實服務或 Mock 服務。每個外部 API（Google Maps、Weather、Exchange Rate）都有對應的 Mock 實作和真實實作，透過設定檔自動切換。

---

## 8. 安全性注意事項

### 8.1 絕對不要做的事

- 將 API Key 寫死在程式碼中
- 將 `.env` 檔案提交到 Git
- 在前端 JavaScript 中暴露 Secret Key
- 使用無限制的 API Key

### 8.2 應該做的事

- 使用環境變數管理所有金鑰
- 將 `.env*` 加入 `.gitignore`
- 設定 API Key 使用限制（IP、Referrer、API 範圍）
- 定期輪換 Key
- 監控 API 使用量

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
