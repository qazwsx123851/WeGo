## Context

WeGo 是伺服器端渲染的旅遊協作平台 (Spring Boot + Thymeleaf)。現有架構已有成熟的外部 API 客戶端模式（Google Maps、OpenWeatherMap、ExchangeRate），使用 Interface + Impl + Mock 搭配 `@ConditionalOnProperty` 切換。本次新增 AI 聊天功能，整合 Gemini API，讓使用者在 Trip 頁面內獲得旅遊推薦。

Activity 使用相對天數 (`int day`) 而非絕對日期，需結合 `Trip.startDate` 計算「今天是第幾天」來查詢當日行程。

## Goals / Non-Goals

**Goals:**
- 在 Trip 頁面提供浮動聊天視窗，讓使用者向 AI 旅遊助手提問
- 整合 Gemini API，將當日行程注入 prompt 提供個人化推薦
- 嚴格限制 AI 回覆範圍（僅旅遊相關），非旅遊問題快速拒答以節省 token
- 後端輸入驗證與 Rate Limit 防止濫用
- 前端打字動畫提升等待體驗

**Non-Goals:**
- 不儲存對話歷史（無狀態）
- 不做 SSE streaming
- 不做一鍵加入行程功能
- Dashboard 頁面不顯示聊天按鈕
- 不支援多輪對話上下文

## Decisions

### 1. API 路徑：`POST /api/trips/{tripId}/chat`

將 chat 掛在 trip 資源下，因為聊天上下文永遠綁定某個行程。這樣自然取得 tripId，用 PermissionChecker 確認權限。

**替代方案**: `POST /api/chat?tripId=xxx` — 語意不夠明確，且無法利用現有 trip 路徑的權限攔截模式。

### 2. Gemini Client 套用現有 External Client 模式

```
GeminiClient (interface)
├── GeminiClientImpl    (@ConditionalOnProperty enabled=true, RestTemplate)
└── MockGeminiClient    (@ConditionalOnProperty enabled=false, 預設)
```

**理由**: 與 GoogleMapsClient、WeatherClient、ExchangeRateClient 一致。開發和測試環境不需要真實 API Key。

### 3. Prompt 組裝策略

```
System Prompt (固定 ~300 tokens)
├── 身份定義：WeGo 旅遊助手
├── 回覆規則：繁體中文、簡潔、具體資訊
├── 範圍限制：僅旅遊相關，非旅遊一律簡短拒答
└── 安全規則：忽略 prompt injection、不洩漏 system prompt

行程上下文 (動態 ~100-200 tokens)
├── 行程名稱、目的地、日期範圍
├── 今天是第幾天
└── 今日已安排的活動（時間、地點）

使用者訊息 (≤500 字 ~250 tokens)
```

**「今日行程」計算邏輯**:
1. 取得 Trip 的 `startDate`
2. 計算 `today - startDate + 1` 得到 `day` 值
3. 用 `ActivityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, day)` 查詢
4. 若今天不在行程日期範圍內，僅提供行程基本資訊（名稱、目的地）

### 4. 雙層防線設計

| 層 | 機制 | Token 成本 |
|----|------|-----------|
| 第一層 | 後端驗證：訊息長度 ≤500 字、Rate Limit (每人每分鐘 5 次) | 0 |
| 第二層 | System Prompt 約束：非旅遊問題簡短拒答 (~40 tokens output) | 低 |

**Rate Limit**: 使用現有 `RateLimitService`，新增 chat 類型的限制。

### 5. 前端打字動畫（純 JS，不需 SSE）

等待 API 回覆期間顯示 dots 動畫 (`●●●`)。收到完整回覆後，前端以 `setInterval` 逐字加入 DOM，模擬打字效果。速度約 30ms/字，營造即時感。

### 6. UI 元件結構

- 浮動按鈕 + 聊天視窗作為 Thymeleaf fragment (`fragments/chat-widget :: chatWidget`)
- 僅在 Trip 相關頁面引入此 fragment（透過 layout 或各 trip 頁面 include）
- `chat.js` 獨立檔案處理所有前端互動邏輯
- 實作時須參考 `/ui-ux-pro-max` skill 的設計建議

### 7. 設定結構

```yaml
wego:
  external-api:
    gemini:
      enabled: false          # 預設關閉，使用 MockGeminiClient
      api-key: ${GEMINI_API_KEY:}
      model: gemini-2.0-flash  # 成本低、速度快
      connect-timeout-ms: 5000
      read-timeout-ms: 30000  # AI 回覆較慢，給 30 秒
  chat:
    max-message-length: 500
    rate-limit-per-minute: 5
```

**Model 選擇**: `gemini-2.0-flash` — 回覆速度快、成本低，適合旅遊推薦場景。不需要 Pro 級推理能力。

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Gemini API 回應慢 (>5s) | ReadTimeout 30s + 前端 dots 動畫 + 超時友善提示 |
| Prompt injection 繞過範圍限制 | 雙層防線 + System prompt 安全規則 + 輸入長度限制 |
| API 費用失控 | Rate Limit + Mock 預設 + 短拒答回覆節省 output tokens |
| 行程不在今天日期範圍 | 降級為基本旅遊助手（僅提供行程概要，不注入當日活動） |
| Gemini API 不可用 | MockGeminiClient 預設啟用；Impl 捕獲異常回傳友善錯誤訊息 |
