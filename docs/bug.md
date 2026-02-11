# WeGo Bug 追蹤記錄

## 文件資訊

| 項目 | 內容 |
|------|------|
| 建立日期 | 2026-02-03 |
| 最後更新 | 2026-02-11 |
| 測試環境 | localhost:8080 |
| 測試方式 | Chrome DevTools MCP E2E 測試 |
| 安全審查 | security-reviewer agent |
| 整體風險等級 | **LOW** (CRITICAL/HIGH 漏洞已全部修復) |

---

## E2E 測試總結

| 測試項目 | 狀態 | 通過率 |
|---------|:----:|:------:|
| 首頁與登入流程 | ✅ | 100% |
| Dashboard 與行程列表 | ✅ | 100% |
| 行程詳情與景點管理 | ✅ | 100% |
| 分帳與文件功能 | ✅ | 100% |
| 全域概覽頁面 | ✅ | 100% |

---

## 待處理項目

### CR-HIGH-002: 缺少 Per-User Rate Limiting

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Rate Limiting |
| **檔案** | `controller/api/ExchangeRateApiController.java` |

目前已有全局 `rateLimitService.isAllowed("exchange-rates", 30)` 限制，但缺少 per-user rate limiting。惡意使用者可能消耗外部 API 配額。

---

### CR-MEDIUM-004: SettlementService 測試覆蓋不足

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Test Coverage |
| **檔案** | `test/service/SettlementServiceTest.java` |

未覆蓋 `exchangeRateService` 為 null 及 `convert()` 拋例外的情境。

---

### CR-MEDIUM-005: 契約註解不完整

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | LOW |
| **類別** | Documentation |
| **檔案** | `service/SettlementService.java:330-342` |

`calculateCurrencyBreakdown` 方法缺少 `calls`/`calledBy` 契約註解。

---

## 已知問題：前後端 Cross-Validation

### CV-001: 離開行程用 `/{userId}` 而非 `/me`

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | LOW |
| **類別** | API 語意 |
| **檔案** | `templates/trip/members.html:505` |

`members.html` 中離開行程使用 `DELETE /api/trips/{tripId}/members/${currentUserId}` 而非更語意化的 `/me` 端點。功能正常但語意不清，建議改用 `/me`。

---

### CV-002: `inviteLink`/`inviteLinkExpiry` 模板變數未提供

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | MEDIUM |
| **類別** | Controller / Template |
| **檔案** | `controller/web/TripController.java` (`showMembersPage()`), `templates/trip/members.html` |

`TripController.showMembersPage()` 沒有加入 `inviteLink` 和 `inviteLinkExpiry` 這兩個 model attribute，但 `members.html` 模板有引用。實際邀請連結透過 AJAX `POST /api/trips/{tripId}/invites` 動態產生，所以不影響功能，但可能造成模板渲染警告。

---

### CV-003: GET 請求附帶不必要的 CSRF header

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | LOW |
| **類別** | 前端 |
| **檔案** | `templates/document/list.html`, `templates/global-overview.html` |

`document/list.html` 和 `global-overview.html` 在 GET download/preview 請求時附帶 CSRF header。GET 請求不需要 CSRF token，不影響功能但屬於不必要的開銷。

---

### CV-004: expense/list 取全部再 find 單筆

| 屬性 | 內容 |
|------|------|
| **狀態** | 🟡 Open |
| **嚴重度** | LOW |
| **類別** | 效能 |
| **檔案** | `templates/expense/list.html` |

`expense/list.html` 的 `fetchExpenseDetail` 使用 `GET /api/trips/{tripId}/expenses` 取全部支出再 `.find()` 篩選，而非直接呼叫尚未被前端使用的 single expense endpoint `GET /api/expenses/{expenseId}`。在支出數量較多時可能影響效能。

---

## 已修復：前後端串接 (FE-001 ~ FE-007)

| ID | 功能 | 修復日期 | 說明 |
|----|------|----------|------|
| FE-001 | 檔案管理 JS 互動 | 2026-02-06 | 上傳進度條、AJAX 刪除、檔案預覽、拖放上傳 |
| FE-002 | 支出列表刪除 | 2026-02-06 | 刪除按鈕 + 確認對話框 + AJAX DELETE |
| FE-003 | 成員管理 JS 互動 | 2026-02-06 | 角色變更、移除成員、邀請連結、離開行程 |
| FE-004 | 結算結清操作 | 2026-02-06 | 批次結清/取消結清 API + 前端按鈕 |
| FE-005 | 景點詳情編輯/刪除 | 2026-02-06 | More Options 下拉選單 + 刪除確認對話框 |
| FE-006 | 個人資料驗證回饋 | 2026-02-06 | 暱稱即時驗證 + Toast 通知 |
| FE-007 | 行程封面預覽 | 2026-02-06 | FileReader 預覽 + 日期驗證 + 字數計數 |

---

## 已修復：前端安全 (SEC-001 ~ SEC-007)

