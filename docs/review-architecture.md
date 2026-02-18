# 架構審查報告

> 審查日期：2026-02-18（第二次審查）
> 前次審查：2026-02-14
> 審查範圍：Controller / Service / Repository 分層、依賴注入、Entity/DTO 分離、例外處理、SOLID/DRY、命名規範

---

## 問題摘要

| 嚴重程度 | 數量 | 與前次比較 |
|----------|------|-----------|
| :red_circle: Critical | 0 | 持平 |
| :yellow_circle: Warning | 3 | -5（8→3）|
| :blue_circle: Suggestion | 8 | -1（9→8）|

**前次審查修復追蹤**：8 項 Warning 中有 5 項已修復（W-01, W-02, W-05, W-06, W-08），1 項已修復（W-03）。

---

## 前次問題修復狀態

| 編號 | 問題 | 狀態 |
|------|------|------|
| W-01 | TripController 包含視圖呈現邏輯 | :white_check_mark: 已修復 — `TripViewHelper` 已建立並使用 |
| W-02 | HomeController 在 Controller 計算 daysUntil | :white_check_mark: 已修復 — 使用 `TripViewHelper.calculateDaysUntilMap()` |
| W-03 | PlaceService.findOrCreate() 回傳 Place Entity | :white_check_mark: 已修復 — 現回傳 `UUID` |
| W-04 | DocumentService.getDocumentForPreview() 回傳 Entity | :warning: 未修復 — 仍回傳 `Document` Entity |
| W-05 | EARTH_RADIUS_METERS 重複定義 4 次 | :white_check_mark: 已修復 — 統一至 `GeoUtils` |
| W-06 | Magic Bytes 驗證邏輯重複 | :white_check_mark: 已修復 — 統一至 `FileValidationUtils` |
| W-07 | requireUserId() 在多個 API Controller 重複 | :warning: 未修復 — 仍在 2 個 Controller 重複 |
| W-08 | UNKNOWN_USER_NAME 重複定義 | :white_check_mark: 已修復 — 統一至 `TripConstants` |
| S-01 | BaseWebController 使用 @Autowired | :warning: 未修復 — 仍使用 field injection |
| S-02 | TripService 依賴過多 (12個) | :warning: 未修復 — 仍為 12 個依賴 |

---

## 1. 分層架構檢查

### 整體評價

分層架構設計良好，Controller / Service / Repository 職責分明：
- **Controller 層**：Web Controller 繼承 `BaseWebController` 提供共用方法，API Controller 各自獨立，統一回傳 `ApiResponse`
- **Service 層**：業務邏輯集中於 Service，包含權限檢查、資料驗證、交易管理
- **Domain 層**：`PermissionChecker`、`DebtSimplifier`、`RouteOptimizer`、`GeoUtils`、`FileValidationUtils` 正確封裝核心邏輯
- **Repository 層**：使用 Spring Data JPA，查詢方法命名清晰
- **ViewHelper 層**：`TripViewHelper`、`ActivityViewHelper`、`ExpenseViewHelper` 將視圖邏輯從 Controller 提取，職責清晰

### 改善項目

#### :yellow_circle: W-01（新）：TodoWebController 重複 BaseWebController 的權限邏輯

**檔案**：`src/main/java/com/wego/controller/web/TodoWebController.java:81-88`

```java
// TodoWebController 自行實作 findCurrentMember + canEdit
TripResponse.MemberSummary currentMember = trip.getMembers().stream()
        .filter(m -> m.getUserId().equals(user.getId()))
        .findFirst()
        .orElse(null);

boolean canEdit = currentMember != null &&
        (currentMember.getRole() == Role.OWNER ||
         currentMember.getRole() == Role.EDITOR);
```

`BaseWebController` 已提供 `findCurrentMember()` (line 75) 和 `canEdit()` (line 91) 方法，`TodoWebController` 繼承了 `BaseWebController` 卻未使用這些方法，而是自行重複實作。

