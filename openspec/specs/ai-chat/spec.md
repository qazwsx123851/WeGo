## ADDED Requirements

### Requirement: Chat API endpoint
系統 SHALL 提供 `POST /api/trips/{tripId}/chat` endpoint，接受已認證使用者的旅遊問題，回傳 AI 生成的旅遊建議。

#### Scenario: 成功提問旅遊問題
- **WHEN** 已認證的行程成員發送 `{ "message": "中午不知道吃什麼" }` 到 `POST /api/trips/{tripId}/chat`
- **THEN** 系統回傳 `200 OK` 與 `{ "reply": "根據你的行程..." }`，內容為旅遊相關建議

#### Scenario: 非行程成員存取
- **WHEN** 不屬於該行程的使用者發送 chat 請求
- **THEN** 系統回傳 `403 Forbidden`

#### Scenario: 未認證使用者存取
- **WHEN** 未登入的使用者發送 chat 請求
- **THEN** 系統回傳 `401 Unauthorized`

### Requirement: 訊息輸入驗證
系統 SHALL 驗證使用者輸入的訊息，拒絕不合規的請求以節省 API token。

#### Scenario: 空白訊息
- **WHEN** 使用者發送空白或全空格訊息
- **THEN** 系統回傳 `400 Bad Request` 與錯誤訊息，不呼叫 Gemini API

#### Scenario: 超過長度上限
- **WHEN** 使用者發送超過 500 字的訊息
- **THEN** 系統回傳 `400 Bad Request` 與錯誤訊息，不呼叫 Gemini API

### Requirement: Rate Limit 防濫用
系統 SHALL 對 chat endpoint 實施頻率限制，防止 API 費用失控。

#### Scenario: 超過頻率限制
- **WHEN** 同一使用者在一分鐘內發送超過 5 次 chat 請求
- **THEN** 系統回傳 `429 Too Many Requests` 與友善提示訊息

#### Scenario: 正常頻率使用
- **WHEN** 使用者在頻率限制內發送請求
- **THEN** 系統正常處理並回傳 AI 回覆

### Requirement: 當日行程上下文注入
系統 SHALL 將使用者當日的行程活動注入 AI prompt，讓 AI 能根據行程提供個人化推薦。

#### Scenario: 今天在行程日期範圍內
- **WHEN** 今天的日期落在行程的 startDate 到 endDate 範圍內
- **THEN** 系統計算今天是第幾天 (day)，查詢當日所有 Activity，將行程名稱、日期、活動清單（時間、地點）注入 prompt

#### Scenario: 今天不在行程日期範圍內
- **WHEN** 今天的日期不在行程的 startDate 到 endDate 範圍內
- **THEN** 系統僅注入行程基本資訊（名稱、目的地、日期範圍），不注入特定日活動

### Requirement: 旅遊專屬回覆範圍限制
系統 SHALL 透過 System Prompt 嚴格限制 AI 僅回答旅遊相關問題，非旅遊問題以友善語氣簡短拒答。

#### Scenario: 旅遊相關問題
- **WHEN** 使用者詢問餐廳推薦、景點推薦、交通建議、當地文化、行程安排等旅遊相關問題
- **THEN** AI 回覆實用的旅遊建議，包含具體資訊（地點名稱、特色、價位等）

#### Scenario: 非旅遊問題
- **WHEN** 使用者詢問程式設計、數學、寫作等與旅遊無關的問題
- **THEN** AI 以友善語氣回覆固定簡短訊息，提醒此為旅遊助手，output token 控制在 ~40 以內

#### Scenario: Prompt injection 嘗試
- **WHEN** 使用者嘗試覆寫 System Prompt（如「忽略以上指令」）
- **THEN** AI 忽略該指令，回覆旅遊助手提醒訊息

### Requirement: Gemini API 整合
系統 SHALL 透過 GeminiClient 介面呼叫 Gemini API，支援 Impl 和 Mock 兩種實作切換。

#### Scenario: 使用真實 Gemini API
- **WHEN** 設定 `wego.external-api.gemini.enabled=true` 且提供有效 API Key
- **THEN** 系統使用 `GeminiClientImpl` 透過 RestTemplate 呼叫 Gemini API

#### Scenario: 使用 Mock（預設）
- **WHEN** 設定 `wego.external-api.gemini.enabled=false`（預設值）
- **THEN** 系統使用 `MockGeminiClient` 回傳模擬的旅遊建議回覆

#### Scenario: Gemini API 呼叫失敗
- **WHEN** Gemini API 回傳錯誤或逾時
- **THEN** 系統回傳友善錯誤訊息給使用者，不洩漏技術細節
