# WeGo TDD 測試開發指南

> 更新日期：2026-02-12

## 1. TDD 概述

### 1.1 什麼是 TDD

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

### 1.2 TDD 的好處

| 優點 | 說明 |
|------|------|
| 更好的設計 | 先思考如何使用 API，產生更好的介面設計 |
| 即時回饋 | 每次修改都能立即驗證是否破壞既有功能 |
| 文件化 | 測試即文件，展示程式碼的預期行為 |
| 信心重構 | 有完整測試覆蓋，重構時更有信心 |
| 減少 Debug | 問題在早期就被發現 |

### 1.3 WeGo 專案的測試策略

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

## 2. 測試環境設定

### 2.1 Maven 依賴

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

### 2.2 測試設定檔

測試設定檔位於 `src/test/resources/application-test.yml`，主要配置包括：

- 資料來源使用 H2 記憶體資料庫（`jdbc:h2:mem:testdb`）
- JPA 使用 `create-drop` 模式自動建立與銷毀結構
- OAuth2 使用測試用 client-id 和 client-secret
- 停用所有外部 API（Google Maps、Weather、Exchange Rate 設為 `enabled: false`）

### 2.3 測試目錄結構

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

## 3. 單元測試 (Unit Tests)

### 3.1 命名規範

測試方法命名格式為 `methodName_scenario_expectedBehavior`，例如：

- `createTrip_withValidInput_shouldReturnCreatedTrip`
- `createTrip_withNullTitle_shouldThrowValidationException`
- `calculateSettlement_withThreeMembers_shouldReturnSimplifiedDebts`

### 3.2 Service 層測試

Service 層測試使用 `@ExtendWith(MockitoExtension.class)` 搭配 `@Mock` 和 `@InjectMocks` 建立測試環境。關鍵要點：

- 以 `@Mock` 模擬 Repository 和外部依賴
- 以 `@InjectMocks` 自動注入被測試的 Service
- 使用 `@BeforeEach` 準備通用測試資料（透過 Fixture 工廠）
- 使用 `@Nested` 和 `@DisplayName` 依功能分組測試
- 使用 Given-When-Then 三段式結構撰寫每個測試
- 以 `verify()` 驗證 Repository 被正確呼叫
- 以 `assertThatThrownBy()` 驗證例外情況
- 使用 `argThat()` 驗證傳入 Repository 的參數內容

### 3.3 Domain 層測試

Domain 層測試為純邏輯測試，不需要 Mock 框架。直接建立被測試物件，提供輸入資料，驗證輸出結果。

測試重點包括：

- 驗證核心演算法的正確性（如債務簡化計算）
- 測試正常情境（如三人均分、四人多筆支出）
- 測試已結清情境（淨額為零應回傳空清單）
- 使用有意義的測試資料（如具體金額、具體成員配置）

### 3.4 路線優化測試

路線優化測試驗證最近鄰居演算法的排序結果：

- 三個以上景點應按最短路徑排序
- 兩個以下景點應直接返回原列表
- 空列表應返回空列表
- 使用具體座標驗證排序結果

---

## 4. 整合測試 (Integration Tests)

### 4.1 Repository 整合測試

Repository 整合測試使用 `@DataJpaTest` 搭配 Testcontainers 啟動真實 PostgreSQL 容器。關鍵要點：

- 使用 `@Container` 宣告 PostgreSQL 容器
- 使用 `@DynamicPropertySource` 動態配置資料來源
- 使用 `TestEntityManager` 進行 flush 和 clear 確保測試隔離
- 測試自訂查詢方法的正確性（如依用戶 ID 查詢、依日期範圍查詢）

### 4.2 API 整合測試

API 整合測試使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 搭配 `MockMvc`。關鍵要點：

- 使用 `@ActiveProfiles("test")` 載入測試設定
- 在 `@BeforeEach` 中清理資料庫並建立測試用戶
- 使用 `@Nested` 依 API 端點分組（如 `POST /api/trips`、`GET /api/trips/{tripId}`）
- 驗證 HTTP 狀態碼、回應 JSON 結構、資料庫狀態
- 測試情境包括：有效請求（201）、驗證失敗（400）、未認證（401）、無權限（403）、不存在（404）

---

## 5. 測試資料工廠 (Test Fixtures)

測試資料工廠位於 `fixture/` 目錄，為每個核心實體提供工廠方法，用於快速建立測試資料。

