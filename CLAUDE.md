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

### ⚠️ Null-Safe 必須

```html
<!-- ❌ 錯誤 -->
<span th:text="${user.address.city}">City</span>

<!-- ✅ 正確 -->
<span th:text="${user?.address?.city}">City</span>
<span th:if="${(trip.memberCount ?: 0) > 0}">Members</span>
```

### 快速參考
| 場景 | 模式 |
|------|------|
| 巢狀屬性 | `${obj?.nested?.prop}` |
| 帶預設值 | `${obj?.prop} ?: 'default'` |
| 數值比較 | `${(obj?.count ?: 0) > 0}` |
| 日期格式 | `${#temporals.format(date, 'yyyy-MM-dd')}` |
| 金額格式 | `${#numbers.formatDecimal(amount, 0, 'COMMA', 2, 'POINT')}` |

### 禁止
- ❌ `th:utext` 輸出使用者資料 (XSS)
- ❌ `th:onclick` 拼接變數 (JS injection)
- ❌ `#dates` 處理 java.time (用 `#temporals`)

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
