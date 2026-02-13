# WeGo 旅遊規劃協作 Web App - 需求規格書 (PRD)

> 最後更新: 2026-02-12

## 1. 產品定義

* **產品名稱**：WeGo
* **產品定位**：專為好友、小團體設計的「重協作」旅遊規劃平台。
* **核心目標**：解決行程排版混亂、交通時間難估算、憑證分散以及分帳麻煩等痛點。
* **目標用戶**：3-10 人的朋友出遊團、家庭旅遊、公司小型團建。

---

## 2. 技術架構 (Technical Stack)

| 層級 | 技術選型 | 說明 |
|------|----------|------|
| 後端框架 | Spring Boot 3.x (Java 17+) | RESTful API + Server-Side Rendering |
| 前端引擎 | Thymeleaf | HTML Template Engine |
| 樣式框架 | Tailwind CSS | Mobile-First RWD |
| 互動動畫 | Lottie-web | JSON-based animations |
| 資料庫 | Supabase (PostgreSQL 15+) | 含 Supabase Storage 檔案儲存 |
| 部署平台 | Railway | CI/CD from GitHub |
| 地圖服務 | Google Maps API | Routes API, Maps JavaScript, Places |
| 天氣服務 | OpenWeatherMap API | 5-day forecast |
| 匯率服務 | ExchangeRate-API | 每日更新匯率 |

### 專案統計

| 項目 | 數量 |
|------|------|
| 單元測試 | ~1011 個測試方法，74 個測試檔案 |
| E2E 測試 | ~118 個測試案例，10 個 spec 檔案 |
| REST API 端點 | 55 個 |
| Web 端點 | 37 個 |
| Service 類別 | 17 個 |
| Entity 類別 | 10 個 |
| Enum 類別 | 6 個 |
| Repository | 10 個 |
| HTML 模板 | 27 個 |
| JS 模組 | 6 個 |
| Domain 元件 | 4 個（DebtSimplifier, RouteOptimizer, PermissionChecker, ExpenseAggregator） |
| 外部服務整合 | 4 個（Google Maps, OpenWeatherMap, ExchangeRate-API, Supabase Storage） |

---

## 3. 核心功能需求

### A. 行程編輯器 (Itinerary Editor)

#### A-1. 拖拽排序
* **觸發方式**：長按景點卡片 300ms 後進入拖拽模式
* **視覺反饋**：拖拽中卡片放大 1.05 倍並顯示陰影
* **儲存機制**：放開後立即更新 `sort_order`，寫入資料庫
* **限制**：僅能在同一天內拖拽，跨日需使用「移動到其他日期」功能

#### A-2. 交通預估
* **支援交通方式**：開車 (driving)、步行 (walking)、大眾運輸 (transit)、騎車 (bicycling)、飛機 (flight)、高鐵 (high_speed_rail)
* **資料來源**：Google Maps Routes API（已從 Distance Matrix API 遷移）
* **快取策略**：相同起訖點的查詢結果快取 24 小時，減少 API 呼叫
* **顯示內容**：預估時間 + 距離 + 計算來源標記（Google API / Haversine / 手動）
* **預設交通方式**：用戶可在行程設定中選擇預設，單一景點可覆寫

#### A-3. 智慧排序建議
* **演算法**：貪婪演算法 (Greedy Nearest Neighbor)，以最短總距離為目標
* **觸發方式**：用戶點擊「優化路線」按鈕
* **結果呈現**：顯示優化前後的總距離/時間比較，用戶確認後套用
* **限制**：單日景點數超過 15 個時提示「景點過多，建議分日」

#### A-4. 天氣預報
* **資料來源**：OpenWeatherMap 5-day / 3-hour Forecast API
* **顯示範圍**：行程日期起 5 天內的天氣
* **顯示內容**：天氣圖示 + 最高/最低溫 + 降雨機率
* **更新頻率**：每 6 小時更新一次
* **超出範圍**：超過 5 天顯示「天氣預報尚未開放」

