# WeGo 開發計劃書 (Development Plan)

> 最後更新: 2026-02-12

## 進度摘要

```
Phase 0: ████████████████████ 100% ✅ 完成 (2026-01-28)
Phase 1: ████████████████████ 100% ✅ 完成 (2026-01-30)
Phase 2: ████████████████████ 100% ✅ 完成 (2026-02-01)
Phase 3: ████████████████████ 100% ✅ 完成 (2026-02-03)
Phase 4: ████████████████████ 100% ✅ 完成 (2026-02-04)
```

**測試狀態**: ~864 unit tests + ~118 E2E tests, 0 failures
**資料庫狀態**: 10 tables deployed to Supabase ✅
**Storage 狀態**: documents + trip-covers buckets ✅

### Phase 4 完成功能 (2026-02-04)
- [x] 安全修復: 認證繞過、IDOR、XSS、Rate Limiting
- [x] 深色模式: Navbar 切換、FOUC 防護、Chart.js 主題
- [x] E2E 測試: Playwright ~118 tests (auth, trip, activity, expense, document, todo, dark-mode, member, settlement, profile)
- [x] 無障礙支援: iframe title、image alt text

### Phase 3 完成功能 (2026-02-03)
- [x] 多幣別支援: 即時匯率轉換 (8 種貨幣)
- [x] 統計圖表: 類別分析、趨勢圖、成員統計 (Chart.js)
- [x] 債務簡化演算法優化
- [x] 快取機制: Caffeine + 資料庫索引

### Phase 2 完成功能 (2026-02-01)
- [x] Transport Mode 系統: 7 種交通模式、來源追蹤、警告提示
- [x] 批次重算交通時間 (含 Lottie 動畫)
- [x] Activity 拖曳重排功能
- [x] Activity 編輯/更新功能
- [x] 全域支出概覽頁面 (`/expenses`)
- [x] 全域文件概覽頁面 (`/documents`)
- [x] 個人檔案頁面 (`/profile`)
- [x] 統一錯誤處理 (WebExceptionHandler + error.html)
- [x] Todo 代辦事項功能
- [x] RouteOptimizer 路線優化
- [x] WeatherService 天氣預報

---

## 文件資訊

| 項目 | 內容 |
|------|------|
| 專案名稱 | WeGo - 旅遊規劃協作平台 |
| 版本 | 1.0.0 |
| 建立日期 | 2024-01 |
| 最後更新 | 2026-02-12 |
| 狀態 | Complete - 所有 Phase (0-4) 已完成 |

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
| Phase 1 | MVP | 可用的基本版本，驗證產品概念 | ✅ 完成 |
| Phase 2 | 協作強化 | 提升多人協作體驗 | ✅ 完成 |
| Phase 3 | 分帳進階 | 多幣別、統計圖表、債務簡化 | ✅ 完成 |
| Phase 4 | 品質提升 | 安全強化、深色模式、E2E 測試 | ✅ 完成 |

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

### 2.4 Supabase Storage ✅ (2026-01-29 完成)

已建立 `documents` storage bucket：

| 屬性 | 值 |
|------|-----|
| Bucket Name | `documents` |
| Public | `false` (私有) |
| File Size Limit | 10 MB |
| Allowed MIME Types | PDF, JPEG, PNG, HEIC |

**存取方式**: 後端使用 `service_role` key 存取，透過 signed URL 提供檔案下載。

Storage RLS 政策為選用設定，可透過 Supabase Dashboard 配置。政策包含三項：允許有編輯權限的成員上傳、允許成員讀取、允許上傳者或擁有者刪除。

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
- [x] **P1-T-002**: 建立 TripMember Entity ✅ (2026-01-28)
- [x] **P1-T-003**: 建立 InviteLink Entity ✅ (2026-01-28)
- [x] **P1-T-004**: 建立 Repository 層 ✅ (2026-01-28)

