# WeGo Bug 追蹤記錄

## 文件資訊

| 項目 | 內容 |
|------|------|
| 建立日期 | 2026-02-03 |
| 最後更新 | 2026-02-03 |
| 測試環境 | localhost:8080 |
| 測試方式 | Chrome DevTools MCP E2E 測試 |
| 安全審查 | security-reviewer agent |
| 整體風險等級 | **CRITICAL** (後端有 IDOR 及認證繞過漏洞) |

---

## E2E 測試總結

### 測試概要

| 測試項目 | 狀態 | 通過率 |
|---------|:----:|:------:|
| 首頁與登入流程 | ✅ | 100% |
| Dashboard 與行程列表 | ✅ | 100% |
| 行程詳情與景點管理 | ✅ | 100% |
| 分帳與文件功能 | ✅ | 100% |
| 全域概覽頁面 | ✅ | 100% |

### 通過的功能

| 頁面/功能 | 路由 | 測試結果 |
|----------|------|---------|
| 首頁載入 | `/` | ✅ 正常 |
| Google OAuth 跳轉 | `/oauth2/authorization/google` | ✅ 正常 |
| Dashboard | `/dashboard` | ✅ 行程卡片、歡迎訊息正常 |
| 行程詳情頁 | `/trips/{id}` | ✅ 封面、天氣預報、快捷統計 |
| 景點列表 | `/trips/{id}/activities` | ✅ 日期選擇、交通時間、拖曳排序 |
| 景點詳情 | `/trips/{id}/activities/{id}` | ✅ 地圖整合、交通資訊 |
| 編輯景點 | `/trips/{id}/activities/{id}/edit` | ✅ 表單正常載入 |
| 分帳列表頁 | `/trips/{id}/expenses` | ✅ 總覽卡片正常 |
| 新增支出 | `/trips/{id}/expenses/create` | ✅ 表單正常載入 |
| 檔案管理 | `/trips/{id}/documents` | ✅ 分類篩選、上傳對話框 |
| 全域分帳總覽 | `/expenses` | ✅ 統計卡片正常 |
| 全域檔案總覽 | `/documents` | ✅ 搜尋、篩選功能 |
| 個人檔案 | `/profile` | ✅ 資訊、統計、快速操作 |

---

## 待修復 Bug

*（目前無待修復的功能性 Bug）*

---

## 安全審查問題

> 審查日期: 2026-02-03
> 審查工具: security-reviewer agent
> 風險等級: MEDIUM

### 問題摘要

| 嚴重度 | 數量 |
|--------|:----:|
| **HIGH** | 3 |
| **MEDIUM** | 3 |
| **LOW** | 1 |

---

### SEC-001: XSS 風險 - th:utext 直接輸出

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | Cross-Site Scripting (XSS) |
| **檔案** | `templates/error/error.html:58` |

**問題描述**

使用 `th:utext` 直接輸出 `errorMessage`，若訊息可帶入使用者字串，會有 XSS 風險。

**現有程式碼**

```html
<p class="text-body text-gray-500 dark:text-gray-400 mb-6"
   th:utext="${errorMessage != null ? errorMessage : '...'}">
```

**修復方案**

```html
<!-- 改用 th:text + CSS whitespace-pre-line -->
<p class="text-body text-gray-500 dark:text-gray-400 mb-6 whitespace-pre-line"
   th:text="${errorMessage != null ? errorMessage : '發生了未預期的錯誤，請稍後再試。'}">
```

並更新 Controller 中的訊息，將 `<br/>` 改為 `\n`。

---

### SEC-002: JavaScript 注入 - th:onclick 拼接

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | JavaScript Injection |
| **檔案** | `templates/document/list.html:62` |

**問題描述**

`th:onclick` 直接拼接 `category.value` 到 JS 字串，若值含引號會破壞 JS，甚至造成注入。

**現有程式碼**

```html
<button th:each="category : ${categories}"
        th:onclick="'filterByCategory(\'' + ${category.value} + '\')'">
```

