# WeGo 軟體設計文件 (Software Design Document)

## 文件資訊

| 項目 | 內容 |
|------|------|
| 專案名稱 | WeGo - 旅遊規劃協作平台 |
| 版本 | 1.0.0 |
| 最後更新 | 2026-02-11 |
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
│   ├── OAuth2Config.java             # OAuth2 設定
│   ├── WebConfig.java                # Web MVC 設定
│   └── CacheConfig.java              # 快取設定
│
├── controller/                       # 控制器層
│   ├── web/                          # 頁面控制器
│   │   ├── HomeController.java
│   │   ├── TripController.java
│   │   └── AuthController.java
│   └── api/                          # REST API 控制器
│       ├── TripApiController.java
│       ├── ActivityApiController.java
│       ├── ExpenseApiController.java
│       └── DocumentApiController.java
│
├── service/                          # 服務層
│   ├── TripService.java
│   ├── ActivityService.java
│   ├── ExpenseService.java
│   ├── SettlementService.java
│   ├── DocumentService.java
│   ├── TodoService.java
│   └── external/                     # 外部服務整合
│       ├── GoogleMapsService.java
│       ├── WeatherService.java
│       └── ExchangeRateService.java
│
├── repository/                       # 資料存取層
│   ├── UserRepository.java
│   ├── TripRepository.java
│   ├── TripMemberRepository.java
│   ├── ActivityRepository.java
│   ├── PlaceRepository.java
│   ├── ExpenseRepository.java
│   ├── ExpenseSplitRepository.java
│   ├── DocumentRepository.java
│   └── TodoRepository.java
│
├── entity/                           # JPA 實體
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
│   │   ├── CreateTripRequest.java
│   │   ├── UpdateTripRequest.java
│   │   ├── CreateActivityRequest.java
│   │   └── CreateExpenseRequest.java
│   ├── response/                     # 回應 DTO
│   │   ├── TripResponse.java
│   │   ├── TripDetailResponse.java
│   │   ├── ActivityResponse.java
│   │   └── SettlementResponse.java
│   └── mapper/                       # 映射器
│       ├── TripMapper.java
│       ├── ActivityMapper.java
│       └── ExpenseMapper.java
│
├── domain/                           # 領域邏輯
│   ├── settlement/                   # 分帳領域
│   │   ├── DebtSimplifier.java       # 債務簡化演算法
│   │   └── Settlement.java           # 結算結果
│   ├── route/                        # 路線領域
│   │   └── RouteOptimizer.java       # 路線優化演算法
│   └── permission/                   # 權限領域
│       ├── Permission.java
│       └── PermissionChecker.java
│
├── exception/                        # 例外處理
│   ├── GlobalExceptionHandler.java
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
└── util/                             # 工具類別
    ├── DateUtils.java
    └── CurrencyUtils.java
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
```java
public class PermissionChecker {

    public boolean canEdit(UUID tripId, UUID userId) {
        TripMember member = memberRepository.findByTripAndUser(tripId, userId);
        return member != null &&
               (member.getRole() == Role.OWNER || member.getRole() == Role.EDITOR);
    }

    public boolean canDelete(UUID tripId, UUID userId) {
        TripMember member = memberRepository.findByTripAndUser(tripId, userId);
        return member != null && member.getRole() == Role.OWNER;
    }
}
```

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
```java
public class RouteOptimizer {

    /**
     * 使用貪婪最近鄰居演算法優化路線
     * 時間複雜度: O(n²)
     */
    public List<Activity> optimize(List<Activity> activities) {
        if (activities.size() <= 2) {
            return activities;
        }

        List<Activity> optimized = new ArrayList<>();
        Set<Activity> remaining = new HashSet<>(activities);

        // 從第一個景點開始
        Activity current = activities.get(0);
        optimized.add(current);
        remaining.remove(current);

        while (!remaining.isEmpty()) {
            Activity nearest = findNearest(current, remaining);
            optimized.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }

        return optimized;
    }

    private Activity findNearest(Activity from, Set<Activity> candidates) {
        return candidates.stream()
            .min(Comparator.comparing(a ->
                calculateDistance(from.getPlace(), a.getPlace())))
            .orElseThrow();
    }
}
```

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
```java
public class DebtSimplifier {

    /**
     * 簡化債務關係，最小化交易次數
     * 使用貪婪演算法配對最大債權人與最大債務人
     */
    public List<Settlement> simplify(List<ExpenseSplit> splits,
                                      Map<UUID, BigDecimal> payments) {
        // 計算每人淨額（正數=應收，負數=應付）
        Map<UUID, BigDecimal> balances = calculateBalances(splits, payments);

        // 分離債權人與債務人
        PriorityQueue<Balance> creditors = new PriorityQueue<>(
            Comparator.comparing(Balance::getAmount).reversed());
        PriorityQueue<Balance> debtors = new PriorityQueue<>(
            Comparator.comparing(Balance::getAmount));

        balances.forEach((userId, amount) -> {
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new Balance(userId, amount));
            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new Balance(userId, amount.abs()));
            }
        });

        // 貪婪配對
        List<Settlement> settlements = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();

            BigDecimal amount = creditor.getAmount()
                .min(debtor.getAmount());

            settlements.add(new Settlement(
                debtor.getUserId(),   // from
                creditor.getUserId(), // to
                amount
            ));

            // 處理餘額
            BigDecimal creditorRemaining = creditor.getAmount().subtract(amount);
            BigDecimal debtorRemaining = debtor.getAmount().subtract(amount);

            if (creditorRemaining.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new Balance(creditor.getUserId(), creditorRemaining));
            }
            if (debtorRemaining.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new Balance(debtor.getUserId(), debtorRemaining));
            }
        }

        return settlements;
    }
}
```

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
| 後端 REST endpoint 總數 | ~50 | 含 Web Controller + API Controller |
| 前端實際使用的 endpoint | ~24 | JavaScript 模組 + Thymeleaf inline 呼叫 |
| Orphan endpoints (API-only) | ~26 | 後端有但前端未使用，供未來 mobile/SPA 使用 |

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
```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
}

// 成功回應
{
    "success": true,
    "data": { ... },
    "message": null,
    "errorCode": null,
    "timestamp": "2024-01-15T10:30:00"
}

// 錯誤回應
{
    "success": false,
    "data": null,
    "message": "行程不存在",
    "errorCode": "TRIP_NOT_FOUND",
    "timestamp": "2024-01-15T10:30:00"
}
```

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

