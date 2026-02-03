# WeGo - AI 開發指南

> 旅遊規劃協作平台 | Spring Boot 3.x + Thymeleaf + Tailwind CSS + Supabase

## 快速參考

| 指令 | 說明 |
|------|------|
| `./mvnw spring-boot:run` | 啟動開發伺服器 |
| `./mvnw test` | 執行測試 |
| `./mvnw jacoco:report` | 測試覆蓋率報告 |

---

## 技術棧

| 層級 | 技術 |
|------|------|
| 後端 | Spring Boot 3.x (Java 17+) |
| 前端 | Thymeleaf + Tailwind CSS |
| 資料庫 | Supabase (PostgreSQL 15+) |
| 外部 API | Google Maps, OpenWeatherMap, ExchangeRate-API |

---

## 核心實體與權限

```
User ─┬─< TripMember >─── Trip ──< Activity, Expense, Document, Todo
      └────────────────────────────────────────────────────────────────
```

| 功能 | Owner | Editor | Viewer |
|------|:-----:|:------:|:------:|
| 檢視行程 | ✅ | ✅ | ✅ |
| 編輯景點/支出 | ✅ | ✅ | ❌ |
| 移除成員/刪除行程 | ✅ | ❌ | ❌ |

---

## 編碼規範

### 分層架構
| 層級 | 職責 | 依賴 |
|------|------|------|
| Controller | HTTP 處理 | Service, DTO |
| Service | 業務協調 | Domain, Repository |
| Domain | 核心邏輯 | Entity |
| Repository | 資料存取 | Entity |

### 命名規範
- **Entity**: 單數 (`User`, `Trip`)
- **Service**: `{Domain}Service`
- **Controller**: `{Domain}Controller` (Web) / `{Domain}ApiController` (REST)
- **DTO**: `{Action}{Entity}Request` / `{Entity}Response`

### 方法契約 (必須)
```java
/**
 * @contract
 *   - pre: user != null
 *   - post: Trip 已持久化
 *   - calls: TripRepository#save
 */
public TripResponse createTrip(CreateTripRequest request, User user) { }
```

### 禁止事項
- ❌ 硬編碼 API Key
- ❌ 使用 `System.out.println`
- ❌ 忽略例外
- ❌ Service 層存取 HttpServletRequest

---

## 環境變數

```bash
# 必須
DATABASE_URL=postgresql://...
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_SERVICE_KEY=eyJ...  # 必須是 service_role key (JWT 格式)
GOOGLE_CLIENT_ID=xxx
GOOGLE_CLIENT_SECRET=xxx

# 可選（無則使用 Mock）
GOOGLE_MAPS_API_KEY=
OPENWEATHERMAP_API_KEY=
EXCHANGERATE_API_KEY=
```

---

## Thymeleaf SpEL 規範

> 完整參考: `~/.claude/skills/thymeleaf-spel.md`
> 檢查工具: `/thymeleaf-check`

### 五種表達式類型

| 類型 | 語法 | 用途 | 範例 |
|------|------|------|------|
| Variable | `${...}` | 存取 model 屬性 | `${user.name}` |
| Selection | `*{...}` | 配合 `th:object` | `*{name}` |
| Message | `#{...}` | i18n 訊息 | `#{welcome.message}` |
| **Link** | `@{...}` | URL 建構 | `@{/users/{id}(id=${user.id})}` |
| Fragment | `~{...}` | 引用片段 | `~{fragments/header :: nav}` |

### ⚠️ CRITICAL: 表達式不可混用

```html
<!-- ❌ 錯誤: 在 ${} 內使用 @{} 會導致 EL1059E -->
<form th:action="${isEdit ? @{/trips/{id}/edit(id=${trip.id})} : @{/trips/create}}">

<!-- ✅ 正確: 使用字串拼接 -->
<form th:action="${isEdit ? '/trips/' + trip.id + '/edit' : '/trips/create'}">

<!-- ✅ 正確: 使用 Literal Substitution -->
<a th:href="|/trips/${tripId}/activities/${activityId}|">Link</a>
```

### ⚠️ Null-Safe 必須

