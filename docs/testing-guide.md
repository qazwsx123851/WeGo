# WeGo 測試指南

> 本文件整合 TDD 方法論、測試案例規格與 API 端點清單，供撰寫測試時統一參考。

## Part 1: TDD 方法論

> 更新日期：2026-02-12

### 1. TDD 概述

#### 1.1 什麼是 TDD

Test-Driven Development (測試驅動開發) 是一種軟體開發方法，遵循「紅-綠-重構」循環：

```
    ┌─────────────┐
    │   1. RED    │  ← 撰寫失敗的測試
    │  寫測試     │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │  2. GREEN   │  ← 撰寫最小程式碼使測試通過
    │  寫程式碼   │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │ 3. REFACTOR │  ← 重構程式碼，保持測試通過
    │  重構       │
    └──────┬──────┘
           │
           └───────────→ 回到步驟 1
```

#### 1.2 TDD 的好處

| 優點 | 說明 |
|------|------|
| 更好的設計 | 先思考如何使用 API，產生更好的介面設計 |
| 即時回饋 | 每次修改都能立即驗證是否破壞既有功能 |
| 文件化 | 測試即文件，展示程式碼的預期行為 |
| 信心重構 | 有完整測試覆蓋，重構時更有信心 |
| 減少 Debug | 問題在早期就被發現 |

#### 1.3 WeGo 專案的測試策略

```
                    ┌─────────────────┐
                    │    E2E Tests    │  ← 少量，驗證關鍵流程
                    │  (Playwright)   │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │      Integration Tests      │  ← 中量，驗證模組整合
              │   (Spring Boot Test)        │
              └──────────────┬──────────────┘
                             │
    ┌────────────────────────┴────────────────────────┐
    │                   Unit Tests                    │  ← 大量，驗證單一類別
    │               (JUnit 5 + Mockito)               │
    └─────────────────────────────────────────────────┘
```

**測試比例建議**: Unit : Integration : E2E = 70% : 20% : 10%

---

### 2. 測試環境設定

#### 2.1 Maven 依賴

專案使用以下測試相關依賴，皆宣告於 `pom.xml` 中：

| 依賴 | 說明 |
|------|------|
| spring-boot-starter-test | Spring Boot 測試基礎，包含 JUnit 5、Mockito、AssertJ 等 |
| junit-jupiter | JUnit 5 測試框架 |
| mockito-core / mockito-junit-jupiter | Mockito 模擬框架與 JUnit 5 整合 |
| assertj-core | 流暢斷言語法 |
| h2 | 測試用記憶體資料庫 |
| testcontainers (postgresql / junit-jupiter) | 整合測試用 PostgreSQL 容器 |

Maven 插件配置：

| 插件 | 說明 |
|------|------|
| maven-surefire-plugin | 執行單元測試（`*Test.java`），排除 `*IntegrationTest.java` 和 `*E2ETest.java` |
| maven-failsafe-plugin | 執行整合測試（`*IntegrationTest.java`） |
| jacoco-maven-plugin | 測試覆蓋率報告產生 |

#### 2.2 測試設定檔

測試設定檔位於 `src/test/resources/application-test.yml`，主要配置包括：

- 資料來源使用 H2 記憶體資料庫（`jdbc:h2:mem:testdb`）
- JPA 使用 `create-drop` 模式自動建立與銷毀結構
- OAuth2 使用測試用 client-id 和 client-secret
- 停用所有外部 API（Google Maps、Weather、Exchange Rate 設為 `enabled: false`）

#### 2.3 測試目錄結構

```
src/test/java/com/wego/
├── unit/                           # 單元測試
│   ├── service/
│   ├── domain/
│   └── mapper/
│
├── integration/                    # 整合測試
│   ├── repository/
│   ├── api/
│   └── security/
│
├── e2e/                            # 端對端測試
│
├── fixture/                        # 測試資料工廠
│
└── config/                         # 測試設定
```

---

### 3. 單元測試 (Unit Tests)

#### 3.1 命名規範

測試方法命名格式為 `methodName_scenario_expectedBehavior`，例如：

- `createTrip_withValidInput_shouldReturnCreatedTrip`
- `createTrip_withNullTitle_shouldThrowValidationException`
- `calculateSettlement_withThreeMembers_shouldReturnSimplifiedDebts`

#### 3.2 Service 層測試

Service 層測試使用 `@ExtendWith(MockitoExtension.class)` 搭配 `@Mock` 和 `@InjectMocks` 建立測試環境。關鍵要點：

- 以 `@Mock` 模擬 Repository 和外部依賴
- 以 `@InjectMocks` 自動注入被測試的 Service
- 使用 `@BeforeEach` 準備通用測試資料（透過 Fixture 工廠）
- 使用 `@Nested` 和 `@DisplayName` 依功能分組測試
- 使用 Given-When-Then 三段式結構撰寫每個測試
- 以 `verify()` 驗證 Repository 被正確呼叫
- 以 `assertThatThrownBy()` 驗證例外情況
- 使用 `argThat()` 驗證傳入 Repository 的參數內容

#### 3.3 Domain 層測試

Domain 層測試為純邏輯測試，不需要 Mock 框架。直接建立被測試物件，提供輸入資料，驗證輸出結果。

測試重點包括：

- 驗證核心演算法的正確性（如債務簡化計算）
- 測試正常情境（如三人均分、四人多筆支出）
- 測試已結清情境（淨額為零應回傳空清單）
- 使用有意義的測試資料（如具體金額、具體成員配置）

#### 3.4 路線優化測試