### B. 協作與管理

#### B-1. 好友共同編輯
* **邀請方式**：產生專屬邀請連結，可設定為 Editor 或 Viewer 角色
* **連結有效期**：24 小時 / 7 天 / 永久（可選）
* **同步機制**：基本同步模式，用戶刷新頁面即可看到他人變更
* **成員上限**：單一行程最多 10 位成員
* **衝突處理**：採用 Last-Write-Wins，後儲存者覆蓋先前內容

#### B-2. 代辦事項 (Todo List)
* **欄位**：標題、負責人、截止日期、狀態（待辦/進行中/完成）
* **指派**：可指派給任一行程成員
* **通知**：被指派者下次進入行程時顯示提醒（非即時推播）
* **排序**：依截止日期排序，已完成項目置底

#### B-3. 分帳系統
* **幣別支援**：多幣別（8 種貨幣），每筆支出可選擇幣別
* **基準幣別**：每個行程設定一個基準幣別（如 TWD），結算時統一換算
* **匯率來源**：ExchangeRate-API，每日自動更新
* **分帳規則**：
  - 均分（預設）
  - 自訂金額
  - 排除特定成員
* **結算顯示**：計算最少交易次數的債務簡化結果（如：A 付給 B $500）
* **結算方式**：僅顯示金額，用戶自行透過其他管道轉帳
* **標記結清**：可標記單筆債務為「已結清」

### C. 憑證與檔案

#### C-1. 多格式上傳
* **支援格式**：PDF、JPG、PNG、HEIC（自動轉換為 JPG）
* **單檔限制**：10 MB
* **行程總量**：100 MB / 每行程
* **儲存位置**：Supabase Storage

#### C-2. 快速關聯
* **關聯對象**：可綁定至特定日期或特定景點
* **預覽方式**：點擊即開啟預覽（圖片直接顯示、PDF 使用瀏覽器內建檢視器）

---

## 4. 畫面規劃與 UI 邏輯

### 4-1. 行程總覽 (Dashboard)
* **佈局**：大卡片縱向列表，顯示旅程名稱、日期、目的地縮圖與參與者頭像
* **排序**：進行中行程優先，其次依出發日期排序
* **空狀態**：顯示 Lottie 動畫（跳動的行李箱）+ 「建立第一個行程」按鈕

### 4-2. 行程主頁 (Main Editor)
* **手機版佈局**：
  - **頂部**：行程名稱 + 日期切換 Tabs + [清單/地圖] 切換鈕
  - **中間**：垂直景點卡片清單，卡片間顯示交通時間
  - **底部**：固定式功能導覽列（首頁/景點/分帳/檔案/設定）
* **平板/桌機版**：
  - 左側（60%）：景點清單
  - 右側（40%）：Google Map 即時同步顯示景點標記與路線

### 4-3. 互動抽屜 (Bottom Drawer)
* **觸發**：點擊景點卡片
* **內容**：詳細資訊、筆記編輯區、關聯憑證列表、刪除/移動按鈕
* **關閉**：下滑或點擊背景遮罩

---

## 5. 互動設計 (Lottie & Tailwind)

| 觸發場景 | Lottie 動畫效果 | Tailwind 動畫類別 |
|----------|-----------------|-------------------|
| 頁面載入 | 飛機或走路的小人 | `animate-pulse` (載入佔位符) |
| 任務完成 | 綠色勾勾彈出 | `line-through` (文字劃掉) |
| 分帳結清 | 撒紙花 (Celebration) | `scale-110` (按鈕反饋) |
| 路徑優化中 | 旋轉齒輪 | `animate-spin` |
| 空狀態 | 跳動的行李箱 | - |
| 上傳成功 | 綠色打勾 | `opacity-0 → opacity-100` |

---

## 6. 資料庫實體設計 (Entity Design)

### 實體關係圖 (ER Diagram)

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

### 實體定義（10 個 Entity）

