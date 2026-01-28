# WeGo - AI 開發指南

## 專案概述

WeGo 是一個專為好友、小團體設計的「重協作」旅遊規劃平台，解決行程排版混亂、交通時間難估算、憑證分散以及分帳麻煩等痛點。

### 目標用戶
- 3-10 人的朋友出遊團
- 家庭旅遊
- 公司小型團建

---

## 技術棧

| 層級 | 技術 |
|------|------|
| 後端 | Spring Boot 3.x (Java 17+) |
| 前端 | Thymeleaf + Tailwind CSS |
| 動畫 | Lottie-web |
| 資料庫 | Supabase (PostgreSQL 15+) |
| 部署 | Railway |
| 地圖 | Google Maps API |
| 天氣 | OpenWeatherMap API |
| 匯率 | ExchangeRate-API |

---

## 專案結構

```
wego/
├── src/
│   ├── main/
│   │   ├── java/com/wego/
│   │   │   ├── WegoApplication.java    # Spring Boot 入口
│   │   │   ├── config/                 # 設定 (Security, OAuth, Web)
│   │   │   ├── controller/
│   │   │   │   ├── web/                # 頁面控制器
│   │   │   │   └── api/                # REST API 控制器
│   │   │   ├── service/                # 業務邏輯
│   │   │   ├── repository/             # 資料存取層
│   │   │   ├── entity/                 # JPA 實體
│   │   │   ├── dto/                    # 資料傳輸物件
│   │   │   ├── domain/                 # 領域邏輯 (演算法)
│   │   │   ├── exception/              # 例外處理
│   │   │   ├── security/               # OAuth2 相關
│   │   │   └── util/                   # 工具類別
│   │   └── resources/
│   │       ├── templates/              # Thymeleaf 模板
│   │       ├── static/                 # 靜態資源
│   │       └── application.yml
│   └── test/
├── docs/                               # 專案文件
│   ├── requirements.md                 # 需求規格書 (PRD)
│   ├── software-design-document.md     # 軟體設計文件 (SDD)
│   ├── test-cases.md                   # 測試案例規格書
│   ├── ui-design-guide.md              # UI 設計指南
│   ├── tdd-guide.md                    # TDD 測試開發指南
│   ├── ai-coding-guidelines.md         # AI 輔助開發規範
│   ├── api-keys-setup.md               # API Keys 設定指南
│   └── plan.md                         # 開發計劃書
├── pom.xml
└── README.md
```

---

## 開發指令

```bash
# 啟動開發伺服器
./mvnw spring-boot:run

# 執行單元測試
./mvnw test

# 執行整合測試
./mvnw verify

# 建置 JAR
./mvnw clean package -DskipTests

# 產生測試覆蓋率報告
./mvnw jacoco:report
# 報告位置: target/site/jacoco/index.html
```

---

## 核心實體

```
User ─┬─< TripMember >─── Trip
      │                    │
      │                    ├──< Activity >── Place
      │                    │
      │                    ├──< Expense >──< ExpenseSplit
      │                    │
      │                    ├──< Document
      │                    │
      └────────────────────┴──< Todo
```

### 主要實體說明

| 實體 | 說明 |
|------|------|
| `User` | 使用者 (OAuth 登入) |
| `Trip` | 行程 |
| `TripMember` | 行程成員與角色 (OWNER/EDITOR/VIEWER) |
| `Activity` | 行程中的景點/活動 |
| `Place` | 地理位置資訊 |
| `Expense` | 支出記錄 |
| `ExpenseSplit` | 分帳明細 |
| `Document` | 上傳的憑證檔案 |
| `Todo` | 代辦事項 |
| `InviteLink` | 邀請連結 |

---

## 權限模型

| 功能 | Owner | Editor | Viewer |
|------|:-----:|:------:|:------:|
| 檢視行程 | ✅ | ✅ | ✅ |
| 編輯景點 | ✅ | ✅ | ❌ |
| 新增支出 | ✅ | ✅ | ❌ |
| 移除成員 | ✅ | ❌ | ❌ |
| 刪除行程 | ✅ | ❌ | ❌ |

---

## 編碼規範

### 分層架構原則

| 層級 | 職責 | 允許依賴 |
|------|------|----------|
| Controller | HTTP 處理 | Service, DTO |
| Service | 業務協調 | Domain, Repository, External |
| Domain | 核心邏輯 | Entity, Value Object |
| Repository | 資料存取 | Entity |

### 方法契約註解 (必須)

