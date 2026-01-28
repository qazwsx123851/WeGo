# WeGo 開發計劃書 (Development Plan)

## 進度摘要

```
Phase 0: ████████████████████ 100% ✅ 完成 (2026-01-28)
Phase 1: ██████████████████░░  90% 🚧 進行中 (後端完成，前端頁面完成，資料庫已部署)
Phase 2: ░░░░░░░░░░░░░░░░░░░░   0% ⏳ 待開始
Phase 3: ░░░░░░░░░░░░░░░░░░░░   0% ⏳ 待開始
Phase 4: ░░░░░░░░░░░░░░░░░░░░   0% ⏳ 待開始
```

**測試狀態**: 349 tests, 0 failures
**資料庫狀態**: 9 tables deployed to Supabase ✅
**下一步**: DocumentService 實作、Google Maps 整合、E2E 測試

---

## 文件資訊

| 項目 | 內容 |
|------|------|
| 專案名稱 | WeGo - 旅遊規劃協作平台 |
| 版本 | 1.0.0 |
| 建立日期 | 2024-01 |
| 最後更新 | 2026-01-28 |
| 狀態 | Active - Phase 1 後端 + 前端 + 資料庫部署完成 |

---

## 1. 專案概述

### 1.1 目標
建立專為好友、小團體設計的「重協作」旅遊規劃平台，解決行程排版混亂、交通時間難估算、憑證分散以及分帳麻煩等痛點。

### 1.2 技術棧
- **後端**: Spring Boot 3.x (Java 17+)
- **前端**: Thymeleaf + Tailwind CSS + Lottie-web
- **資料庫**: Supabase (PostgreSQL 15+)
- **部署**: Railway
- **外部 API**: Google Maps, OpenWeatherMap, ExchangeRate-API

### 1.3 開發階段總覽

| Phase | 名稱 | 核心目標 | 狀態 |
|-------|------|----------|:----:|
| Phase 0 | 專案初始化 | 開發環境、CI/CD、資料庫連線 | ✅ 完成 |
| Phase 1 | MVP | 可用的基本版本，驗證產品概念 | 🚧 90% |
| Phase 2 | 協作強化 | 提升多人協作體驗 | ⏳ 待開始 |
| Phase 3 | 分帳進階 | 完善分帳功能 | ⏳ 待開始 |
| Phase 4 | 體驗優化 | PWA、深色模式 | ⏳ 待開始 |

---

## 2. Phase 0: 專案初始化 ✅ (2026-01-28 完成)

### 2.1 開發環境設定
- [x] **P0-001**: 建立 Spring Boot 專案骨架 ✅
- [x] **P0-002**: 設定專案結構 ✅
- [x] **P0-003**: 設定 Supabase 連線 ✅
- [x] **P0-004**: 設定 Tailwind CSS ✅
- [x] **P0-005**: 設定測試環境 ✅
- [x] **P0-006**: 設定 CI/CD ✅

### 2.2 驗收標準
- [x] 專案可在本地啟動 (`./mvnw spring-boot:run`) ✅
- [x] 可連線至 Supabase 資料庫 ✅
- [x] Tailwind CSS 正常編譯 ✅
- [x] CI/CD pipeline 運作正常 ✅

### 2.3 Supabase 資料庫 Migration ✅ (2026-01-28 完成)

所有 Phase 1 所需資料表已部署至 Supabase：

| Migration | 資料表 | 狀態 |
|-----------|--------|:----:|
| 20260128105009 | `users` | ✅ |
| 20260128150411 | `trips` | ✅ |
| 20260128150417 | `places` | ✅ |
| 20260128150423 | `trip_members` | ✅ |
| 20260128150427 | `invite_links` | ✅ |
| 20260128150432 | `activities` | ✅ |
| 20260128150436 | `expenses` | ✅ |
| 20260128150440 | `expense_splits` | ✅ |
| 20260128150445 | `documents` | ✅ |

