# WeGo - 旅遊規劃協作平台

專為好友、小團體設計的「重協作」旅遊規劃平台，解決行程排版混亂、交通時間難估算、憑證分散以及分帳麻煩等痛點。

## 功能特色

### 行程編輯器
- **拖拽排序** - 長按景點卡片即可拖動調整順序
- **交通預估** - 自動計算景點間距離與抵達時間（支援開車/步行/大眾運輸）
- **智慧排序** - 一鍵優化當日路線，找出最順路的排序
- **天氣預報** - 整合氣象 API，顯示景點 5 天內天氣

### 協作功能
- **多人共編** - 透過邀請連結加入，支援 Owner/Editor/Viewer 三種角色
- **代辦事項** - 分配任務給成員，追蹤完成狀態
- **分帳系統** - 多幣別支援、自動換算、債務簡化計算

### 檔案管理
- **憑證上傳** - 支援 PDF、JPG、PNG、HEIC 格式
- **快速關聯** - 檔案可綁定至特定日期或景點
- **離線預覽** - Service Worker 快取機制

## 技術架構

| 層級 | 技術 |
|------|------|
| 後端 | Spring Boot 3.x (Java 17+) |
| 前端 | Thymeleaf + Tailwind CSS |
| 動畫 | Lottie-web |
| 資料庫 | Supabase (PostgreSQL) |
| 部署 | Railway |
| 地圖 | Google Maps API |
| 天氣 | OpenWeatherMap API |
| 匯率 | ExchangeRate-API |

## 快速開始

### 前置需求

- Java 17+
- Maven 3.8+
- Node.js 18+ (用於 Tailwind CSS 建置)

### 環境變數設定

建立 `.env` 檔案或設定以下環境變數：

```bash
# Database
DATABASE_URL=postgresql://[user]:[password]@[host]:[port]/[database]

# OAuth - Google
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# External APIs
GOOGLE_MAPS_API_KEY=your_google_maps_api_key
OPENWEATHERMAP_API_KEY=your_openweathermap_api_key
EXCHANGERATE_API_KEY=your_exchangerate_api_key

# Supabase Storage
SUPABASE_URL=your_supabase_url
SUPABASE_SERVICE_KEY=your_supabase_service_key
```

### 本地開發

```bash
# 複製專案
git clone https://github.com/your-username/wego.git
cd wego

# 安裝依賴並啟動
./mvnw spring-boot:run
```

應用程式將在 `http://localhost:8080` 啟動。

### 建置與部署

```bash
# 建置 JAR
./mvnw clean package -DskipTests

# 執行
java -jar target/wego-*.jar
```

## 專案結構

```
wego/
├── src/
│   ├── main/
│   │   ├── java/com/wego/
│   │   │   ├── config/        # 設定檔（Security, OAuth, etc.）
│   │   │   ├── controller/    # Web & API Controllers
│   │   │   ├── service/       # 業務邏輯
│   │   │   ├── repository/    # 資料存取層
│   │   │   ├── entity/        # JPA Entities
│   │   │   └── dto/           # Data Transfer Objects
│   │   └── resources/
│   │       ├── templates/     # Thymeleaf 模板
│   │       ├── static/        # 靜態資源（CSS, JS, Images）
│   │       └── application.yml
│   └── test/
├── docs/
│   └── requirements.md        # 完整需求規格書
├── pom.xml
└── README.md
```

## API 文件

### 驗證

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/oauth2/authorization/google` | Google OAuth 登入 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/me` | 取得當前用戶資訊 |

### 行程

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips` | 取得用戶的行程列表 |
| POST | `/api/trips` | 建立新行程 |
| GET | `/api/trips/{tripId}` | 取得行程詳情 |
| PUT | `/api/trips/{tripId}` | 更新行程 |
| DELETE | `/api/trips/{tripId}` | 刪除行程 |

### 景點

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips/{tripId}/activities` | 取得所有景點 |
| POST | `/api/trips/{tripId}/activities` | 新增景點 |
| PUT | `/api/trips/{tripId}/activities/reorder` | 批次更新排序 |
| GET | `/api/trips/{tripId}/activities/optimize` | 取得路線優化建議 |

### 分帳

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips/{tripId}/expenses` | 取得所有支出 |
| POST | `/api/trips/{tripId}/expenses` | 新增支出 |
| GET | `/api/trips/{tripId}/settlements` | 取得結算結果 |

> 完整 API 文件請參閱 [requirements.md](docs/requirements.md)

## 資料模型

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

### 主要實體

| 實體 | 說明 |
|------|------|
| User | 使用者（OAuth 登入） |
| Trip | 行程 |
| TripMember | 行程成員與角色 |
| Activity | 行程中的景點/活動 |
| Place | 地理位置資訊 |
| Expense | 支出記錄 |
| ExpenseSplit | 分帳明細 |
| Document | 上傳的憑證檔案 |
| Todo | 代辦事項 |

## 權限模型

| 功能 | Owner | Editor | Viewer |
|------|:-----:|:------:|:------:|
| 檢視行程 | ✅ | ✅ | ✅ |
| 編輯景點 | ✅ | ✅ | ❌ |
| 新增支出 | ✅ | ✅ | ❌ |
| 移除成員 | ✅ | ❌ | ❌ |
| 刪除行程 | ✅ | ❌ | ❌ |

## 開發路線圖

### Phase 1: MVP
- [x] OAuth 登入（Google）
- [ ] 建立/編輯行程
- [ ] 新增/編輯景點
- [ ] 拖拽排序
- [ ] 交通時間預估
- [ ] 邀請連結
- [ ] 基本分帳

### Phase 2: 協作強化
- [ ] 完整權限模型
- [ ] 代辦事項
- [ ] 智慧排序建議
- [ ] 天氣預報整合

### Phase 3: 分帳進階
- [ ] 多幣別支援
- [ ] 自訂分帳比例
- [ ] 債務簡化演算法

### Phase 4: 體驗優化
- [ ] 離線快取（PWA）
- [ ] 深色模式

## 文件索引

| 文件 | 說明 |
|------|------|
| [requirements.md](docs/requirements.md) | 完整需求規格書 (PRD) |
| [software-design-document.md](docs/software-design-document.md) | 軟體設計文件 (SDD)，包含 SOLID 原則、系統架構、模組設計 |
| [tdd-guide.md](docs/tdd-guide.md) | TDD 測試開發指南，JUnit 5 + Mockito 實作範例 |
| [test-cases.md](docs/test-cases.md) | 測試案例規格書，所有模組的測試案例清單 |
| [ui-design-guide.md](docs/ui-design-guide.md) | UI 設計指南，色彩、元件、頁面設計 |
| [ai-coding-guidelines.md](docs/ai-coding-guidelines.md) | AI 輔助開發規範，防止上下文遺失的契約註解 |

## 貢獻指南

1. Fork 此專案
2. 建立功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交變更 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 開啟 Pull Request

## 授權

此專案採用 MIT 授權 - 詳見 [LICENSE](LICENSE) 檔案

## 聯絡方式

如有任何問題或建議，歡迎開啟 Issue 討論。