#### 3.2.2 服務層 ✅
- [x] **P1-T-005**: 實作 TripService ✅ (2026-01-28)
- [x] **P1-T-006**: 實作 PermissionChecker ✅ (2026-01-28)
- [x] **P1-T-007**: 實作邀請連結功能 ✅ (2026-01-28)

#### 3.2.3 控制器層 ✅
- [x] **P1-T-008**: 實作 TripApiController ✅ (2026-01-28)
- [x] **P1-T-009**: 實作 TripController (Web) ✅ (2026-01-28)

#### 3.2.4 前端頁面 ✅
- [x] **P1-T-010**: 建立 Dashboard 頁面 ✅ (2026-01-28)
- [x] **P1-T-011**: 建立行程建立/編輯表單 ✅ (2026-01-28)
- [x] **P1-T-012**: 建立成員管理頁面 ✅ (2026-01-28)

### 3.3 活動/景點模組 (Activity Module) ✅ 完成

#### 3.3.1 資料層 ✅
- [x] **P1-A-001**: 建立 Place Entity ✅ (2026-01-28)
- [x] **P1-A-002**: 建立 Activity Entity ✅ (2026-01-28)
- [x] **P1-A-003**: 建立 Repository 層 ✅ (2026-01-28)

#### 3.3.2 服務層 ✅
- [x] **P1-A-004**: 實作 ActivityService ✅ (2026-01-28)
- [ ] **P1-A-005**: 實作 GoogleMapsService (待實作)
  - 開發階段使用 Mock 服務

#### 3.3.3 控制器層 ✅
- [x] **P1-A-006**: 實作 ActivityApiController ✅ (2026-01-28)

#### 3.3.4 前端頁面 ✅
- [x] **P1-A-007**: 建立行程主頁 (Editor) ✅ (2026-01-28)
- [x] **P1-A-008**: 實作拖拽排序功能 ✅
- [x] **P1-A-009**: 實作景點詳情 ✅ (2026-01-28)
- [x] **P1-A-010**: 建立新增景點表單 ✅ (2026-01-28)

### 3.4 分帳模組 - 基礎版 (Expense Module) ✅ 完成

#### 3.4.1 資料層 ✅
- [x] **P1-E-001**: 建立 Expense Entity ✅ (2026-01-28)
- [x] **P1-E-002**: 建立 ExpenseSplit Entity ✅ (2026-01-28)
- [x] **P1-E-003**: 建立 Repository 層 ✅ (2026-01-28)

#### 3.4.2 服務層 ✅
- [x] **P1-E-004**: 實作 ExpenseService ✅ (2026-01-28)
- [x] **P1-E-005**: 實作 SettlementService ✅ (2026-01-28)
- [x] **P1-E-006**: 實作 DebtSimplifier ✅ (2026-01-28)

#### 3.4.3 控制器層 ✅
- [x] **P1-E-007**: 實作 ExpenseApiController ✅ (2026-01-28)

#### 3.4.4 前端頁面 ✅
- [x] **P1-E-008**: 建立分帳列表頁面 ✅ (2026-01-28)
- [x] **P1-E-009**: 建立新增支出表單 ✅ (2026-01-28)
- [x] **P1-E-010**: 建立結算頁面 ✅ (2026-01-28)

### 3.5 檔案模組 - 基礎版 (Document Module) ✅ 完成

#### 3.5.1 資料層 ✅
- [x] **P1-F-001**: 建立 Document Entity ✅ (2026-01-28)
- [x] **P1-F-002**: 建立 DocumentRepository ✅ (2026-01-28)

#### 3.5.2 服務層 ✅
- [x] **P1-F-003**: 實作 DocumentService ✅ (2026-01-29)
- [x] **P1-F-007**: 實作 Storage 基礎設施 ✅ (2026-01-29)

#### 3.5.3 控制器層 ✅
- [x] **P1-F-004**: 實作 DocumentApiController ✅ (2026-01-29)

#### 3.5.4 前端頁面 ✅
- [x] **P1-F-005**: 建立檔案列表頁面 ✅ (2026-01-28)
- [x] **P1-F-006**: 建立檔案上傳元件 ✅ (2026-01-28)