**攻擊範例**

若 `category.value` = `'); alert('XSS'); ('`，結果為：
```javascript
filterByCategory(''); alert('XSS'); ('')
```

**修復方案**

```html
<!-- 使用 data-* 屬性 -->
<button th:each="category : ${categories}"
        th:data-category="${category.value}"
        onclick="filterByCategory(this.dataset.category)"
        th:text="${category.label}">
```

---

### SEC-003: JavaScript 注入 - 成員角色變更

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | JavaScript Injection |
| **檔案** | `templates/trip/members.html:226,234` |

**問題描述**

`th:onclick` 使用 Thymeleaf 字面語法 `|...|` 直接拼接 UUID 到 JavaScript 函數呼叫。

**現有程式碼**

```html
<button type="button"
        th:onclick="|changeRole('${trip.id}', '${member.userId}', 'EDITOR')|">
```

**修復方案**

```html
<button type="button"
        th:data-trip-id="${trip.id}"
        th:data-user-id="${member.userId}"
        th:data-role="EDITOR"
        onclick="changeRole(this.dataset.tripId, this.dataset.userId, this.dataset.role)">
```

---

### SEC-004: JavaScript 語法錯誤 - UUID 未加引號

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | MEDIUM |
| **類別** | JavaScript Syntax Error |
| **檔案** | `templates/todo/list.html:134` |

**問題描述**

`TodoUI.showEditModal(${todo.id})` 沒有加引號，UUID 會產生非法 JS（被當成減號運算）。

**現有程式碼**

```html
<div th:onclick="'TodoUI.showEditModal(' + ${todo.id} + ')'">
```

**結果**（無效 JavaScript）：
```javascript
TodoUI.showEditModal(550e8400-e29b-41d4-a716-446655440000)
```

**修復方案**

```html
<div th:data-todo-id="${todo.id}"
     onclick="TodoUI.showEditModal(this.dataset.todoId)">
```

---

### SEC-005: SpEL Null 比較異常

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | MEDIUM |
| **類別** | SpEL Null Handling |
| **檔案** | `templates/fragments/components.html:128` |

**問題描述**

`trip.memberCount > 0` 若 `memberCount` 為 null 會觸發 SpEL 例外。

**現有程式碼**

```html
<div class="flex -space-x-2" th:if="${trip.memberCount > 0}">
```

**修復方案**

```html
<!-- 方案 1: 明確檢查 null -->
<div class="flex -space-x-2" th:if="${trip.memberCount != null and trip.memberCount > 0}">

<!-- 方案 2: 使用 Elvis 運算子 -->
<div class="flex -space-x-2" th:if="${(trip.memberCount ?: 0) > 0}">
```

---

### SEC-006: SpEL Null 例外 - filter 物件

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | MEDIUM |
| **類別** | SpEL Null Handling |
| **檔案** | `templates/document/global-overview.html:193-200` |

**問題描述**

多處 `filter.hasFilters()` 未防 null；若 Controller 忘了傳遞 filter 會直接 SpEL 例外。

**現有程式碼**

```html
<h3 th:if="${filter.hasFilters()}">找不到符合的檔案</h3>
<h3 th:unless="${filter.hasFilters()}">還沒有任何檔案</h3>
```

**修復方案**

```html
<h3 th:if="${filter != null and filter.hasFilters()}">找不到符合的檔案</h3>
<h3 th:unless="${filter != null and filter.hasFilters()}">還沒有任何檔案</h3>
```

---

### SEC-007: DOM-based XSS - innerHTML 插入

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | DOM-based XSS |
| **檔案** | `templates/document/list.html:609` |

**問題描述**

在 `openDocumentDetail` 函數中，`thumbnailImg.src` 直接插入 innerHTML，若被操控可能導致 XSS。

**現有程式碼**

```javascript
if (thumbnailImg) {
    previewArea.innerHTML = `<img src="${thumbnailImg.src}" alt="${fileName}" class="..."/>`;
}
```