#### User（使用者）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| email | VARCHAR(255) | 電子郵件（唯一） |
| nickname | VARCHAR(50) | 顯示名稱 |
| avatar_url | VARCHAR(500) | 頭像網址 |
| provider | VARCHAR(20) | OAuth 提供者（google） |
| provider_id | VARCHAR(255) | OAuth 用戶 ID |
| created_at | TIMESTAMP | 建立時間 |

#### Trip（行程）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| title | VARCHAR(100) | 行程名稱 |
| description | TEXT | 行程描述 |
| start_date | DATE | 開始日期 |
| end_date | DATE | 結束日期 |
| cover_image_url | VARCHAR(500) | 封面圖片 |
| base_currency | CHAR(3) | 基準幣別（ISO 4217，如 TWD） |
| default_transport | VARCHAR(20) | 預設交通方式 |
| owner_id | UUID | 建立者（FK → User） |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 最後更新時間 |

#### TripMember（行程成員）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| trip_id | UUID | FK → Trip |
| user_id | UUID | FK → User |
| role | VARCHAR(10) | 角色（owner / editor / viewer） |
| joined_at | TIMESTAMP | 加入時間 |

#### Place（地點）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| name | VARCHAR(200) | 地點名稱 |
| address | VARCHAR(500) | 地址 |
| lat | DECIMAL(10,7) | 緯度 |
| lng | DECIMAL(10,7) | 經度 |
| google_place_id | VARCHAR(255) | Google Places ID |
| category | VARCHAR(50) | 類別（景點/餐廳/住宿/交通） |

#### Activity（活動/景點）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| trip_id | UUID | FK → Trip |
| place_id | UUID | FK → Place |
| day | INTEGER | 第幾天（1-based） |
| sort_order | INTEGER | 當日排序 |
| start_time | TIME | 預計開始時間（可為 null） |
| duration_minutes | INTEGER | 預計停留時間（分鐘） |
| note | TEXT | 備註 |
| transport_mode | VARCHAR(20) | 前往此景點的交通方式 |
| created_at | TIMESTAMP | 建立時間 |
| updated_at | TIMESTAMP | 最後更新時間 |

#### Expense（支出）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| trip_id | UUID | FK → Trip |
| title | VARCHAR(200) | 支出名稱 |
| amount | DECIMAL(12,2) | 金額 |
| currency | CHAR(3) | 幣別（ISO 4217） |
| exchange_rate | DECIMAL(10,6) | 當時匯率（相對於 Trip.base_currency） |
| payer_id | UUID | 付款者（FK → User） |
| category | VARCHAR(50) | 類別（餐飲/交通/住宿/門票/其他） |
| receipt_url | VARCHAR(500) | 收據圖片 |
| created_at | TIMESTAMP | 建立時間 |

#### ExpenseSplit（分帳明細）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| expense_id | UUID | FK → Expense |
| user_id | UUID | FK → User（應分攤者） |
| amount | DECIMAL(12,2) | 應分攤金額（以 Expense.currency 計） |
| is_settled | BOOLEAN | 是否已結清 |

#### Document（憑證/檔案）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| trip_id | UUID | FK → Trip |
| file_name | VARCHAR(255) | 檔案名稱 |
| file_url | VARCHAR(500) | Supabase Storage URL |
| file_size | INTEGER | 檔案大小（bytes） |
| mime_type | VARCHAR(50) | MIME 類型 |
| related_activity_id | UUID | 關聯景點（可為 null） |
| related_day | INTEGER | 關聯日期（可為 null） |
| uploaded_by | UUID | 上傳者（FK → User） |
| created_at | TIMESTAMP | 上傳時間 |

#### Todo（代辦事項）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| trip_id | UUID | FK → Trip |
| title | VARCHAR(200) | 事項名稱 |
| assignee_id | UUID | 負責人（FK → User，可為 null） |
| due_date | DATE | 截止日期（可為 null） |
| status | VARCHAR(20) | 狀態（pending / in_progress / completed） |
| created_by | UUID | 建立者（FK → User） |
| created_at | TIMESTAMP | 建立時間 |
| completed_at | TIMESTAMP | 完成時間 |