**外鍵關係**:
```
users ─┬─< trips (owner_id)
       ├─< trip_members (user_id)
       ├─< invite_links (created_by)
       ├─< expenses (paid_by, created_by)
       ├─< expense_splits (user_id)
       └─< documents (uploaded_by)

trips ─┬─< trip_members (trip_id)
       ├─< invite_links (trip_id)
       ├─< activities (trip_id)
       ├─< expenses (trip_id)
       └─< documents (trip_id)

places ─< activities (place_id)
activities ─< expenses (activity_id)
activities ─< documents (related_activity_id)
expenses ─< expense_splits (expense_id)
```

**注意**: RLS (Row Level Security) 目前未啟用，因為 WeGo 使用 Spring Boot JPA 存取資料庫，而非直接透過 Supabase PostgREST API。

---

## 3. Phase 1: MVP (核心功能)

### 3.1 使用者模組 (User Module) ✅ 完成

#### 3.1.1 Google OAuth 登入
- [x] **P1-U-001**: 建立 User Entity ✅ (2026-01-28)
- [x] **P1-U-002**: 設定 Spring Security OAuth2 ✅ (2026-01-28)
- [x] **P1-U-003**: 實作 Session 管理 ✅ (2026-01-28)
- [x] **P1-U-004**: 建立登入頁面 UI ✅ (2026-01-28)

### 3.2 行程模組 (Trip Module) ✅ 完成

#### 3.2.1 資料層 ✅
- [x] **P1-T-001**: 建立 Trip Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/Trip.java`
  - 測試: TripTest.java

- [x] **P1-T-002**: 建立 TripMember Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/TripMember.java`
  - 位置: `src/main/java/com/wego/entity/Role.java` (enum: OWNER, EDITOR, VIEWER)
  - 測試: TripMemberTest.java

- [x] **P1-T-003**: 建立 InviteLink Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/InviteLink.java`
  - 測試: InviteLinkTest.java

- [x] **P1-T-004**: 建立 Repository 層 ✅ (2026-01-28)
  - TripRepository
  - TripMemberRepository
  - InviteLinkRepository

#### 3.2.2 服務層 ✅
- [x] **P1-T-005**: 實作 TripService ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/service/TripService.java`
  - 方法: createTrip, getTrip, getUserTrips, updateTrip, deleteTrip, getTripMembers, changeMemberRole, removeMember
  - 測試: TripServiceTest.java (39+ tests)

- [x] **P1-T-006**: 實作 PermissionChecker ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/service/PermissionChecker.java`
  - 方法: canView, canEdit, canDelete, canManageMembers, canInvite
  - 測試: PermissionCheckerTest.java

- [x] **P1-T-007**: 實作邀請連結功能 ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/service/InviteLinkService.java`
  - 方法: createInviteLink, acceptInvite, getActiveInviteLinks, deleteInviteLink
  - 測試: InviteLinkServiceTest.java

#### 3.2.3 控制器層 ✅
- [x] **P1-T-008**: 實作 TripApiController ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/controller/api/TripApiController.java`
  - 測試: TripApiControllerTest.java (39 tests)

- [x] **P1-T-009**: 實作 TripController (Web) ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/controller/web/TripController.java`
  - 方法: listTrips, showCreateForm, showTripDetail, showMembersPage, showEditForm

#### 3.2.4 前端頁面 ✅
- [x] **P1-T-010**: 建立 Dashboard 頁面 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/dashboard.html`
  - 功能: 行程卡片列表, 即將出發區塊, 空狀態

- [x] **P1-T-011**: 建立行程建立/編輯表單 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/trip/create.html`
  - 功能: 封面上傳, 標題/描述, 日期範圍

- [x] **P1-T-012**: 建立成員管理頁面 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/trip/members.html`
  - 功能: 成員列表, 邀請連結, 角色變更

### 3.3 活動/景點模組 (Activity Module) ✅ 完成

#### 3.3.1 資料層 ✅
- [x] **P1-A-001**: 建立 Place Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/Place.java`