路線優化測試驗證最近鄰居演算法的排序結果：

- 三個以上景點應按最短路徑排序
- 兩個以下景點應直接返回原列表
- 空列表應返回空列表
- 使用具體座標驗證排序結果

---

### 4. 整合測試 (Integration Tests)

#### 4.1 Repository 整合測試

Repository 整合測試使用 `@DataJpaTest` 搭配 Testcontainers 啟動真實 PostgreSQL 容器。關鍵要點：

- 使用 `@Container` 宣告 PostgreSQL 容器
- 使用 `@DynamicPropertySource` 動態配置資料來源
- 使用 `TestEntityManager` 進行 flush 和 clear 確保測試隔離
- 測試自訂查詢方法的正確性（如依用戶 ID 查詢、依日期範圍查詢）

#### 4.2 API 整合測試

API 整合測試使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 搭配 `MockMvc`。關鍵要點：

- 使用 `@ActiveProfiles("test")` 載入測試設定
- 在 `@BeforeEach` 中清理資料庫並建立測試用戶
- 使用 `@Nested` 依 API 端點分組（如 `POST /api/trips`、`GET /api/trips/{tripId}`）
- 驗證 HTTP 狀態碼、回應 JSON 結構、資料庫狀態
- 測試情境包括：有效請求（201）、驗證失敗（400）、未認證（401）、無權限（403）、不存在（404）

---

### 5. 測試資料工廠 (Test Fixtures)

測試資料工廠位於 `fixture/` 目錄，為每個核心實體提供工廠方法，用於快速建立測試資料。

#### 設計原則

| 原則 | 說明 |
|------|------|
| 計數器遞增 | 使用 `AtomicLong` 確保每次生成唯一資料（如 email、nickname） |
| 合理預設值 | 提供有意義的預設值（如日期設為未來 7-10 天） |
| 可覆寫 | 提供多載方法允許覆寫特定欄位（如指定 email、金額、幣別） |
| 關聯建立 | 提供建立關聯資料的輔助方法（如均分分攤明細） |

#### 可用的 Fixture 類別

| 類別 | 說明 |
|------|------|
| UserFixture | 建立測試用戶，可指定 email 和 nickname |
| TripFixture | 建立測試行程，可指定 ID、日期範圍 |
| ActivityFixture | 建立測試景點 |
| ExpenseFixture | 建立測試支出，可指定金額、幣別；提供均分分攤明細產生方法 |

---

### 6. 測試覆蓋率目標

#### 6.1 覆蓋率要求

| 層級 | 行覆蓋率目標 | 分支覆蓋率目標 |
|------|-------------|---------------|
| Service | >= 80% | >= 70% |
| Domain | >= 90% | >= 85% |
| Controller | >= 70% | >= 60% |
| Repository | >= 60% | - |
| **整體** | **>= 80%** | **>= 70%** |

#### 6.2 排除覆蓋率計算的項目

JaCoCo 排除以下類別的覆蓋率計算：

- `WegoApplication`（啟動類）
- `config/**`（設定類）
- `entity/**`（實體類）
- `dto/**`（資料傳輸物件）
- `exception/**`（例外類）

#### 6.3 執行測試與產生報告

| 指令 | 說明 |
|------|------|
| `./mvnw test` | 執行單元測試 |
| `./mvnw verify` | 執行整合測試 |
| `./mvnw jacoco:report` | 產生覆蓋率報告 |

報告位置：`target/site/jacoco/index.html`

---

### 7. TDD 工作流程

#### 7.1 開發一個新功能的步驟

以「新增景點」功能為例：

**Step 1: 寫測試 (RED)**

撰寫一個測試方法，呼叫尚不存在的 `activityService.createActivity()` 方法，斷言回傳結果不為 null 且包含預期的景點名稱。執行測試，確認測試失敗（因為方法尚未實作）。

**Step 2: 寫程式碼 (GREEN)**

實作最小程式碼使測試通過：建立 Activity 物件、設定 ID、儲存至 Repository、透過 Mapper 轉換為 Response 回傳。執行測試，確認測試通過。

**Step 3: 重構 (REFACTOR)**

加入完整的業務邏輯：權限檢查、找或建立地點、設定排序順序等。使用 Builder 模式改善程式碼可讀性。執行測試，確認測試仍然通過。

#### 7.2 TDD Checklist

開發新功能時，確認以下項目：

- [ ] 先寫測試，測試失敗
- [ ] 寫最小程式碼讓測試通過
- [ ] 重構程式碼
- [ ] 所有測試仍然通過
- [ ] 測試覆蓋正常情況與邊界情況
- [ ] 測試命名清楚描述預期行為

---

### 8. 持續整合 (CI) 設定

#### GitHub Actions 工作流程

CI 工作流程定義於 `.github/workflows/test.yml`，在 push 和 pull request 到 `main`/`develop` 時觸發。

**執行步驟：**

1. 啟動 PostgreSQL 15 服務容器（用於整合測試）
2. 設定 JDK 17（Temurin 發行版），啟用 Maven 快取
3. 執行單元測試（`./mvnw test`）
4. 執行整合測試（`./mvnw verify -DskipUnitTests`），連接測試用 PostgreSQL
5. 產生 JaCoCo 覆蓋率報告
6. 上傳覆蓋率至 Codecov
7. 檢查覆蓋率門檻（低於 80% 則失敗）

---

### 9. 常見問題與最佳實踐

#### 9.1 測試應該獨立