**修復方案**

```javascript
// 使用 DOM API 代替 innerHTML
previewArea.innerHTML = '';
const img = document.createElement('img');
img.src = thumbnailImg.src;
img.alt = fileName;
img.className = 'max-w-full max-h-full object-contain rounded-lg';
previewArea.appendChild(img);
```

---

## 安全修復優先順序

| 優先級 | ID | 問題 | 原因 |
|:------:|-----|------|------|
| 1 | SEC-002 | JS 注入 (document/list.html) | 可被利用執行任意 JS |
| 2 | SEC-003 | JS 注入 (members.html) | 可被利用執行任意 JS |
| 3 | SEC-007 | DOM XSS (list.html) | innerHTML 注入風險 |
| 4 | SEC-001 | XSS (error.html) | 目前訊息硬編碼，風險較低 |
| 5 | SEC-004 | JS 語法錯誤 (todo) | 功能無法正常運作 |
| 6 | SEC-005 | SpEL null (components) | 可能導致頁面崩潰 |
| 7 | SEC-006 | SpEL null (global-overview) | 可能導致頁面崩潰 |

---

## 後端安全審查問題

> 審查日期: 2026-02-03
> 審查工具: security-reviewer agent
> 風險等級: CRITICAL

### 問題摘要

| 嚴重度 | 數量 |
|--------|:----:|
| **CRITICAL** | 2 |
| **HIGH** | 2 |
| **MEDIUM** | 2 |
| **LOW** | 1 |

---

### BE-SEC-001: 跨行程文件洩漏 (IDOR)

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | CRITICAL |
| **類別** | Insecure Direct Object Reference (IDOR) |
| **檔案** | `src/main/java/com/wego/service/DocumentService.java:341` |

**問題描述**

`getDocumentsByActivity(activityId, tripId, userId)` 只用 `activityId` 查詢文件，未驗證該 activity 是否屬於傳入的 `tripId`。

**攻擊情境**

```
GET /trips/{tripId-A}/activities/{activityId-from-B}/documents

1. permissionChecker.canView(tripId-A, userId) ✅ 通過（攻擊者是 Trip A 成員）
2. documentRepository.findByRelatedActivityId(activityId-from-B)
   → 回傳 Trip B 的文件！❌ 跨行程洩漏
```

**現有程式碼**

```java
@Transactional(readOnly = true)
public List<DocumentResponse> getDocumentsByActivity(UUID activityId, UUID tripId, UUID userId) {
    if (!permissionChecker.canView(tripId, userId)) {
        throw new ForbiddenException("您沒有權限查看此行程的檔案");
    }
    // 缺少：驗證 activity 是否屬於 tripId
    List<Document> documents = documentRepository.findByRelatedActivityId(activityId);
    // ...
}
```

**修復方案**

```java
@Transactional(readOnly = true)
public List<DocumentResponse> getDocumentsByActivity(UUID activityId, UUID tripId, UUID userId) {
    if (!permissionChecker.canView(tripId, userId)) {
        throw new ForbiddenException("您沒有權限查看此行程的檔案");
    }

    // 新增：驗證 activity 屬於該 trip
    Activity activity = activityRepository.findById(activityId)
        .orElseThrow(() -> new ResourceNotFoundException("景點不存在"));

    if (!activity.getTripId().equals(tripId)) {
        throw new ForbiddenException("該景點不屬於此行程");
    }

    List<Document> documents = documentRepository.findByRelatedActivityId(activityId);
    // ...
}
```

---

### BE-SEC-002: 匿名用戶被授權為固定使用者

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | CRITICAL |
| **類別** | Authentication Bypass |
| **檔案** | `ExpenseApiController.java:220`, `DocumentApiController.java:236`, `TodoApiController.java:202` |

**問題描述**

多個 API Controller 在 `principal == null` 時回傳固定 UUID，等同「匿名被授權為固定使用者」。