**建議**：改用 `findCurrentMember(trip, user.getId())` 和 `canEdit(currentMember)`。

---

## 2. 依賴注入

### 整體評價

依賴注入設計良好：
- 所有 Service 和 Controller 均使用 `@RequiredArgsConstructor`（除 `BaseWebController` 和 `SettlementService`）
- `SettlementService` 使用手動建構子以支援 `@Nullable` 可選依賴，這是正確做法
- 外部 API Client 使用介面 + `@ConditionalOnProperty` 切換真實/Mock 實現
- **未發現循環依賴**

#### :blue_circle: S-01：BaseWebController 使用 @Autowired field injection

**檔案**：`src/main/java/com/wego/controller/web/BaseWebController.java:31-35`

```java
@Autowired
protected UserService userService;
@Autowired
protected TripService tripService;
```

其他所有 Controller 和 Service 均使用 `@RequiredArgsConstructor` 的建構子注入。`BaseWebController` 是唯一使用 field injection 的類別。

**建議**：改為建構子注入或在子類別中注入後向上傳遞。不過因為這是 abstract class + 子類用 `@RequiredArgsConstructor`，field injection 是合理的折衷方案，影響不大。

#### :blue_circle: S-02：TripService 依賴過多 Repository

**檔案**：`src/main/java/com/wego/service/TripService.java:81-92`

`TripService` 注入了 9 個 Repository + 3 個其他依賴（共 12 個依賴），主因是 `deleteTrip()` 需要級聯刪除所有關聯實體。

**建議**：考慮使用 JPA Cascade Delete 或將刪除邏輯提取至 `TripDeletionService`，降低 `TripService` 的耦合度。

---

## 3. Entity 與 DTO 分離

### 整體評價

Entity/DTO 分離做得**非常好**：
- 所有 API 回應使用 DTO（`TripResponse`、`ActivityResponse`、`ExpenseResponse` 等）
- 每個 DTO 都有 `fromEntity()` 靜態工廠方法
- Request DTO 使用 Builder 模式（`CreateTripRequest`、`UpdateTripRequest` 等）
- Entity 不直接暴露給前端
- `PlaceService.findOrCreate()` 已改為回傳 `UUID`（前次 W-03 已修復）

#### :yellow_circle: W-02（保留）：DocumentService.getDocumentForPreview() 回傳 Entity

**檔案**：`src/main/java/com/wego/service/DocumentService.java:419`

```java
public Document getDocumentForPreview(UUID tripId, UUID documentId, UUID userId) {
    // ... returns Document entity
}
```

此方法回傳 `Document` Entity 供 `DocumentApiController` 直接使用（`DocumentApiController.java:183`）。

**建議**：提供 DTO 或定義 `DocumentPreviewResponse`，避免 Entity 洩漏至 Controller。

---

## 4. 例外處理

### 整體評價

例外處理架構**設計優秀**：
- `GlobalExceptionHandler`（`@RestControllerAdvice`）處理 API Controller，回傳 `ApiResponse` JSON
- `WebExceptionHandler`（`@ControllerAdvice`）處理 Web Controller，回傳錯誤頁面 ModelAndView
- 使用 `basePackages` 精確區分作用範圍
- 例外階層清晰：`BusinessException` -> `ResourceNotFoundException` / `ForbiddenException` / `UnauthorizedException` / `ValidationException`
- 所有例外都有 `errorCode`，便於前端處理

#### :blue_circle: S-03：GlobalExceptionHandler 缺少 ValidationException 處理

**檔案**：`src/main/java/com/wego/exception/GlobalExceptionHandler.java`

`WebExceptionHandler` 有明確的 `ValidationException` handler（line 71），但 `GlobalExceptionHandler` 沒有。`ValidationException` 繼承 `BusinessException`，會被 `handleBusinessException()` 捕獲並回傳 400，但 errorCode 會是 Service 層設定的值而非統一的 `VALIDATION_ERROR`。