### 設計原則

| 原則 | 說明 |
|------|------|
| 計數器遞增 | 使用 `AtomicLong` 確保每次生成唯一資料（如 email、nickname） |
| 合理預設值 | 提供有意義的預設值（如日期設為未來 7-10 天） |
| 可覆寫 | 提供多載方法允許覆寫特定欄位（如指定 email、金額、幣別） |
| 關聯建立 | 提供建立關聯資料的輔助方法（如均分分攤明細） |

### 可用的 Fixture 類別

| 類別 | 說明 |
|------|------|
| UserFixture | 建立測試用戶，可指定 email 和 nickname |
| TripFixture | 建立測試行程，可指定 ID、日期範圍 |
| ActivityFixture | 建立測試景點 |
| ExpenseFixture | 建立測試支出，可指定金額、幣別；提供均分分攤明細產生方法 |

---

## 6. 測試覆蓋率目標

### 6.1 覆蓋率要求

| 層級 | 行覆蓋率目標 | 分支覆蓋率目標 |
|------|-------------|---------------|
| Service | >= 80% | >= 70% |
| Domain | >= 90% | >= 85% |
| Controller | >= 70% | >= 60% |
| Repository | >= 60% | - |
| **整體** | **>= 80%** | **>= 70%** |

### 6.2 排除覆蓋率計算的項目

JaCoCo 排除以下類別的覆蓋率計算：

- `WegoApplication`（啟動類）
- `config/**`（設定類）
- `entity/**`（實體類）
- `dto/**`（資料傳輸物件）
- `exception/**`（例外類）

### 6.3 執行測試與產生報告

| 指令 | 說明 |
|------|------|
| `./mvnw test` | 執行單元測試 |
| `./mvnw verify` | 執行整合測試 |
| `./mvnw jacoco:report` | 產生覆蓋率報告 |

報告位置：`target/site/jacoco/index.html`

---

## 7. TDD 工作流程

### 7.1 開發一個新功能的步驟

以「新增景點」功能為例：

**Step 1: 寫測試 (RED)**

撰寫一個測試方法，呼叫尚不存在的 `activityService.createActivity()` 方法，斷言回傳結果不為 null 且包含預期的景點名稱。執行測試，確認測試失敗（因為方法尚未實作）。

**Step 2: 寫程式碼 (GREEN)**

實作最小程式碼使測試通過：建立 Activity 物件、設定 ID、儲存至 Repository、透過 Mapper 轉換為 Response 回傳。執行測試，確認測試通過。

**Step 3: 重構 (REFACTOR)**

加入完整的業務邏輯：權限檢查、找或建立地點、設定排序順序等。使用 Builder 模式改善程式碼可讀性。執行測試，確認測試仍然通過。

### 7.2 TDD Checklist

開發新功能時，確認以下項目：

- [ ] 先寫測試，測試失敗
- [ ] 寫最小程式碼讓測試通過
- [ ] 重構程式碼
- [ ] 所有測試仍然通過
- [ ] 測試覆蓋正常情況與邊界情況
- [ ] 測試命名清楚描述預期行為

---

## 8. 持續整合 (CI) 設定

### GitHub Actions 工作流程

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

## 9. 常見問題與最佳實踐

### 9.1 測試應該獨立

每個測試方法必須獨立運作，不依賴其他測試的執行結果或執行順序。不要使用 `@Order` 建立測試間的依賴關係。每個測試應在自身的 `@BeforeEach` 或測試方法中建立所需的資料。

### 9.2 避免過度 Mock

只 Mock 外部依賴（如 Repository、外部 API），不要 Mock 核心業務邏輯。例如，測試結算功能時，應 Mock `ExpenseRepository` 但使用真實的 `DebtSimplifier`，以確保測試真正驗證了業務邏輯的正確性。

### 9.3 測試邊界條件

使用 `@Nested` 和 `@DisplayName` 組織邊界條件測試群組，至少涵蓋以下情境：

- 空列表
- 單一元素
- 最大值
- null 輸入

### 9.4 使用有意義的測試資料

測試資料應反映真實使用場景，使測試具有文件化效果。例如，分帳測試使用「團體晚餐 3000 元」而非無意義的 `123.45`。測試方法名稱應包含業務語境，如 `calculateSettlement_withDinnerExpense_shouldSplitEvenly`。