每個測試方法必須獨立運作，不依賴其他測試的執行結果或執行順序。不要使用 `@Order` 建立測試間的依賴關係。每個測試應在自身的 `@BeforeEach` 或測試方法中建立所需的資料。

#### 9.2 避免過度 Mock

只 Mock 外部依賴（如 Repository、外部 API），不要 Mock 核心業務邏輯。例如，測試結算功能時，應 Mock `ExpenseRepository` 但使用真實的 `DebtSimplifier`，以確保測試真正驗證了業務邏輯的正確性。

#### 9.3 測試邊界條件

使用 `@Nested` 和 `@DisplayName` 組織邊界條件測試群組，至少涵蓋以下情境：

- 空列表
- 單一元素
- 最大值
- null 輸入

#### 9.4 使用有意義的測試資料

測試資料應反映真實使用場景，使測試具有文件化效果。例如，分帳測試使用「團體晚餐 3000 元」而非無意義的 `123.45`。測試方法名稱應包含業務語境，如 `calculateSettlement_withDinnerExpense_shouldSplitEvenly`。

---

## Part 2: 測試案例規格

> 此為最低需求行為規格，完整測試套件包含額外案例。

> 更新日期：2026-02-12

### 文件說明

本文件定義 WeGo 專案各模組的測試案例，供開發時參考實作。

---

### 1. 使用者模組 (User Module)

#### 1.1 OAuth 登入

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| U-001 | 首次 Google 登入 | Google OAuth 授權成功，email: new@test.com | 自動建立帳號，返回用戶資訊 | Integration |
| U-002 | 重複 Google 登入 | 已存在的 email | 直接登入，返回既有用戶資訊 | Integration |
| U-003 | OAuth 授權失敗 | 用戶拒絕授權 | 導向登入頁，顯示錯誤訊息 | Integration |
| U-005 | OAuth Token 過期 | 過期的 token | 返回 401，要求重新登入 | Integration |

#### 1.2 Session 管理

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| U-010 | 取得當前用戶 | 有效 Session | 返回用戶資訊 | Unit |
| U-011 | Session 過期 | 過期 Session Cookie | 返回 401 | Integration |
| U-012 | 登出 | POST /api/auth/logout | 清除 Session，返回 200 | Integration |

---

### 2. 行程模組 (Trip Module)

#### 2.1 建立行程

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| T-001 | 正常建立行程 | title: "東京行", startDate: 2024-03-15, endDate: 2024-03-19 | 返回 201，行程已建立，建立者為 OWNER | Unit |
| T-002 | 缺少標題 | title: null | 返回 400，錯誤訊息「行程名稱不可為空」 | Unit |
| T-003 | 標題過長 | title: 101 字元 | 返回 400，錯誤訊息「行程名稱不可超過100字」 | Unit |
| T-004 | 結束日期早於開始 | startDate: 2024-03-20, endDate: 2024-03-15 | 返回 400，錯誤訊息「結束日期不可早於開始日期」 | Unit |
| T-005 | 開始日期為過去 | startDate: 2020-01-01 | 返回 400，錯誤訊息「開始日期不可為過去」 | Unit |
| T-006 | 未登入建立行程 | 無 Session | 返回 401 | Integration |

#### 2.2 查詢行程

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| T-010 | 查詢行程列表 | 用戶有 3 個行程 | 返回 3 個行程，依日期排序 | Unit |
| T-011 | 查詢行程列表（空） | 用戶無行程 | 返回空陣列 | Unit |
| T-012 | 查詢單一行程（成員） | tripId, 用戶是成員 | 返回行程詳情 | Unit |
| T-013 | 查詢單一行程（非成員） | tripId, 用戶非成員 | 返回 403 | Unit |
| T-014 | 查詢不存在行程 | 不存在的 tripId | 返回 404，錯誤碼 TRIP_NOT_FOUND | Unit |

#### 2.3 更新行程

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| T-020 | Owner 更新標題 | role: OWNER, newTitle | 更新成功 | Unit |
| T-021 | Editor 更新標題 | role: EDITOR | 返回 403（僅 OWNER 可修改基本資訊） | Unit |
| T-022 | Viewer 更新 | role: VIEWER | 返回 403 | Unit |
| T-023 | 更新為空標題 | title: "" | 返回 400 | Unit |

#### 2.4 刪除行程

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| T-030 | Owner 刪除行程 | role: OWNER | 刪除成功，關聯資料同時刪除 | Unit |
| T-031 | Editor 刪除行程 | role: EDITOR | 返回 403 | Unit |
| T-032 | Viewer 刪除行程 | role: VIEWER | 返回 403 | Unit |

#### 2.5 成員管理

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| T-040 | 產生邀請連結 (Owner) | role: OWNER, inviteRole: EDITOR, expiry: 7days | 返回邀請連結 | Unit |
| T-041 | 產生邀請連結 (Editor) | role: EDITOR | 返回邀請連結 | Unit |
| T-042 | 產生邀請連結 (Viewer) | role: VIEWER | 返回 403 | Unit |
| T-043 | 接受邀請（有效連結） | 有效 token | 加入行程，角色為連結指定角色 | Integration |
| T-044 | 接受邀請（過期連結） | 過期 token | 返回 400，錯誤碼 INVALID_INVITE_LINK | Integration |
| T-045 | 接受邀請（已是成員） | 已是成員 | 返回 400，錯誤訊息「已是行程成員」 | Unit |
| T-046 | 成員數達上限 | 已有 10 人 | 返回 400，錯誤碼 MEMBER_LIMIT_EXCEEDED | Unit |
| T-047 | Owner 移除成員 | role: OWNER, targetRole: EDITOR | 移除成功 | Unit |
| T-048 | Editor 移除成員 | role: EDITOR | 返回 403 | Unit |
| T-049 | Owner 變更成員角色 | EDITOR → VIEWER | 變更成功 | Unit |
| T-050 | 嘗試變更為 OWNER | targetRole: OWNER | 返回 400（需使用轉移功能） | Unit |

