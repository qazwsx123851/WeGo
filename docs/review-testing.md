# WeGo 測試審查報告

> 審查日期：2026-02-20（第三次全面更新）
> 審查範圍：全專案單元測試、整合測試、E2E 測試
> 專案版本：main branch (commit e5eaafc)

---

## 一、測試執行結果摘要

| 指標 | 數值 |
|------|------|
| 測試總數 | **1,175** (補寫後，原 1,152) |
| 通過 | 1,175 |
| 失敗 | 0 |
| 錯誤 | 0 |
| 跳過 | 0 |
| 測試檔案數 | **88** |
| 執行時間 | ~30 秒 |
| JaCoCo 指令覆蓋率 | **76%** |
| JaCoCo 分支覆蓋率 | **58%** |

---

## 二、完整 API 端點清單

### 2.1 REST API 端點 (14 Controllers, 64 端點)

#### HealthController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/health` | 健康檢查 | ✅ |

#### AuthApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/auth/me` | 取得目前使用者 | ✅ |
| POST | `/api/auth/logout` | API 登出 | ✅ |

#### TripApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| POST | `/api/trips` | 建立行程 | ✅ |
| GET | `/api/trips` | 取得使用者行程列表 (分頁) | ✅ |
| GET | `/api/trips/{tripId}` | 取得單一行程 | ✅ |
| PUT | `/api/trips/{tripId}` | 更新行程 | ✅ |
| DELETE | `/api/trips/{tripId}` | 刪除行程 | ✅ |
| GET | `/api/trips/{tripId}/members` | 取得行程成員 | ✅ |
| DELETE | `/api/trips/{tripId}/members/me` | 離開行程 | ✅ |
| DELETE | `/api/trips/{tripId}/members/{userId}` | 移除成員 | ✅ |
| PUT | `/api/trips/{tripId}/members/{userId}/role` | 變更成員角色 | ✅ |
| POST | `/api/trips/{tripId}/invites` | 建立邀請連結 | ✅ |
| GET | `/api/trips/{tripId}/invites` | 取得有效邀請連結 | ✅ |
| POST | `/api/invites/{token}/accept` | 接受邀請 | ✅ |

#### ActivityApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| POST | `/api/trips/{tripId}/activities` | 建立景點 | ✅ |
| GET | `/api/trips/{tripId}/activities` | 取得景點列表 (可依日篩選) | ✅ |
| PUT | `/api/activities/{activityId}` | 更新景點 | ✅ |
| DELETE | `/api/activities/{activityId}` | 刪除景點 | ✅ |
| PUT | `/api/trips/{tripId}/activities/reorder` | 重新排序景點 | ✅ |
| GET | `/api/trips/{tripId}/activities/optimize` | 路線優化建議 | ✅ |
| POST | `/api/trips/{tripId}/activities/apply-optimization` | 套用路線優化 | ✅ |

#### ExpenseApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| POST | `/api/trips/{tripId}/expenses` | 建立支出 | ✅ |
| GET | `/api/trips/{tripId}/expenses` | 取得支出列表 | ✅ |
| PUT | `/api/expenses/{expenseId}` | 更新支出 | ✅ |
| DELETE | `/api/expenses/{expenseId}` | 刪除支出 | ✅ |
| GET | `/api/trips/{tripId}/settlement` | 取得分帳結果 | ✅ |
| PUT | `/api/expense-splits/{splitId}/settle` | 標記已結清 | ✅ |
| PUT | `/api/expense-splits/{splitId}/unsettle` | 取消結清 | ✅ |
| PUT | `/api/trips/{tripId}/settlement/settle` | 批次結清兩人之間 | ✅ |
| PUT | `/api/trips/{tripId}/settlement/unsettle` | 批次取消結清 | ✅ |

#### TodoApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| POST | `/api/trips/{tripId}/todos` | 建立待辦 | ✅ |
| GET | `/api/trips/{tripId}/todos` | 取得待辦列表 | ✅ |
| GET | `/api/trips/{tripId}/todos/{todoId}` | 取得單一待辦 | ✅ |
| PUT | `/api/trips/{tripId}/todos/{todoId}` | 更新待辦 | ✅ |
| DELETE | `/api/trips/{tripId}/todos/{todoId}` | 刪除待辦 | ✅ |
| GET | `/api/trips/{tripId}/todos/stats` | 待辦統計 | ✅ |

#### DocumentApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| POST | `/api/trips/{tripId}/documents` | 上傳文件 | ✅ |
| GET | `/api/trips/{tripId}/documents` | 取得文件列表 | ✅ |
| GET | `/api/trips/{tripId}/documents/{documentId}` | 取得文件詳情 | ✅ |
| GET | `/api/trips/{tripId}/documents/{documentId}/download` | 取得下載 URL | ✅ |
| GET | `/api/trips/{tripId}/documents/{documentId}/preview` | 文件預覽 | ✅ |
| DELETE | `/api/trips/{tripId}/documents/{documentId}` | 刪除文件 | ✅ |
| GET | `/api/trips/{tripId}/documents/storage` | 儲存空間用量 | ✅ |
| GET | `/api/trips/{tripId}/activities/{activityId}/documents` | 景點關聯文件 | ✅ |

#### PlaceApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/places/search` | 搜尋地點 | ✅ |
| GET | `/api/places/{placeId}` | 取得地點詳情 | ✅ |

#### DirectionApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/directions` | 取得導航路線 | ✅ |

#### StatisticsApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/trips/{tripId}/statistics/category` | 分類統計 | ✅ |
| GET | `/api/trips/{tripId}/statistics/trend` | 支出趨勢 | ✅ |
| GET | `/api/trips/{tripId}/statistics/members` | 成員統計 | ✅ |

#### ExchangeRateApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/exchange-rates` | 取得匯率 | ✅ |
| GET | `/api/exchange-rates/latest` | 取得所有匯率 | ✅ |
| GET | `/api/exchange-rates/convert` | 貨幣換算 | ✅ |
| GET | `/api/exchange-rates/currencies` | 支援貨幣列表 | ✅ |

#### WeatherApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/weather` | 取得天氣 (指定日期) | ✅ |
| GET | `/api/weather/forecast` | 取得 5 日預報 | ✅ |

#### ChatApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| POST | `/api/trips/{tripId}/chat` | AI 聊天 | ✅ |

#### PersonalExpenseApiController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/api/trips/{tripId}/personal-expenses` | 個人支出列表 | ✅ |
| POST | `/api/trips/{tripId}/personal-expenses` | 建立個人支出 | ✅ |
| PUT | `/api/trips/{tripId}/personal-expenses/{id}` | 更新個人支出 | ✅ |
| DELETE | `/api/trips/{tripId}/personal-expenses/{id}` | 刪除個人支出 | ✅ |
| GET | `/api/trips/{tripId}/personal-expenses/summary` | 個人支出摘要 | ✅ |
| PUT | `/api/trips/{tripId}/personal-expenses/budget` | 設定個人預算 | ✅ |

### 2.2 Web 端點 (15 Controllers, 40 端點)

#### HomeController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/` | 首頁 | ✅ |
| GET | `/dashboard` | 儀表板 | ✅ |

#### TripController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips` | 行程列表 | ✅ |
| GET | `/trips/create` | 建立行程表單 | ✅ |
| POST | `/trips/create` | 建立行程提交 | ✅ |
| GET | `/trips/{id}` | 行程詳情 | ✅ |
| GET | `/trips/{id}/edit` | 編輯行程表單 | ✅ |
| POST | `/trips/{id}/edit` | 編輯行程提交 | ✅ |

#### ActivityWebController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips/{tripId}/activities` | 景點列表 | ✅ |
| GET | `/trips/{tripId}/activities/{activityId}` | 景點詳情 | ✅ |
| GET | `/trips/{tripId}/activities/new` | 新增景點表單 | ✅ |
| POST | `/trips/{tripId}/activities` | 新增景點提交 | ✅ |
| GET | `/trips/{tripId}/activities/{activityId}/edit` | 編輯景點表單 | ✅ |
| POST | `/trips/{tripId}/activities/{activityId}` | 編輯景點提交 | ✅ |
| POST | `/trips/{tripId}/activities/{activityId}/delete` | 刪除景點 | ✅ |
| GET | `/trips/{tripId}/activities/{activityId}/duplicate` | 複製景點 | ✅ |
| POST | `/trips/{tripId}/recalculate-transport` | 重算交通時間 | ✅ |

#### ExpenseWebController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips/{tripId}/expenses` | 支出列表 | ✅ |
| GET | `/trips/{tripId}/expenses/create` | 新增支出表單 | ✅ |
| POST | `/trips/{tripId}/expenses` | 新增支出提交 | ✅ |
| GET | `/trips/{tripId}/expenses/{expenseId}` | 支出詳情 | ✅ |
| GET | `/trips/{tripId}/expenses/{expenseId}/edit` | 編輯支出表單 | ✅ |
| POST | `/trips/{tripId}/expenses/{expenseId}` | 編輯支出提交 | ✅ |
| GET | `/trips/{tripId}/expenses/statistics` | 支出統計頁 | ✅ |