**建議**：在 `GlobalExceptionHandler` 中新增明確的 `ValidationException` handler，使行為與 `WebExceptionHandler` 一致。影響不大，因為目前行為已正確。

#### :blue_circle: S-04：WebExceptionHandler.handleGenericException 暴露 ex.getMessage()

**檔案**：`src/main/java/com/wego/exception/WebExceptionHandler.java:83`

```java
return createErrorView(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), request);
```

`createErrorView` 會將 `ex.getMessage()` 放入 `errorDetails` model 屬性，若 Thymeleaf 模板顯示此值可能洩漏內部資訊。`GlobalExceptionHandler` 正確地使用了固定的 "An unexpected error occurred"。

**建議**：改為使用固定訊息，將原始錯誤僅記錄在 log 中。

---

## 5. SOLID / DRY 原則

### DRY 違反

#### :yellow_circle: W-03（保留）：`requireUserId()` 在多個 API Controller 重複

**檔案**：
- `src/main/java/com/wego/controller/api/ActivityApiController.java:276-281`
- `src/main/java/com/wego/controller/api/ChatApiController.java:61-66`

同樣的 `requireUserId()` helper 方法在 2 個 API Controller 中重複。

**建議**：提取至 `BaseApiController` 或直接使用 `principal.getId()`（因為 Spring Security 已保證 authenticated endpoint 的 principal 非 null）。

### SOLID 分析

- **S（單一職責）**：大部分遵守良好。`TripService` 稍微超載（含圖片上傳邏輯），但在合理範圍內。ViewHelper 的引入顯著改善了 Controller 的單一職責。
- **O（開放封閉）**：外部 API Client 使用介面 + 條件實作，符合 OCP。
- **L（Liskov 替換）**：例外階層正確，`BusinessException` 子類可互換使用。
- **I（介面隔離）**：`GoogleMapsClient`、`WeatherClient`、`StorageClient`、`ExchangeRateClient`、`GeminiClient` 介面設計適當。
- **D（依賴反轉）**：Service 層依賴 Repository 介面和外部 Client 介面，正確遵守 DIP。

---

## 6. 命名規範與日誌

### 整體評價

命名規範遵守良好：
- Entity 使用單數（`User`、`Trip`、`Activity`）
- Service 使用 `{Domain}Service`（`TripService`、`ActivityService`）
- Controller 分 `{Domain}Controller`（Web）和 `{Domain}ApiController`（REST）
- DTO 使用 `Create{Entity}Request`、`Update{Entity}Request`、`{Entity}Response`
- ViewHelper 使用 `{Domain}ViewHelper`
- Domain 工具類命名清晰（`GeoUtils`、`FileValidationUtils`、`DebtSimplifier`）

### Magic Number

#### :blue_circle: S-05：Controller 中的 Magic Number

**檔案**：
- `src/main/java/com/wego/controller/web/TripController.java:58` -- `PageRequest.of(0, 50, ...)`
- `src/main/java/com/wego/controller/web/HomeController.java:67` -- `PageRequest.of(0, 10, ...)`
- `src/main/java/com/wego/controller/web/HomeController.java:80` -- `today.plusDays(30)`

**建議**：提取為命名常數（`MAX_TRIPS_PER_PAGE`、`UPCOMING_DAYS_WINDOW`）。

#### :blue_circle: S-06：ExchangeRateApiController 內嵌 DTO

**檔案**：`src/main/java/com/wego/controller/api/ExchangeRateApiController.java:191`

```java
public record ConversionResult(String from, String to, BigDecimal originalAmount, ...)
```

`ConversionResult` 定義為 Controller 的 inner record。雖然目前只在此處使用，但若日後需要在 Service 層共用會有問題。

**建議**：移至 `dto/response/` 目錄。低優先，目前影響不大。

### 日誌使用

#### :blue_circle: S-07：日誌等級使用適當