**現有程式碼**

```java
// ExpenseApiController.java line 220-237
private UUID getCurrentUserId(OAuth2User principal) {
    if (principal == null) {
        // For testing purposes, generate a consistent UUID
        return UUID.fromString("00000000-0000-0000-0000-000000000001");  // CRITICAL!
    }
    // ...
    return UUID.fromString("00000000-0000-0000-0000-000000000001");  // CRITICAL!
}
```

**攻擊情境**

1. 攻擊者發送未認證的 API 請求
2. Controller 將 userId 設為 `00000000-0000-0000-0000-000000000001`
3. 如果此 UUID 是某個真實用戶，攻擊者可以：
   - 建立/修改/刪除該用戶有權限的支出
   - 上傳/刪除文件
   - 建立/修改/刪除 Todo

**修復方案**

```java
private UUID getCurrentUserId(OAuth2User principal) {
    if (principal == null) {
        throw new UnauthorizedException("認證已過期，請重新登入");
    }
    String sub = principal.getAttribute("sub");
    if (sub == null) {
        throw new UnauthorizedException("無效的認證資訊");
    }
    try {
        return UUID.fromString(sub);
    } catch (IllegalArgumentException e) {
        return UUID.nameUUIDFromBytes(sub.getBytes());
    }
}
```

---

### BE-SEC-003: 刪除行程時關聯資料殘留

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | Data Integrity |
| **檔案** | `src/main/java/com/wego/service/TripService.java:260-273` |

**問題描述**

`deleteTrip` 只刪除成員與 Trip 本身，其他關聯資料（activities/expenses/documents/todos、儲存桶）會殘留。

**現有程式碼**

```java
@Transactional
public void deleteTrip(UUID tripId, UUID userId) {
    Trip trip = findTripById(tripId);

    if (!permissionChecker.canDelete(tripId, userId)) {
        throw new ForbiddenException("只有行程建立者可以刪除行程");
    }

    // Delete related entities (in proper order for FK constraints)
    tripMemberRepository.deleteByTripId(tripId);  // 只刪除成員
    tripRepository.delete(trip);                   // 只刪除行程
    // 缺少：activities, expenses, documents, todos, invite links
}
```

**殘留資料**

| 資料類型 | Repository 方法 | 狀態 |
|----------|-----------------|------|
| Activity | `activityRepository.deleteByTripId()` | ❌ 未調用 |
| Expense | `expenseRepository.deleteByTripId()` | ❌ 未調用 |
| ExpenseSplit | `expenseSplitRepository.deleteByExpenseId()` | ❌ 未調用 |
| Document | `documentRepository.deleteByTripId()` | ❌ 未調用 |
| Todo | `todoRepository.deleteByTripId()` | ❌ 未調用 |
| InviteLink | `inviteLinkRepository.deleteByTripId()` | ❌ 未調用 |
| Storage 檔案 | `storageClient.deleteFile()` | ❌ 未調用 |

**修復方案**

```java
@Transactional
public void deleteTrip(UUID tripId, UUID userId) {
    Trip trip = findTripById(tripId);

    if (!permissionChecker.canDelete(tripId, userId)) {
        throw new ForbiddenException("只有行程建立者可以刪除行程");
    }

    // 1. Delete expense splits first (FK to expense)
    List<Expense> expenses = expenseRepository.findByTripId(tripId);
    expenses.forEach(e -> expenseSplitRepository.deleteByExpenseId(e.getId()));

    // 2. Delete expenses
    expenseRepository.deleteByTripId(tripId);

    // 3. Delete documents (also clean storage)
    documentRepository.findByTripIdOrderByCreatedAtDesc(tripId).forEach(doc -> {
        storageClient.deleteFile(storageBucket, doc.getTripId() + "/" + doc.getFileName());
    });
    documentRepository.deleteByTripId(tripId);

    // 4. Delete activities
    activityRepository.deleteByTripId(tripId);

    // 5. Delete todos
    todoRepository.deleteByTripId(tripId);

    // 6. Delete invite links
    inviteLinkRepository.deleteByTripId(tripId);

    // 7. Delete members
    tripMemberRepository.deleteByTripId(tripId);

    // 8. Delete trip cover image
    if (trip.getCoverImageUrl() != null) {
        deleteCoverImage(trip.getCoverImageUrl());
    }

    // 9. Finally delete trip
    tripRepository.delete(trip);
}
```