```json
{
    "success": false,
    "data": null,
    "message": "驗證失敗",
    "errorCode": "VALIDATION_ERROR",
    "errors": [
        {
            "field": "title",
            "message": "行程名稱不可為空",
            "rejectedValue": null
        },
        {
            "field": "endDate",
            "message": "結束日期不可早於開始日期",
            "rejectedValue": "2024-01-01"
        }
    ],
    "timestamp": "2024-01-15T10:30:00"
}
```

### 4.3 分頁與查詢參數

#### 分頁回應格式

```java
public class PagedResponse<T> {
    private boolean success;
    private List<T> data;
    private PaginationMeta pagination;
    private LocalDateTime timestamp;
}

public class PaginationMeta {
    private int page;           // 當前頁碼（0-based）
    private int size;           // 每頁筆數
    private int totalPages;     // 總頁數
    private long totalElements; // 總筆數
    private boolean hasNext;
    private boolean hasPrevious;
}
```

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

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1704067200
```

#### 超過限制回應

```json
{
    "success": false,
    "message": "請求過於頻繁，請稍後再試",
    "errorCode": "RATE_LIMIT_EXCEEDED",
    "retryAfter": 60
}
```

### 4.5 Health Check 端點

| 端點 | 用途 | 認證 |
|------|------|------|
| `GET /actuator/health` | 基本健康檢查（for Load Balancer）| 無 |
| `GET /actuator/health/liveness` | Kubernetes 存活探針 | 無 |
| `GET /actuator/health/readiness` | Kubernetes 就緒探針 | 無 |

#### 回應範例

```json
// GET /actuator/health
{
    "status": "UP"
}

// GET /actuator/health/readiness（檢查依賴服務）
{
    "status": "UP",
    "components": {
        "db": { "status": "UP" },
        "diskSpace": { "status": "UP" }
    }
}
```

### 4.6 DTO 設計範例

```java
// Request DTO - 使用 Validation
@Data
public class CreateTripRequest {

    @NotBlank(message = "行程名稱不可為空")
    @Size(max = 100, message = "行程名稱不可超過100字")
    private String title;

    @Size(max = 500, message = "描述不可超過500字")
    private String description;

    @NotNull(message = "開始日期不可為空")
    @FutureOrPresent(message = "開始日期不可為過去")
    private LocalDate startDate;

    @NotNull(message = "結束日期不可為空")
    private LocalDate endDate;

