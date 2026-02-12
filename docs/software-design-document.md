# WeGo 軟體設計文件 (Software Design Document)

## 文件資訊

| 項目 | 內容 |
|------|------|
| 專案名稱 | WeGo - 旅遊規劃協作平台 |
| 版本 | 1.0.0 |
| 最後更新 | 2026-02-12 |
| 狀態 | Draft |

---

## 1. 系統概述

### 1.1 目的
本文件描述 WeGo 系統的軟體架構設計，作為開發團隊實作的依據，確保系統具備可維護性、可測試性與可擴展性。

### 1.2 範圍
涵蓋系統架構、模組設計、資料流、介面規格與設計決策。

### 1.3 設計原則

本專案遵循以下設計原則：

#### SOLID 原則
| 原則 | 說明 | 在 WeGo 的應用 |
|------|------|----------------|
| **S**ingle Responsibility | 單一職責 | 每個 Service 只負責一個領域邏輯 |
| **O**pen/Closed | 開放封閉 | 使用介面擴展功能，不修改既有程式碼 |
| **L**iskov Substitution | 里氏替換 | 子類別可完全替換父類別 |
| **I**nterface Segregation | 介面隔離 | 細粒度介面，避免胖介面 |
| **D**ependency Inversion | 依賴反轉 | 高層模組不依賴低層，皆依賴抽象 |