#### PersonalExpenseWebController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips/{tripId}/personal-expenses/create` | 個人支出建立表單 | ✅ |
| POST | `/trips/{tripId}/personal-expenses` | 個人支出建立提交 | ✅ |
| GET | `/trips/{tripId}/personal-expenses/{id}/edit` | 個人支出編輯表單 | ✅ |
| POST | `/trips/{tripId}/personal-expenses/{id}` | 個人支出編輯提交 | ✅ |

#### TodoWebController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips/{tripId}/todos` | 待辦列表 | ✅ |

#### DocumentWebController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips/{tripId}/documents` | 文件列表 | ✅ |
| GET | `/trips/{tripId}/documents/new` | 文件上傳表單 | ✅ |

#### MemberWebController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips/{tripId}/members` | 成員列表 | ✅ |

#### SettlementWebController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/trips/{tripId}/settlement` | 分帳頁面 | ✅ |

#### InviteController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/invite/{token}` | 邀請頁面 | ✅ |
| POST | `/invite/{token}/accept` | 接受邀請 | ✅ |

#### ProfileController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/profile` | 個人檔案 | ✅ |
| GET | `/profile/edit` | 編輯個人檔案表單 | ✅ |
| POST | `/profile/edit` | 編輯個人檔案提交 | ✅ |

#### GlobalExpenseController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/expenses` | 全域支出總覽 | ✅ |

#### GlobalDocumentController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| GET | `/documents` | 全域文件總覽 | ✅ |

#### ErrorController
| 方法 | 路徑 | 說明 | 測試覆蓋 |
|------|------|------|:--------:|
| ANY | `/error` | 錯誤頁面處理 | ✅ |

---

## 三、測試覆蓋率分析 (JaCoCo)

### 3.1 各套件覆蓋率

| 套件 | 指令覆蓋率 | 分支覆蓋率 | 評級 |
|------|:----------:|:----------:|:----:|
| `domain.geo` | 100% | n/a | A+ |
| `constant` | 100% | n/a | A+ |
| `domain.route` | 99% | 82% | A+ |
| `config` | 98% | 64% | A |
| `domain.permission` | 96% | 50% | A |
| `domain.settlement` | 94% | 83% | A |
| `security` | 90% | 77% | A |
| `domain.file` | 84% | 35% | B+ |
| `controller.api` | 83% | 60% | B+ |
| `service` | 78% | 66% | B |
| `service.external` | 78% | 62% | B |
| `util` | 77% | 87% | B |
| `controller.web` | 72% | 56% | B- |
| `dto.response` | 69% | 31% | C+ |
| `exception` | 69% | 48% | C+ |
| `domain.statistics` | 66% | 39% | C |
| `dto` | 58% | 27% | C- |
| `entity` | 51% | 28% | D+ |
| `dto.request` | 16% | 37% | F |
| **全專案總計** | **76%** | **58%** | **B** |

### 3.2 覆蓋率分析

- **指令覆蓋率 76%** 接近 80% 門檻，考量 DTO/Entity 為 Lombok 自動生成的 getter/setter/builder，實際業務邏輯覆蓋率更高
- **分支覆蓋率 58%** 主要受到 web controller 中大量 null-check/error-handling 分支影響
- `dto.request` 16% 覆蓋率低是因為這些 class 主要為 Lombok `@Data`/`@Builder`，實際邏輯極少
- `entity` 51% 覆蓋率低因為大量 Lombok 生成的方法；本次已補寫 PersonalExpense 和 User 測試

---

## 四、測試檔案清單 (88 檔)

### 4.1 Controller 測試 (28 檔)