每個 public 方法須包含：
- **Precondition**: 呼叫前必須滿足的條件
- **Postcondition**: 呼叫後保證的結果
- **Calls / Called by**: 依賴關係

```java
/**
 * 建立新行程並設定建立者為 Owner。
 *
 * @contract
 *   - pre: user != null, request.endDate >= request.startDate
 *   - post: Trip 已持久化, TripMember 已建立 (role=OWNER)
 *   - calls: TripRepository#save, TripMemberRepository#save
 *   - calledBy: TripApiController#createTrip
 */
public TripResponse createTrip(CreateTripRequest request, User user) {
    // ...
}
```

### 命名規範

- **Entity**: 單數名詞 (`User`, `Trip`, `Activity`)
- **Repository**: `{Entity}Repository`
- **Service**: `{Domain}Service`
- **Controller**: `{Domain}Controller` (Web) / `{Domain}ApiController` (REST)
- **DTO**: `{Action}{Entity}Request` / `{Entity}Response`

### 禁止事項

- ❌ 不要在程式碼中硬編碼 API Key 或密碼
- ❌ 不要使用 `System.out.println`，使用 Logger
- ❌ 不要忽略例外，必須適當處理或向上拋出
- ❌ 不要在 Service 層直接存取 HttpServletRequest

---

## 測試規範

### 測試覆蓋率目標

| 層級 | 目標 |
|------|------|
| Service | ≥ 80% |
| Domain | ≥ 90% |
| Controller | ≥ 70% |
| **整體** | **≥ 80%** |

### TDD 工作流程

```
1. RED    → 撰寫失敗的測試
2. GREEN  → 撰寫最小程式碼使測試通過
3. REFACTOR → 重構，保持測試通過
```

### 測試命名規範

```java
// 格式: methodName_scenario_expectedBehavior
@Test
void createTrip_withValidInput_shouldReturnCreatedTrip() { }

@Test
void createTrip_withNullTitle_shouldThrowValidationException() { }
```

### 測試案例對照表

測試案例定義於 `docs/test-cases.md`，編號規則：

| 前綴 | 模組 |
|------|------|
| U- | User (使用者) |
| T- | Trip (行程) |
| A- | Activity (景點) |
| E- | Expense (分帳) |
| D- | DebtSimplifier (債務簡化) |
| F- | File/Document (檔案) |
| TD- | Todo (代辦) |
| P- | Permission (權限) |

---

## API 設計規範

### 統一回應格式

```json
// 成功
{
    "success": true,
    "data": { ... },
    "message": null,
    "errorCode": null,
    "timestamp": "2024-01-15T10:30:00"
}

// 錯誤
{
    "success": false,
    "data": null,
    "message": "行程不存在",
    "errorCode": "TRIP_NOT_FOUND",
    "timestamp": "2024-01-15T10:30:00"
}
```

### 常用錯誤碼

| 錯誤碼 | HTTP Status | 說明 |
|--------|-------------|------|
| `VALIDATION_ERROR` | 400 | 請求參數驗證失敗 |
| `AUTH_REQUIRED` | 401 | 需要登入 |
| `ACCESS_DENIED` | 403 | 無權限存取 |
| `TRIP_NOT_FOUND` | 404 | 行程不存在 |
| `EXTERNAL_API_ERROR` | 502 | 外部 API 呼叫失敗 |

---

## Git 工作流程

### 分支策略

```
main (production)
  └── develop
        ├── feature/P1-U-001-user-entity
        ├── feature/P1-T-005-trip-service
        └── bugfix/xxx
```

### Commit 訊息格式

```
<type>: <description>

[optional body]
```

**Types**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

### PR 審查清單

- [ ] 測試通過
- [ ] 覆蓋率 ≥ 80%
- [ ] 契約註解完整
- [ ] 無安全漏洞
- [ ] 無 console.log

---

## 開發階段

| Phase | 名稱 | 核心功能 |
|-------|------|----------|
| Phase 1 | MVP | 登入、行程、景點、基礎分帳、檔案上傳 |
| Phase 2 | 協作強化 | 權限模型、代辦事項、智慧排序、天氣 |
| Phase 3 | 分帳進階 | 多幣別、自訂分帳、統計圖表 |
| Phase 4 | 體驗優化 | PWA、深色模式、動畫 |

詳細開發計劃請參考 `docs/plan.md`。

---

## 環境變數

開發時需設定以下環境變數（參考 `.env.example`）：