### 3.6 共用元件與基礎設施 ✅ 完成

#### 3.6.1 例外處理 ✅
- [x] **P1-C-001**: 建立 GlobalExceptionHandler ✅
- [x] **P1-C-002**: 建立自訂例外類別 ✅

#### 3.6.2 統一回應格式 ✅
- [x] **P1-C-003**: 建立 ApiResponse ✅

#### 3.6.3 前端共用元件 ✅
- [x] **P1-C-004**: 建立 Tailwind 元件庫 ✅ (2026-01-28)
- [x] **P1-C-005**: 建立錯誤頁面 ✅ (2026-01-28)
- [x] **P1-C-006**: 整合 Lottie 動畫 ✅ (2026-01-28)

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
- [x] 用戶可上傳憑證檔案 ✅ (API + UI，Supabase Storage 整合完成)

#### 測試覆蓋率
- [x] Unit Tests: 396 tests passing ✅
- [ ] Integration Tests: 待補強
- [x] E2E Tests: 76 tests passing (156 skipped - 需認證) ✅

#### 效能指標
- [ ] 首頁載入時間 (LCP): < 2.5 秒 (待驗證)
- [ ] API 回應時間 (P95): < 500ms (待驗證)

---

## 4. Phase 2: 協作強化 ✅ 完成

### 4.1 完整權限模型
- [x] **P2-001**: 完善權限檢查邏輯 ✅

### 4.2 代辦事項模組 (Todo Module)
- [x] **P2-T-001**: 建立 Todo Entity ✅
- [x] **P2-T-002**: 建立 TodoRepository ✅
- [x] **P2-T-003**: 實作 TodoService ✅
- [x] **P2-T-004**: 實作 TodoApiController ✅
- [x] **P2-T-005**: 建立代辦事項頁面 ✅

### 4.3 智慧排序建議
- [x] **P2-002**: 實作 RouteOptimizer ✅
- [x] **P2-003**: 實作優化路線 UI ✅

### 4.4 天氣預報整合
- [x] **P2-004**: 實作 WeatherService ✅
- [x] **P2-005**: 天氣預報 UI ✅

---

## 5. Phase 3: 分帳進階 ✅ 完成

### 5.1 多幣別支援
- [x] **P3-001**: 實作 ExchangeRateService ✅
- [x] **P3-002**: 擴展 ExpenseService ✅
- [x] **P3-003**: 擴展 SettlementService ✅
- [x] **P3-004**: 多幣別 UI ✅

### 5.2 自訂分帳比例
- [x] **P3-005**: 擴展 ExpenseService ✅
- [x] **P3-006**: 自訂分帳 UI ✅

### 5.3 支出分類統計
- [x] **P3-007**: 實作支出統計功能 (Chart.js) ✅

---

## 6. Phase 4: 品質提升 ✅ 完成

### 6.1 安全強化
- [x] **P4-SEC-001**: 認證繞過修復 (4 controllers) ✅
- [x] **P4-SEC-002**: IDOR 漏洞修復 ✅
- [x] **P4-SEC-003**: XSS 漏洞修復 ✅
- [x] **P4-SEC-004**: Rate Limiting (Bucket4j + Caffeine) ✅

### 6.2 深色模式
- [x] **P4-DARK-001**: Navbar 切換按鈕 ✅
- [x] **P4-DARK-002**: FOUC 防護 ✅
- [x] **P4-DARK-003**: Chart.js 主題響應 ✅

### 6.3 E2E 測試
- [x] **P4-E2E-001**: Playwright 環境建置 ✅
- [x] **P4-E2E-002**: OAuth Mock 機制 ✅
- [x] **P4-E2E-003**: ~118 tests 通過 ✅

### 6.4 無障礙支援
- [x] **P4-A11Y-001**: WCAG 2.1 審計 ✅
- [x] **P4-A11Y-002**: Image alt text ✅
- [x] **P4-A11Y-003**: Iframe title ✅