```html
<!-- ❌ 錯誤: 會拋出 EL1007E -->
<span th:text="${user.address.city}">City</span>

<!-- ✅ 正確: 全鏈路 Safe Navigation -->
<span th:text="${user?.address?.city}">City</span>

<!-- ✅ 正確: 數值比較必須加預設值 -->
<span th:if="${(trip?.memberCount ?: 0) > 0}">Members</span>

<!-- ✅ 正確: BigDecimal 比較 -->
<span th:classappend="${(balance ?: T(java.math.BigDecimal).ZERO).compareTo(T(java.math.BigDecimal).ZERO) >= 0} ? 'text-green' : 'text-red'">
```

### 快速參考

| 場景 | 模式 |
|------|------|
| 巢狀屬性 | `${obj?.nested?.prop}` |
| 帶預設值 | `${obj?.prop} ?: 'default'` |
| 數值比較 | `${(obj?.count ?: 0) > 0}` |
| 日期格式 | `${#temporals.format(date, 'yyyy-MM-dd')}` |
| 金額格式 | `${#numbers.formatDecimal(amount, 0, 'COMMA', 2, 'POINT')}` |
| 字串截取 | `${str?.length() > 0 ? #strings.substring(str, 0, 1) : '?'}` |
| 集合迭代 | `th:if="${list != null}" th:each="item : ${list}"` |

### 禁止事項

| 禁止 | 原因 | 替代方案 |
|------|------|----------|
| `th:utext="${userInput}"` | XSS 漏洞 | 用 `th:text` |
| `th:onclick="'fn(' + ${val} + ')'"` | JS injection | 用 `th:data-*` + JS |
| `th:action="${cond ? @{url1} : @{url2}}"` | 語法錯誤 | 用字串拼接 |
| `#dates.format(localDate)` | 類型錯誤 | 用 `#temporals` |
| `${list[0].prop}` 無 null check | NPE | 先檢查 `list != null and #lists.size(list) > 0` |

### 常見錯誤代碼

| 錯誤代碼 | 原因 | 修正 |
|----------|------|------|
| `EL1007E` | 存取 null 物件的屬性 | 使用 `?.` |
| `EL1059E` | `@` 或 `&` 後接非識別符 | 不要在 `${}` 內用 `@{}` |
| `EL1021E` | 找不到類別 | 檢查 `T()` 語法 |
| `EL1030E` | 運算子無法應用 | 檢查 null 值 |

---

## 關鍵錯誤模式

> 完整列表: `docs/bug.md`

| 錯誤 | 症狀 | 修正 |
|------|------|------|
| **SpEL Null** | `EL1007E: Property cannot be found on null` | 使用 `?.` 或 `?:` |
| **Controller 類型** | `415 Unsupported Media Type` | Web 用 `@RequestParam`，API 用 `@RequestBody` |
| **Supabase Key** | 檔案上傳失敗 | 使用 `service_role` key (JWT 格式) |
| **CSP 阻擋** | Console 顯示 CSP violation | 更新 `SecurityConfig.java` |
| **Tailwind JIT** | 動態 class 無效 | 用組件類別或 safelist |

---

## 開發前檢查清單

**Thymeleaf**:
- [ ] `${...}` 對 nullable 物件使用 `?.`
- [ ] 數值比較使用 `${(obj?.count ?: 0) > 0}`
- [ ] 日期用 `#temporals`，金額用 `#numbers.formatDecimal`
- [ ] 無 `th:utext` 或 `th:onclick` 拼接

**一般**:
- [ ] Fragment 無循環引用
- [ ] 路由使用複數 (`/trips/`)
- [ ] CSP 允許所需資源
- [ ] Supabase 使用 service_role key

---

## 相關文件

| 文件 | 內容 |
|------|------|
| `docs/CONTRIB.md` | 專案結構、開發流程 |
| `docs/bug.md` | 完整錯誤模式列表 |
| `docs/tdd-guide.md` | TDD 測試規範 |
| `docs/software-design-document.md` | 架構設計、ADR |
| `~/.claude/skills/thymeleaf-spel.md` | Thymeleaf 完整參考 |
| `~/.claude/rules/` | Agent、Git、測試規範 |
