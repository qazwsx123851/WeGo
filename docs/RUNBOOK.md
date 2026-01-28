# WeGo 維運手冊 (Runbook)

> 最後更新: 2026-01-28 | 部署平台: Railway

## 部署架構

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   GitHub        │────▶│    Railway      │────▶│   Supabase      │
│   (main branch) │     │   (Java 17)     │     │   (PostgreSQL)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 部署流程

### 自動部署 (推薦)

推送到 `main` 分支會自動觸發 Railway 部署：

```bash
git push origin main
```

GitHub Actions 工作流程：
1. Checkout code
2. Setup JDK 17 + Node.js 20
3. Install frontend dependencies
4. Build Tailwind CSS
5. Build JAR (`./mvnw clean package -DskipTests`)
6. Deploy to Railway

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

| 變數 | 說明 |
|------|------|
| `DATABASE_URL` | Supabase PostgreSQL 連線 URL |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | Supabase 資料庫密碼 |
| `SUPABASE_URL` | `https://xxx.supabase.co` |
| `SUPABASE_SERVICE_KEY` | Supabase Service Key |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Secret |
| `PORT` | `8080` (Railway 預設) |

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

## 緊急聯絡

| 角色 | 聯絡方式 |
|------|----------|
| 維運 | (待設定) |
| 資料庫 | Supabase Support |
| 部署 | Railway Support |

---

## 相關文件

| 文件 | 說明 |
|------|------|
| [CONTRIB.md](./CONTRIB.md) | 開發貢獻指南 |
| [api-keys-setup.md](./api-keys-setup.md) | API Keys 設定指南 |
| [software-design-document.md](./software-design-document.md) | 軟體設計文件 |