---

### BE-SEC-004: 更新支出時缺少 splits 驗證

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | Data Integrity (Financial) |
| **檔案** | `src/main/java/com/wego/service/ExpenseService.java:205-231` |

**問題描述**

`updateExpense` 在變更 `splitType` 或 `amount` 時，沒有重新驗證 splits 的總和/百分比/份數一致性。

**攻擊情境**

```
1. 建立 CUSTOM expense: amount=1000, splits=[A:500, B:500]
2. 更新 expense: amount=2000 (不提供 splitType)
3. 結果: amount=2000, splits=[A:500, B:500] (總額不符!)
```

**現有程式碼**

```java
// 只處理 EQUAL 類型的重算，CUSTOM/PERCENTAGE/SHARES 被忽略
} else if (request.getAmount() != null && expense.getSplitType() == SplitType.EQUAL) {
    // Recalculate equal splits if amount changed
    // ...
}
// 缺少：CUSTOM splits 的驗證
```

**修復方案**

```java
} else if (request.getAmount() != null) {
    if (expense.getSplitType() == SplitType.EQUAL) {
        // ... existing EQUAL logic
    } else if (expense.getSplitType() == SplitType.CUSTOM) {
        throw new BusinessException("SPLITS_REQUIRED",
            "變更金額時需要重新提供 CUSTOM 分帳明細");
    }
    // PERCENTAGE and SHARES don't need recalculation as they're ratio-based
}
```

---

### BE-SEC-005: Cover image 刪除用錯 bucket

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | MEDIUM |
| **類別** | Resource Leak |
| **檔案** | `src/main/java/com/wego/service/TripService.java:605-627` |

**問題描述**

`extractStoragePath()` 用於解析 cover image URL，但錯誤地使用 `storageBucket` (documents) 而非 `coverImageBucket` (trip-covers)。

**現有程式碼**

```java
private String extractStoragePath(String url) {
    String marker = "/public/" + supabaseProperties.getStorageBucket() + "/";  // 錯誤！
    // storageBucket = "documents"
    // coverImageBucket = "trip-covers"
    // ...
}
```

**影響**

- URL 解析永遠找不到匹配的 marker，回傳 `null`
- 舊的 cover image 永遠不會被刪除
- Storage 空間浪費（孤兒檔案累積）

**修復方案**

```java
private String extractStoragePath(String url) {
    String marker = "/public/" + supabaseProperties.getCoverImageBucket() + "/";
    // ...
}
```

---

### BE-SEC-006: 建立支出時未驗證用戶是否為行程成員

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | MEDIUM |
| **類別** | Authorization |
| **檔案** | `src/main/java/com/wego/service/ExpenseService.java:95-108` |

**問題描述**

`createExpense` 未驗證 `paidBy` 與 `splits` 內的 `userId` 是否為行程成員。

**現有程式碼**

```java
Expense expense = Expense.builder()
        .tripId(tripId)
        .paidBy(request.getPaidBy())      // 未驗證是否為成員
        // ...
        .build();

// splits 中的 userId 也未驗證
splits.add(ExpenseSplit.builder()
        .userId(splitReq.getUserId())  // 未驗證是否為成員
        // ...
        .build());
```

**修復方案**