```bash
# 必須
DATABASE_URL=postgresql://...
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_SERVICE_KEY=eyJ...
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxx

# 可選（沒有則使用 Mock）
GOOGLE_MAPS_API_KEY=
OPENWEATHERMAP_API_KEY=
EXCHANGERATE_API_KEY=
```

---

## 相關文件

| 文件 | 說明 |
|------|------|
| [README.md](./README.md) | 專案概述 |
| [docs/requirements.md](./docs/requirements.md) | 完整需求規格書 (PRD) |
| [docs/software-design-document.md](./docs/software-design-document.md) | 軟體設計文件 (SDD) |
| [docs/test-cases.md](./docs/test-cases.md) | 測試案例規格書 |
| [docs/ui-design-guide.md](./docs/ui-design-guide.md) | UI 設計指南 |
| [docs/tdd-guide.md](./docs/tdd-guide.md) | TDD 測試開發指南 |
| [docs/ai-coding-guidelines.md](./docs/ai-coding-guidelines.md) | AI 輔助開發規範 |
| [docs/api-keys-setup.md](./docs/api-keys-setup.md) | API Keys 設定指南 |
| [docs/plan.md](./docs/plan.md) | 開發計劃書 |

---

## AI 開發注意事項

### 修改程式碼前必須

1. 閱讀目標方法的契約註解
2. 確認 `@calledBy` 列出的呼叫方
3. 確認 `@calls` 列出的依賴方法
4. 確認測試案例涵蓋修改範圍

### 修改程式碼後必須

1. 更新方法的契約註解
2. 執行相關單元測試
3. 確認未破壞既有測試
4. 覆蓋率維持 ≥ 80%

### 核心演算法

| 演算法 | 位置 | 用途 |
|--------|------|------|
| DebtSimplifier | `domain/settlement/` | 債務簡化（貪婪配對） |
| RouteOptimizer | `domain/route/` | 路線優化（最近鄰居） |

修改演算法前務必閱讀 `docs/software-design-document.md` 中的 ADR 記錄。

---

## AI Agent 與 Skill 使用規範

### 自動選擇原則

根據任務類型自動選擇適當的 Agent 或 Skill：

| 任務類型 | 優先使用 | 說明 |
|----------|----------|------|
| **UI/UX 設計** | `ui-ux-pro-max` skill | 設計系統、色彩、字體、元件 |
| **程式碼審查** | `code-reviewer` agent | 品質、安全、最佳實踐 |
| **安全性審查** | `security-reviewer` agent | OWASP、輸入驗證、認證 |
| **測試開發** | `tdd-guide` agent | TDD 流程、測試案例 |
| **建置錯誤** | `build-error-resolver` agent | 修復編譯錯誤 |
| **複雜功能規劃** | `planner` agent | 實作計畫、風險評估 |
| **架構設計** | `architect` agent | 系統設計、技術決策 |
| **E2E 測試** | `e2e-runner` agent | Playwright 端對端測試 |
| **文件更新** | `doc-updater` agent | 更新文件和 CODEMAPS |
| **程式碼清理** | `refactor-cleaner` agent | 移除死碼、重構 |
| **資料庫審查** | `database-reviewer` agent | SQL、Schema、效能 |

### UI/UX 任務識別關鍵字

當任務包含以下關鍵字時，**優先使用 `ui-ux-pro-max` skill**：

- 設計、design、UI、UX、介面
- 色彩、顏色、color、palette
- 字體、typography、font
- 元件、component、按鈕、表單
- 響應式、responsive、RWD
- 動畫、animation、transition
- 無障礙、accessibility、a11y
- 深色模式、dark mode
- 風格、style、glassmorphism、minimalism

### ui-ux-pro-max 使用方式

```bash
# 生成完整設計系統（必須先執行）
python3 .claude/skills/ui-ux-pro-max/scripts/search.py "<產品類型> <風格關鍵字>" --design-system -p "WeGo"

# 搜尋特定領域
python3 .claude/skills/ui-ux-pro-max/scripts/search.py "<關鍵字>" --domain <style|color|typography|ux|chart>

# 取得技術棧指南
python3 .claude/skills/ui-ux-pro-max/scripts/search.py "<關鍵字>" --stack html-tailwind
```

### Agent 使用時機

| 情境 | 立即使用的 Agent |
|------|------------------|
| 寫完程式碼後 | `code-reviewer` |
| 新功能開發前 | `planner` → `tdd-guide` |
| 建置失敗時 | `build-error-resolver` |
| 涉及安全性時 | `security-reviewer` |
| 架構決策時 | `architect` |
| 資料庫變更時 | `database-reviewer` |