#### InviteLink（邀請連結）
| 欄位 | 型別 | 說明 |
|------|------|------|
| id | UUID | 主鍵 |
| trip_id | UUID | FK → Trip |
| token | VARCHAR(64) | 邀請碼（唯一） |
| role | VARCHAR(10) | 加入後角色（editor / viewer） |
| expires_at | TIMESTAMP | 過期時間（null 表示永久） |
| created_by | UUID | 建立者 |
| created_at | TIMESTAMP | 建立時間 |

---

## 7. 部署說明

### 環境變數

必要環境變數包括：`DATABASE_URL`（PostgreSQL 連線字串）、`GOOGLE_CLIENT_ID` 和 `GOOGLE_CLIENT_SECRET`（Google OAuth 憑證）、`SUPABASE_URL` 和 `SUPABASE_SERVICE_KEY`（Supabase 存取）。

可選環境變數：`GOOGLE_MAPS_API_KEY`、`OPENWEATHERMAP_API_KEY`、`EXCHANGERATE_API_KEY`（未設定時使用對應的 Mock 實作）。

### 部署流程
1. **資料庫**：於 Supabase 建立 Project，執行 Schema Migration
2. **代碼託管**：Push 專案至 GitHub
3. **上架**：在 Railway 連結 GitHub Repository，設定上述環境變數
4. **Domain**：設定自訂域名（選用）

---

## 8. 使用者角色與權限

### 角色定義

| 角色 | 說明 |
|------|------|
| **Owner** | 行程建立者，擁有完整權限 |
| **Editor** | 可編輯行程內容的成員 |
| **Viewer** | 僅能檢視，不可修改 |

### 權限矩陣

| 功能 | Owner | Editor | Viewer |
|------|:-----:|:------:|:------:|
| 檢視行程內容 | ✅ | ✅ | ✅ |
| 新增/編輯/刪除景點 | ✅ | ✅ | ❌ |
| 拖拽排序景點 | ✅ | ✅ | ❌ |
| 新增/編輯支出 | ✅ | ✅ | ❌ |
| 刪除他人建立的支出 | ✅ | ❌ | ❌ |
| 上傳/刪除檔案 | ✅ | ✅ | ❌ |
| 新增/編輯代辦事項 | ✅ | ✅ | ❌ |
| 產生邀請連結 | ✅ | ✅ | ❌ |
| 移除成員 | ✅ | ❌ | ❌ |
| 變更成員角色 | ✅ | ❌ | ❌ |
| 編輯行程基本資訊 | ✅ | ❌ | ❌ |
| 刪除行程 | ✅ | ❌ | ❌ |
| 轉移 Owner 權限 | ✅ | ❌ | ❌ |

### 邀請連結規則
* Owner 和 Editor 可產生邀請連結
* 邀請連結可指定加入者角色為 Editor 或 Viewer（不可指定為 Owner）
* 連結有效期選項：24 小時 / 7 天 / 永久
* 單一行程成員上限：10 人

---

## 9. 驗證機制

### OAuth 登入流程

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  User   │────>│  WeGo   │────>│ Google  │────>│  WeGo   │
│         │     │ Frontend│     │  OAuth  │     │ Backend │
└─────────┘     └─────────┘     └─────────┘     └─────────┘
     │               │               │               │
     │ 1. Click      │               │               │
     │ "Login with   │               │               │
     │  Google"      │               │               │
     │──────────────>│               │               │
     │               │ 2. Redirect   │               │
     │               │──────────────>│               │
     │               │               │ 3. User       │
     │               │               │ Authorizes    │
     │               │               │───┐           │
     │               │               │<──┘           │
     │               │ 4. Callback   │               │
     │               │ with code     │               │
     │               │<──────────────│               │
     │               │               │ 5. Exchange   │
     │               │               │ code for token│
     │               │               │──────────────>│
     │               │               │               │ 6. Fetch
     │               │               │               │ user info
     │               │               │               │───┐
     │               │               │               │<──┘
     │               │ 7. Set session│               │
     │               │ cookie        │               │
     │<──────────────│<──────────────────────────────│
