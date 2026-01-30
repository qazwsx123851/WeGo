# WeGo 開發貢獻指南

> 最後更新: 2026-01-28 | 自動生成自 pom.xml 和 .env.example
>
> **變更日誌**: 新增 spring-dotenv 自動載入環境變數

## 技術棧

| 層級 | 技術 | 版本 |
|------|------|------|
| 後端 | Spring Boot | 3.2.2 |
| Java | OpenJDK | 17 |
| 前端模板 | Thymeleaf | (Spring Boot managed) |
| CSS 框架 | Tailwind CSS | 3.x |
| 資料庫 | PostgreSQL (Supabase) | 15+ |
| 建置工具 | Maven | 3.9.x |
| Node.js | (Frontend build) | 20.11.0 |
| 環境變數 | spring-dotenv | 4.0.0 |

---

## 開發環境設定

### 前置需求

- JDK 17+
- Maven 3.9+ (或使用專案內的 `./mvnw`)
- Node.js 20+ (Maven 會自動下載)
- PostgreSQL 資料庫 (建議使用 Supabase)

### 環境變數設定

複製 `.env.example` 為 `.env` 並設定：

```bash
cp .env.example .env
```

| 變數 | 必須 | 說明 | 格式範例 |
|------|:----:|------|----------|
| `DATABASE_URL` | ✅ | PostgreSQL 連線 URL | `jdbc:postgresql://host:5432/db` |
| `DATABASE_USERNAME` | ✅ | 資料庫使用者 | `postgres` |
| `DATABASE_PASSWORD` | ✅ | 資料庫密碼 | |
| `SUPABASE_URL` | ✅ | Supabase 專案 URL | `https://xxx.supabase.co` |
| `SUPABASE_SERVICE_KEY` | ✅ | Supabase **Service Role** Key (⚠️ 不是 anon/publishable key) | `eyJhbGciOiJIUzI1NiIs...` |
| `GOOGLE_CLIENT_ID` | ✅ | Google OAuth Client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | ✅ | Google OAuth Secret | `GOCSPX-xxx` |
| `GOOGLE_MAPS_API_KEY` | ❌ | Google Maps API (可選) | |
| `GOOGLE_MAPS_ENABLED` | ❌ | 啟用 Google Maps | `true` / `false` |
| `OPENWEATHERMAP_API_KEY` | ❌ | 天氣 API (可選) | |
| `OPENWEATHERMAP_ENABLED` | ❌ | 啟用天氣 API | `true` / `false` |
| `EXCHANGERATE_API_KEY` | ❌ | 匯率 API (可選) | |
| `EXCHANGERATE_ENABLED` | ❌ | 啟用匯率 API | `true` / `false` |

### 載入環境變數

專案使用 `spring-dotenv` 自動載入 `.env` 檔案，無需手動 export：

```bash
# 只需複製 .env.example 並填入實際值
cp .env.example .env

# 直接啟動，.env 會自動載入
./mvnw spring-boot:run
```

> **注意**: 單元測試使用 H2 記憶體資料庫，不需要 .env 檔案。

---

## Maven 指令參考

| 指令 | 說明 |
|------|------|
| `./mvnw spring-boot:run` | 啟動開發伺服器 (port 8080) |
| `./mvnw test` | 執行單元測試 |
| `./mvnw verify` | 執行測試 + 整合測試 |
| `./mvnw clean package -DskipTests` | 建置 JAR (略過測試) |
| `./mvnw jacoco:report` | 產生測試覆蓋率報告 |
| `./mvnw compile -Dskip.frontend=true` | 編譯 (略過前端建置) |
| `./mvnw checkstyle:check` | 程式碼風格檢查 |

### 覆蓋率報告位置

```
target/site/jacoco/index.html
```

### 前端建置 (Tailwind CSS)

前端建置整合在 Maven 生命週期中，會自動執行：

```bash
# 手動執行 (在 src/main/frontend 目錄)
npm install
npm run build  # 輸出到 dist/styles.css
```

