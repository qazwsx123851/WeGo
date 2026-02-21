# WeGo 測試審查報告

> 審查日期：2026-02-21（第四次全面更新）
> 審查範圍：全專案單元測試、整合測試、E2E 測試
> 專案版本：main branch (commit 6141afd)

---

## 一、測試執行結果摘要

| 指標 | 數值 |
|------|------|
| 測試總數 | **1,182** (補寫後，原 1,175) |
| 通過 | 1,182 |
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
- `entity` 51% 覆蓋率低因為大量 Lombok 生成的方法；已補寫 PersonalExpense 和 User 測試

---

## 四、測試檔案清單 (88 檔)

### 4.1 Controller 測試 (28 檔)

| 類型 | 測試檔案 | 被測目標 | 測試數量 |
|------|----------|----------|:--------:|
| API | `HealthControllerTest` | HealthController | 1 |
| API | `AuthApiControllerTest` | AuthApiController | 5 |
| API | `TripApiControllerTest` | TripApiController | 39 |
| API | `ActivityApiControllerTest` | ActivityApiController | 16 |
| API | `ExpenseApiControllerTest` | ExpenseApiController | 18 |
| API | `TodoApiControllerTest` | TodoApiController | 18 |
| API | `DocumentApiControllerTest` | DocumentApiController | 20 |
| API | `PlaceApiControllerTest` | PlaceApiController | 17 |
| API | `DirectionApiControllerTest` | DirectionApiController | 18 |
| API | `StatisticsApiControllerTest` | StatisticsApiController | 14 |
| API | `ExchangeRateApiControllerTest` | ExchangeRateApiController | 18 |
| API | `WeatherApiControllerTest` | WeatherApiController | 10 |
| API | `ChatApiControllerTest` | ChatApiController | 8 |
| API | `PersonalExpenseApiControllerTest` | PersonalExpenseApiController | 9 |
| Web | `HomeControllerTest` | HomeController | 3 |
| Web | `TripControllerTest` | TripController | 16 |
| Web | `ActivityWebControllerTest` | ActivityWebController | 20 |
| Web | `ExpenseWebControllerTest` | ExpenseWebController | 14 |
| Web | `PersonalExpenseWebControllerTest` | PersonalExpenseWebController | **12** |
| Web | `TodoWebControllerTest` | TodoWebController | 3 |
| Web | `DocumentWebControllerTest` | DocumentWebController | 7 |
| Web | `MemberWebControllerTest` | MemberWebController | 6 |
| Web | `SettlementWebControllerTest` | SettlementWebController | 4 |
| Web | `InviteControllerTest` | InviteController | 7 |
| Web | `ProfileControllerTest` | ProfileController | 6 |
| Web | `GlobalExpenseControllerTest` | GlobalExpenseController | 2 |
| Web | `GlobalDocumentControllerTest` | GlobalDocumentController | 2 |
| Web | `ErrorControllerTest` | ErrorController | 6 |

### 4.2 Service 測試 (22 檔)

| 測試檔案 | 被測目標 | 測試數量 |
|----------|----------|:--------:|
| `UserServiceTest` | UserService | 11 |
| `TripServiceTest` | TripService | 16 |
| `ActivityServiceTest` | ActivityService | 32 |
| `ExpenseServiceTest` | ExpenseService | 18 |
| `SettlementServiceTest` | SettlementService | 21 |
| `TodoServiceTest` | TodoService | 32 |
| `DocumentServiceTest` | DocumentService | 31 |
| `InviteLinkServiceTest` | InviteLinkService | 11 |
| `PlaceServiceTest` | PlaceService | 11 |
| `ChatServiceTest` | ChatService | 38 |
| `WeatherServiceTest` | WeatherService | 23 |
| `ExchangeRateServiceTest` | ExchangeRateService | 30 |
| `StatisticsServiceTest` | StatisticsService | 13 |
| `StatisticsCacheDelegateTest` | StatisticsCacheDelegate | 12 |
| `PersonalExpenseServiceTest` | PersonalExpenseService | 31 |
| `GlobalExpenseServiceTest` | GlobalExpenseService | 6 |
| `GlobalDocumentServiceTest` | GlobalDocumentService | 7 |
| `RateLimitServiceTest` | RateLimitService | 8 |
| `TransportCalculationServiceTest` | TransportCalculationService | 26 |
| `ActivityViewHelperTest` | ActivityViewHelper | 17 |
| `ExpenseViewHelperTest` | ExpenseViewHelper | 22 |
| `TripViewHelperTest` | TripViewHelper | 21 |

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
| `PersonalExpenseTest` | PersonalExpense entity |
| `UserTest` | User entity |

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
- ✅ PersonalExpenseWebController (`PersonalExpenseWebControllerTest` - **本次擴充至 12 tests**)
- ✅ Budget 設定與 BudgetStatus 計算
- ✅ AUTO + MANUAL merge sorting
- ✅ PersonalExpense entity (`PersonalExpenseTest` - 16 tests)
- ✅ E2E (`personal-expense.spec.ts`)

