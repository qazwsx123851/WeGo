# WeGo AI 輔助開發規範

> 更新日期：2026-02-12

## 目的

防止 AI 輔助開發時因上下文遺失導致的程式碼品質下降，建立可追溯、可驗證的開發流程。

---

## 1. 方法級契約註解 (Method Contract)

### 1.1 註解格式

每個 public 方法必須包含以下結構的 Javadoc 契約註解：

**完整版格式**（適用於複雜方法）包含以下區塊：

| 區塊 | 說明 |
|------|------|
| 功能簡述 | 一句話描述方法功能 |
| Contract | 前置條件 (Precondition)、後置條件 (Postcondition)、不變量 (Invariant) |
| Dependencies | 呼叫方 (Called by)、被呼叫方 (Calls) |
| Side Effects | 副作用列表（資料庫寫入、快取更新、外部 API 呼叫等） |
| @param | 參數說明 |
| @return | 回傳值說明 |
| @throws | 例外情境說明 |
| @see | 相關方法參考 |
| @since | 版本號 |

### 1.2 範例說明

以「建立新行程」方法為例，契約應記載：

- **前置條件**：user 必須已登入且不為 null；request.endDate >= request.startDate
- **後置條件**：回傳的 Trip 已持久化至資料庫；TripMember 已建立，role = OWNER
- **不變量**：一個 Trip 永遠只有一個 OWNER
- **呼叫方**：TripApiController#createTrip
- **被呼叫方**：TripRepository#save, TripMemberRepository#save
- **副作用**：寫入 trip 表、寫入 trip_member 表
- **例外**：當結束日期早於開始日期時拋出 BusinessException

### 1.3 簡化版（適用於簡單方法）

簡單方法可使用 `@contract` 標籤以縮寫格式記載前置條件（pre）、後置條件（post）、呼叫方（calledBy）和被呼叫方（calls）。

---

## 2. 類別級架構標記

### 2.1 類別責任標記

每個類別的 Javadoc 必須標記其在架構中的角色，包含以下資訊：

- **@layer**：架構層級
- **@responsibility**：負責的功能範圍，以及不負責的功能（指向其他 Service）
- **@collaborators**：協作的其他類別及其用途
- **@invariants**：類別層級的不變量

### 2.2 架構層級標記

| 標記 | 說明 | 允許依賴 |
|------|------|----------|
| `@layer Controller` | 處理 HTTP | Service, DTO |
| `@layer Application Service` | 業務協調 | Domain, Repository, External |
| `@layer Domain Service` | 核心邏輯 | Entity, Value Object |
| `@layer Repository` | 資料存取 | Entity |
| `@layer External` | 外部整合 | - |

---

## 3. 不變量檢查 (Invariant Checks)

### 3.1 Entity 層驗證

每個 Entity 類別應定義 `validate()` 方法，使用 assert 語句檢查所有不變量。例如 Trip 實體的不變量包括：startDate 不為 null、endDate 不為 null、endDate 不早於 startDate、title 不為空且不超過 100 字。

### 3.2 Service 層驗證

Service 方法應遵循三段式結構：

1. **驗證前置條件**：使用 Preconditions 工具類檢查參數非 null、業務規則成立
2. **執行業務邏輯**：核心處理
3. **驗證後置條件**：使用 Postconditions 工具類確認結果符合預期（如物件已持久化）

---

## 4. 架構決策記錄 (ADR)

### 4.1 ADR 檔案位置

ADR 文件存放於 `docs/adr/` 目錄，以編號命名（如 `0001-use-thymeleaf-for-frontend.md`）。

### 4.2 ADR 模板

每份 ADR 包含以下章節：

| 章節 | 內容 |
|------|------|
| 標題 | ADR-{編號}: {標題} |
| 狀態 | Proposed / Accepted / Deprecated / Superseded by ADR-xxx |
| 背景 | 描述問題背景與需求 |
| 決策 | 描述做出的決策 |
| 理由 | 為什麼選擇這個方案 |
| 後果 | 這個決策帶來的影響 |
| 相關程式碼 | 列出相關的類別路徑 |
| 修改此決策前必須考慮 | 列出修改此設計會影響的地方與必須同時修改的檔案 |

### 4.3 範例 ADR

以「ADR-003: 債務簡化演算法」為例：

- **背景**：分帳功能需要將複雜的多人債務關係簡化為最少交易次數
- **決策**：使用貪婪演算法配對最大債權人與最大債務人
- **理由**：實作簡單、時間複雜度 O(n log n)、成員數量有限（<= 10 人）效能足夠、結果直觀易理解
- **後果**：不保證全域最優解但差異可接受、需要處理浮點數精度問題
- **相關程式碼**：DebtSimplifier、SettlementService
- **修改前須考慮**：DebtSimplifierTest 的所有測試案例、SettlementService 的結算邏輯、前端結算頁面的顯示邏輯