| 類型 | 測試檔案 | 被測目標 |
|------|----------|----------|
| API | `HealthControllerTest` | HealthController |
| API | `AuthApiControllerTest` | AuthApiController |
| API | `TripApiControllerTest` | TripApiController |
| API | `ActivityApiControllerTest` | ActivityApiController |
| API | `ExpenseApiControllerTest` | ExpenseApiController |
| API | `TodoApiControllerTest` | TodoApiController |
| API | `DocumentApiControllerTest` | DocumentApiController |
| API | `PlaceApiControllerTest` | PlaceApiController |
| API | `DirectionApiControllerTest` | DirectionApiController |
| API | `StatisticsApiControllerTest` | StatisticsApiController |
| API | `ExchangeRateApiControllerTest` | ExchangeRateApiController |
| API | `WeatherApiControllerTest` | WeatherApiController |
| API | `ChatApiControllerTest` | ChatApiController |
| API | `PersonalExpenseApiControllerTest` | PersonalExpenseApiController |
| Web | `HomeControllerTest` | HomeController |
| Web | `TripControllerTest` | TripController |
| Web | `ActivityWebControllerTest` | ActivityWebController |
| Web | `ExpenseWebControllerTest` | ExpenseWebController |
| Web | `PersonalExpenseWebControllerTest` | PersonalExpenseWebController |
| Web | `TodoWebControllerTest` | TodoWebController |
| Web | `DocumentWebControllerTest` | DocumentWebController |
| Web | `MemberWebControllerTest` | MemberWebController |
| Web | `SettlementWebControllerTest` | SettlementWebController |
| Web | `InviteControllerTest` | InviteController |
| Web | `ProfileControllerTest` | ProfileController |
| Web | `GlobalExpenseControllerTest` | GlobalExpenseController |
| Web | `GlobalDocumentControllerTest` | GlobalDocumentController |
| Web | `ErrorControllerTest` | ErrorController |

### 4.2 Service 測試 (22 檔)

| 測試檔案 | 被測目標 |
|----------|----------|
| `UserServiceTest` | UserService |
| `TripServiceTest` | TripService |
| `ActivityServiceTest` | ActivityService |
| `ExpenseServiceTest` | ExpenseService |
| `SettlementServiceTest` | SettlementService |
| `TodoServiceTest` | TodoService |
| `DocumentServiceTest` | DocumentService |
| `InviteLinkServiceTest` | InviteLinkService |
| `PlaceServiceTest` | PlaceService |
| `ChatServiceTest` | ChatService |
| `WeatherServiceTest` | WeatherService |
| `ExchangeRateServiceTest` | ExchangeRateService |
| `StatisticsServiceTest` | StatisticsService |
| `StatisticsCacheDelegateTest` | StatisticsCacheDelegate |
| `PersonalExpenseServiceTest` | PersonalExpenseService |
| `GlobalExpenseServiceTest` | GlobalExpenseService |
| `GlobalDocumentServiceTest` | GlobalDocumentService |
| `RateLimitServiceTest` | RateLimitService |
| `TransportCalculationServiceTest` | TransportCalculationService |
| `ActivityViewHelperTest` | ActivityViewHelper |
| `ExpenseViewHelperTest` | ExpenseViewHelper |
| `TripViewHelperTest` | TripViewHelper |

### 4.3 External Client 測試 (10 檔)

| 測試檔案 | 被測目標 |
|----------|----------|
| `GoogleMapsClientImplTest` | Google Maps API client |
| `GoogleMapsExceptionTest` | GoogleMapsException |
| `OpenWeatherMapClientTest` | OpenWeatherMap API client |
| `ExchangeRateApiClientTest` | ExchangeRate API client |
| `GeminiClientImplTest` | Gemini AI client |
| `SupabaseStorageClientTest` | Supabase storage client |
| `MockGoogleMapsClientTest` | Mock Google Maps client |
| `MockWeatherClientTest` | Mock weather client |
| `MockExchangeRateClientTest` | Mock exchange rate client |
| `MockGeminiClientTest` | Mock Gemini client |

### 4.4 Entity 測試 (11 檔)

| 測試檔案 | 被測目標 |
|----------|----------|
| `ActivityTest` | Activity entity |
| `DocumentTest` | Document entity |
| `ExpenseTest` | Expense entity |
| `ExpenseSplitTest` | ExpenseSplit entity |
| `InviteLinkTest` | InviteLink entity |
| `PlaceTest` | Place entity |
| `TripTest` | Trip entity |
| `TripMemberTest` | TripMember entity |
| `RoleTest` | Role enum |
| `PersonalExpenseTest` | PersonalExpense entity **(本次新增)** |
| `UserTest` | User entity **(本次新增)** |

### 4.5 Domain/Other 測試 (13 檔)