| ID | 問題 | 修復日期 | 修復方式 |
|----|------|----------|----------|
| SEC-001 | XSS `th:utext` (`error.html`) | 2026-02-06 | 改用 `th:text` |
| SEC-002 | JS 注入 `th:onclick` (`document/list.html`) | 2026-02-06 | 改用 `data-*` 屬性 |
| SEC-003 | JS 注入 `th:onclick` (`members.html`) | 2026-02-06 | 改用 `data-*` 屬性 |
| SEC-004 | UUID 未加引號 (`todo/list.html`) | 2026-02-06 | 改用 `data-*` 屬性 |
| SEC-005 | SpEL null `memberCount` (`components.html`) | 2026-02-06 | 改用 `(trip?.memberCount ?: 0) > 0` |
| SEC-006 | SpEL null `filter` (`global-overview.html`) | 2026-02-06 | 加入 `filter != null and` 防護 |
| SEC-007 | DOM XSS `innerHTML` (`document/list.html`) | 2026-02-06 | 改用 DOM API |

---

## 已修復：後端安全 (BE-SEC-001 ~ BE-SEC-007)

| ID | 問題 | 嚴重度 | 修復日期 | 修復方式 |
|----|------|--------|----------|----------|
| BE-SEC-001 | IDOR 跨行程文件洩漏 | CRITICAL | 2026-02-06 | 改用 `findByTripIdAndRelatedActivityId()` 雙參數查詢 |
| BE-SEC-002 | 匿名用戶固定 UUID | CRITICAL | 2026-02-06 | `principal == null` 時拋出 `UnauthorizedException` |
| BE-SEC-003 | 刪除行程資料殘留 | HIGH | 2026-02-06 | 完整級聯刪除 + Supabase 儲存檔案清理 |
| BE-SEC-004 | 更新支出 splits 驗證 | HIGH | 2026-02-06 | 新增 CUSTOM/PERCENTAGE/SHARES 分帳重算驗證 |
| BE-SEC-005 | Cover image 用錯 bucket | MEDIUM | 2026-02-06 | 改用 `getCoverImageBucket()` |
| BE-SEC-006 | 建立支出未驗證成員 | MEDIUM | 2026-02-06 | 新增 `paidBy` 成員驗證 |
| BE-SEC-007 | N+1 查詢 | LOW | 2026-02-06 | 改用 `buildTodoResponses()` 批次版本 |

---

## 已修復：程式碼審查 (CR-*)

| ID | 問題 | 修復日期 | 修復方式 |
|----|------|----------|----------|
| CR-HIGH-001 | API 端點認證 | 2026-02-06 | 確認為非問題：`SecurityConfig` `.anyRequest().authenticated()` 已全局保護 |
| CR-HIGH-003 | 匯率轉換失敗處理 | 2026-02-06 | 結算頁面新增 `conversionWarnings` 警告 |
| CR-MEDIUM-001 | 匯率快取無 TTL | 2026-02-06 | 新增 `CACHE_TTL_MS` 過期機制 |
| CR-MEDIUM-002 | 前端幣別代碼驗證 | 2026-02-06 | 新增 `/^[A-Z]{3}$/` 正規驗證 |
| CR-MEDIUM-003 | expense-statistics.js | 2026-02-06 | 確認檔案存在且功能完整 |

---

## 已修復：頁面 Bug

| ID | 問題 | 修復日期 | 根因 |
|----|------|----------|------|
| BUG-001 | 編輯景點頁面 500 | 2026-02-03 | 模板引用不存在的 `activity.type` |
| BUG-002 | 新增支出頁面 500 | 2026-02-03 | Controller 傳 `TripMember` 但模板用 `member.user.id` |

---

## 安全檢查清單

| 檢查項目 | 狀態 |
|---------|:----:|
| API Key 存於環境變數 | ✅ |
| API Key 不記錄在日誌 | ✅ |
| Currency Code 驗證 | ✅ |
| Rate Limiting | ⚠️ 全局有，per-user 缺 |
| Timeout 設定 | ✅ |
| Circuit Breaker | ✅ |
| Fallback 機制 | ✅ |
| BigDecimal 金額計算 | ✅ |
| RoundingMode HALF_UP | ✅ |
| 統計 API 授權 | ✅ |

---

## 變更記錄

| 日期 | 變更內容 |
|------|----------|
| 2026-02-03 | 建立文件，記錄 E2E 測試結果及 BUG-001、BUG-002 |
| 2026-02-03 | 新增前端安全審查 (SEC-001~007)、後端安全審查 (BE-SEC-001~007)、Phase 3 程式碼審查 (CR-*) |
| 2026-02-03 | ✅ 修復 BUG-001、BUG-002 |
| 2026-02-04 | 新增前後端串接缺失分析 (FE-001~007) |
| 2026-02-06 | ✅ 修復 FE-004、FE-006、FE-007 |
| 2026-02-06 | ✅ 修復 BE-SEC-004、BE-SEC-006、CR-HIGH-003、CR-MEDIUM-002 |
| 2026-02-06 | ✅ 全面審查：確認 16 項 Bug 已修復並更新狀態 |
| 2026-02-06 | ✅ 修復 SEC-006 (filter null 防護)、BE-SEC-003 補充 (deleteTrip 儲存檔案清理) |
| 2026-02-06 | 精簡文件：移除已修復項目的舊程式碼、過時優先順序表，整合為摘要表格 |
| 2026-02-11 | 新增前後端 Cross-Validation 已知問題 (CV-001~004) |