```

### 支援的 OAuth Provider

| Provider | Client ID 來源 | Scope |
|----------|----------------|-------|
| Google | Google Cloud Console | email, profile |

### Session 管理
* **方式**：Cookie-based Session（HttpOnly, Secure, SameSite=Lax）
* **有效期**：7 天，每次請求自動延長
* **登出**：清除 Session Cookie

### 首次登入處理
1. 檢查 `provider` + `provider_id` 是否已存在
2. 若存在：直接登入
3. 若不存在：自動建立帳號，使用 OAuth 回傳的 email 與 nickname

---

## 10. 非功能性需求 (NFR)

### 效能目標

| 指標 | 目標值 |
|------|--------|
| 首頁載入時間（LCP） | < 2.5 秒 |
| API 回應時間（P95） | < 500 ms |
| Time to Interactive（TTI） | < 3.5 秒 |
| 同時在線人數 | 支援 20 人 |

### 可用性
* **SLA 目標**：99.5%（月度）
* **計畫性維護**：提前 24 小時公告
* **資料備份**：Supabase 自動每日備份，保留 7 天

### 安全性

| 項目 | 措施 |
|------|------|
| 傳輸加密 | 強制 HTTPS（TLS 1.2+） |
| XSS 防護 | Thymeleaf 自動 HTML Escape |
| CSRF 防護 | Spring Security CSRF Token |
| SQL Injection | 使用 JPA Parameterized Query |
| 敏感資料 | 環境變數儲存，不進版控 |
| 檔案上傳 | 驗證 MIME Type，限制檔案大小 |
| Rate Limiting | Bucket4j + Caffeine cache |

### 瀏覽器支援
* Chrome 90+
* Safari 14+
* Firefox 90+
* Edge 90+
* iOS Safari 14+
* Android Chrome 90+

---

## 11. 使用者故事 (User Stories)

### 行程管理

| ID | 角色 | 故事 | 驗收條件 |
|----|------|------|----------|
| US-01 | 用戶 | 我想要使用 Google 帳號快速登入，以便不需要記住額外密碼 | OAuth 登入成功後自動建立/登入帳號 |
| US-02 | 用戶 | 我想要建立新行程，以便開始規劃旅遊 | 可輸入名稱、日期，建立後導向行程主頁 |
| US-03 | Owner | 我想要邀請朋友加入行程，以便一起規劃 | 可產生連結、設定角色與有效期 |
| US-04 | 成員 | 我想要透過邀請連結加入行程，以便參與規劃 | 點擊連結後登入即自動加入 |

### 景點編輯

| ID | 角色 | 故事 | 驗收條件 |
|----|------|------|----------|
| US-05 | Editor | 我想要新增景點到行程中，以便安排要去的地方 | 可搜尋地點或輸入地址新增 |
| US-06 | Editor | 我想要拖拽調整景點順序，以便優化行程動線 | 長按拖拽後順序即時更新 |
| US-07 | Editor | 我想要看到景點間的交通時間，以便掌握行程節奏 | 顯示預估時間與距離 |
| US-08 | Editor | 我想要一鍵優化路線，以便找到最順的排序 | 顯示優化建議，確認後套用 |

### 分帳

| ID | 角色 | 故事 | 驗收條件 |
|----|------|------|----------|
| US-09 | Editor | 我想要記錄旅途中的支出，以便事後結算 | 可輸入金額、幣別、付款者、分攤者 |
| US-10 | 成員 | 我想要查看誰欠誰多少錢，以便結清款項 | 顯示簡化後的債務關係 |
| US-11 | 成員 | 我想要標記某筆帳已結清，以便追蹤結算進度 | 可勾選「已結清」 |

### 檔案管理

| ID | 角色 | 故事 | 驗收條件 |
|----|------|------|----------|
| US-12 | Editor | 我想要上傳機票/訂房確認信，以便集中管理憑證 | 支援 PDF、JPG、PNG 上傳 |
| US-13 | 成員 | 我想要快速找到特定景點的相關憑證 | 檔案可關聯至景點，一鍵開啟 |

---

## 12. API 端點規劃

### 驗證相關

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/oauth2/authorization/google` | Google OAuth 登入 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/me` | 取得當前用戶資訊 |

### 行程管理

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips` | 取得用戶的行程列表 |
| POST | `/api/trips` | 建立新行程 |
| GET | `/api/trips/{tripId}` | 取得行程詳情 |
| PUT | `/api/trips/{tripId}` | 更新行程基本資訊 |
| DELETE | `/api/trips/{tripId}` | 刪除行程 |
| GET | `/api/trips/{tripId}/members` | 取得行程成員列表 |
| DELETE | `/api/trips/{tripId}/members/{userId}` | 移除成員 |
| PUT | `/api/trips/{tripId}/members/{userId}/role` | 變更成員角色 |