---

### 3. 活動/景點模組 (Activity Module)

#### 3.1 新增景點

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| A-001 | 新增景點（Editor） | placeName, day: 1, role: EDITOR | 新增成功，sortOrder 自動設為最後 | Unit |
| A-002 | 新增景點（Viewer） | role: VIEWER | 返回 403 | Unit |
| A-003 | 新增景點到不存在的天數 | day: 10（行程只有5天） | 返回 400，錯誤訊息「天數超出行程範圍」 | Unit |
| A-004 | 新增景點（使用 Google Place ID） | googlePlaceId | 自動取得地點詳情並建立 | Integration |

#### 3.2 更新景點

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| A-010 | 更新筆記 | note: "記得帶傘" | 更新成功 | Unit |
| A-011 | 更新時間 | startTime: "10:00", duration: 120 | 更新成功 | Unit |
| A-012 | 更新交通方式 | transportMode: "transit" | 更新成功 | Unit |

#### 3.3 刪除景點

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| A-020 | Editor 刪除景點 | role: EDITOR | 刪除成功，關聯文件解除關聯 | Unit |
| A-021 | Viewer 刪除景點 | role: VIEWER | 返回 403 | Unit |

#### 3.4 排序

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| A-030 | 同一天內拖拽排序 | activities: [A, B, C] → [A, C, B] | 更新 sortOrder | Unit |
| A-031 | 跨天移動景點 | activity A 從 day1 移到 day2 | 更新 day 和 sortOrder | Unit |
| A-032 | 空列表排序 | activities: [] | 不做任何事，返回成功 | Unit |

#### 3.5 路線優化

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| A-040 | 3 個景點優化 | A(0,0), B(10,0), C(5,0) | 返回優化建議 [A, C, B] | Unit |
| A-041 | 2 個景點 | A, B | 返回原順序（無需優化） | Unit |
| A-042 | 15 個以上景點 | 16 個景點 | 返回 400，建議分日 | Unit |
| A-043 | 優化後套用 | confirmed: true | 更新 sortOrder | Unit |

#### 3.6 交通時間

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| A-050 | 取得交通時間（開車） | origin, destination, mode: driving | 返回距離和時間 | Integration |
| A-051 | 取得交通時間（快取命中） | 相同參數第二次呼叫 | 從快取返回，不呼叫 API | Integration |
| A-052 | Google Maps API 失敗 | API 返回錯誤 | 返回 502，錯誤碼 EXTERNAL_API_ERROR | Integration |

---

### 4. 分帳模組 (Expense Module)

#### 4.1 新增支出

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| E-001 | 新增支出（均分） | amount: 3000, payer: A, splitType: EQUAL, members: [A,B,C] | 建立支出，每人分攤 1000 | Unit |
| E-002 | 新增支出（自訂金額） | splitType: CUSTOM, splits: {A: 1000, B: 1500, C: 500} | 依指定金額分攤 | Unit |
| E-003 | 新增支出（排除成員） | splitType: EQUAL, excludeMembers: [C] | A, B 各分攤 1500 | Unit |
| E-004 | 分攤金額不等於總額 | amount: 3000, splits 合計: 2500 | 返回 400，錯誤訊息「分攤金額不等於總額」 | Unit |
| E-005 | 新增外幣支出 | currency: JPY, amount: 10000 | 自動取得當時匯率並記錄 | Integration |
| E-006 | Viewer 新增支出 | role: VIEWER | 返回 403 | Unit |

#### 4.2 更新支出

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| E-010 | 建立者更新支出 | 自己建立的支出 | 更新成功 | Unit |
| E-011 | 非建立者更新支出 | 他人建立的支出, role: EDITOR | 更新成功（Editor 可編輯） | Unit |
| E-012 | Viewer 更新支出 | role: VIEWER | 返回 403 | Unit |

#### 4.3 刪除支出

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| E-020 | 建立者刪除支出 | 自己建立的支出 | 刪除成功 | Unit |
| E-021 | Editor 刪除他人支出 | 他人建立的支出, role: EDITOR | 返回 403 | Unit |
| E-022 | Owner 刪除他人支出 | role: OWNER | 刪除成功 | Unit |

#### 4.4 結算計算

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| E-030 | 三人均分結算 | A付300三人分, B付600三人分 | C付B 300 | Unit |
| E-031 | 複雜多筆結算 | 多筆支出、多幣別 | 返回簡化後的債務清單 | Unit |
| E-032 | 無支出結算 | 無支出記錄 | 返回空清單 | Unit |
| E-033 | 已全部結清 | 所有淨額為 0 | 返回空清單 | Unit |
| E-034 | 多幣別結算 | TWD + JPY 支出 | 統一換算為基準幣別後結算 | Unit |

#### 4.5 標記結清

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| E-040 | 標記單筆結清 | settlementId | 更新 isSettled = true | Unit |
| E-041 | 取消結清標記 | isSettled: false | 更新 isSettled = false | Unit |

---

### 5. 債務簡化演算法 (DebtSimplifier)

