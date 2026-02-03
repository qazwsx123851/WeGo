# WeGo - 旅遊規劃協作平台

專為好友、小團體設計的「重協作」旅遊規劃平台，解決行程排版混亂、交通時間難估算、憑證分散以及分帳麻煩等痛點。

## 功能特色

### 行程管理
- **拖拽排序** - 長按景點卡片即可拖動調整順序
- **交通預估** - 自動計算景點間距離與抵達時間（支援步行/開車/大眾運輸/騎車/高鐵/飛機）
- **智慧排序** - 一鍵優化當日路線
- **天氣預報** - 顯示景點未來天氣

### 協作功能
- **多人共編** - 支援 Owner/Editor/Viewer 三種角色
- **邀請連結** - 透過連結快速加入行程
- **代辦事項** - 分配任務、追蹤進度

### 分帳系統
- **多幣別支援** - 即時匯率轉換（TWD/USD/JPY/EUR 等 8 種貨幣）
- **多種分帳方式** - 均分、百分比、自訂金額
- **債務簡化** - 自動計算最少轉帳次數
- **統計圖表** - 類別分析、趨勢圖、成員統計

### 檔案管理
- **憑證上傳** - 支援 PDF、JPG、PNG、HEIC
- **快速關聯** - 檔案可綁定至日期或景點

## 技術架構

| 層級 | 技術 |
|------|------|
| 後端 | Spring Boot 3.2 (Java 17) |
| 前端 | Thymeleaf + Tailwind CSS |
| 資料庫 | Supabase (PostgreSQL) |
| 部署 | Railway |
| 外部 API | Google Maps、OpenWeatherMap、ExchangeRate-API |

## 快速開始

### 前置需求

- Java 17+
- Maven 3.8+

### 環境變數

複製 `.env.example` 為 `.env` 並填入：

```bash
# 必須
DATABASE_URL=jdbc:postgresql://...
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_SERVICE_KEY=eyJ...  # 必須是 service_role key
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxx

# 可選（有 Mock 替代）
GOOGLE_MAPS_API_KEY=
OPENWEATHERMAP_API_KEY=
EXCHANGERATE_API_KEY=
```

### 執行

```bash
# 開發
./mvnw spring-boot:run

# 測試
./mvnw test

# 建置
./mvnw clean package -DskipTests
```

應用程式將在 `http://localhost:8080` 啟動。

## 專案結構

```
wego/
├── src/main/java/com/wego/
│   ├── config/          # 設定（Security, Cache, OAuth）
│   ├── controller/      # Web & API Controllers
│   ├── service/         # 業務邏輯
│   ├── domain/          # 領域邏輯（演算法）
│   ├── repository/      # 資料存取
│   ├── entity/          # JPA Entities
│   └── dto/             # DTOs
├── src/main/resources/
│   ├── templates/       # Thymeleaf 模板
│   └── static/          # CSS, JS
└── src/test/            # 測試（786 tests）
```

## 開發進度

| Phase | 狀態 | 功能 |
|-------|:----:|------|
| Phase 1 | ✅ | OAuth 登入、行程 CRUD、景點管理、交通預估、邀請連結、基本分帳 |
| Phase 2 | ✅ | 權限模型、代辦事項、智慧排序、天氣預報 |
| Phase 3 | ✅ | 多幣別匯率、統計圖表、債務簡化 |
| Phase 4 | 🚧 | PWA 離線、深色模式 |

## 文件

| 文件 | 說明 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | AI 開發指南 |
| [docs/requirements.md](docs/requirements.md) | 需求規格書 |
| [docs/software-design-document.md](docs/software-design-document.md) | 軟體設計文件 |
| [docs/bug.md](docs/bug.md) | Bug 追蹤與安全審查 |

## 授權

MIT License