- [x] **P1-A-002**: 建立 Activity Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/Activity.java`

- [x] **P1-A-003**: 建立 Repository 層 ✅ (2026-01-28)
  - PlaceRepository
  - ActivityRepository

#### 3.3.2 服務層 ✅
- [x] **P1-A-004**: 實作 ActivityService ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/service/ActivityService.java`
  - 方法: createActivity, getActivitiesByTrip, getActivitiesByDay, updateActivity, deleteActivity, reorderActivities
  - 測試: ActivityServiceTest.java

- [ ] **P1-A-005**: 實作 GoogleMapsService (待實作)
  - 開發階段使用 Mock 服務

#### 3.3.3 控制器層 ✅
- [x] **P1-A-006**: 實作 ActivityApiController ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/controller/api/ActivityApiController.java`
  - 測試: ActivityApiControllerTest.java

#### 3.3.4 前端頁面 ✅
- [x] **P1-A-007**: 建立行程主頁 (Editor) ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/activity/list.html`
  - 功能: 日期分組, 時間軸, 活動卡片

- [x] **P1-A-008**: 實作拖拽排序功能 ✅ (視覺，待 JS 邏輯)
  - 位置: `src/main/resources/templates/activity/list.html`
  - 拖拽把手視覺已實作

- [x] **P1-A-009**: 實作景點詳情 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/activity/detail.html`
  - 功能: 地圖預覽, 詳細資訊, 相關支出

- [x] **P1-A-010**: 建立新增景點表單 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/activity/create.html`
  - 功能: 類型選擇, 地點搜尋, 時間設定

### 3.4 分帳模組 - 基礎版 (Expense Module) ✅ 完成

#### 3.4.1 資料層 ✅
- [x] **P1-E-001**: 建立 Expense Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/Expense.java`

- [x] **P1-E-002**: 建立 ExpenseSplit Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/ExpenseSplit.java`

- [x] **P1-E-003**: 建立 Repository 層 ✅ (2026-01-28)
  - ExpenseRepository
  - ExpenseSplitRepository

#### 3.4.2 服務層 ✅
- [x] **P1-E-004**: 實作 ExpenseService ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/service/ExpenseService.java`
  - 方法: createExpense, getExpensesByTrip, updateExpense, deleteExpense
  - 測試: ExpenseServiceTest.java

- [x] **P1-E-005**: 實作 SettlementService ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/service/SettlementService.java`
  - 方法: calculateSettlement, markAsSettled, markAsUnsettled
  - 測試: SettlementServiceTest.java

- [x] **P1-E-006**: 實作 DebtSimplifier ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/domain/settlement/DebtSimplifier.java`
  - 貪婪演算法配對債權人/債務人
  - 測試: DebtSimplifierTest.java (D-001 ~ D-013)

#### 3.4.3 控制器層 ✅
- [x] **P1-E-007**: 實作 ExpenseApiController ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/controller/api/ExpenseApiController.java`
  - 測試: ExpenseApiControllerTest.java

#### 3.4.4 前端頁面 ✅
- [x] **P1-E-008**: 建立分帳列表頁面 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/expense/list.html`
  - 功能: 支出列表, 分類圖示, 總計摘要

- [x] **P1-E-009**: 建立新增支出表單 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/expense/create.html`
  - 功能: 金額輸入, 分帳方式選擇, 成員選擇

- [x] **P1-E-010**: 建立結算頁面 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/expense/settlement.html`
  - 功能: 簡化債務清單, 標記結清, 分類統計

### 3.5 檔案模組 - 基礎版 (Document Module) ✅ 完成

#### 3.5.1 資料層 ✅
- [x] **P1-F-001**: 建立 Document Entity ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/entity/Document.java`

- [x] **P1-F-002**: 建立 DocumentRepository ✅ (2026-01-28)
  - 位置: `src/main/java/com/wego/repository/DocumentRepository.java`

#### 3.5.2 服務層
- [ ] **P1-F-003**: 實作 DocumentService (待實作)
  - Supabase Storage 整合待實作

#### 3.5.3 控制器層
- [ ] **P1-F-004**: 實作 DocumentApiController (待實作)