---

## 7. 測試策略

### 7.1 當前測試狀態 (2026-02-12 更新)

單元測試: ~864 個測試方法，58 個測試檔案 (100% 通過)
E2E 測試: ~118 個測試案例，10 個 spec 檔案 (100% 通過)
總測試數: ~982

**資料庫連線**: Supabase PostgreSQL ✅ (10 tables deployed)
**Supabase Storage**: documents + trip-covers buckets ✅

### 7.2 測試分布

| 模組 | 單元測試 | E2E 測試 | 狀態 |
|------|----------|----------|------|
| User Module | 68+ | - | ✅ |
| Trip Module | 100+ | 18 | ✅ |
| Activity Module | 40+ | 18 | ✅ |
| Expense Module | 60+ | 12 | ✅ |
| Settlement Module | 30+ | 6 | ✅ |
| Document Module | 47 | 15 | ✅ |
| Todo Module | 30+ | 14 | ✅ |
| Auth/Security | 50+ | 26 | ✅ |
| Dark Mode | - | 28 | ✅ |
| Member Module | - | 8 | ✅ |
| Profile Module | - | 5 | ✅ |
| Exception Handling | 13 | - | ✅ |
| Controllers | 100+ | - | ✅ |

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
| W8 | 檔案上傳 | P1-F-001 ~ P1-F-007 | ✅ 完成 |
| W8 | 資料庫部署 | Supabase Migration (9 tables) | ✅ 完成 |
| W9 | 整合測試 | E2E 測試, Bug fixes | ✅ 完成 |
| W10 | MVP 發布 | 部署, 驗收 | ⏳ |

### 9.2 專案完成狀態

所有 Phase (0-4) 已完成。主要成就：
- ✅ OAuth 登入、行程/景點/分帳/文件 CRUD
- ✅ 多人協作、權限模型、邀請連結
- ✅ 多幣別匯率、統計圖表
- ✅ 安全強化、深色模式、E2E 測試
- ✅ ~982 tests (~864 unit + ~118 E2E)

---

## 10. 風險管理

(保持不變)

---

## 附錄

### A. Phase 1 已建立檔案清單 (2026-01-28)

#### Entity 層 (10 個)
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

#### Repository 層 (10 個)
- `src/main/java/com/wego/repository/UserRepository.java` ✅
- `src/main/java/com/wego/repository/TripRepository.java` ✅
- `src/main/java/com/wego/repository/TripMemberRepository.java` ✅
- `src/main/java/com/wego/repository/InviteLinkRepository.java` ✅
- `src/main/java/com/wego/repository/PlaceRepository.java` ✅
- `src/main/java/com/wego/repository/ActivityRepository.java` ✅
- `src/main/java/com/wego/repository/ExpenseRepository.java` ✅
- `src/main/java/com/wego/repository/ExpenseSplitRepository.java` ✅
- `src/main/java/com/wego/repository/DocumentRepository.java` ✅
- `src/main/java/com/wego/repository/TodoRepository.java` ✅

#### Service 層 (17 個)
- `src/main/java/com/wego/service/UserService.java` ✅
- `src/main/java/com/wego/service/TripService.java` ✅
- `src/main/java/com/wego/service/InviteLinkService.java` ✅
- `src/main/java/com/wego/service/PermissionChecker.java` ✅
- `src/main/java/com/wego/service/ActivityService.java` ✅
- `src/main/java/com/wego/service/ExpenseService.java` ✅
- `src/main/java/com/wego/service/SettlementService.java` ✅
- `src/main/java/com/wego/service/DocumentService.java` ✅
- `src/main/java/com/wego/service/TodoService.java` ✅
- `src/main/java/com/wego/service/TransportCalculationService.java` ✅
- `src/main/java/com/wego/service/GlobalExpenseService.java` ✅
- `src/main/java/com/wego/service/GlobalDocumentService.java` ✅
- `src/main/java/com/wego/service/StatisticsService.java` ✅
- `src/main/java/com/wego/service/ExchangeRateService.java` ✅
- `src/main/java/com/wego/service/WeatherService.java` ✅
- `src/main/java/com/wego/service/CustomOAuth2UserService.java` ✅
- `src/main/java/com/wego/service/WebExceptionHandler.java` ✅