#### 其他原則
- **DRY** (Don't Repeat Yourself) - 避免重複程式碼
- **KISS** (Keep It Simple, Stupid) - 保持簡單
- **YAGNI** (You Aren't Gonna Need It) - 不實作目前不需要的功能

---

## 2. 系統架構

### 2.1 高層架構圖

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Thymeleaf  │  │  REST API   │  │  Static Resources       │  │
│  │  Templates  │  │  Controllers│  │  (CSS, JS, Images)      │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Application Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Services   │  │    DTOs     │  │  Mappers                │  │
│  │             │  │             │  │  (Entity ↔ DTO)         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Domain Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Entities   │  │  Value      │  │  Domain Services        │  │
│  │             │  │  Objects    │  │  (Business Logic)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Infrastructure Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ Repositories│  │  External   │  │  Security               │  │
│  │ (JPA)       │  │  API Clients│  │  (OAuth, Session)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        External Systems                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────┐ │
│  │  Supabase   │  │Google Maps  │  │OpenWeather  │  │Exchange│ │
│  │  PostgreSQL │  │    API      │  │    API      │  │Rate API│ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 分層職責

| 層級 | 職責 | 主要元件 |
|------|------|----------|
| Presentation | 處理 HTTP 請求/回應、頁面渲染 | Controllers, Templates, DTOs |
| Application | 協調業務流程、轉換資料 | Services, Mappers |
| Domain | 核心業務邏輯、領域規則 | Entities, Value Objects, Domain Services |
| Infrastructure | 技術實作、外部整合 | Repositories, API Clients, Security |

### 2.3 套件結構

```
com.wego
├── WegoApplication.java              # Spring Boot 入口
│
├── config/                           # 設定類別
│   ├── SecurityConfig.java           # Spring Security 設定
│   ├── WebConfig.java                # Web MVC 設定
│   ├── CacheConfig.java              # 快取設定
│   └── SupabaseProperties.java       # Supabase 設定
│
├── controller/                       # 控制器層
│   ├── web/                          # 頁面控制器 (37 個 Web 端點)
│   │   ├── BaseWebController.java    # Web Controller 基礎類別
│   │   ├── HomeController.java
│   │   ├── TripController.java
│   │   ├── AuthController.java
│   │   ├── GlobalExpenseController.java
│   │   ├── GlobalDocumentController.java
│   │   ├── ProfileController.java
│   │   ├── DocumentWebController.java # 文件列表與上傳
│   │   ├── MemberWebController.java   # 成員管理頁面
│   │   ├── ExpenseWebController.java
│   │   ├── TodoWebController.java
│   │   ├── SettlementWebController.java
│   │   └── InviteController.java
│   └── api/                          # REST API 控制器 (55 個 REST 端點)
│       ├── TripApiController.java
│       ├── ActivityApiController.java
│       ├── ExpenseApiController.java
│       ├── DocumentApiController.java
│       ├── TodoApiController.java
│       ├── ExchangeRateApiController.java
│       ├── PlaceApiController.java
│       ├── DirectionApiController.java
│       ├── WeatherApiController.java
│       ├── StatisticsApiController.java
│       ├── HealthController.java
│       └── AuthApiController.java
│
├── service/                          # 服務層 (20 個 Service)
│   ├── UserService.java
│   ├── TripService.java
│   ├── InviteLinkService.java
│   ├── ActivityService.java
│   ├── ExpenseService.java
│   ├── SettlementService.java
│   ├── DocumentService.java
│   ├── TodoService.java
│   ├── TransportCalculationService.java
│   ├── GlobalExpenseService.java
│   ├── GlobalDocumentService.java
│   ├── StatisticsService.java
│   ├── ExchangeRateService.java
│   ├── PlaceService.java
│   ├── ActivityViewHelper.java       # Activity 顯示邏輯（從 Controller 提取）
│   ├── ExpenseViewHelper.java        # Expense 顯示邏輯（從 Controller 提取）
│   └── external/                     # 外部服務整合 (4 個，各有 Mock 實作)
│       ├── GoogleMapsService.java
│       ├── WeatherService.java
│       ├── ExchangeRateClient.java
│       └── StorageClient.java
│
├── repository/                       # 資料存取層 (10 個 Repository)
│   ├── UserRepository.java
│   ├── TripRepository.java
│   ├── TripMemberRepository.java
│   ├── ActivityRepository.java
│   ├── PlaceRepository.java
│   ├── ExpenseRepository.java
│   ├── ExpenseSplitRepository.java
│   ├── DocumentRepository.java
│   ├── TodoRepository.java
│   └── InviteLinkRepository.java
│
├── entity/                           # JPA 實體 (10 個 Entity)
│   ├── User.java
│   ├── Trip.java
│   ├── TripMember.java
│   ├── Activity.java
│   ├── Place.java
│   ├── Expense.java
│   ├── ExpenseSplit.java
│   ├── Document.java
│   ├── Todo.java
│   └── InviteLink.java
│
├── dto/                              # 資料傳輸物件
│   ├── request/                      # 請求 DTO
│   ├── response/                     # 回應 DTO
│   └── mapper/                       # 映射器
│
├── domain/                           # 領域邏輯 (5 個 Domain 元件)
│   ├── TripConstants.java            # 共用常數 (MAX_MEMBERS_PER_TRIP)
│   ├── settlement/
│   │   ├── DebtSimplifier.java       # 債務簡化演算法
│   │   └── Settlement.java           # 結算結果
│   ├── route/
│   │   └── RouteOptimizer.java       # 路線優化演算法
│   ├── permission/
│   │   ├── Permission.java
│   │   └── PermissionChecker.java    # 含請求級 Caffeine 快取
│   └── expense/
│       └── ExpenseAggregator.java    # 支出聚合
│
├── exception/                        # 例外處理
│   ├── GlobalExceptionHandler.java
│   ├── WebExceptionHandler.java
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   └── ForbiddenException.java
│
├── security/                         # 安全相關
│   ├── CustomOAuth2UserService.java
│   ├── UserPrincipal.java
│   └── CurrentUser.java              # 自訂註解
│
└── enum/                             # 列舉類別 (6 個 Enum)
    ├── Role.java
    ├── TransportMode.java
    ├── TransportSource.java
    ├── TransportWarning.java
    ├── ExpenseCategory.java
    └── TodoStatus.java
```

---

## 3. 模組設計

### 3.1 使用者模組 (User Module)

#### 類別圖
```
┌─────────────────────┐
│   <<interface>>     │
│   UserRepository    │
├─────────────────────┤
│ + findByEmail()     │
│ + findByProvider()  │
└─────────────────────┘
           △
           │
┌─────────────────────┐       ┌─────────────────────┐
│      User           │       │   UserService       │
│     <<entity>>      │◄──────│                     │
├─────────────────────┤       ├─────────────────────┤
│ - id: UUID          │       │ - userRepository    │
│ - email: String     │       ├─────────────────────┤
│ - nickname: String  │       │ + getCurrentUser()  │
│ - avatarUrl: String │       │ + updateProfile()   │
│ - provider: String  │       │ + findOrCreate()    │
│ - providerId: String│       └─────────────────────┘
│ - createdAt: Time   │
└─────────────────────┘
```

#### 職責
- 管理使用者資料
- 處理 OAuth 登入後的使用者建立/更新
- 提供當前登入使用者資訊

### 3.2 行程模組 (Trip Module)

#### 類別圖
```
┌─────────────────────┐
│   TripService       │
├─────────────────────┤
│ - tripRepository    │
│ - memberRepository  │
│ - permissionChecker │
├─────────────────────┤
│ + createTrip()      │
│ + getTrip()         │
│ + updateTrip()      │
│ + deleteTrip()      │
│ + addMember()       │
│ + removeMember()    │
│ + generateInvite()  │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐       ┌─────────────────────┐
│       Trip          │       │    TripMember       │
│     <<entity>>      │◄──────│     <<entity>>      │
├─────────────────────┤       ├─────────────────────┤
│ - id: UUID          │       │ - id: UUID          │
│ - title: String     │       │ - tripId: UUID      │
│ - startDate: Date   │       │ - userId: UUID      │
│ - endDate: Date     │       │ - role: Role        │
│ - baseCurrency: Str │       │ - joinedAt: Time    │
│ - ownerId: UUID     │       └─────────────────────┘
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│  PermissionChecker  │
├─────────────────────┤
│ + canEdit()         │
│ + canDelete()       │
│ + canInvite()       │
│ + canManageMembers()│
└─────────────────────┘
```

#### 權限檢查邏輯

PermissionChecker 透過查詢 TripMember 的角色來判斷權限。`canEdit` 方法允許 OWNER 和 EDITOR 角色編輯行程；`canDelete` 方法僅允許 OWNER 角色刪除行程。所有權限檢查皆先驗證使用者是否為行程成員，再根據角色判斷操作權限。

**請求級快取**：PermissionChecker 使用 Caffeine 快取（`permission-check`，5 秒 TTL，最大 500 筆）避免同一請求內多次查詢 DB。快取 key 格式為 `"tripId:userId"`。測試環境使用 `NoOpCacheManager` 繞過快取。

### 3.3 活動模組 (Activity Module)

#### 類別圖
```
┌─────────────────────┐
│  ActivityService    │
├─────────────────────┤
│ - activityRepo      │
│ - placeRepo         │
│ - googleMapsService │
│ - routeOptimizer    │
├─────────────────────┤
│ + createActivity()  │
│ + updateActivity()  │
│ + deleteActivity()  │
│ + reorderActivities()│
│ + getOptimizedRoute()│
│ + getTransitTime()  │
└─────────────────────┘
           │
           ├─────────────────────┐
           ▼                     ▼
┌─────────────────────┐  ┌─────────────────────┐
│     Activity        │  │   RouteOptimizer    │
│    <<entity>>       │  │  <<domain service>> │
├─────────────────────┤  ├─────────────────────┤
│ - id: UUID          │  │ + optimize()        │
│ - tripId: UUID      │  │ - nearestNeighbor() │
│ - placeId: UUID     │  │ - calculateDistance()│
│ - day: int          │  └─────────────────────┘
│ - sortOrder: int    │
│ - startTime: Time   │
│ - durationMinutes   │
│ - note: String      │
│ - transportMode     │
└─────────────────────┘
```

#### 路線優化演算法

RouteOptimizer 使用貪婪最近鄰居演算法（Greedy Nearest Neighbor）優化路線，時間複雜度為 O(n^2)。演算法流程如下：若景點數量小於等於 2 則直接返回原順序；否則從第一個景點出發，每次選擇距離最近的未拜訪景點作為下一個目標，重複此過程直到所有景點都被拜訪。距離計算使用景點的地理座標，透過 Haversine 公式或 Google Maps API 取得實際距離。

### 3.4 分帳模組 (Expense Module)

#### 類別圖
```
┌─────────────────────┐
│   ExpenseService    │
├─────────────────────┤
│ - expenseRepo       │
│ - splitRepo         │
│ - exchangeService   │
├─────────────────────┤
│ + createExpense()   │
│ + updateExpense()   │
│ + deleteExpense()   │
│ + splitEqually()    │
│ + splitCustom()     │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ SettlementService   │
├─────────────────────┤
│ - expenseRepo       │
│ - debtSimplifier    │
├─────────────────────┤
│ + calculateSettlement()│
│ + markAsSettled()   │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│   DebtSimplifier    │
│ <<domain service>>  │
├─────────────────────┤
│ + simplify()        │
│ - buildGraph()      │
│ - findOptimalFlow() │
└─────────────────────┘
```

#### 債務簡化演算法

DebtSimplifier 使用貪婪演算法簡化債務關係，以最小化交易次數為目標。演算法流程：

1. **計算淨額**：根據所有支出分攤記錄與實際付款金額，計算每位成員的淨額（正數表示應收款，負數表示應付款）。
2. **分離債權人與債務人**：使用優先佇列（PriorityQueue）分別存放債權人（按應收金額降序）和債務人（按應付金額降序）。
3. **貪婪配對**：每次從兩個佇列中各取出最大金額的債權人與債務人進行配對，取兩者金額中的較小值作為本次結算金額，並將剩餘金額放回對應的佇列。重複此過程直到所有債務結清。

此演算法可有效將 N 人之間的複雜債務關係簡化為最少 N-1 筆交易。

---

## 4. 介面設計

### 4.1 REST API 設計原則

- 使用 RESTful 風格命名
- 回傳統一的 Response 格式
- 使用適當的 HTTP 狀態碼
- 支援分頁與排序

#### API 端點統計

| 項目 | 數量 | 說明 |
|------|------|------|
| 後端 REST endpoint 總數 | 55 | 含 Web Controller + API Controller |
| Web 端點 | 37 | Thymeleaf 頁面控制器 |
| 前端實際使用的 endpoint | ~24 | JavaScript 模組 + Thymeleaf inline 呼叫 |

> 詳細前後端 API 對照表請參考 [docs/api-reference.md](./api-reference.md)

#### API 路徑命名模式

- 資源建立/列表：`/api/trips/{tripId}/[resource]` (帶 tripId)
- Activity/Expense 的更新/刪除：`/api/[resource]/{id}` (不帶 tripId)
- 此不一致為設計決策：update/delete 透過 resourceId 即可唯一識別，不需要 tripId

#### 雙介面並行架構

系統提供兩套平行介面：

| 介面 | Controller 類型 | 參數綁定 | 回傳格式 | 用途 |
|------|----------------|----------|----------|------|
| Web Form | `controller/web/*` | `@RequestParam` | HTML (Thymeleaf) | 目前前端使用 |
| REST API | `controller/api/*` | `@RequestBody` | JSON (ApiResponse) | 供未來 mobile/SPA 使用 |

#### 統一回應格式

ApiResponse 物件包含以下欄位：`success`（布林值）、`data`（泛型資料）、`message`（訊息字串）、`errorCode`（錯誤碼）、`timestamp`（時間戳記）。成功時 `success` 為 `true` 並攜帶 `data`；失敗時 `success` 為 `false` 並攜帶 `message` 與 `errorCode`。

### 4.2 錯誤碼定義

| 錯誤碼 | HTTP Status | 說明 |
|--------|-------------|------|
| `VALIDATION_ERROR` | 400 | 請求參數驗證失敗 |
| `INVALID_INVITE_LINK` | 400 | 邀請連結無效或已過期 |
| `MEMBER_LIMIT_EXCEEDED` | 400 | 成員數已達上限（10 人）|
| `DUPLICATE_MEMBER` | 400 | 已是行程成員 |
| `FILE_TOO_LARGE` | 400 | 檔案過大（上限 10MB）|
| `UNSUPPORTED_FILE_TYPE` | 400 | 不支援的檔案格式 |
| `STORAGE_LIMIT_EXCEEDED` | 400 | 行程儲存空間已滿（100MB）|
| `AUTH_REQUIRED` | 401 | 需要登入 |
| `ACCESS_DENIED` | 403 | 無權限存取 |
| `EXPENSE_NOT_OWNED` | 403 | 無法刪除他人的支出 |
| `TRIP_NOT_FOUND` | 404 | 行程不存在 |
| `ACTIVITY_NOT_FOUND` | 404 | 景點不存在 |
| `EXPENSE_NOT_FOUND` | 404 | 支出不存在 |
| `DOCUMENT_NOT_FOUND` | 404 | 檔案不存在 |
| `TODO_NOT_FOUND` | 404 | 代辦事項不存在 |
| `USER_NOT_FOUND` | 404 | 用戶不存在 |
| `RATE_LIMIT_EXCEEDED` | 429 | 請求過於頻繁 |
| `EXTERNAL_API_ERROR` | 502 | 外部 API 呼叫失敗 |
| `SERVICE_UNAVAILABLE` | 503 | 服務暫時無法使用 |

#### 驗證錯誤回應格式

驗證失敗時回傳 `VALIDATION_ERROR` 錯誤碼，並在 `errors` 陣列中列出每個欄位的驗證錯誤，包含 `field`（欄位名稱）、`message`（錯誤訊息）、`rejectedValue`（被拒絕的值）三個屬性。

### 4.3 分頁與查詢參數

#### 分頁回應格式

分頁回應使用 PagedResponse 物件，包含 `data`（資料列表）和 `pagination`（分頁資訊）。分頁資訊包括：`page`（當前頁碼，0-based）、`size`（每頁筆數）、`totalPages`（總頁數）、`totalElements`（總筆數）、`hasNext`（是否有下一頁）、`hasPrevious`（是否有上一頁）。

#### 查詢參數規範

| 端點 | 支援參數 |
|------|----------|
| `GET /api/trips` | `?page=0&size=10&sort=startDate,desc` |
| `GET /api/trips/{id}/activities` | `?day=1&sort=sortOrder,asc` |
| `GET /api/trips/{id}/expenses` | `?category=餐飲&payerId={uuid}&page=0&size=20&sort=createdAt,desc` |
| `GET /api/trips/{id}/documents` | `?relatedDay=1&relatedActivityId={uuid}` |
| `GET /api/trips/{id}/todos` | `?status=pending&assigneeId={uuid}&sort=dueDate,asc` |

### 4.4 Rate Limiting

> 針對小規模使用（≤10 人），採用簡化的限流策略。

| 端點類型 | 限制 | 說明 |
|----------|------|------|
| 認證端點 `/oauth2/**` | 10 次/分鐘/IP | 防止暴力嘗試 |
| 邀請連結 `/api/invites/**` | 20 次/分鐘/IP | 防止連結猜測 |
| 檔案上傳 `/api/*/documents` | 10 次/小時/用戶 | 防止儲存濫用 |
| 外部 API 代理 `/api/places/**` | 30 次/分鐘/用戶 | 控制 API 成本 |
| 一般 API | 100 次/分鐘/用戶 | 一般使用足夠 |

#### 回應標頭

Rate Limiting 回應包含三個標頭：`X-RateLimit-Limit`（限制次數）、`X-RateLimit-Remaining`（剩餘次數）、`X-RateLimit-Reset`（重置時間戳記）。

#### 超過限制回應

超過限制時回傳 HTTP 429，錯誤碼為 `RATE_LIMIT_EXCEEDED`，並包含 `retryAfter`（重試等待秒數）欄位。

### 4.5 Health Check 端點

| 端點 | 用途 | 認證 |
|------|------|------|
| `GET /actuator/health` | 基本健康檢查（for Load Balancer）| 無 |
| `GET /actuator/health/liveness` | Kubernetes 存活探針 | 無 |
| `GET /actuator/health/readiness` | Kubernetes 就緒探針 | 無 |

Health Check 回傳 JSON 格式，基本端點回傳 `status: "UP"`；readiness 端點額外包含 `components` 物件，顯示各依賴服務（如 db、diskSpace）的狀態。

### 4.6 DTO 設計

Request DTO 使用 Jakarta Validation 註解進行輸入驗證，包括 `@NotBlank`、`@Size`、`@NotNull`、`@FutureOrPresent`、`@Pattern` 等。Response DTO 僅包含前端所需的欄位，避免洩漏內部實作細節。

主要 DTO 設計規範：
- Request DTO 命名格式：`Create{Entity}Request`、`Update{Entity}Request`
- Response DTO 命名格式：`{Entity}Response`、`{Entity}DetailResponse`
- 使用 Builder 模式建構 Response DTO
- 巢狀物件使用內部靜態類別（如 `TripResponse.MemberSummary`）

---

## 5. 資料流設計

### 5.1 建立行程流程

```
┌────────┐    ┌────────────┐    ┌─────────────┐    ┌────────────┐
│ Client │───>│ Controller │───>│ TripService │───>│ Repository │
└────────┘    └────────────┘    └─────────────┘    └────────────┘
     │              │                  │                  │
     │  POST /api/  │                  │                  │
     │  trips       │                  │                  │
     │─────────────>│                  │                  │
     │              │  createTrip()    │                  │
     │              │─────────────────>│                  │
     │              │                  │  validate        │
     │              │                  │─────┐            │
     │              │                  │<────┘            │
     │              │                  │                  │
     │              │                  │  save(trip)      │
     │              │                  │─────────────────>│
     │              │                  │                  │
     │              │                  │  save(member)    │
     │              │                  │  [OWNER role]    │
     │              │                  │─────────────────>│
     │              │                  │                  │
     │              │  TripResponse    │                  │
     │              │<─────────────────│                  │
     │  201 Created │                  │                  │
     │<─────────────│                  │                  │
```

### 5.2 分帳結算流程

```
┌────────┐    ┌────────────┐    ┌─────────────────┐    ┌───────────────┐
│ Client │───>│ Controller │───>│SettlementService│───>│ DebtSimplifier│
└────────┘    └────────────┘    └─────────────────┘    └───────────────┘
     │              │                    │                      │
     │  GET /api/   │                    │                      │
     │  settlements │                    │                      │
     │─────────────>│                    │                      │
     │              │  calculate()       │                      │
     │              │───────────────────>│                      │
     │              │                    │  getExpenses()       │
     │              │                    │─────────┐            │
     │              │                    │<────────┘            │
     │              │                    │                      │
     │              │                    │  convertCurrency()   │
     │              │                    │─────────┐            │
     │              │                    │<────────┘            │
     │              │                    │                      │
     │              │                    │  simplify()          │
     │              │                    │─────────────────────>│
     │              │                    │                      │
     │              │                    │  List<Settlement>    │
     │              │                    │<─────────────────────│
     │              │  SettlementResponse│                      │
     │              │<───────────────────│                      │
     │  200 OK      │                    │                      │
     │<─────────────│                    │                      │
```

---

## 6. 外部系統整合

### 6.1 外部服務 API 端點

> 這些端點作為外部 API 的代理，提供統一的錯誤處理與快取機制。

| Method | Endpoint | 說明 | 外部服務 |
|--------|----------|------|----------|
| GET | `/api/places/search?query={q}&lat={lat}&lng={lng}` | 搜尋地點 | Google Places |
| GET | `/api/places/{placeId}` | 取得地點詳情 | Google Places |
| GET | `/api/directions?origin={}&dest={}&mode={}` | 取得交通路線 | Google Distance Matrix |
| GET | `/api/weather?lat={lat}&lng={lng}&date={date}` | 取得天氣預報 | OpenWeatherMap |
| GET | `/api/exchange-rates?from={}&to={}` | 取得匯率 | ExchangeRate-API |

### 6.2 外部服務整合架構

系統整合 4 個外部服務，每個服務皆透過介面抽象化，並提供 Mock 實作供開發與測試使用：

| 服務 | 介面 | 正式實作 | Mock 實作 | 用途 |
|------|------|----------|-----------|------|
| Google Maps | GoogleMapsClient | GoogleMapsClientImpl | MockGoogleMapsClient (Haversine) | 地點搜尋、交通路線計算 |
| OpenWeatherMap | WeatherClient | OpenWeatherMapClient | MockWeatherClient | 5 天天氣預報 |
| ExchangeRate-API | ExchangeRateClient | ExchangeRateApiClient | MockExchangeRateClient (固定匯率) | 匯率查詢與轉換 |
| Supabase Storage | StorageClient | SupabaseStorageClient | MockStorageClient | 檔案上傳/下載 |

Google Maps API 已從 Distance Matrix API 遷移至 Routes API（computeRouteMatrix），使用 Header `X-Goog-Api-Key` 進行認證，並支援 TRANSIT 至 DRIVING 的自動降級。

### 6.3 快取策略

| 資料類型 | 快取時間 | 最大數量 | 說明 |
|----------|----------|----------|------|
| 交通路線 | 10 分鐘 | 200 | CacheManager: directions |
| 天氣預報 | 6 小時 | 200 | CacheManager: weather |
| 匯率 (全部) | 1 小時 | 50 | CacheManager: exchange-rate-all |
| 匯率 (備援) | 24 小時 | 50 | CacheManager: exchange-rate-all-fallback |
| 地點搜尋 | 5 分鐘 | 500 | CacheManager: places |
| 統計資料 | 5 分鐘 | 100 | CacheManager: statistics |
| 權限檢查 | 5 秒 | 500 | CacheManager: permission-check |

### 6.4 資料庫索引設計

#### 核心索引

| 表格 | 索引名稱 | 欄位 | 類型 | 說明 |
|------|----------|------|------|------|
| `user` | `idx_user_provider` | (provider, provider_id) | UNIQUE | OAuth 登入查詢 |
| `user` | `idx_user_email` | (email) | UNIQUE | Email 查詢 |
| `trip` | `idx_trip_owner` | (owner_id) | INDEX | 查詢用戶建立的行程 |
| `trip_member` | `idx_trip_member_unique` | (trip_id, user_id) | UNIQUE | 確保成員唯一 |
| `trip_member` | `idx_trip_member_user` | (user_id) | INDEX | 查詢用戶加入的行程 |
| `activity` | `idx_activity_trip_day` | (trip_id, day, sort_order) | INDEX | 行程內景點排序查詢 |
| `expense` | `idx_expense_trip` | (trip_id, created_at DESC) | INDEX | 行程支出時間排序 |
| `expense_split` | `idx_split_expense` | (expense_id) | INDEX | 支出明細查詢 |
| `expense_split` | `idx_split_user` | (user_id, is_settled) | INDEX | 用戶待結清查詢 |
| `document` | `idx_document_trip` | (trip_id) | INDEX | 行程檔案查詢 |
| `document` | `idx_document_activity` | (related_activity_id) | INDEX | 景點關聯檔案 |
| `todo` | `idx_todo_trip_status` | (trip_id, status, due_date) | INDEX | 代辦清單排序（部分索引，僅索引未完成項目） |
| `invite_link` | `idx_invite_token` | (token) | UNIQUE | 邀請連結查詢 |
| `invite_link` | `idx_invite_expires` | (expires_at) | INDEX | 過期連結清理 |

#### 索引維護注意事項

- **定期分析**：每週執行 `ANALYZE` 更新統計資訊
- **索引膨脹**：監控索引大小，必要時 `REINDEX`
- **未使用索引**：透過 `pg_stat_user_indexes` 監控，移除未使用索引

---

## 7. 安全設計

### 7.1 認證流程

```
┌────────┐    ┌────────────┐    ┌─────────────┐    ┌────────────┐
│ Client │───>│   WeGo     │───>│   OAuth     │───>│   WeGo     │
│        │    │  Frontend  │    │  Provider   │    │  Backend   │
└────────┘    └────────────┘    └─────────────┘    └────────────┘
     │              │                  │                  │
     │  Click       │                  │                  │
     │  "Login"     │                  │                  │
     │─────────────>│                  │                  │
     │              │                  │                  │
     │              │  Redirect to     │                  │
     │              │  /oauth2/auth    │                  │
     │<─────────────│                  │                  │
     │              │                  │                  │
     │  Authorize   │                  │                  │
     │─────────────────────────────────>│                  │
     │              │                  │                  │
     │              │  Callback with   │                  │
     │              │  auth code       │                  │
     │<─────────────────────────────────│                  │
     │              │                  │                  │
     │              │                  │  Exchange code   │
     │              │                  │  for token       │
     │              │                  │<─────────────────│
     │              │                  │                  │
     │              │                  │  User info       │
     │              │                  │─────────────────>│
     │              │                  │                  │
     │              │  Set Session     │                  │
     │              │  Cookie          │                  │
     │<─────────────────────────────────────────────────────│
```

### 7.2 授權檢查

所有 Web Controller 使用 `@CurrentUser UserPrincipal` 註解取得已認證使用者，透過 `principal.getUser()` 直接取得 User 實體（零 DB 查詢）。API Controller 則透過 `sub` attribute 解析 UUID。

系統使用 AOP 切面（PermissionAspect）搭配自訂 `@RequiresPermission` 註解進行授權檢查。切面在方法執行前從請求中提取 tripId 和 userId，透過 PermissionChecker 驗證使用者是否具備所需權限（如 EDIT、DELETE），若無權限則拋出 ForbiddenException。

授權檢查涵蓋三個層級：
- **OWNER**：完整權限（刪除行程、管理成員、變更角色）
- **EDITOR**：編輯權限（新增/修改景點、支出、文件、代辦事項）
- **VIEWER**：唯讀權限（僅能檢視行程內容）

---

## 8. 設計決策記錄 (ADR)

### ADR-001: 選擇 Thymeleaf 作為模板引擎

**狀態**: 已採納

**背景**: 需要選擇前端技術方案

**決策**: 使用 Thymeleaf Server-Side Rendering

**理由**:
- 與 Spring Boot 整合良好
- 學習曲線低，團隊熟悉度高
- SEO 友好
- 無需另起前端專案

**後果**:
- 即時協作功能需額外整合（如 WebSocket）
- 複雜互動需搭配 JavaScript

---

### ADR-002: 使用貪婪演算法進行路線優化

**狀態**: 已採納

**背景**: 需要提供路線優化建議功能

**決策**: 使用貪婪最近鄰居演算法 (Greedy Nearest Neighbor)

**理由**:
- 實作簡單，時間複雜度 O(n^2) 可接受
- 景點數量有限（每日 < 15 個）
- 結果品質在可接受範圍內

**替代方案**:
- Traveling Salesman Problem (TSP) 精確解 - 計算量過大
- 遺傳演算法 - 過於複雜

---

### ADR-003: 分帳結算使用貪婪配對

**狀態**: 已採納

**背景**: 需要簡化複雜的多人債務關係

**決策**: 使用貪婪演算法配對最大債權人與債務人

**理由**:
- 可有效減少交易次數
- 實作簡單易維護
- 結果直觀易理解

---

## 9. 附錄

### 9.1 名詞解釋

| 術語 | 定義 |
|------|------|
| Trip | 一次完整的旅行行程 |
| Activity | 行程中的單一活動或景點安排 |
| Place | 實際地理位置（可被多個 Activity 參照） |
| Expense | 一筆支出記錄 |
| ExpenseSplit | 單筆支出的分攤明細 |
| Settlement | 結算後的債務關係（誰付給誰多少） |

### 9.2 專案統計

| 項目 | 數量 |
|------|------|
| 單元測試 | 1007 個測試方法，72 個測試檔案 |
| E2E 測試 | ~118 個測試案例，10 個 spec 檔案 |
| REST API 端點 | 55 個 |
| Web 端點 | 37 個 |
| Service 類別 | 20 個 (含 2 個 ViewHelper) |
| Entity 類別 | 10 個 |
| Enum 類別 | 6 個 |
| Repository | 10 個 |
| HTML 模板 | 27 個 |
| JS 模組 | 7 個 (含 common.js 共用工具) |
| Domain 元件 | 5 個 (含 TripConstants) |
| 外部服務整合 | 4 個 |

### 9.3 參考文件

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Google Maps Platform](https://developers.google.com/maps/documentation)
