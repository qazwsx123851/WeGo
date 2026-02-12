# WeGo API 前後端對照表

## 文件資訊

| 項目 | 內容 |
|------|------|
| 建立日期 | 2026-02-11 |
| 最後更新 | 2026-02-12 |
| 後端 REST endpoint 總數 | 55 |
| 前端實際使用的 endpoint | ~24 |
| Orphan endpoints (API-only) | ~31 |

---

## A. 前端使用的 REST API (24 endpoints)

### JavaScript 模組 API 呼叫

| 模組 | Endpoint | Method | CSRF | 說明 |
|------|----------|--------|------|------|
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

### Thymeleaf 模板 inline API 呼叫

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

### Web Controller 表單提交 (HTML POST)

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

## B. Orphan Endpoints (後端有但前端未使用, ~31 個)

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

## C. 權限模型

| 角色 | 可做的 API 操作 |
|------|----------------|
| **OWNER** | 所有操作 + 刪除行程 + 移除成員 + 變更角色 |
| **EDITOR** | CRUD 景點/支出/Todo/文件 + 建立邀請連結 |
| **VIEWER** | 所有 GET 操作 (只讀) |

---

## D. API 路徑命名模式

### 一般規則

- 資源路徑使用複數 (`/trips/`, `/activities/`, `/expenses/`)
- 巢狀資源使用 `/api/trips/{tripId}/[resource]` 格式
- CSRF Token 必須附帶在所有 POST/PUT/DELETE 請求

### 已知不一致

| 操作 | 路徑模式 | 說明 |
|------|----------|------|
| Activity 建立/列表 | `/api/trips/{tripId}/activities` | 帶 tripId |
| Activity 更新/刪除 | `/api/activities/{activityId}` | **不帶** tripId |
| Expense 建立/列表 | `/api/trips/{tripId}/expenses` | 帶 tripId |
| Expense 更新/刪除 | `/api/expenses/{expenseId}` | **不帶** tripId |

> 這是設計決策：update/delete 操作透過 activityId/expenseId 即可唯一識別資源，不需要 tripId。但與 create/list 的路徑風格不一致，開發時需注意。

---

## E. CSRF Token 使用方式

| 項目 | 值 |
|------|---|
| Cookie 名稱 | `XSRF-TOKEN` |
| Header 名稱 | `X-XSRF-TOKEN` |
| Repository | `CookieCsrfTokenRepository` (Spring default for cookie-based) |

### 前端取得方式

前端透過 Thymeleaf 模板中的 `<meta>` 標籤取得 CSRF Token（`_csrf` 和 `_csrf_header`），在發送 POST/PUT/DELETE 請求時，從 meta 標籤讀取 token 值並設定於請求的 HTTP header 中。

### 規則

- 所有 `POST`/`PUT`/`DELETE` 請求**必須**附帶 CSRF token
- `GET` 請求**不需要** CSRF token
- CSRF 豁免端點：`/api/health`, `/api/weather/**`, `/api/test/auth/**`