#### 5.1 基本情境

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| D-001 | 兩人簡單債務 | A 應收 100, B 應付 100 | B → A: 100 | Unit |
| D-002 | 三人鏈式債務 | A→B 100, B→C 100 | 簡化為 A→C 100 | Unit |
| D-003 | 三人環狀債務 | A→B 100, B→C 100, C→A 100 | 空清單（互相抵銷） | Unit |
| D-004 | 多人複雜債務 | 4人多筆複雜債務 | 最小化交易次數 | Unit |
| D-005 | 有零頭的分帳 | 1000 / 3 = 333.33... | 正確處理小數（四捨五入） | Unit |

#### 5.2 邊界情況

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| D-010 | 空輸入 | 空的支出清單 | 返回空清單 | Unit |
| D-011 | 單人支出 | 只有一人付款 | 其他人付給付款者 | Unit |
| D-012 | 金額為零 | amount: 0 | 正確處理，不產生債務 | Unit |
| D-013 | 極大金額 | 數百萬元 | 正確計算，無溢位 | Unit |

---

### 6. 檔案模組 (Document Module)

#### 6.1 上傳檔案

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| F-001 | 上傳 PDF | file.pdf, size: 5MB | 上傳成功，返回 URL | Integration |
| F-002 | 上傳 JPG | file.jpg | 上傳成功 | Integration |
| F-003 | 上傳 HEIC | file.heic | 自動轉換為 JPG 後上傳 | Integration |
| F-004 | 超過大小限制 | file: 15MB | 返回 400，錯誤碼 FILE_TOO_LARGE | Unit |
| F-005 | 不支援的格式 | file.exe | 返回 400，錯誤碼 UNSUPPORTED_FILE_TYPE | Unit |
| F-006 | 行程總量超限 | 行程已有 95MB，上傳 10MB | 返回 400，錯誤訊息「行程檔案容量已滿」 | Unit |
| F-007 | Viewer 上傳 | role: VIEWER | 返回 403 | Unit |

#### 6.2 關聯檔案

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| F-010 | 關聯到景點 | documentId, activityId | 更新 relatedActivityId | Unit |
| F-011 | 關聯到日期 | documentId, day: 2 | 更新 relatedDay | Unit |
| F-012 | 解除關聯 | activityId: null | 清除關聯 | Unit |

#### 6.3 刪除檔案

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| F-020 | 上傳者刪除 | 自己上傳的檔案 | 刪除成功，同時刪除 Storage 檔案 | Integration |
| F-021 | Owner 刪除他人檔案 | role: OWNER | 刪除成功 | Integration |
| F-022 | Editor 刪除他人檔案 | role: EDITOR | 返回 403 | Unit |

---

### 7. 代辦事項模組 (Todo Module)

#### 7.1 新增代辦

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| TD-001 | 新增代辦 | title: "訂機票", assigneeId, dueDate | 建立成功，status: PENDING | Unit |
| TD-002 | 新增無指派人代辦 | assigneeId: null | 建立成功 | Unit |
| TD-003 | Viewer 新增 | role: VIEWER | 返回 403 | Unit |

#### 7.2 更新代辦

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| TD-010 | 標記完成 | status: COMPLETED | 更新成功，設定 completedAt | Unit |
| TD-011 | 重新開啟 | status: PENDING | 更新成功，清除 completedAt | Unit |
| TD-012 | 變更指派人 | newAssigneeId | 更新成功 | Unit |

---

### 8. 權限檢查 (PermissionChecker)

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| P-001 | Owner canEdit | role: OWNER | true | Unit |
| P-002 | Editor canEdit | role: EDITOR | true | Unit |
| P-003 | Viewer canEdit | role: VIEWER | false | Unit |
| P-004 | Owner canDelete | role: OWNER | true | Unit |
| P-005 | Editor canDelete | role: EDITOR | false | Unit |
| P-006 | Owner canManageMembers | role: OWNER | true | Unit |
| P-007 | Editor canManageMembers | role: EDITOR | false | Unit |
| P-008 | 非成員 canView | 非成員 | false | Unit |

---

### 9. 外部 API 整合

#### 9.1 Google Maps API

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| G-001 | 搜尋地點 | query: "淺草寺" | 返回地點列表 | Integration |
| G-002 | 取得距離矩陣 | origins, destinations, mode | 返回距離與時間 | Integration |
| G-003 | API 配額用盡 | - | 返回 502，錯誤訊息 | Integration |
| G-004 | 無效 API Key | - | 返回 502 | Integration |

#### 9.2 天氣 API

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| W-001 | 取得 5 天預報 | lat, lng | 返回天氣資料 | Integration |
| W-002 | 超出預報範圍 | 10 天後的日期 | 返回「天氣預報尚未開放」 | Unit |

#### 9.3 匯率 API

| 測試 ID | 測試案例 | 輸入 | 預期結果 | 類型 |
|---------|----------|------|----------|------|
| X-001 | 取得匯率 | from: JPY, to: TWD | 返回匯率 | Integration |
| X-002 | 使用快取匯率 | 24 小時內第二次 | 從快取返回 | Integration |
| X-003 | 不支援的幣別 | from: XXX | 返回 400 | Unit |

---

### 10. 測試執行優先級

#### 10.1 CI/CD 流程

```
1. Pre-commit (本地)
   └── 執行相關模組的 Unit Tests

2. Pull Request
   └── 執行所有 Unit Tests
   └── 執行所有 Integration Tests
   └── 檢查覆蓋率 >= 80%

3. Merge to Main
   └── 執行所有 Unit Tests
   └── 執行所有 Integration Tests
   └── 執行 E2E Tests
   └── 部署到 Staging

4. Release
   └── 執行完整測試套件
   └── 效能測試
   └── 部署到 Production
```