所有 Service 和 Controller 均使用 `@Slf4j`：
- `log.debug()` 用於方法進入點和詳細追蹤
- `log.info()` 用於成功的狀態變更操作
- `log.warn()` 用於預期的失敗情境
- `log.error()` 用於非預期的異常

未發現 `System.out.println` 使用。

#### :blue_circle: S-08：部分 catch block 的日誌等級可調整

**檔案**：`src/main/java/com/wego/controller/web/ActivityWebController.java:311-322`

```java
} catch (ResourceNotFoundException e) {
    log.error("Failed to create activity: {}", e.getMessage(), e);
```

業務例外（如 `ResourceNotFoundException`、`ForbiddenException`）使用 `log.error()` + 完整 stack trace 不太適當，應使用 `log.warn()`。

---

## 架構亮點

1. **PermissionChecker**：集中式權限檢查 + Caffeine cache（5s TTL），避免重複 DB 查詢，設計優秀
2. **ViewHelper 模式**：`TripViewHelper`、`ActivityViewHelper`、`ExpenseViewHelper` 將呈現邏輯從 Controller 提取，職責清晰（前次建議已落實）
3. **例外處理雙軌制**：API 回傳 JSON / Web 回傳 ErrorPage，使用 `basePackages` 精確分流
4. **外部 API 抽象**：所有外部服務（Google Maps、天氣、匯率、Gemini、Storage）均使用介面 + Mock 實作
5. **AI Chat 安全設計**：系統提示與用戶資料結構性分離，防止 prompt injection
6. **N+1 問題處理**：`getUserTrips()` 使用 batch query、`mapActivitiesToResponses()` 使用 `buildPlaceLookup()`
7. **ApiResponse 統一格式**：所有 API 回傳 `{ success, data, message, errorCode, timestamp }`
8. **Entity/DTO 完整分離**：每個 Entity 都有對應 Response DTO，使用 `fromEntity()` 靜態工廠方法
9. **Domain 工具類統一**：`GeoUtils` 統一 Haversine 計算、`FileValidationUtils` 統一 magic bytes 驗證（前次建議已落實）
10. **常數統一管理**：`TripConstants` 集中管理跨 Service 共用常數

---

## 架構評分

| 項目 | 前次分數 | 本次分數 | 說明 |
|------|:--------:|:--------:|------|
| 分層架構 | 9 | 9.5 | TripViewHelper 引入後 Controller 職責更清晰 |
| 依賴注入 | 9 | 9 | 持平，BaseWebController field injection 影響小 |
| Entity/DTO 分離 | 8.5 | 9 | PlaceService 已修復，僅剩 DocumentService 1 處 |
| 例外處理 | 9 | 9 | 持平，設計優秀 |
| SOLID/DRY | 7.5 | 8.5 | GeoUtils + FileValidationUtils + TripConstants 消除多處重複 |
| 命名規範 | 9 | 9 | 持平，命名清晰一致 |
| 日誌規範 | 8.5 | 8.5 | 持平，少數日誌等級可調整 |

### **總分：8.9 / 10**（前次 8.6 → +0.3）

---

## 改善優先順序

### 高優先（建議立即處理）
1. **W-01**：`TodoWebController` 使用 `BaseWebController` 的 `findCurrentMember()` 和 `canEdit()` 方法，消除重複
2. **W-02**：`DocumentService.getDocumentForPreview()` 改回傳 DTO

### 中優先（下次迭代處理）
3. **W-03**：提取 `requireUserId()` 至 `BaseApiController` 或改用 `principal.getId()`
4. **S-02**：考慮拆分 `TripService` 的刪除邏輯至 `TripDeletionService`
5. **S-04**：`WebExceptionHandler` 的 generic exception 使用固定訊息

### 低優先（有空時處理）
6. **S-05**：提取 Controller 中的 magic number 為命名常數
7. **S-06**：`ExchangeRateApiController.ConversionResult` 移至 `dto/response/`
8. **S-08**：業務例外的日誌等級從 `error` 調整為 `warn`