    @Pattern(regexp = "^[A-Z]{3}$", message = "幣別格式錯誤")
    private String baseCurrency = "TWD";
}

// Response DTO - 只包含需要的欄位
@Data
@Builder
public class TripResponse {
    private UUID id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverImageUrl;
    private int memberCount;
    private List<MemberSummary> members;

    @Data
    @Builder
    public static class MemberSummary {
        private UUID userId;
        private String nickname;
        private String avatarUrl;
    }
}
```

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

#### 回應範例

```json
// GET /api/directions?origin=台北車站&dest=淺草寺&mode=transit
{
    "success": true,
    "data": {
        "distance": "2,180 km",
        "distanceMeters": 2180000,
        "duration": "3 小時 45 分",
        "durationSeconds": 13500,
        "mode": "transit"
    },
    "cached": true,
    "cacheExpires": "2024-01-16T10:30:00"
}

// GET /api/weather?lat=35.6895&lng=139.6917&date=2024-03-15
{
    "success": true,
    "data": {
        "date": "2024-03-15",
        "tempHigh": 18,
        "tempLow": 10,
        "condition": "晴",
        "icon": "01d",
        "rainProbability": 5
    }
}

// GET /api/exchange-rates?from=JPY&to=TWD
{
    "success": true,
    "data": {
        "from": "JPY",
        "to": "TWD",
        "rate": 0.22,
        "updatedAt": "2024-01-15T00:00:00Z"
    }
}
```

### 6.2 Google Maps API 實作

```java
@Service
public class GoogleMapsService {

    private final RestTemplate restTemplate;
    private final String apiKey;

    /**
     * 計算兩點間的交通時間與距離
     * 結果快取 24 小時
     */
    @Cacheable(value = "directions", key = "#origin + '-' + #destination + '-' + #mode")
    public DirectionResult getDirections(String origin,
                                         String destination,
                                         TravelMode mode) {
        String url = String.format(
            "https://maps.googleapis.com/maps/api/distancematrix/json" +
            "?origins=%s&destinations=%s&mode=%s&key=%s",
            origin, destination, mode.getValue(), apiKey
        );

        // 呼叫 API 並解析回應
        // ...
    }

    /**
     * 搜尋地點
     */
    public List<PlaceResult> searchPlaces(String query,
                                          double lat,
                                          double lng,
                                          int radius) {
        // ...
    }
}
```

### 6.3 快取策略

| 資料類型 | 快取時間 | 說明 |
|----------|----------|------|
| 交通路線 | 24 小時 | 相同起訖點結果不常變動 |
| 天氣預報 | 6 小時 | 配合 API 更新頻率 |
| 匯率 | 24 小時 | 每日更新 |
| 地點搜尋 | 1 小時 | 搜尋結果可能更新 |

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
| `todo` | `idx_todo_trip_status` | (trip_id, status, due_date) | INDEX | 代辦清單排序 |
| `invite_link` | `idx_invite_token` | (token) | UNIQUE | 邀請連結查詢 |
| `invite_link` | `idx_invite_expires` | (expires_at) | INDEX | 過期連結清理 |

#### 索引 SQL 範例

```sql
-- PostgreSQL 索引建立範例
CREATE UNIQUE INDEX idx_user_provider ON "user" (provider, provider_id);
CREATE UNIQUE INDEX idx_trip_member_unique ON trip_member (trip_id, user_id);
CREATE INDEX idx_activity_trip_day ON activity (trip_id, day, sort_order);
CREATE INDEX idx_expense_trip ON expense (trip_id, created_at DESC);
CREATE INDEX idx_todo_trip_status ON todo (trip_id, status, due_date)
    WHERE status != 'completed';  -- 部分索引，只索引未完成項目
```

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

```java
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                   RequiresPermission requiresPermission)
            throws Throwable {

        UUID tripId = extractTripId(joinPoint);
        UUID userId = getCurrentUserId();
        Permission required = requiresPermission.value();

        if (!permissionChecker.hasPermission(tripId, userId, required)) {
            throw new ForbiddenException("無權限執行此操作");
        }

        return joinPoint.proceed();
    }
}

// 使用方式
@RequiresPermission(Permission.EDIT)
public void updateActivity(UUID tripId, UpdateActivityRequest request) {
    // ...
}
```

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
- 實作簡單，時間複雜度 O(n²) 可接受
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

### 9.2 參考文件

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [Google Maps Platform](https://developers.google.com/maps/documentation)