#### 10.2 測試標籤

測試使用 JUnit 5 的 `@Tag` 標記分類：

| 標籤 | 說明 | 執行時機 |
|------|------|----------|
| `fast` | 快速測試 | PR 必跑 |
| `slow` | 慢速測試（如整合測試） | 排程執行 |
| `critical` | 關鍵路徑測試 | 永遠執行 |

---

### 11. 測試資料管理

#### 11.1 測試資料庫

| 環境 | 資料庫 | 用途 |
|------|--------|------|
| Unit Test | H2 In-Memory | 快速、隔離 |
| Integration Test | Testcontainers PostgreSQL | 真實資料庫行為 |
| E2E Test | Staging PostgreSQL | 完整環境測試 |

#### 11.2 測試資料清理

每個測試方法執行前（`@BeforeEach`），依照外鍵約束順序清理資料庫。清理順序為：Document → ExpenseSplit → Expense → Activity → Todo → TripMember → Trip → User。

---

### 12. E2E 測試案例 (End-to-End)

> 使用 Playwright 執行，模擬真實使用者操作流程。

#### 12.1 使用者登入流程

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-001 | Google OAuth 登入 | 1. 進入首頁<br>2. 點擊「以 Google 登入」<br>3. 完成 Google 授權 | 導向 Dashboard，顯示用戶名稱 |
| E2E-002 | 未登入存取行程頁 | 1. 直接訪問 /trips/xxx | 導向登入頁 |
| E2E-003 | 登出流程 | 1. 已登入狀態<br>2. 點擊登出 | 導向首頁，Session 已清除 |

#### 12.2 行程建立與編輯流程

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-010 | 建立新行程完整流程 | 1. 登入<br>2. 點擊「建立行程」<br>3. 填寫名稱「東京五日遊」<br>4. 選擇日期 3/15-3/19<br>5. 點擊確認 | 行程建立成功，導向行程主頁 |
| E2E-011 | 新增景點完整流程 | 1. 進入行程主頁<br>2. 點擊「新增景點」<br>3. 搜尋「淺草寺」<br>4. 選擇搜尋結果<br>5. 設定時間 10:00 | 景點出現在清單中，顯示時間 |
| E2E-012 | 拖拽排序景點 | 1. 行程有 3 個景點 A, B, C<br>2. 長按景點 C<br>3. 拖拽到 A 之後 | 順序變為 A, C, B，交通時間重新計算 |
| E2E-013 | 跨日移動景點 | 1. Day1 有景點 A<br>2. 點擊景點 A 選單<br>3. 選擇「移至 Day2」 | 景點移至 Day2，Day1 清單更新 |
| E2E-014 | 刪除景點 | 1. 點擊景點卡片<br>2. 在抽屜中點擊刪除<br>3. 確認刪除 | 景點從清單移除 |

#### 12.3 協作邀請流程

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-020 | 產生邀請連結 | 1. Owner 進入行程<br>2. 點擊「邀請成員」<br>3. 選擇 Editor 角色<br>4. 選擇 7 天有效期<br>5. 產生連結 | 顯示可複製的邀請連結 |
| E2E-021 | 透過連結加入行程 | 1. 新用戶點擊邀請連結<br>2. 以 Google 登入<br>3. 確認加入 | 成功加入行程，角色為 Editor |
| E2E-022 | 過期連結無法加入 | 1. 使用過期的邀請連結 | 顯示「連結已過期」錯誤訊息 |
| E2E-023 | 成員清單顯示 | 1. 行程有 3 位成員<br>2. 進入成員管理頁 | 顯示所有成員與角色 |

#### 12.4 分帳完整流程

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-030 | 新增支出（均分） | 1. 進入分帳頁<br>2. 點擊「新增支出」<br>3. 輸入「午餐」¥3000<br>4. 選擇付款者 A<br>5. 選擇均分（A, B, C）<br>6. 確認 | 支出出現在清單，每人分攤 ¥1000 |
| E2E-031 | 新增支出（自訂金額） | 1. 新增支出 $1000<br>2. 選擇自訂分攤<br>3. A: $500, B: $300, C: $200 | 依指定金額分攤 |
| E2E-032 | 多幣別支出 | 1. 新增 ¥10000 支出<br>2. 新增 $500 TWD 支出<br>3. 查看結算 | 統一換算為基準幣別顯示 |
| E2E-033 | 查看結算結果 | 1. 行程有多筆支出<br>2. 點擊「查看結算」 | 顯示簡化後的債務清單 |
| E2E-034 | 標記債務結清 | 1. 在結算頁面<br>2. 點擊某筆債務的「標記已結清」 | 該筆標記為已結清，UI 更新 |
| E2E-035 | 全部結清慶祝動畫 | 1. 所有債務都標記結清 | 顯示撒花動畫 |

#### 12.5 檔案管理流程

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-040 | 上傳機票 PDF | 1. 進入檔案頁<br>2. 點擊上傳<br>3. 選擇 PDF 檔案（5MB） | 上傳成功，顯示在清單中 |
| E2E-041 | 上傳 HEIC 圖片 | 1. 上傳 HEIC 格式圖片 | 自動轉換為 JPG，上傳成功 |
| E2E-042 | 關聯檔案至景點 | 1. 點擊檔案選單<br>2. 選擇「關聯至景點」<br>3. 選擇「淺草寺」 | 檔案關聯成功，景點顯示附件圖示 |
| E2E-043 | 從景點檢視關聯檔案 | 1. 點擊景點卡片<br>2. 在抽屜中點擊附件 | 開啟檔案預覽 |
| E2E-044 | 檔案大小超限 | 1. 嘗試上傳 15MB 檔案 | 顯示「檔案過大」錯誤 |

