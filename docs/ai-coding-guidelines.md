# WeGo AI 輔助開發規範

## 目的

防止 AI 輔助開發時因上下文遺失導致的程式碼品質下降，建立可追溯、可驗證的開發流程。

---

## 1. 方法級契約註解 (Method Contract)

### 1.1 註解格式

每個 public 方法必須包含以下註解：

```java
/**
 * [簡述功能]
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Precondition:</b> [呼叫前必須滿足的條件]</li>
 *   <li><b>Postcondition:</b> [呼叫後保證的結果]</li>
 *   <li><b>Invariant:</b> [執行前後不變的條件]</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li><b>Called by:</b> [哪些方法/類別會呼叫此方法]</li>
 *   <li><b>Calls:</b> [此方法會呼叫哪些關鍵方法]</li>
 * </ul>
 *
 * <h3>Side Effects</h3>
 * <ul>
 *   <li>[列出副作用：資料庫寫入、快取更新、外部 API 呼叫等]</li>
 * </ul>
 *
 * @param xxx 參數說明
 * @return 回傳值說明
 * @throws XxxException 何時拋出
 * @see RelatedClass#relatedMethod 相關方法
 * @since 1.0.0
 */
```

### 1.2 範例

```java
/**
 * 建立新行程並設定建立者為 Owner。
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Precondition:</b> user 必須已登入且不為 null</li>
 *   <li><b>Precondition:</b> request.endDate >= request.startDate</li>
 *   <li><b>Postcondition:</b> 回傳的 Trip 已持久化至資料庫</li>
 *   <li><b>Postcondition:</b> TripMember 已建立，role = OWNER</li>
 *   <li><b>Invariant:</b> 一個 Trip 永遠只有一個 OWNER</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li><b>Called by:</b> TripApiController#createTrip</li>
 *   <li><b>Calls:</b> TripRepository#save, TripMemberRepository#save</li>
 * </ul>
 *
 * <h3>Side Effects</h3>
 * <ul>
 *   <li>寫入 trip 表</li>
 *   <li>寫入 trip_member 表</li>
 * </ul>
 *
 * @param request 建立行程的請求資料
 * @param user 當前登入用戶
 * @return 建立完成的行程資訊
 * @throws BusinessException 當結束日期早於開始日期
 * @see TripMemberService#addOwner
 * @since 1.0.0
 */
@Transactional
public TripResponse createTrip(CreateTripRequest request, User user) {
    // 實作...
}
```

### 1.3 簡化版（適用於簡單方法）

```java
/**
 * 檢查用戶是否有編輯權限。
 *
 * @contract
 *   - pre: tripId, userId 不為 null
 *   - post: 回傳 true 若用戶為 OWNER 或 EDITOR
 *   - calls: TripMemberRepository#findByTripAndUser
 *   - calledBy: ActivityService, ExpenseService, DocumentService
 */
public boolean canEdit(UUID tripId, UUID userId) {
    // 實作...
}
```

---

## 2. 類別級架構標記

### 2.1 類別責任標記

每個類別必須標記其在架構中的角色：

```java
/**
 * 行程服務 - 處理行程 CRUD 與成員管理。
 *
 * @layer Application Service
 * @responsibility
 *   - 行程的建立、查詢、更新、刪除
 *   - 成員邀請與權限管理
 *   - 不處理：景點排序（見 ActivityService）、分帳（見 ExpenseService）
 *
 * @collaborators
 *   - TripRepository: 資料存取
 *   - TripMemberRepository: 成員資料存取
 *   - PermissionChecker: 權限驗證
 *   - InviteLinkService: 邀請連結管理
 *
 * @invariants
 *   - 一個 Trip 只能有一個 OWNER
 *   - Trip 刪除時，關聯的 Activity、Expense、Document 必須同時刪除
 */
@Service
@RequiredArgsConstructor
public class TripService {
    // ...
}
```

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

### 3.1 使用 assert 或專用方法

```java
public class Trip {

    /**
     * @invariant startDate <= endDate
     * @invariant title 不為空且 <= 100 字
     * @invariant baseCurrency 為有效 ISO 4217 代碼
     */
    public void validate() {
        assert startDate != null : "startDate cannot be null";
        assert endDate != null : "endDate cannot be null";
        assert !endDate.isBefore(startDate) : "endDate must be >= startDate";
        assert title != null && !title.isBlank() : "title cannot be blank";
        assert title.length() <= 100 : "title must be <= 100 chars";
    }
}
```

### 3.2 Service 層驗證

```java
@Service
public class TripService {

    public TripResponse createTrip(CreateTripRequest request, User user) {
        // 1. 驗證前置條件
        Preconditions.checkNotNull(user, "user cannot be null");
        Preconditions.checkArgument(
            !request.getEndDate().isBefore(request.getStartDate()),
            "endDate must be >= startDate"
        );

        // 2. 執行業務邏輯
        Trip trip = // ...

        // 3. 驗證後置條件
        Postconditions.ensure(trip.getId() != null, "trip must be persisted");

        return mapper.toResponse(trip);
    }
}
```

---

## 4. 架構決策記錄 (ADR)

### 4.1 ADR 檔案位置

```
docs/adr/
├── 0001-use-thymeleaf-for-frontend.md
├── 0002-greedy-algorithm-for-route-optimization.md
├── 0003-debt-simplification-algorithm.md
├── 0004-oauth-only-authentication.md
└── template.md
```