### 5.6 邀請連結功能
- ✅ InviteLinkService (`InviteLinkServiceTest` - 建立/接受/過期/上限)
- ✅ InviteController Web 流程 (`InviteControllerTest`)
- ✅ TripApiController invite endpoints (`TripApiControllerTest`)
- ✅ InviteLinkRepository (`InviteLinkRepositoryTest`)
- ✅ E2E (`invite.spec.ts`)

---

## 六、本次新增/擴充測試

### PersonalExpenseWebControllerTest 擴充 (+7 tests，5 → 12)

**路徑**: `src/test/java/com/wego/controller/web/PersonalExpenseWebControllerTest.java`

| 新增測試 | 覆蓋功能 |
|----------|----------|
| `CreatePersonalExpenseSuccess.validData_redirectsToExpensesTab` | POST 建立成功後重導向 |
| `ShowEditForm.showEditForm_shouldReturnView` | GET 編輯表單正常載入既有費用資料 |
| `ShowEditForm.showEditForm_notFound_shouldThrow` | GET 編輯表單費用不存在回傳 404 |
| `UpdatePersonalExpense.validData_redirectsToExpensesTab` | POST 更新成功後重導向 |
| `UpdatePersonalExpense.bindingErrors_returnsEditForm` | POST 更新驗證錯誤返回表單 |
| `UnauthenticatedAccess.createForm_notAuthenticated_shouldRedirect` | 未登入 GET 建立表單重導向 |
| `UnauthenticatedAccess.createSubmit_notAuthenticated_shouldRedirect` | 未登入 POST 建立重導向 |

---

## 七、發現的問題與建議

### 🔴 Critical (0 項)

無嚴重問題。所有 1,182 個測試全部通過，104 個端點 100% 覆蓋，核心業務邏輯均有測試覆蓋。

### 🟡 Warning (0 項，已全數修復)

| # | 問題 | 說明 | 狀態 |
|---|------|------|:----:|
| W1 | PersonalExpenseWebController 缺少成功路徑測試 | 僅有 5 個錯誤路徑測試 | ✅ 已補寫至 12 |
| W2 | PersonalExpenseWebController 缺少未認證測試 | 無未登入重導向測試 | ✅ 已補寫 |
| W3 | PersonalExpenseWebController 缺少編輯表單測試 | 無 GET /{id}/edit 正常路徑 | ✅ 已補寫 |

### 🔵 Suggestion (4 項)

| # | 建議 | 說明 |
|---|------|------|
| S1 | 增加 integration test 層 | 可考慮增加 Service + Repository 整合測試 (`@DataJpaTest`) 驗證 query 正確性 |
| S2 | 增加 edge case 測試 | 超大分頁、並發建立行程、極端匯率值、max member limit 邊界等 |
| S3 | E2E 覆蓋擴充 | Settlement 結算流程可新增更多 E2E 測試場景 |
| S4 | 考慮 mutation testing | 使用 PIT Mutation Testing 驗證測試有效性 |

---

## 八、測試面向整體評分

| 維度 | 分數 | 說明 |
|------|:----:|------|
| 覆蓋率完整度 | 8.5 | 76% 指令覆蓋率，22/22 Service、28/28 Controller 均有測試。104 端點 100% 覆蓋 |
| 測試品質 | 9.0 | 結構清晰（`@Nested` 分組、`@DisplayName`），模式一致，正反路徑皆測 |
| 邊界測試 | 7.5 | 基本正向/反向路徑完善，部分邊界情況可加強 |
| E2E 覆蓋 | 8.5 | 12 個 spec 檔覆蓋所有主要使用者流程 |
| 測試可維護性 | 9.0 | 統一模式、共用 setup、ViewHelper 抽取降低 Controller 測試複雜度 |
| 測試速度 | 9.0 | 1,182 個測試 30 秒內完成，Service 測試全部使用 Mockito 無 DB 依賴 |

### 總評分：**8.5 / 10**

專案測試基礎紮實，所有 Controller 和 Service 均有對應測試，E2E 覆蓋主要使用者流程。主要改進空間在於提升 entity/DTO 套件的覆蓋率（多為 Lombok 生成碼）和增加更多邊界情境測試。

## 問題統計

| 嚴重程度 | 數量 |
|----------|:----:|
| 🔴 Critical | 0 |
| 🟡 Warning | 0（已全數修復） |
| 🔵 Suggestion | 4 |