#### External Service 層 (4 個，各有 Mock)
- `src/main/java/com/wego/service/external/StorageClient.java` ✅
- `src/main/java/com/wego/service/external/SupabaseStorageClient.java` ✅
- `src/main/java/com/wego/service/external/MockStorageClient.java` ✅
- `src/main/java/com/wego/service/external/StorageException.java` ✅

#### Domain 層 (4 個)
- `src/main/java/com/wego/domain/settlement/DebtSimplifier.java` ✅
- `src/main/java/com/wego/domain/route/RouteOptimizer.java` ✅
- `src/main/java/com/wego/domain/permission/PermissionChecker.java` ✅
- `src/main/java/com/wego/domain/expense/ExpenseAggregator.java` ✅

#### Controller 層
- `src/main/java/com/wego/controller/web/HomeController.java` ✅
- `src/main/java/com/wego/controller/web/TripController.java` ✅
- `src/main/java/com/wego/controller/web/ErrorController.java` ✅
- `src/main/java/com/wego/controller/web/GlobalExpenseController.java` ✅
- `src/main/java/com/wego/controller/web/GlobalDocumentController.java` ✅
- `src/main/java/com/wego/controller/web/ProfileController.java` ✅
- `src/main/java/com/wego/controller/api/HealthController.java` ✅
- `src/main/java/com/wego/controller/api/AuthApiController.java` ✅
- `src/main/java/com/wego/controller/api/TripApiController.java` ✅
- `src/main/java/com/wego/controller/api/ActivityApiController.java` ✅
- `src/main/java/com/wego/controller/api/ExpenseApiController.java` ✅
- `src/main/java/com/wego/controller/api/DocumentApiController.java` ✅
- `src/main/java/com/wego/controller/api/TodoApiController.java` ✅

#### Frontend Templates (27 個)
- `src/main/resources/templates/index.html` ✅
- `src/main/resources/templates/login.html` ✅
- `src/main/resources/templates/dashboard.html` ✅
- `src/main/resources/templates/fragments/layout.html` ✅
- `src/main/resources/templates/fragments/components.html` ✅
- `src/main/resources/templates/error/404.html` ✅
- `src/main/resources/templates/error/500.html` ✅
- `src/main/resources/templates/error/error.html` ✅
- `src/main/resources/templates/trip/create.html` ✅
- `src/main/resources/templates/trip/view.html` ✅
- `src/main/resources/templates/trip/members.html` ✅
- `src/main/resources/templates/activity/list.html` ✅
- `src/main/resources/templates/activity/create.html` ✅
- `src/main/resources/templates/activity/detail.html` ✅
- `src/main/resources/templates/expense/list.html` ✅
- `src/main/resources/templates/expense/create.html` ✅
- `src/main/resources/templates/expense/settlement.html` ✅
- `src/main/resources/templates/expense/global-overview.html` ✅
- `src/main/resources/templates/document/list.html` ✅
- `src/main/resources/templates/document/upload.html` ✅
- `src/main/resources/templates/document/global-overview.html` ✅
- `src/main/resources/templates/todo/list.html` ✅
- `src/main/resources/templates/profile/index.html` ✅
- `src/main/resources/templates/profile/edit.html` ✅
- (其他模板)

#### Static Resources (6 個 JS 模組)
- `src/main/resources/static/css/styles.css` ✅
- `src/main/resources/static/js/app.js` ✅
- (其他 JS 模組)

### B. 任務編號規則

任務編號格式：`P{phase}-{module}-{sequence}`，例如 P1-U-001 表示 Phase 1 User Module 第 1 項任務。

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
| [api-keys-setup.md](./api-keys-setup.md) | API Keys 設定指南 |