### 邀請連結

| Method | Endpoint | 說明 |
|--------|----------|------|
| POST | `/api/trips/{tripId}/invites` | 產生邀請連結 |
| GET | `/api/invites/{token}` | 取得邀請資訊 |
| POST | `/api/invites/{token}/accept` | 接受邀請加入行程 |

### 景點/活動

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips/{tripId}/activities` | 取得行程所有活動 |
| POST | `/api/trips/{tripId}/activities` | 新增活動 |
| PUT | `/api/trips/{tripId}/activities/{activityId}` | 更新活動 |
| DELETE | `/api/trips/{tripId}/activities/{activityId}` | 刪除活動 |
| PUT | `/api/trips/{tripId}/activities/reorder` | 批次更新排序 |
| GET | `/api/trips/{tripId}/activities/optimize` | 取得路線優化建議 |

### 分帳

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips/{tripId}/expenses` | 取得行程所有支出 |
| POST | `/api/trips/{tripId}/expenses` | 新增支出 |
| PUT | `/api/trips/{tripId}/expenses/{expenseId}` | 更新支出 |
| DELETE | `/api/trips/{tripId}/expenses/{expenseId}` | 刪除支出 |
| GET | `/api/trips/{tripId}/settlements` | 取得結算結果 |
| PUT | `/api/trips/{tripId}/settlements/{settlementId}` | 標記結清 |

### 檔案

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips/{tripId}/documents` | 取得行程所有檔案 |
| POST | `/api/trips/{tripId}/documents` | 上傳檔案 |
| DELETE | `/api/trips/{tripId}/documents/{documentId}` | 刪除檔案 |

### 代辦事項

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/trips/{tripId}/todos` | 取得行程所有待辦 |
| POST | `/api/trips/{tripId}/todos` | 新增待辦 |
| PUT | `/api/trips/{tripId}/todos/{todoId}` | 更新待辦 |
| DELETE | `/api/trips/{tripId}/todos/{todoId}` | 刪除待辦 |

### 外部服務

| Method | Endpoint | 說明 |
|--------|----------|------|
| GET | `/api/places/search` | 搜尋地點（Google Places） |
| GET | `/api/weather` | 取得天氣預報 |
| GET | `/api/directions` | 取得交通路線 |
| GET | `/api/exchange-rates` | 取得匯率 |

---

## 13. 驗收標準 (Acceptance Criteria)

### AC-01: OAuth 登入

**首次使用 Google 登入**：用戶尚未註冊，點擊「以 Google 登入」並在 Google 授權頁面同意授權後，系統自動建立帳號，用戶被導向 Dashboard 頁面並顯示歡迎訊息。

**已註冊用戶登入**：用戶已使用 Google 註冊過，點擊「以 Google 登入」後直接被導向 Dashboard 頁面。