| 測試檔案 | 被測目標 |
|----------|----------|
| `UserPrincipalTest` | Security principal |
| `CustomOAuth2UserServiceTest` | OAuth2 user service |
| `PermissionCheckerTest` | Permission domain |
| `DebtSimplifierTest` | Settlement domain |
| `RouteOptimizerTest` | Route domain |
| `ExpenseAggregatorTest` | Statistics domain |
| `CurrencyConverterTest` | Currency utility |
| `UserResponseTest` | DTO response |
| `ApiResponseTest` | API response DTO |
| `RateLimitConfigTest` | Config |
| `GlobalExceptionHandlerTest` | Exception handler |
| `ExceptionsTest` | Custom exceptions |

### 4.6 Repository 測試 (4 檔)

| 測試檔案 | 被測目標 |
|----------|----------|
| `UserRepositoryTest` | User repository |
| `TripRepositoryTest` | Trip repository |
| `TripMemberRepositoryTest` | TripMember repository |
| `InviteLinkRepositoryTest` | InviteLink repository |

### 4.7 E2E 測試 (12 spec 檔)

| 測試檔案 | 流程覆蓋 |
|----------|----------|
| `auth.spec.ts` | OAuth2 登入流程 |
| `dashboard.spec.ts` | 儀表板 |
| `trip-crud.spec.ts` | 行程 CRUD |
| `activity-crud.spec.ts` | 景點 CRUD |
| `expense-crud.spec.ts` | 支出 CRUD |
| `todo-crud.spec.ts` | 待辦 CRUD |
| `document.spec.ts` | 文件上傳/下載 |
| `member.spec.ts` | 成員管理 |
| `settlement.spec.ts` | 分帳流程 |
| `invite.spec.ts` | 邀請連結 |
| `personal-expense.spec.ts` | 個人支出 |
| `profile.spec.ts` | 個人檔案 |

---

## 五、核心功能驗證

### 5.1 使用者認證流程
- ✅ OAuth2 登入 (`CustomOAuth2UserServiceTest` - 8 tests)
- ✅ UserPrincipal 安全主體 (`UserPrincipalTest` - 7 tests)
- ✅ API 認證 (`AuthApiControllerTest` - 測試 /me 和 /logout)
- ✅ 權限檢查 (`PermissionCheckerTest` - 15 tests, cache + canView/canEdit)
- ✅ E2E 認證 (`auth.spec.ts`)

### 5.2 行程 CRUD
- ✅ 建立行程 (`TripServiceTest`, `TripApiControllerTest`, `TripControllerTest`)
- ✅ 讀取行程 (分頁查詢、單一查詢)
- ✅ 更新行程 (含封面圖片上傳)
- ✅ 刪除行程 (級聯刪除)
- ✅ 成員管理 (加入/離開/移除/角色變更)
- ✅ E2E (`trip-crud.spec.ts`)

### 5.3 地點搜尋與收藏
- ✅ Google Maps 搜尋 (`PlaceApiControllerTest`, `GoogleMapsClientImplTest`)
- ✅ PlaceService findOrCreate (`PlaceServiceTest`)
- ✅ Mock fallback (`MockGoogleMapsClientTest`)
- ✅ 快取與 Rate Limiting (`PlaceApiControllerTest`)

### 5.4 AI 推薦的 Fallback 機制
- ✅ ChatService prompt assembly (`ChatServiceTest` - 14 tests)
- ✅ GeminiClient circuit breaker (`GeminiClientImplTest` - 10 tests)
- ✅ MockGeminiClient fallback (`MockGeminiClientTest` - 6 tests)
- ✅ Rate limiting (`RateLimitServiceTest`)
- ✅ Input sanitization (ChatService security tests)

### 5.5 個人支出與預算功能
- ✅ PersonalExpenseService (`PersonalExpenseServiceTest` - 全功能覆蓋)
- ✅ PersonalExpenseApiController (`PersonalExpenseApiControllerTest`)
- ✅ PersonalExpenseWebController (`PersonalExpenseWebControllerTest`)
- ✅ Budget 設定與 BudgetStatus 計算
- ✅ AUTO + MANUAL merge sorting
- ✅ PersonalExpense entity (`PersonalExpenseTest` - **本次新增**, 16 tests)
- ✅ E2E (`personal-expense.spec.ts`)

### 5.6 邀請連結功能
- ✅ InviteLinkService (`InviteLinkServiceTest` - 建立/接受/過期/上限)
- ✅ InviteController Web 流程 (`InviteControllerTest`)
- ✅ TripApiController invite endpoints (`TripApiControllerTest`)
- ✅ InviteLinkRepository (`InviteLinkRepositoryTest`)
- ✅ E2E (`invite.spec.ts`)

---