```java
private void validateExpenseUsers(UUID tripId, UUID paidBy, List<SplitRequest> splits) {
    // Validate paidBy is a member
    if (!tripMemberRepository.existsByTripIdAndUserId(tripId, paidBy)) {
        throw new ValidationException("INVALID_PAYER", "付款人必須是行程成員");
    }

    // Validate all split users are members
    if (splits != null) {
        Set<UUID> memberIds = tripMemberRepository.findByTripId(tripId).stream()
                .map(TripMember::getUserId)
                .collect(Collectors.toSet());

        for (SplitRequest split : splits) {
            if (!memberIds.contains(split.getUserId())) {
                throw new ValidationException("INVALID_SPLIT_USER",
                    "分帳對象必須是行程成員: " + split.getUserId());
            }
        }
    }
}
```

---

### BE-SEC-007: N+1 查詢問題（效能）

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 已優化但未使用 |
| **嚴重度** | LOW |
| **類別** | Performance |
| **檔案** | `src/main/java/com/wego/service/TodoService.java:123` |

**問題描述**

`getTodosByTrip()` 使用 `buildTodoResponse()` (單筆版本) 而非已實作的 `buildTodoResponses()` (批次優化版本)。

**現有程式碼**

```java
// Line 123-136 - 目前使用的方法
public List<TodoResponse> getTodosByTrip(UUID tripId, UUID userId) {
    List<Todo> todos = todoRepository.findByTripIdOrderedByDueDateAndStatus(tripId);
    return todos.stream()
            .map(this::buildTodoResponse)  // N+1 問題
            .collect(Collectors.toList());
}

// Line 358-396 - 已實作但未使用的優化版本
private List<TodoResponse> buildTodoResponses(List<Todo> todos) {
    // 批次查詢所有用戶，避免 N+1
    // ...
}
```

**修復方案**

```java
public List<TodoResponse> getTodosByTrip(UUID tripId, UUID userId) {
    List<Todo> todos = todoRepository.findByTripIdOrderedByDueDateAndStatus(tripId);
    return buildTodoResponses(todos);  // 使用批次優化版本
}
```

---

## 後端安全修復優先順序

| 優先級 | ID | 問題 | 原因 |
|:------:|-----|------|------|
| 1 | BE-SEC-001 | IDOR 跨行程文件洩漏 | 可讀取任意行程的文件 |
| 2 | BE-SEC-002 | 匿名用戶認證繞過 | 未認證可執行操作 |
| 3 | BE-SEC-003 | 刪除行程資料殘留 | 資料完整性問題 |
| 4 | BE-SEC-004 | Splits 驗證缺失 | 財務資料不一致 |
| 5 | BE-SEC-005 | Cover image bucket 錯誤 | 儲存空間浪費 |
| 6 | BE-SEC-006 | 成員驗證缺失 | 資料一致性問題 |
| 7 | BE-SEC-007 | N+1 查詢 | 效能問題 |

---

## Thymeleaf 安全最佳實踐

### 1. 永遠不要對使用者資料使用 th:utext
```html
<!-- 禁止 -->
<p th:utext="${userInput}">

<!-- 正確 -->
<p th:text="${userInput}">
```

### 2. 使用 data-* 屬性處理 JavaScript
```html
<!-- 避免 -->
<button th:onclick="'doSomething(\'' + ${value} + '\')'"">

<!-- 推薦 -->
<button th:data-value="${value}" onclick="doSomething(this.dataset.value)">
```

### 3. 始終處理 Null 值
```html
<!-- 風險 -->
<span th:text="${object.property}">

<!-- 安全 -->
<span th:if="${object != null}" th:text="${object.property}">
```

---

## 程式碼審查結果 (Phase 3)

> 審查日期: 2026-02-03
> 審查工具: code-reviewer agent
> 審查範圍: Phase 3 分帳進階功能 (14 檔案, +616/-39 行)
> 審查結論: ⚠️ Warning - 可合併但需注意

### 審查摘要

| 嚴重度 | 數量 |
|--------|:----:|
| **CRITICAL** | 0 |
| **HIGH** | 3 |
| **MEDIUM** | 5 |
| **SUGGESTIONS** | 3 |

---

