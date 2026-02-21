# WeGo - 旅遊規劃協作平台

專為好友、小團體設計的「重協作」旅遊規劃平台，解決行程排版混亂、交通時間難估算、憑證分散以及分帳麻煩等痛點。

---

## 功能特色

### 行程管理
- **拖拽排序** - 長按景點卡片即可拖動調整順序，支援跨日移動
- **交通預估** - 自動計算景點間距離與抵達時間，支援 7 種交通方式（步行、開車、大眾運輸、騎車、高鐵、飛機、手動輸入）
- **智慧排序** - 一鍵優化當日路線（Greedy Nearest Neighbor 演算法）
- **天氣預報** - 顯示景點未來天氣，支援 5 天預報

### 協作功能
- **多人共編** - 支援 Owner / Editor / Viewer 三種角色，細粒度權限控制
- **邀請連結** - 透過連結快速加入行程，可設定角色與有效期限
- **代辦事項** - 分配任務、指派成員、追蹤完成進度

### 分帳系統
- **多幣別支援** - 即時匯率轉換，支援 TWD、USD、JPY、EUR 等 8 種貨幣
- **多種分帳方式** - 均分（EQUAL）、百分比（PERCENTAGE）、自訂金額（CUSTOM）、按比例（SHARES）
- **債務簡化** - Greedy 配對演算法，自動計算最少轉帳次數
- **統計圖表** - 類別分析、趨勢圖、成員消費統計（Chart.js）
- **結算管理** - 逐筆或批次標記結清
- **個人記帳** - 個人支出追蹤（手動新增 + 自動同步分帳份額）、7 類別分布圓餅圖、每日花費長條圖、預算設定與三色進度條

### 檔案管理
- **憑證上傳** - 支援 PDF、JPG、PNG、HEIC，單檔上限 10MB
- **快速關聯** - 檔案可綁定至日期或景點
- **即時預覽** - 檔案預覽含安全沙箱（CSP 隔離）
- **儲存空間** - 每行程 100MB 額度，使用量即時顯示

### AI 旅遊助手
- **智慧問答** - 基於行程資料的 AI 旅遊建議（Gemini API）
- **即時推薦** - 餐廳、景點、交通、文化體驗的在地建議
- **安全防護** - Prompt injection 防護、Circuit breaker、Rate limiting

### 使用者體驗
- **深色模式** - 支援系統偏好設定與手動切換，FOUC 防護
- **響應式設計** - Mobile-first，適配 375px 至 1440px+
- **Glassmorphism 風格** - 毛玻璃卡片、底部導覽、Lottie 動畫
- **無障礙支援** - 觸控目標 44x44px、色彩對比 4.5:1、ARIA 標籤

---

## 技術架構

| 層級 | 技術 |
|------|------|
| 後端 | Spring Boot 3.x（Java 17+） |
| 前端 | Thymeleaf + Tailwind CSS + Chart.js |
| 資料庫 | Supabase（PostgreSQL 15+） |
| 檔案儲存 | Supabase Storage |
| 部署 | Railway |
| 外部 API | Google Maps、OpenWeatherMap、ExchangeRate-API、Gemini API |
| 認證 | Google OAuth 2.0 + Spring Security |
| 快取 | Caffeine（統計、匯率、權限、Signed URL） |
| 限流 | Bucket4j（IP 層）+ 應用層限流 |

### 專案規模

| 指標 | 數量 |
|------|------|
| REST API 端點 | 64 個 |
| Web 頁面端點 | 40 個 |
| Service 類別 | 22 個（含 ViewHelper） |
| Entity / Enum | 11 / 6 個 |
| Repository | 11 個 |
| HTML 模板 | 34 個 |
| JS 模組 | 9 個 |
| 單元測試 | 1138 個（86 個測試檔案） |
| E2E 測試 | 12 個 Playwright spec |

---

## 快速開始

### 前置需求

- Java 17+
- Maven 3.8+
- Node.js 18+（E2E 測試用）

## 專案結構

```
wego/
├── src/main/java/com/wego/
│   ├── config/          # 設定（Security, Cache, OAuth, Rate Limit, HttpClient）
│   ├── constant/        # 常數（ExpenseCategories, TripConstants）
│   ├── controller/      # Web Controllers + API Controllers
│   │   └── api/         # REST API 端點（64 個）
│   ├── service/         # 業務邏輯（22 個 Service，含 ViewHelper）
│   ├── domain/          # 領域邏輯（DebtSimplifier, RouteOptimizer, PermissionChecker, ExpenseAggregator）
│   ├── repository/      # 資料存取（11 個 Repository）
│   ├── entity/          # JPA Entities（11 個 Entity + 6 個 Enum）
│   ├── dto/             # Request / Response DTOs
│   └── external/        # 外部 API 整合（5 組 Interface + Impl + Mock）
├── src/main/resources/
│   ├── templates/       # Thymeleaf 模板（34 個）
│   └── static/js/       # JS 模組（9 個）
├── src/test/            # 單元測試（1138 tests, 86 files）
├── e2e/                 # E2E 測試（Playwright, 12 specs）
└── docs/                # 專案文件
```

---

## 開發進度

| Phase | 狀態 | 功能 |
|-------|:----:|------|
| Phase 1 | 完成 | OAuth 登入、行程 CRUD、景點管理、交通預估、邀請連結、基本分帳 |
| Phase 2 | 完成 | 權限模型、代辦事項、智慧排序、天氣預報、檔案管理 |
| Phase 3 | 完成 | 多幣別匯率、統計圖表、債務簡化、Caffeine 快取 |
| Phase 4 | 完成 | 安全強化、深色模式、E2E 測試、無障礙支援、效能優化 |
| Phase 5 | 完成 | 個人記帳（AUTO+MANUAL 合併、預算追蹤）、7 個費用類別、AI 聊天整合 |

---

## 文件索引

### 核心文件

| 文件 | 說明 |
|------|------|
| [CLAUDE.md](CLAUDE.md) | AI 開發指南與 Thymeleaf SpEL 規範 |
| [docs/software-design-document.md](docs/software-design-document.md) | 軟體設計文件（架構、ADR、Entity 設計） |
| [docs/requirements.md](docs/requirements.md) | 需求規格書 |
| [docs/CONTRIB.md](docs/CONTRIB.md) | 貢獻指南與開發流程 |

### 開發指南

| 文件 | 說明 |
|------|------|
| [docs/testing-guide.md](docs/testing-guide.md) | 測試指南（TDD + 測試案例 + API 端點參考） |
| [docs/setup-guide.md](docs/setup-guide.md) | 環境設定與 API Key 設定指南 |
| [docs/ui-design-guide.md](docs/ui-design-guide.md) | UI 設計指南 |
| [docs/ai-coding-guidelines.md](docs/ai-coding-guidelines.md) | AI 輔助開發規範 |

### 運維與品質

| 文件 | 說明 |
|------|------|
| [docs/RUNBOOK.md](docs/RUNBOOK.md) | 運維手冊 |
| [docs/quality-review-report.md](docs/quality-review-report.md) | 品質審查報告（綜合評分 8.6/10） |

---

## 授權

MIT License
