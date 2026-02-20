# WeGo 架構審查報告

> 審查日期：2026-02-20（第三次審查）
> 前次審查：2026-02-18
> 審查範圍：Controller / Service / Repository / Domain / Entity / DTO / Config / Exception
> 審查員：Architecture Reviewer Agent

---

## 目錄

1. [分層架構](#1-分層架構)
2. [依賴注入](#2-依賴注入)
3. [Entity 與 DTO 分離](#3-entity-與-dto-分離)
4. [例外處理](#4-例外處理)
5. [SOLID 原則](#5-solid-原則)
6. [DRY 原則](#6-dry-原則)
7. [命名規範](#7-命名規範)
8. [Magic Number](#8-magic-number)
9. [日誌使用](#9-日誌使用)
10. [整體評分](#10-整體評分)

---

## 1. 分層架構

### 優點

- **清晰的四層架構**：Controller → Service → Domain/Repository → Entity，層級邊界明確
- **Web 與 API Controller 分離**：`controller/web/` 和 `controller/api/` 分開，Web 回傳 Thymeleaf 視圖、API 回傳 `ApiResponse<T>` JSON
- **BaseWebController 提取共用邏輯**：`getCurrentUser()`、`loadTrip()`、`findCurrentMember()`、`canEdit()`、`isOwner()` 集中在基類，避免重複
- **ViewHelper 層**：`TripViewHelper`、`ExpenseViewHelper`、`ActivityViewHelper` 將視圖準備邏輯從 Controller 中抽離
- **Domain 層獨立**：`PermissionChecker`、`DebtSimplifier`、`RouteOptimizer`、`ExpenseAggregator` 等放在 `domain/` 包中，不依賴 Spring 框架

### 問題

#### 🟡 Warning — TripController.createTrip 包含過多表單處理邏輯

**檔案**：`src/main/java/com/wego/controller/web/TripController.java:97-189`

`createTrip()` 方法中，日期驗證、封面圖片上傳、錯誤回填 model 屬性等邏輯全部寫在 Controller 內，方法超過 90 行。`updateTrip()` 方法也有相同問題（行 311-405）。

**修正建議**：將 model 屬性回填邏輯提取為 `populateFormModel()` 私有方法，減少 catch 區塊中的重複代碼。

#### 🟡 Warning — ExpenseWebController.showExpenses 方法過長

**檔案**：`src/main/java/com/wego/controller/web/ExpenseWebController.java:77-176`

`showExpenses()` 方法近 100 行，包含個人支出摘要的 JSON 序列化、預算百分比計算等邏輯。

**修正建議**：將個人支出 tab 的資料準備邏輯提取到 `ExpenseViewHelper` 或新增 `PersonalExpenseViewHelper`。

#### 🔵 Suggestion — TripService 職責偏多

**檔案**：`src/main/java/com/wego/service/TripService.java`（共 715 行）

TripService 同時負責：Trip CRUD、成員管理（加入/移除/角色變更）、封面圖片上傳/刪除/驗證、非同步儲存清理。注入了 12 個依賴。

**修正建議**：可考慮將成員管理抽出為 `TripMemberService`，封面圖片管理抽出為 `CoverImageService`。目前規模尚可接受，但若繼續增長應優先拆分。

---

## 2. 依賴注入

### 優點

- **大量使用構造器注入**：所有 Service 和 API Controller 都使用 `@RequiredArgsConstructor` + `private final` 欄位，符合 Spring 最佳實踐
- **無循環依賴**：經檢查所有 Service 間的依賴關係為單向的，未發現循環參照
- **SettlementService 使用 `@Nullable` 可選依賴**：`ExchangeRateService` 和 `StatisticsService` 宣告為可空，支援測試環境下不啟用匯率服務

### 問題

#### 🟡 Warning — BaseWebController 使用 Field Injection

**檔案**：`src/main/java/com/wego/controller/web/BaseWebController.java:31-35`

```java
@Autowired
protected UserService userService;

@Autowired
protected TripService tripService;
```

使用 `@Autowired` 欄位注入而非構造器注入，與專案其他地方的風格不一致，且使得子類的依賴不透明。

**修正建議**：改為透過構造器注入。由於是抽象類，可使用 `@RequiredArgsConstructor` 或讓子類在構造器中傳入。不過由於所有 Web Controller 都繼承此類且已使用 `@RequiredArgsConstructor`，也可維持現狀（Spring 官方支持 abstract class 的 field injection）。

---

## 3. Entity 與 DTO 分離

### 優點

- **完整的 DTO 體系**：所有 API 回應使用 `*Response` DTO，所有請求使用 `*Request` DTO，Entity 不直接暴露給前端
- **統一的 `fromEntity()` 靜態工廠方法**：`TripResponse.fromEntity(trip)`、`ExpenseResponse.fromEntity(expense)` 等模式統一
- **`ApiResponse<T>` 統一包裝**：所有 REST API 回應使用相同格式，包含 `success`、`data`、`errorCode`、`message`、`timestamp`
- **Request DTO 使用 Builder 模式**：配合 `@Valid` 進行驗證

### 問題

#### 🟡 Warning — TripResponse 使用 Setter 填充額外欄位

**檔案**：`src/main/java/com/wego/dto/response/TripResponse.java` 和 `src/main/java/com/wego/service/TripService.java:125-128`

```java
TripResponse response = TripResponse.fromEntity(trip);
response.setMemberCount(1);
response.setCurrentUserRole(Role.OWNER);
```

`TripResponse` 使用 `@Data`（含 Setter），在 `fromEntity()` 後再用 setter 補充欄位。這破壞了 DTO 的不可變性。

**修正建議**：改為在 `fromEntity()` 中接受額外參數，或使用 Builder 模式一次性建構完成。例如：
```java
public static TripResponse fromEntity(Trip trip, int memberCount, Role currentUserRole) { ... }
```

#### 🔵 Suggestion — Response DTO 中的 `@Data` 可改為 `@Getter` + `@Builder`

多個 Response DTO（如 `ExpenseResponse`、`ExpenseSplitResponse`）使用 `@Data` 暴露了 Setter，理論上 Response DTO 應為不可變物件。

**修正建議**：將 Response DTO 的 `@Data` 改為 `@Getter` + `@Builder` + `@AllArgsConstructor`（或改為 Java record）。由於目前 Service 層依賴 setter 填充關聯資訊（如 `paidByName`），此重構需同步調整 Service 層。

---

## 4. 例外處理

### 優點

- **雙層 ExceptionHandler 設計**：
  - `GlobalExceptionHandler`（`@RestControllerAdvice(basePackages = "com.wego.controller.api")`）處理 API 回傳 JSON
  - `WebExceptionHandler`（`@ControllerAdvice(basePackages = "com.wego.controller.web")`）處理 Web 回傳錯誤頁面
- **完整的例外層級**：`BusinessException` → `ResourceNotFoundException` / `ForbiddenException` / `UnauthorizedException` / `ValidationException`
- **errorCode 機制**：所有業務例外都帶有結構化的 `errorCode`，便於前端處理
- **外部服務例外獨立處理**：`GoogleMapsException`、`ExchangeRateException`、`GeminiException` 各有專屬處理邏輯
- **安全的錯誤訊息**：`handleGenericException` 不洩漏內部資訊，僅回傳 "An unexpected error occurred"

### 問題

#### 🔵 Suggestion — WebExceptionHandler 的 `@Order(1)` 可移除

**檔案**：`src/main/java/com/wego/exception/WebExceptionHandler.java:26`

`@Order(1)` 的作用是確保 Web handler 優先於 API handler，但由於兩者已透過 `basePackages` 限定作用範圍，實際上不會衝突。

**修正建議**：可保留作為防禦性措施，或移除以減少困惑。

#### 🔵 Suggestion — ValidationException 可獨立處理

**檔案**：`src/main/java/com/wego/exception/ValidationException.java`

`ValidationException` 繼承 `BusinessException`，但 `GlobalExceptionHandler` 中沒有為它設定專屬 handler。目前 `BusinessException` handler 會捕獲它並回傳 400，行為正確但語義不夠精確。

**修正建議**：在 `GlobalExceptionHandler` 中加入 `@ExceptionHandler(ValidationException.class)` 以提供更精確的錯誤回應（例如區分業務錯誤和驗證錯誤）。

---

## 5. SOLID 原則

### 單一職責原則 (SRP)

#### 優點
- `PermissionChecker` 專職權限檢查
- `DebtSimplifier` 專職債務簡化演算法
- `RouteOptimizer` 專職路線最佳化
- `FileValidationUtils` 專職檔案驗證
- `ViewHelper` 類專職視圖準備邏輯
- `ExpenseAggregator` 專職統計聚合

#### 🟡 Warning — TripService 職責過多（同第 1 節）

### 開閉原則 (OCP)

#### 優點
- **外部服務的介面 + Mock 模式**：`StorageClient` / `MockStorageClient`、`GoogleMapsClient` / `MockGoogleMapsClient`、`WeatherClient` / `MockWeatherClient`、`GeminiClient` / `MockGeminiClient`、`ExchangeRateClient` / `MockExchangeRateClient`。新增外部服務只需實作介面。
- **SplitType 列舉 + switch**：`ExpenseService.createExpenseSplits()` 使用 switch 處理不同分帳類型，新增類型只需擴充 case。

### 里氏替換原則 (LSP)

- **正確實踐**：所有外部客戶端的 Mock 實作完全遵循介面契約
- **BaseWebController** 子類正確繼承並使用父類方法

### 介面隔離原則 (ISP)

- **外部客戶端介面精簡**：`StorageClient` 只有 `uploadFile`、`deleteFile`、`getSignedUrl` 三個方法
- **GeminiClient** 只有 `chat()` 和 `chatWithMetadata()` 兩個方法

### 依賴反轉原則 (DIP)

#### 優點
- Service 層依賴 Repository 介面（Spring Data JPA）
- 外部服務全部透過介面注入（`StorageClient`、`GoogleMapsClient`、`GeminiClient` 等）
- `PermissionChecker` 注入 `TripMemberRepository` 介面

#### 🔵 Suggestion — SettlementService 未使用 `@RequiredArgsConstructor`

**檔案**：`src/main/java/com/wego/service/SettlementService.java:80-99`

使用手動構造器而非 Lombok `@RequiredArgsConstructor`，原因是 `exchangeRateService` 和 `statisticsService` 需要 `@Nullable`。此設計合理但風格與其他 Service 不一致。

---

## 6. DRY 原則

### 優點

- **BaseWebController 消除重複**：所有 Web Controller 共享 `getCurrentUser()`、`loadTrip()`、`findCurrentMember()` 等
- **ViewHelper 消除重複**：`TripViewHelper.calculateTripDuration()` 被多處使用
- **TripConstants 共享常數**：`MAX_MEMBERS_PER_TRIP` 被 `TripService` 和 `InviteLinkService` 共用
- **FileValidationUtils 共享驗證邏輯**：被 `TripService`（封面圖片）和 `DocumentService`（文件上傳）共用
- **CurrencyConverter 共享匯率工具**

### 問題

#### 🟡 Warning — getUserMap() 方法重複定義

**檔案**：
- `src/main/java/com/wego/service/ExpenseService.java:644-647`
- `src/main/java/com/wego/service/SettlementService.java:500-506`

兩個 Service 中都有幾乎相同的 `getUserMap()` 私有方法：

```java
private Map<UUID, User> getUserMap(List<UUID> userIds) {
    return userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
}
```

**修正建議**：提取為 `UserService.getUserMap()` 公開方法，或建立共用的 `UserLookupHelper`。

#### 🟡 Warning — TripController 中 model 屬性回填代碼重複

**檔案**：`src/main/java/com/wego/controller/web/TripController.java`

`createTrip()` 方法中有三處幾乎相同的 model 回填代碼（行 112-123、164-173、178-187）；`updateTrip()` 也有類似問題。

```java
model.addAttribute("name", user.getNickname());
model.addAttribute("picture", user.getAvatarUrl());
model.addAttribute("minDate", LocalDate.now());
model.addAttribute("isEdit", false);
model.addAttribute("trip", CreateTripRequest.builder()...build());
```

**修正建議**：提取為 `populateCreateFormModel(Model model, User user, boolean isEdit, ...)` 方法。

#### 🟡 Warning — buildPlaceLookup() 邏輯重複

**檔案**：
- `src/main/java/com/wego/service/ActivityService.java:552-565`
- `src/main/java/com/wego/service/ChatService.java:262-273`

兩處都有「根據 Activity 清單批量查詢 Place」的邏輯。

**修正建議**：提取為 `PlaceService.buildPlaceLookup(List<Activity>)` 或放在 `ActivityService` 中暴露為 public。

#### 🔵 Suggestion — getFileExtension() 方法重複

**檔案**：
- `src/main/java/com/wego/service/TripService.java:707-714`
- `src/main/java/com/wego/service/DocumentService.java:492-497`

兩處都有 `getFileExtension()` 方法，但 TripService 版本還包含安全檢查（只允許已知擴展名）。

**修正建議**：統一到 `FileValidationUtils` 中。

---

## 7. 命名規範

### 優點

- **嚴格遵循專案命名規範**：
  - Entity：單數（`User`、`Trip`、`Expense`）
  - Service：`{Domain}Service`（`TripService`、`ExpenseService`）
  - Controller：`{Domain}Controller`（Web）/ `{Domain}ApiController`（REST）
  - DTO：`Create{Entity}Request` / `Update{Entity}Request` / `{Entity}Response`
- **變數命名清晰**：`tripId`、`userId`、`baseCurrency`、`memberCount` 等
- **常數命名規範**：`MAX_MEMBERS_PER_TRIP`、`ALLOWED_MIME_TYPES`、`MAX_COVER_IMAGE_SIZE`
- **方法命名一致**：`create*`、`get*`、`update*`、`delete*`、`calculate*`、`validate*`

### 問題

#### 🔵 Suggestion — 混用英文與中文錯誤訊息

**檔案**：多處

部分 Service 使用英文錯誤訊息（如 `ExpenseService`）：
```java
throw new ForbiddenException("No permission to edit this trip");
```

部分使用中文（如 `TripService`）：
```java
throw new ForbiddenException("您沒有權限查看此行程");
```

**修正建議**：統一為中文（面向用戶的訊息），或統一為英文（面向開發者）+ i18n。目前專案以中文面向用戶為主，建議統一為中文。

---

## 8. Magic Number

### 優點

- **大部分魔術數字已提取為常數**：
  - `TripConstants.MAX_MEMBERS_PER_TRIP = 10`
  - `Document.MAX_FILE_SIZE`、`Document.MAX_TRIP_STORAGE`
  - `TripService.MAX_COVER_IMAGE_SIZE = 5 * 1024 * 1024`
  - `ChatService.GAP_THRESHOLD_MINUTES = 120`
  - `PersonalExpenseService.BUDGET_THRESHOLD_YELLOW/RED`
  - `CurrencyConverter.MIN_RATE`、`CurrencyConverter.MAX_RATE`

### 問題

#### 🟡 Warning — ExpenseService 中的容差值未提取為常數

**檔案**：`src/main/java/com/wego/service/ExpenseService.java:519,540`

```java
if (totalSplitAmount.subtract(expenseAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
```

`"0.01"` 出現兩次作為分帳容差值。

**修正建議**：提取為 `private static final BigDecimal SPLIT_TOLERANCE = new BigDecimal("0.01");`

#### 🔵 Suggestion — TripController 中的分頁參數

**檔案**：`src/main/java/com/wego/controller/web/TripController.java:57-58`

```java
Page<TripResponse> tripPage = tripService.getUserTrips(user.getId(),
    PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "startDate")));
```

`50` 和 `"startDate"` 為硬編碼值。

**修正建議**：提取為常數或配置項。

#### 🔵 Suggestion — TripViewHelper 中的預設座標

**檔案**：`src/main/java/com/wego/service/TripViewHelper.java:138-139`

```java
final double defaultLat = 25.0339;
final double defaultLng = 121.5645;
```

台北 101 座標作為預設值，已使用區域變數但建議提取為有名稱的類別常數（如 `DEFAULT_WEATHER_LAT`）。

---

## 9. 日誌使用

### 優點

- **全面使用 SLF4J (`@Slf4j`)**：所有 Service 和 Controller 都使用 Lombok `@Slf4j`，無 `System.out.println`
- **分級合理**：
  - `log.debug`：方法入口、流程追蹤
  - `log.info`：業務操作完成（建立、更新、刪除）
  - `log.warn`：非致命錯誤（權限拒絕、資源不存在、外部服務失敗）
  - `log.error`：例外（含堆疊追蹤）
- **結構化日誌參數**：使用 `log.info("Created trip: {} by user: {}", tripId, userId)` 而非字串拼接
- **敏感資訊未洩漏**：日誌中不包含密碼、API Key 等

### 問題

無重大問題。Debug 日誌數量合理，可透過 Logback 配置在生產環境中控制。

---

## 10. 整體評分

| 面向 | 評分 | 說明 |
|------|:----:|------|
| 分層架構 | 9.0 | Controller/Service/Repository/Domain 分層清晰，ViewHelper 提取得當 |
| 依賴注入 | 8.5 | 構造器注入為主，BaseWebController 的 Field Injection 為唯一例外 |
| Entity/DTO 分離 | 8.5 | DTO 體系完整，fromEntity 模式統一，但 Response 可變性可改善 |
| 例外處理 | 9.5 | 雙層 ExceptionHandler + 結構化 errorCode，設計優秀 |
| SOLID 原則 | 8.5 | 外部服務介面化、Domain 層獨立，TripService 略有職責過重 |
| DRY 原則 | 8.0 | BaseWebController 和 ViewHelper 消除大量重複，但仍有少數重複代碼 |
| 命名規範 | 9.0 | 嚴格遵循專案規範，中英混用為唯一小問題 |
| Magic Number | 8.5 | 大部分已提取為常數，僅少數遺漏 |
| 日誌使用 | 9.5 | 全面使用 SLF4J，分級合理，無 System.out |

### **架構整體評分：8.8 / 10**

### 總結

WeGo 專案的架構設計整體水準很高。四層分層清晰，外部服務透過介面抽象化，例外處理系統完善且安全。主要改進方向集中在：

1. **TripService 拆分**（長期）：當功能繼續增長時，將成員管理和封面圖片管理拆出
2. **消除重複代碼**（中期）：`getUserMap()`、`getFileExtension()`、`buildPlaceLookup()` 提取為共用方法
3. **Response DTO 不可變性**（中期）：改用 `@Getter` + `@Builder` 或 Java record
4. **錯誤訊息語言統一**（短期）：統一為中文
5. **BaseWebController Field Injection**（低優先級）：改為構造器注入

這些問題都不影響功能正確性和安全性，是進一步提升代碼品質的改進方向。

### 與上次審查比較

相較 2026-02-18 的第二次審查，本次觀察到以下改進：
- 新增 `PersonalExpenseService` 遵循既有架構模式，權限檢查一致
- `StatisticsCacheDelegate` 分離快取邏輯
- 支出變動後即時清除分帳與統計快取（`evictExpenseCaches`）
- 前端 JS 模組化持續優化（6 個內聯 JS 已提取為獨立模組）

整體架構品質穩定維持在高水準。