### CR-HIGH-001: API 端點缺少認證要求

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | Authentication |
| **檔案** | `controller/api/ExchangeRateApiController.java:48-62` |

**問題描述**

`/api/exchange-rates` 端點是公開的，未要求登入。雖然匯率資料本身不敏感，但可能被濫用進行 DoS 攻擊或消耗 API 配額。

**修復方案**

```java
@GetMapping
@PreAuthorize("isAuthenticated()")  // 添加此行
public ResponseEntity<ApiResponse<ExchangeRateResponse>> getRate(
        @RequestParam String from,
        @RequestParam String to) {
    // ...
}
```

---

### CR-HIGH-002: 缺少 Rate Limiting 實作

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | Rate Limiting |
| **檔案** | `controller/api/ExchangeRateApiController.java:22-23` |

**問題描述**

契約註解中提到 "TODO: implement" rate limiting，但實際尚未實作。外部 API 有配額限制，惡意使用者可能快速消耗配額。

**現有程式碼**

```java
// * @contract
// *   - Rate limiting: 30 requests per minute per user (TODO: implement)
```

**修復方案**

使用 Spring Boot 的 Rate Limiter 或 Bucket4j：

```java
@RateLimiter(name = "exchangeRate", fallbackMethod = "rateLimitFallback")
@GetMapping
public ResponseEntity<ApiResponse<ExchangeRateResponse>> getRate(...) {
```

---

### CR-HIGH-003: 匯率轉換失敗時靜默回退

| 屬性 | 內容 |
|------|------|
| **狀態** | 🔴 Open |
| **嚴重度** | HIGH |
| **類別** | Data Integrity (Financial) |
| **檔案** | `service/SettlementService.java:308-314` |

**問題描述**

當匯率轉換失敗時，回退使用原始金額可能導致結算計算嚴重錯誤。例如 100 USD 應該等於 3150 TWD，但回退時會變成 100 TWD。

**現有程式碼**

```java
} catch (Exception e) {
    log.error("Failed to convert {} {} to {}: {}", amount, fromCurrency, baseCurrency, e.getMessage());
    // Fallback: return original amount (may cause incorrect calculations, but service keeps running)
    return amount;  // 這裡可能導致財務錯誤！
}
```

**修復方案**

```java
} catch (Exception e) {
    log.error("Failed to convert {} {} to {}: {}", amount, fromCurrency, baseCurrency, e.getMessage());
    throw new ExchangeRateException("CONVERSION_FAILED",
        String.format("無法轉換 %s %s 到 %s，請稍後再試", amount, fromCurrency, baseCurrency));
}
```

或在結算頁面顯示警告訊息標記「部分匯率轉換失敗」。

---

### CR-MEDIUM-001: 前端匯率快取無過期機制

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Cache |
| **檔案** | `static/js/expense.js:231-254` |

**問題描述**

JavaScript 中的匯率快取沒有 TTL，整個頁面生命週期都使用同一個匯率。

**修復方案**

```javascript
// 添加 TTL 檢查（例如 5 分鐘）
const CACHE_TTL_MS = 5 * 60 * 1000;
if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    return cached.rate;
}
```

---

### CR-MEDIUM-002: 前端缺少貨幣代碼驗證

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Input Validation |
| **檔案** | `static/js/expense.js:240` |

**問題描述**

前端直接將貨幣代碼傳送到 API，沒有驗證格式。

**修復方案**

```javascript
const currencyPattern = /^[A-Z]{3}$/;
if (!currencyPattern.test(from) || !currencyPattern.test(to)) {
    console.warn('Invalid currency code format');
    return null;
}
```

---

### CR-MEDIUM-003: 統計頁面 JS 檔案存在性待確認

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Missing File |
| **檔案** | `templates/expense/statistics.html:155` |

**問題描述**

`statistics.html` 引用了 `expense-statistics.js`，需確認該檔案是否存在且功能完整。

```html
<script th:src="@{/js/expense-statistics.js}"></script>
```