---

## 5. AI 輔助開發檢查清單

### 5.1 修改程式碼前 (AI 必須執行)

- [ ] 已閱讀目標方法的契約註解
- [ ] 已確認 @calledBy 列出的呼叫方
- [ ] 已確認 @calls 列出的依賴方法
- [ ] 已閱讀相關 ADR 文件
- [ ] 已確認修改不會違反 @invariant
- [ ] 已確認測試案例涵蓋修改範圍

### 5.2 修改程式碼後 (AI 必須執行)

- [ ] 已更新方法的契約註解
- [ ] 已更新 @calledBy 和 @calls（若有變動）
- [ ] 已執行相關單元測試
- [ ] 已確認未破壞既有測試
- [ ] 已更新 ADR（若涉及架構變更）

### 5.3 AI Prompt 範本

當需要 AI 修改程式碼時，使用以下 prompt 格式：

> 我要修改 [類別名稱] 的 [方法名稱]。
>
> 修改目的：[描述要做什麼]
>
> 請先執行以下步驟：
> 1. 閱讀該方法的契約註解
> 2. 列出所有會受影響的呼叫方 (@calledBy)
> 3. 列出該方法的依賴 (@calls)
> 4. 確認是否有相關的 ADR
> 5. 列出相關的測試案例
>
> 確認以上資訊後，再提出修改方案，並說明：
> - 哪些檔案需要同時修改
> - 哪些測試需要更新
> - 是否違反任何 @invariant

---

## 6. 自動化檢查

### 6.1 ArchUnit 架構測試

專案使用 ArchUnit 自動驗證架構規則，主要檢查項目：

- Service 層不得依賴 Controller 層
- Domain 層不得依賴 Repository 或 External 層
- Service 類別的 public 方法應有契約文件

### 6.2 Pre-commit Hook

Git pre-commit hook 執行以下檢查：

1. 執行 ArchUnit 架構測試（`ArchitectureTest`），失敗時中止 commit
2. 掃描 Service 層 public 方法是否缺少 `@contract` 或 Javadoc 註解（警告層級）

### 6.3 CI 檢查

GitHub Actions 的架構檢查工作流程在 push 和 pull request 時觸發：

1. 執行 ArchUnit 架構測試
2. 當 domain 或 service 檔案有變更時，提醒開發者確認 ADR 是否需要更新

---

## 7. 檔案變更影響範圍快速參考

### 7.1 核心實體修改影響表

| 修改實體 | 必須同時檢查 |
|----------|-------------|
| `User` | UserRepository, TripMemberService, OAuth2UserService |
| `Trip` | TripService, TripMemberService, ActivityService, ExpenseService |
| `Activity` | ActivityService, RouteOptimizer, DocumentService |
| `Expense` | ExpenseService, SettlementService, DebtSimplifier |
| `TripMember` | PermissionChecker, TripService, 所有需要權限檢查的 Service |

### 7.2 服務層修改影響表

| 修改服務 | 必須同時檢查 |
|----------|-------------|
| `TripService` | TripApiController, ActivityService, ExpenseService |
| `ActivityService` | ActivityApiController, TripService |
| `ExpenseService` | ExpenseApiController, SettlementService |
| `SettlementService` | ExpenseApiController, DebtSimplifier |
| `PermissionChecker` | 所有 Service 的權限相關方法 |

### 7.3 演算法修改影響表

| 修改演算法 | 必須同時檢查 | 相關 ADR |
|----------|-------------|----------|
| `DebtSimplifier` | SettlementService, 結算測試案例 | ADR-003 |
| `RouteOptimizer` | ActivityService, 路線優化測試案例 | ADR-002 |

---

## 8. 註解維護規則

### 8.1 何時必須更新註解

- 修改方法簽名（參數、回傳值）
- 新增或移除依賴（@calls）
- 方法被新的地方呼叫（@calledBy）
- 修改前置/後置條件
- 修改副作用（新增 DB 寫入、API 呼叫等）

### 8.2 註解審查 Checklist

Pull Request 審查時，必須確認：

- [ ] 新增方法有完整契約註解
- [ ] 修改方法的註解已同步更新
- [ ] @calledBy 和 @calls 正確
- [ ] @invariant 未被違反
- [ ] 相關 ADR 已更新（若需要）

---

## 附錄：自訂註解（可選）

如果想要更結構化，可以建立自訂 `@Contract` 註解，以陣列屬性記錄 preconditions、postconditions、invariants、calledBy、calls、sideEffects 等資訊，實現契約的結構化宣告。使用 `@Retention(RUNTIME)` 和 `@Target(METHOD)` 確保註解可在執行期間存取。
