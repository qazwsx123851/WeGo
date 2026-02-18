## Why

使用者在規劃行程時常不知道「要吃什麼」或「附近有什麼好玩的」。目前 WeGo 只提供行程管理功能，缺少智慧推薦。透過整合 Gemini API 打造 AI 旅遊助手，讓使用者在行程頁面內直接獲得個人化的餐廳、景點推薦，提升平台價值。

## What Changes

- 新增 Trip 頁面浮動聊天視窗 UI（僅 Trip 頁面可見）
- 新增 `POST /api/trips/{tripId}/chat` REST endpoint
- 新增 `GeminiClient` 外部 API 客戶端（Interface + Impl + Mock，套用現有 external client 模式）
- 新增 `ChatService` 負責 prompt 組裝（System prompt + 當日行程上下文 + 使用者問題）與呼叫 Gemini
- System Prompt 嚴格限制 AI 只回答旅遊相關問題，非旅遊問題以友善語氣拒答（節省 token）
- 後端輸入驗證：訊息長度上限 500 字、Rate Limit 限制每人每分鐘請求次數
- 前端打字動畫效果：等待時顯示 dots 動畫，收到回覆後逐字顯示
- 無對話歷史儲存（無狀態設計）
- 不做 SSE streaming（一次回傳完整回覆）
- 不做一鍵加入行程功能

## Capabilities

### New Capabilities
- `ai-chat`: AI 旅遊聊天助手 — Gemini API 整合、prompt 組裝、旅遊專屬回覆範圍限制、輸入驗證、Rate Limit
- `chat-ui`: 聊天浮動視窗 UI — Trip 頁面浮動按鈕、對話介面、打字動畫、前端互動

### Modified Capabilities
(無既有 capability 需要修改)

## Impact

- **新增層級**: Controller (`ChatApiController`), Service (`ChatService`), External Client (`GeminiClient` / `GeminiClientImpl` / `MockGeminiClient`)
- **新增 DTO**: `ChatRequest`, `ChatResponse`
- **新增設定**: `GeminiProperties` (`wego.external-api.gemini.*`)
- **Thymeleaf**: Trip 相關頁面需嵌入聊天 widget fragment
- **JavaScript**: 新增 `chat.js` 處理聊天互動與打字動畫
- **權限**: 使用現有 `PermissionChecker` 確認使用者為行程成員
- **SecurityConfig**: `/api/trips/*/chat` 需要認證，CSRF 正常套用
- **pom.xml**: 不需新增依賴（使用現有 RestTemplate）
- **UI/UX**: 實作時須參考 `/ui-ux-pro-max` skill 建議