---

## 開發工作流程

### 1. 建立分支

```bash
git checkout -b feature/P1-T-001-trip-entity
```

分支命名規則: `feature/P{phase}-{module}-{id}-{description}`

### 2. TDD 開發

```
1. RED    → 撰寫失敗的測試
2. GREEN  → 撰寫最小程式碼使測試通過
3. REFACTOR → 重構，保持測試通過
```

### 3. 執行測試

```bash
./mvnw test
```

### 4. 確認覆蓋率 (≥ 80%)

```bash
./mvnw jacoco:report
open target/site/jacoco/index.html
```

### 5. 提交變更

```bash
git add .
git commit -m "feat: add Trip entity with validation"
```

Commit 類型: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

---

## 測試規範

### 測試覆蓋率目標

| 層級 | 目標 |
|------|------|
| Service | ≥ 80% |
| Domain | ≥ 90% |
| Controller | ≥ 70% |
| **整體** | **≥ 80%** |

### 測試命名規範

```java
// 格式: methodName_scenario_expectedBehavior
@Test
void createTrip_withValidInput_shouldReturnCreatedTrip() { }

@Test
void createTrip_withNullTitle_shouldThrowValidationException() { }
```

### 測試設定檔

測試使用 H2 記憶體資料庫，設定在 `src/test/resources/application-test.yml`

---

## 專案結構

```
src/
├── main/
│   ├── java/com/wego/
│   │   ├── WegoApplication.java    # Spring Boot 入口
│   │   ├── config/                 # 設定類別
│   │   ├── controller/
│   │   │   ├── web/                # 頁面控制器
│   │   │   └── api/                # REST API
│   │   ├── service/                # 業務邏輯
│   │   ├── repository/             # 資料存取
│   │   ├── entity/                 # JPA 實體
│   │   ├── dto/                    # 資料傳輸物件
│   │   ├── domain/                 # 領域邏輯
│   │   ├── exception/              # 例外處理
│   │   └── security/               # OAuth2 相關
│   ├── resources/
│   │   ├── templates/              # Thymeleaf 模板
│   │   ├── static/                 # 靜態資源
│   │   └── application.yml
│   └── frontend/                   # Tailwind CSS 原始碼
└── test/
    └── java/com/wego/             # 測試類別
```

---

## 認證系統

### Google OAuth 登入流程

```
使用者 ──▶ /login ──▶ Google OAuth ──▶ /login/oauth2/code/google
                                              │
                                              ▼
                                    CustomOAuth2UserService
                                              │
                              ┌───────────────┼───────────────┐
                              ▼               ▼               ▼
                        新使用者          舊使用者         錯誤
                              │               │               │
                              ▼               ▼               ▼
                         建立 User      更新 User      登入失敗
                              │               │
                              └───────┬───────┘
                                      ▼
                              UserPrincipal 放入 SecurityContext
                                      │
                                      ▼
                              重導向至 /dashboard
```

### API 端點

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/auth/me` | GET | 取得目前使用者資訊 |
| `/api/auth/logout` | POST | 登出 (API 版本) |
| `/logout` | GET | 登出 (Web 版本) |

### 使用 @CurrentUser 註解

```java
@GetMapping("/profile")
public String profile(@CurrentUser UserPrincipal principal) {
    UUID userId = principal.getId();
    String email = principal.getEmail();
    // ...
}
```

---

## 相關文件

| 文件 | 說明 |
|------|------|
| [RUNBOOK.md](./RUNBOOK.md) | 部署與維運手冊 |
| [requirements.md](./requirements.md) | 需求規格書 (PRD) |
| [software-design-document.md](./software-design-document.md) | 軟體設計文件 |
| [test-cases.md](./test-cases.md) | 測試案例規格書 |
| [tdd-guide.md](./tdd-guide.md) | TDD 開發指南 |
| [api-keys-setup.md](./api-keys-setup.md) | API Keys 設定指南 |