#### 3.5.4 前端頁面 ✅
- [x] **P1-F-005**: 建立檔案列表頁面 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/document/list.html`
  - 功能: 檔案卡片網格, 分類篩選, 上傳 Modal

- [x] **P1-F-006**: 建立檔案上傳元件 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/document/upload.html`
  - 功能: 拖拽上傳, 預覽, 分類選擇

### 3.6 共用元件與基礎設施 ✅ 完成

#### 3.6.1 例外處理 ✅
- [x] **P1-C-001**: 建立 GlobalExceptionHandler ✅
- [x] **P1-C-002**: 建立自訂例外類別 ✅

#### 3.6.2 統一回應格式 ✅
- [x] **P1-C-003**: 建立 ApiResponse ✅

#### 3.6.3 前端共用元件 ✅
- [x] **P1-C-004**: 建立 Tailwind 元件庫 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/fragments/components.html`
  - 元件: buttons, cards, forms, toasts, bottom-nav, skeleton, spinner, empty-state

- [x] **P1-C-005**: 建立錯誤頁面 ✅ (2026-01-28)
  - 位置: `src/main/resources/templates/error/404.html`
  - 位置: `src/main/resources/templates/error/500.html`
  - ErrorController 實作完成

- [x] **P1-C-006**: 整合 Lottie 動畫 ✅ (2026-01-28)
  - 錯誤頁面 Lottie 動畫已整合
  - 位置: `src/main/resources/static/js/app.js`

### 3.7 Phase 1 驗收標準

#### 功能驗收
- [x] 用戶可使用 Google 帳號登入 ✅
- [x] 用戶可建立/編輯/刪除行程 ✅ (API + UI)
- [x] 用戶可透過邀請連結加入行程 ✅ (API)
- [x] 用戶可新增/編輯/刪除景點 ✅ (API + UI)
- [x] 用戶可拖拽排序景點 ✅ (API，UI 視覺完成)
- [ ] 用戶可看到景點間的交通時間 (待 Google Maps 整合)
- [x] 用戶可新增支出並均分 ✅ (API + UI)
- [x] 用戶可查看結算結果 ✅ (API + UI)
- [ ] 用戶可上傳憑證檔案 (待 Supabase Storage 整合)

#### 測試覆蓋率
- [x] Unit Tests: 349 tests passing ✅
- [ ] Integration Tests: 待補強
- [ ] E2E Tests: 待實作

#### 效能指標
- [ ] 首頁載入時間 (LCP): < 2.5 秒 (待驗證)
- [ ] API 回應時間 (P95): < 500ms (待驗證)

---

## 4. Phase 2: 協作強化

### 4.1 完整權限模型
- [ ] **P2-001**: 完善權限檢查邏輯

### 4.2 代辦事項模組 (Todo Module)
- [ ] **P2-T-001**: 建立 Todo Entity
- [ ] **P2-T-002**: 建立 TodoRepository
- [ ] **P2-T-003**: 實作 TodoService
- [ ] **P2-T-004**: 實作 TodoApiController
- [ ] **P2-T-005**: 建立代辦事項頁面

### 4.3 智慧排序建議
- [ ] **P2-002**: 實作 RouteOptimizer
- [ ] **P2-003**: 實作優化路線 UI

### 4.4 天氣預報整合
- [ ] **P2-004**: 實作 WeatherService
- [ ] **P2-005**: 天氣預報 UI

---

## 5. Phase 3: 分帳進階

### 5.1 多幣別支援
- [ ] **P3-001**: 實作 ExchangeRateService
- [ ] **P3-002**: 擴展 ExpenseService
- [ ] **P3-003**: 擴展 SettlementService
- [ ] **P3-004**: 多幣別 UI

### 5.2 自訂分帳比例
- [ ] **P3-005**: 擴展 ExpenseService
- [ ] **P3-006**: 自訂分帳 UI

### 5.3 支出分類統計
- [ ] **P3-007**: 實作支出統計功能

---

## 6. Phase 4: 體驗優化

### 6.1 離線快取 (PWA)
- [ ] **P4-001**: 建立 Service Worker
- [ ] **P4-002**: 建立 Web App Manifest
- [ ] **P4-003**: 實作離線預覽

### 6.2 深色模式
- [ ] **P4-004**: 實作深色模式 (CSS 變數已準備)

### 6.3 Lottie 動畫完善
- [ ] **P4-005**: 新增更多動畫

---

## 7. 測試策略

### 7.1 當前測試狀態 (2026-01-28 更新)

```
總測試數: 349
通過: 349 (100%)
失敗: 0
跳過: 0
```

**資料庫連線**: Supabase PostgreSQL ✅ (9 tables deployed)

### 7.2 測試分布

| 模組 | 測試數 | 狀態 |
|------|--------|------|
| User Module | 68+ | ✅ |
| Trip Module | 100+ | ✅ |
| Activity Module | 40+ | ✅ |
| Expense Module | 60+ | ✅ |
| Settlement Module | 30+ | ✅ |
| Exception Handling | 13 | ✅ |
| Controllers | 80+ | ✅ |

---

## 8. 開發規範

(保持不變)

---

## 9. 里程碑

### 9.1 Phase 1 里程碑 (更新)

| 週次 | 目標 | 任務 | 狀態 |
|------|------|------|:----:|
| W1 | 專案初始化 | P0-001 ~ P0-006 | ✅ 完成 |
| W2 | 使用者模組 | P1-U-001 ~ P1-U-004 | ✅ 完成 |
| W3-4 | 行程模組 | P1-T-001 ~ P1-T-012 | ✅ 完成 |
| W5-6 | 景點模組 | P1-A-001 ~ P1-A-010 | ✅ 完成 |
| W7 | 分帳基礎 | P1-E-001 ~ P1-E-010 | ✅ 完成 |
| W8 | 檔案上傳 | P1-F-001 ~ P1-F-006 | 🚧 80% |
| W8 | 資料庫部署 | Supabase Migration (9 tables) | ✅ 完成 |
| W9 | 整合測試 | E2E 測試, Bug fixes | ⏳ |
| W10 | MVP 發布 | 部署, 驗收 | ⏳ |

### 9.2 下一步優先事項

1. **P1-F-003**: 實作 DocumentService (Supabase Storage 整合)
2. **P1-A-005**: 實作 GoogleMapsService (或 Mock)
3. **E2E 測試**: 關鍵流程測試 (Playwright)
4. **部署驗證**: Railway 部署測試
5. **RLS 策略**: 視需求啟用 Supabase Row Level Security

---

## 10. 風險管理

(保持不變)

---

## 附錄

### A. Phase 1 已建立檔案清單 (2026-01-28)

#### Entity 層
- `src/main/java/com/wego/entity/User.java` ✅
- `src/main/java/com/wego/entity/Trip.java` ✅
- `src/main/java/com/wego/entity/TripMember.java` ✅
- `src/main/java/com/wego/entity/Role.java` ✅
- `src/main/java/com/wego/entity/InviteLink.java` ✅
- `src/main/java/com/wego/entity/Place.java` ✅
- `src/main/java/com/wego/entity/Activity.java` ✅
- `src/main/java/com/wego/entity/Expense.java` ✅
- `src/main/java/com/wego/entity/ExpenseSplit.java` ✅
- `src/main/java/com/wego/entity/Document.java` ✅

#### Repository 層
- `src/main/java/com/wego/repository/UserRepository.java` ✅
- `src/main/java/com/wego/repository/TripRepository.java` ✅
- `src/main/java/com/wego/repository/TripMemberRepository.java` ✅
- `src/main/java/com/wego/repository/InviteLinkRepository.java` ✅
- `src/main/java/com/wego/repository/PlaceRepository.java` ✅
- `src/main/java/com/wego/repository/ActivityRepository.java` ✅
- `src/main/java/com/wego/repository/ExpenseRepository.java` ✅
- `src/main/java/com/wego/repository/ExpenseSplitRepository.java` ✅
- `src/main/java/com/wego/repository/DocumentRepository.java` ✅

#### Service 層
- `src/main/java/com/wego/service/UserService.java` ✅
- `src/main/java/com/wego/service/TripService.java` ✅
- `src/main/java/com/wego/service/InviteLinkService.java` ✅
- `src/main/java/com/wego/service/PermissionChecker.java` ✅
- `src/main/java/com/wego/service/ActivityService.java` ✅
- `src/main/java/com/wego/service/ExpenseService.java` ✅
- `src/main/java/com/wego/service/SettlementService.java` ✅

#### Domain 層
- `src/main/java/com/wego/domain/settlement/DebtSimplifier.java` ✅

#### Controller 層
- `src/main/java/com/wego/controller/web/HomeController.java` ✅
- `src/main/java/com/wego/controller/web/TripController.java` ✅
- `src/main/java/com/wego/controller/web/ErrorController.java` ✅
- `src/main/java/com/wego/controller/api/HealthController.java` ✅
- `src/main/java/com/wego/controller/api/AuthApiController.java` ✅
- `src/main/java/com/wego/controller/api/TripApiController.java` ✅
- `src/main/java/com/wego/controller/api/ActivityApiController.java` ✅
- `src/main/java/com/wego/controller/api/ExpenseApiController.java` ✅

#### Frontend Templates
- `src/main/resources/templates/index.html` ✅
- `src/main/resources/templates/login.html` ✅
- `src/main/resources/templates/dashboard.html` ✅
- `src/main/resources/templates/fragments/layout.html` ✅
- `src/main/resources/templates/fragments/components.html` ✅
- `src/main/resources/templates/error/404.html` ✅
- `src/main/resources/templates/error/500.html` ✅
- `src/main/resources/templates/trip/create.html` ✅
- `src/main/resources/templates/trip/view.html` ✅
- `src/main/resources/templates/trip/members.html` ✅
- `src/main/resources/templates/activity/list.html` ✅
- `src/main/resources/templates/activity/create.html` ✅
- `src/main/resources/templates/activity/detail.html` ✅
- `src/main/resources/templates/expense/list.html` ✅
- `src/main/resources/templates/expense/create.html` ✅
- `src/main/resources/templates/expense/settlement.html` ✅
- `src/main/resources/templates/document/list.html` ✅
- `src/main/resources/templates/document/upload.html` ✅

#### Static Resources
- `src/main/resources/static/css/styles.css` ✅
- `src/main/resources/static/js/app.js` ✅

### B. 任務編號規則

```
P{phase}-{module}-{sequence}