#### 12.6 代辦事項流程

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-050 | 新增代辦事項 | 1. 進入代辦頁<br>2. 輸入「訂機票」<br>3. 指派給成員 B<br>4. 設定截止日期 | 代辦出現在清單，狀態為待辦 |
| E2E-051 | 標記代辦完成 | 1. 點擊代辦的勾選框 | 狀態變為完成，顯示完成動畫 |
| E2E-052 | 篩選代辦狀態 | 1. 切換「僅顯示待辦」 | 只顯示未完成項目 |

#### 12.7 路線優化流程

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-060 | 一鍵優化路線 | 1. 當日有 5 個景點<br>2. 點擊「優化路線」<br>3. 等待計算完成 | 顯示優化建議與距離比較 |
| E2E-061 | 套用優化建議 | 1. 查看優化建議<br>2. 點擊「套用」 | 景點順序更新，交通時間重算 |
| E2E-062 | 取消優化建議 | 1. 查看優化建議<br>2. 點擊「取消」 | 維持原順序 |

#### 12.8 響應式設計測試

| 測試 ID | 測試案例 | 視窗大小 | 預期結果 |
|---------|----------|----------|----------|
| E2E-070 | 手機版行程主頁 | 375x667 | 顯示底部導覽列，地圖為切換模式 |
| E2E-071 | 平板版行程主頁 | 768x1024 | 左側清單 + 右側地圖並排 |
| E2E-072 | 桌機版行程主頁 | 1440x900 | 完整雙欄佈局 |
| E2E-073 | 手機版底部抽屜 | 375x667 | 點擊景點後底部抽屜滑出 |

#### 12.9 錯誤處理測試

| 測試 ID | 測試案例 | 步驟 | 預期結果 |
|---------|----------|------|----------|
| E2E-080 | 網路斷線提示 | 1. 模擬網路斷線<br>2. 嘗試操作 | 顯示「網路連線異常」提示 |
| E2E-081 | API 錯誤提示 | 1. 模擬伺服器 500 錯誤 | 顯示「系統忙碌中，請稍後再試」 |
| E2E-082 | 權限不足提示 | 1. Viewer 嘗試編輯景點 | 顯示「您沒有編輯權限」 |
| E2E-083 | 行程不存在 | 1. 訪問不存在的行程 ID | 顯示 404 頁面 |

#### 12.10 效能基準測試

| 測試 ID | 測試案例 | 指標 | 目標值 |
|---------|----------|------|--------|
| E2E-090 | Dashboard 載入時間 | LCP | < 2.5s |
| E2E-091 | 行程主頁載入時間 | LCP | < 3.0s |
| E2E-092 | 新增景點回應時間 | API Response | < 500ms |
| E2E-093 | 拖拽排序回應時間 | API Response | < 300ms |

---

### 附錄：測試命名對照表

| 測試 ID 前綴 | 模組 |
|-------------|------|
| U- | User (使用者) |
| T- | Trip (行程) |
| A- | Activity (景點) |
| E- | Expense (支出) |
| D- | DebtSimplifier (債務簡化) |
| F- | File/Document (檔案) |
| TD- | Todo (代辦) |
| P- | Permission (權限) |
| G- | Google Maps |
| W- | Weather |
| X- | Exchange Rate |
| E2E- | End-to-End 測試 |

---

## Part 3: API 端點清單

### 文件資訊

| 項目 | 內容 |
|------|------|
| 建立日期 | 2026-02-11 |
| 最後更新 | 2026-02-12 |
| 後端 REST endpoint 總數 | 58 |
| 前端實際使用的 endpoint | ~24 |
| Orphan endpoints (API-only) | ~31 |

---

### A. 前端使用的 REST API (24 endpoints)

#### JavaScript 模組 API 呼叫

| 模組 | Endpoint | Method | CSRF | 說明 |
|------|----------|--------|------|------|
| `chat.js` | `/api/trips/{tripId}/chat` | POST | Yes | AI 聊天 |
| `app.js` (WeatherUI) | `/api/weather/forecast?lat=&lng=` | GET | No | 天氣預報 |
| `expense.js` | `/api/exchange-rates?from=&to=` | GET | No | 匯率查詢 |
| `todo.js` | `/api/trips/{tripId}/todos` | GET | No | Todo 列表 |
| `todo.js` | `/api/trips/{tripId}/todos` | POST | Yes | 建立 Todo |
| `todo.js` | `/api/trips/{tripId}/todos/{todoId}` | GET | No | 取得單一 Todo |
| `todo.js` | `/api/trips/{tripId}/todos/{todoId}` | PUT | Yes | 更新 Todo |
| `todo.js` | `/api/trips/{tripId}/todos/{todoId}` | DELETE | Yes | 刪除 Todo |
| `drag-reorder.js` | `/api/trips/{tripId}/activities/reorder` | PUT | Yes | 景點排序 |
| `route-optimizer.js` | `/api/trips/{tripId}/activities/optimize?day=` | GET | No | 路線優化建議 |
| `route-optimizer.js` | `/api/trips/{tripId}/activities/apply-optimization` | POST | Yes | 套用路線優化 |
| `expense-statistics.js` | `/api/trips/{tripId}/statistics/category` | GET | No | 分類統計 |
| `expense-statistics.js` | `/api/trips/{tripId}/statistics/trend` | GET | No | 趨勢統計 |
| `expense-statistics.js` | `/api/trips/{tripId}/statistics/members` | GET | No | 成員統計 |