## 六、本次新增測試

### 6.1 `PersonalExpenseTest.java` (16 tests)
**路徑**: `src/test/java/com/wego/entity/PersonalExpenseTest.java`

| 測試群組 | 測試數量 | 覆蓋內容 |
|----------|:--------:|----------|
| Builder Defaults | 4 | 預設 currency (TWD)、createdAt、自訂覆寫、可選欄位 null |
| getAmountInBaseCurrency | 4 | null rate、zero rate、正常匯率、小數精度 |
| Equals & HashCode | 6 | 同 ID 相等、不同 ID、null ID、self、null、異型 |
| toString | 1 | 包含所有關鍵欄位 |
| All Fields | 1 | Builder 全欄位設定驗證 |

### 6.2 `UserTest.java` (7 tests)
**路徑**: `src/test/java/com/wego/entity/UserTest.java`

| 測試群組 | 測試數量 | 覆蓋內容 |
|----------|:--------:|----------|
| Builder Defaults | 3 | 預設 provider (google)、createdAt、自訂覆寫 |
| All Fields | 2 | 全欄位設定、null avatarUrl |
| Setter | 2 | nickname setter、avatarUrl setter |

---

## 七、發現的問題與建議

### 🔴 Critical (0 項)

無嚴重問題。所有 1,175 個測試全部通過，核心業務邏輯均有測試覆蓋。

### 🟡 Warning (3 項)

| # | 問題 | 檔案路徑 | 說明 |
|---|------|----------|------|
| W1 | Entity 套件覆蓋率偏低 (51%) | `src/main/java/com/wego/entity/` | 大部分為 Lombok 生成 code，但部分 entity (如 `Todo`, `TransportWarning`) 有業務方法未被直接測試。建議為有業務邏輯的 entity 方法補寫 unit test |
| W2 | `dto.request` 覆蓋率極低 (16%) | `src/main/java/com/wego/dto/request/` | Request DTO 多為 Lombok @Data，builder 方法在其他測試中間接使用。可視為低風險 |
| W3 | `domain.statistics` 覆蓋率 66% | `src/main/java/com/wego/domain/statistics/` | `CategoryBreakdown`, `MemberStatistics`, `TrendDataPoint` 的部分 builder/getter 未覆蓋。核心聚合邏輯 `ExpenseAggregator` 已有充分測試 |

### 🔵 Suggestion (4 項)

| # | 建議 | 說明 |
|---|------|------|
| S1 | 增加 integration test 層 | 目前 Controller 測試以 `@WebMvcTest` (slice) 和 `@SpringBootTest` (full) 為主，可考慮增加 Service + Repository 整合測試 (使用 `@DataJpaTest`) 驗證 query 正確性 |
| S2 | 增加 edge case 測試 | 如：超大分頁、並發建立行程、極端匯率值、max member limit 邊界、超長描述截斷等 |
| S3 | TestAuthController 覆蓋率 1% | `src/main/java/com/wego/controller/TestAuthController.java` 僅在 E2E 測試中使用，已有 `@Profile({"test", "e2e"})` 限制，不會在 production 啟用 |
| S4 | 考慮加入 mutation testing | 使用 PIT Mutation Testing 驗證測試的有效性，確認測試確實能捕捉到 bug |

---

## 八、測試面向整體評分

| 維度 | 分數 | 說明 |
|------|:----:|------|
| 覆蓋率完整度 | 8.5 | 76% 指令覆蓋率，接近 80% 門檻。22/22 Service、28/28 Controller 均有測試 |
| 測試品質 | 9.0 | 測試結構清晰，使用 `@Nested` 分組、`@DisplayName` 描述、Mockito mock。模式一致 |
| 邊界測試 | 7.5 | 基本正向/反向路徑覆蓋完善，部分邊界情況可加強 |
| E2E 覆蓋 | 8.5 | 12 個 spec 檔覆蓋所有主要使用者流程 |
| 測試可維護性 | 9.0 | 遵循統一模式、共用 setup、ViewHelper 抽取降低 Controller 測試複雜度 |
| 測試速度 | 9.0 | 1,175 個測試 30 秒內完成，Service 測試全部使用 Mockito 無 DB 依賴 |

### 總評分：**8.5 / 10**

專案測試基礎紮實，所有 Controller 和 Service 均有對應測試，E2E 覆蓋主要使用者流程。主要改進空間在於提升 entity/DTO 套件的覆蓋率（多為 Lombok 生成碼）和增加更多邊界情境測試。