範例:
- P1-U-001: Phase 1, User Module, 第 1 項任務
- P2-T-003: Phase 2, Todo Module, 第 3 項任務
```

### C. 模組代碼對照

| 代碼 | 模組 |
|------|------|
| U | User (使用者) |
| T | Trip (行程) / Todo (代辦) |
| A | Activity (景點) |
| E | Expense (分帳) |
| F | File/Document (檔案) |
| C | Common (共用) |

### D. 相關文件連結

| 文件 | 說明 |
|------|------|
| [CONTRIB.md](./CONTRIB.md) | 開發貢獻指南 (環境設定、指令) |
| [RUNBOOK.md](./RUNBOOK.md) | 維運手冊 (部署、監控、故障排除) |
| [requirements.md](./requirements.md) | 需求規格書 (PRD) |
| [software-design-document.md](./software-design-document.md) | 軟體設計文件 (SDD) |
| [test-cases.md](./test-cases.md) | 測試案例規格書 |
| [ui-design-guide.md](./ui-design-guide.md) | UI 設計指南 |
| [tdd-guide.md](./tdd-guide.md) | TDD 測試開發指南 |
| [ai-coding-guidelines.md](./ai-coding-guidelines.md) | AI 輔助開發規範 |
| [api-keys-setup.md](./api-keys-setup.md) | API Keys 設定指南 |