### AC-02: 拖拽排序

**成功拖拽排序**：用戶為行程的 Editor，當日有 3 個以上景點時，長按景點卡片超過 300ms 進入拖拽模式（放大 + 陰影），拖拽到新位置並放開後，景點順序立即更新且交通時間重新計算。

**Viewer 無法拖拽**：用戶為行程的 Viewer，長按景點卡片時不會進入拖拽模式。

### AC-03: 分帳結算

**計算均分帳務**：行程有 3 位成員 A、B、C，A 付了 $300 由三人均分，B 付了 $600 由三人均分。查看結算頁面時顯示簡化後的債務：A 付給 B $100、C 付給 B $200。

---

## 14. MVP 範圍與里程碑

### Phase 1: MVP（核心功能）✅ 完成
**目標**：可用的基本版本，驗證產品概念

| 功能 | 優先級 |
|------|--------|
| OAuth 登入（Google） | P0 |
| 建立/編輯行程 | P0 |
| 新增/編輯景點 | P0 |
| 拖拽排序 | P0 |
| 交通時間預估 | P0 |
| 邀請連結（基本權限） | P0 |
| 基本分帳（單幣別、均分） | P1 |
| 檔案上傳 | P1 |

### Phase 2: 協作強化 ✅ 完成
**目標**：提升多人協作體驗

| 功能 | 優先級 |
|------|--------|
| 完整權限模型 | P1 |
| 代辦事項 | P1 |
| 智慧排序建議 | P2 |
| 天氣預報整合 | P2 |

### Phase 3: 分帳進階 ✅ 完成
**目標**：完善分帳功能

| 功能 | 優先級 |
|------|--------|
| 多幣別支援 | P1 |
| 自訂分帳比例 | P2 |
| 債務簡化演算法 | P2 |
| 支出分類統計 | P3 |

### Phase 4: 體驗優化 ✅ 完成
**目標**：提升使用體驗

| 功能 | 優先級 |
|------|--------|
| 安全強化 | P0 |
| E2E 測試 | P1 |
| Lottie 動畫完善 | P3 |
| 深色模式 | P3 |

---

## 15. 風險與假設

### 技術風險

| 風險 | 影響 | 緩解措施 |
|------|------|----------|
| Google Maps API 費用超支 | 成本增加 | 實作快取機制、監控用量、設定預算上限 |
| Supabase 免費額度不足 | 需升級付費方案 | 監控用量、優化查詢、壓縮檔案 |
| OAuth Provider 政策變更 | 登入功能受影響 | 支援多個 Provider、保留傳統登入備案 |

### 產品風險

| 風險 | 影響 | 緩解措施 |
|------|------|----------|
| 用戶不習慣協作規劃 | 留存率低 | 強化 onboarding、提供範本行程 |
| 分帳計算爭議 | 用戶體驗差 | 清楚顯示計算邏輯、提供明細匯出 |

### 假設條件

| 假設 | 若不成立的影響 |
|------|----------------|
| 用戶有穩定網路連線 | 需提前實作離線功能 |
| 用戶有 Google 帳號 | 需支援傳統註冊 |
| 目標市場為台灣 | 需調整幣別預設、語系 |
| 單一行程成員不超過 10 人 | 需優化協作機制與 API 效能 |

---

## 附錄

### A. 術語表

| 術語 | 定義 |
|------|------|
| Trip | 一次完整的旅行行程 |
| Activity | 行程中的單一活動或景點 |
| Place | 實際地理位置，可被多個 Activity 參照 |
| Expense | 一筆支出記錄 |
| Settlement | 結算後的債務關係 |

### B. 參考資源

* [Google Maps Platform](https://developers.google.com/maps)
* [OpenWeatherMap API](https://openweathermap.org/api)
* [ExchangeRate-API](https://www.exchangerate-api.com/)
* [Supabase Documentation](https://supabase.com/docs)
* [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