#### Thymeleaf 模板 inline API 呼叫

| 模板 | Endpoint | Method | CSRF | 說明 |
|------|----------|--------|------|------|
| `members.html` | `/api/trips/{tripId}/members/{userId}/role` | PUT | Yes | 變更角色 |
| `members.html` | `/api/trips/{tripId}/members/{userId}` | DELETE | Yes | 移除成員 |
| `members.html` | `/api/trips/{tripId}/invites` | POST | Yes | 產生邀請連結 |
| `members.html` | `/api/trips/{tripId}/members/{currentUserId}` | DELETE | Yes | 離開行程 |
| `expense/list.html` | `/api/trips/{tripId}/expenses` | GET | No | 支出列表 |
| `expense/list.html` + `detail.html` | `/api/expenses/{expenseId}` | DELETE | Yes | 刪除支出 |
| `settlement.html` | `/api/trips/{tripId}/settlement/settle` | PUT | Yes | 結清債務 |
| `document/list.html` + `upload.html` | `/api/trips/{tripId}/documents` | POST | Yes | 上傳文件 |
| `document/list.html` + `global.html` | `/api/trips/{tripId}/documents/{docId}/download` | GET | No | 下載文件 |
| `document/list.html` | `/api/trips/{tripId}/documents/{docId}` | DELETE | Yes | 刪除文件 |
| `document/list.html` + `global.html` | `/api/trips/{tripId}/documents/{docId}/preview` | GET | No | 預覽文件 |
| `activity/create.html` | `/api/places/search?query=&lat=&lng=&radius=` | GET | No | 搜尋地點 |

#### Web Controller 表單提交 (HTML POST)

| 模板 | Action | 說明 |
|------|--------|------|
| `trip/create.html` | `POST /trips/create` 或 `/trips/{id}/edit` | 建立/編輯行程 |
| `activity/create.html` | `POST /trips/{id}/activities[/{activityId}]` | 建立/更新景點 |
| `activity/detail.html` + `create.html` | `POST /trips/{id}/activities/{activityId}/delete` | 刪除景點 |
| `expense/create.html` | `POST /trips/{id}/expenses[/{expenseId}]` | 建立/更新支出 |
| `profile/edit.html` | `POST /profile/edit` | 更新暱稱 |
| `invite.html` | `POST /invite/{token}/accept` | 接受邀請 |
| `profile/index.html` | `POST /logout` | 登出 |

---

### B. Orphan Endpoints (後端有但前端未使用, ~31 個)

以下 endpoint 後端已實作但前端目前未使用，標記為 **API-only**，供未來 mobile app / SPA 使用：

| 類別 | Endpoints | 說明 |
|------|----------|------|
| Auth | `GET /api/auth/me`, `POST /api/auth/logout` | API 認證，前端用 web session |
| Trip CRUD | `POST /api/trips`, `GET /api/trips`, `GET /api/trips/{tripId}`, `PUT /api/trips/{tripId}`, `DELETE /api/trips/{tripId}` | Trip 操作，前端用 web form |
| Trip Members | `GET /api/trips/{tripId}/members`, `DELETE /api/trips/{tripId}/members/me`, `GET /api/trips/{tripId}/invites`, `POST /api/invites/{token}/accept` | 部分前端用 inline JS |
| Activity CRUD | `POST /api/trips/{tripId}/activities`, `PUT /api/activities/{activityId}`, `DELETE /api/activities/{activityId}` | 前端用 web form |
| Activity Documents | `GET /api/trips/{tripId}/activities/{activityId}/documents` | 取得景點關聯文件列表 |
| Expense CRUD | `POST /api/trips/{tripId}/expenses`, `PUT /api/expenses/{expenseId}` | 前端用 web form |
| Settlement | `GET /api/trips/{tripId}/settlement`, `PUT /api/expense-splits/{splitId}/settle`, `PUT /api/expense-splits/{splitId}/unsettle`, `PUT /api/trips/{tripId}/settlement/unsettle` | 前端只用 batch settle |
| Document | `GET /api/trips/{tripId}/documents/{id}` (single), `GET /api/trips/{tripId}/documents/storage`, `GET /api/activities/{id}/documents` | 細粒度查詢 |
| Place/Direction | `GET /api/places/{placeId}`, `GET /api/directions` | 前端用 web form 建景點 |
| Exchange Rate | `GET /api/exchange-rates/latest`, `GET /api/exchange-rates/convert`, `GET /api/exchange-rates/currencies` | 前端只用基本匯率查詢 |
| Statistics | `GET /api/trips/{tripId}/statistics/category`, `GET /api/trips/{tripId}/statistics/trend`, `GET /api/trips/{tripId}/statistics/members` | 支出統計分析端點 |
| Todo | `GET /api/trips/{tripId}/todos/stats` | Todo 統計 |
| Weather | `GET /api/weather` (單一日期) | 前端用 forecast |
| Health | `GET /api/health` | 健康檢查 |

---

### C. 權限模型

| 角色 | 可做的 API 操作 |
|------|----------------|
| **OWNER** | 所有操作 + 刪除行程 + 移除成員 + 變更角色 |
| **EDITOR** | CRUD 景點/支出/Todo/文件 + 建立邀請連結 |
| **VIEWER** | 所有 GET 操作 (只讀) |