---

### CR-MEDIUM-004: 測試案例覆蓋不足

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Test Coverage |
| **檔案** | `test/service/SettlementServiceTest.java` |

**問題描述**

測試案例未覆蓋：
1. `exchangeRateService` 為 null 的情境
2. `exchangeRateService.convert()` 拋出例外的情境

---

### CR-MEDIUM-005: 契約註解不完整

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Documentation |
| **檔案** | `service/SettlementService.java:330-342` |

**問題描述**

`calculateCurrencyBreakdown` 方法缺少完整的契約註解（calls/calledBy）。

---

### 安全檢查清單結果

| 檢查項目 | 狀態 | 說明 |
|---------|:----:|------|
| API Key 存於環境變數 | ✅ Pass | 使用 `${EXCHANGERATE_API_KEY:}` |
| API Key 不記錄在日誌 | ✅ Pass | `sanitizeError()` 脫敏處理 |
| Currency Code 驗證 | ✅ Pass | `^[A-Z]{3}$` 正規表達式 |
| Rate Limiting | ⚠️ Partial | 有 Circuit Breaker，缺少 per-user rate limiting |
| Timeout 設定 | ✅ Pass | connect: 5s, read: 10s |
| Circuit Breaker | ✅ Pass | 5 次失敗後熔斷，5 分鐘冷卻 |
| Fallback 機制 | ✅ Pass | 24 小時 fallback 快取 |
| BigDecimal 使用 | ✅ Pass | 所有金額計算使用 BigDecimal |
| RoundingMode | ✅ Pass | 使用 HALF_UP, scale=2 |
| 統計 API 授權 | ✅ Pass | `permissionChecker.canView()` |

---

### 程式碼審查修復優先順序

| 優先級 | ID | 問題 | 原因 |
|:------:|-----|------|------|
| 1 | CR-HIGH-001 | API 認證缺失 | 可能被濫用消耗配額 |
| 2 | CR-HIGH-003 | 匯率轉換失敗處理 | 可能導致財務計算錯誤 |
| 3 | CR-HIGH-002 | Rate Limiting | 防止 DoS 攻擊 |
| 4 | CR-MEDIUM-001 | 前端快取 TTL | 匯率可能過時 |
| 5 | CR-MEDIUM-004 | 測試覆蓋率 | 確保邊界情況正確處理 |

---

## 已修復 Bug

### BUG-001: 編輯景點頁面 500 錯誤 ✅

| 屬性 | 內容 |
|------|------|
| **修復日期** | 2026-02-03 |
| **根因** | 模板使用不存在的 `activity.type` 欄位 |
| **修復** | 移除 `th:checked` 中的 `activity.type` 引用，改為固定值 |

---

### BUG-002: 新增支出頁面 500 錯誤 ✅

| 屬性 | 內容 |
|------|------|
| **修復日期** | 2026-02-03 |
| **根因** | 模板使用 `member.user.id/name`，但 Controller 傳遞的是 `TripMember`（無嵌套 User） |
| **修復** | Controller 改用 `trip.getMembers()`；模板改用 `member.userId` / `member.nickname` |

---

## 變更記錄

| 日期 | 變更內容 |
|------|----------|
| 2026-02-03 | 建立文件，記錄 E2E 測試結果及 2 個 Bug |
| 2026-02-03 | 新增前端安全審查結果，記錄 7 個安全問題 (SEC-001 ~ SEC-007) |
| 2026-02-03 | 新增後端安全審查結果，記錄 7 個安全問題 (BE-SEC-001 ~ BE-SEC-007) |
| 2026-02-03 | 新增 Phase 3 程式碼審查結果 (CR-HIGH-001 ~ CR-HIGH-003, CR-MEDIUM-001 ~ CR-MEDIUM-005) |
| 2026-02-03 | ✅ 修復 BUG-001 (編輯景點 500) 和 BUG-002 (新增支出 500)，移至已修復區塊 |