### 4.2 ADR 模板

```markdown
# ADR-{編號}: {標題}

## 狀態
{Proposed | Accepted | Deprecated | Superseded by ADR-xxx}

## 背景
{描述問題背景與需求}

## 決策
{描述做出的決策}

## 理由
{為什麼選擇這個方案}

## 後果
{這個決策帶來的影響}

## 相關程式碼
- `com.wego.service.XxxService`
- `com.wego.domain.Xxx`

## 修改此決策前必須考慮
- {列出修改此設計會影響的地方}
- {列出必須同時修改的檔案}
```

### 4.3 範例 ADR

```markdown
# ADR-003: 債務簡化演算法

## 狀態
Accepted

## 背景
分帳功能需要將複雜的多人債務關係簡化為最少交易次數。

## 決策
使用貪婪演算法配對最大債權人與最大債務人。

## 理由
- 實作簡單，時間複雜度 O(n log n)
- 成員數量有限（≤ 10 人），效能足夠
- 結果直觀易理解

## 後果
- 不保證全域最優解，但差異可接受
- 需要處理浮點數精度問題

## 相關程式碼
- `com.wego.domain.settlement.DebtSimplifier`
- `com.wego.service.SettlementService`

## 修改此決策前必須考慮
- DebtSimplifierTest 的所有測試案例
- SettlementService 的結算邏輯
- 前端結算頁面的顯示邏輯
```

---

## 5. AI 輔助開發檢查清單

### 5.1 修改程式碼前 (AI 必須執行)

```markdown
## Pre-Modification Checklist

- [ ] 已閱讀目標方法的契約註解
- [ ] 已確認 @calledBy 列出的呼叫方
- [ ] 已確認 @calls 列出的依賴方法
- [ ] 已閱讀相關 ADR 文件
- [ ] 已確認修改不會違反 @invariant
- [ ] 已確認測試案例涵蓋修改範圍
```

### 5.2 修改程式碼後 (AI 必須執行)

```markdown
## Post-Modification Checklist

- [ ] 已更新方法的契約註解
- [ ] 已更新 @calledBy 和 @calls（若有變動）
- [ ] 已執行相關單元測試
- [ ] 已確認未破壞既有測試
- [ ] 已更新 ADR（若涉及架構變更）
```

### 5.3 AI Prompt 範本

當需要 AI 修改程式碼時，使用以下 prompt 格式：

```
我要修改 [類別名稱] 的 [方法名稱]。

修改目的：[描述要做什麼]

請先執行以下步驟：
1. 閱讀該方法的契約註解
2. 列出所有會受影響的呼叫方 (@calledBy)
3. 列出該方法的依賴 (@calls)
4. 確認是否有相關的 ADR
5. 列出相關的測試案例

確認以上資訊後，再提出修改方案，並說明：
- 哪些檔案需要同時修改
- 哪些測試需要更新
- 是否違反任何 @invariant
```

---

## 6. 自動化檢查

### 6.1 ArchUnit 架構測試

```java
@AnalyzeClasses(packages = "com.wego")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..repository..", "..external..");

    @ArchTest
    static final ArchRule services_should_have_contract_javadoc =
        classes()
            .that().resideInAPackage("..service..")
            .and().arePublic()
            .should(haveContractDocumentation());
}
```

### 6.2 Pre-commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "Running architecture tests..."
./mvnw test -Dtest=ArchitectureTest

if [ $? -ne 0 ]; then
    echo "Architecture tests failed. Commit aborted."
    exit 1
fi

echo "Checking contract annotations..."
# 檢查是否有 public method 缺少 @contract 或 Javadoc
grep -r "public.*(" src/main/java/com/wego/service/*.java | \
    grep -v "@contract\|/\*\*" && {
    echo "Warning: Some public methods may lack contract documentation"
}
```

### 6.3 CI 檢查

```yaml
# .github/workflows/architecture.yml
name: Architecture Check

on: [push, pull_request]

jobs:
  arch-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run ArchUnit Tests
        run: ./mvnw test -Dtest=ArchitectureTest

      - name: Check ADR Changes
        run: |
          # 如果修改了 domain 或 service，檢查是否需要更新 ADR
          CHANGED_FILES=$(git diff --name-only HEAD~1)
          if echo "$CHANGED_FILES" | grep -q "src/main/java/com/wego/domain\|src/main/java/com/wego/service"; then
            echo "Domain/Service files changed. Please ensure ADRs are up to date."
          fi
```

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

如果想要更結構化，可以建立自訂註解：

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Contract {
    String[] preconditions() default {};
    String[] postconditions() default {};
    String[] invariants() default {};
    String[] calledBy() default {};
    String[] calls() default {};
    String[] sideEffects() default {};
}

// 使用方式
@Contract(
    preconditions = {"user != null", "request.endDate >= request.startDate"},
    postconditions = {"回傳的 Trip 已持久化", "TripMember 已建立"},
    invariants = {"一個 Trip 只有一個 OWNER"},
    calledBy = {"TripApiController#createTrip"},
    calls = {"TripRepository#save", "TripMemberRepository#save"},
    sideEffects = {"寫入 trip 表", "寫入 trip_member 表"}
)
public TripResponse createTrip(CreateTripRequest request, User user) {
    // ...
}
```
