## 1. 設定與基礎建設

- [x] 1.1 新增 `GeminiProperties` 設定類別（`wego.external-api.gemini.*`：enabled、apiKey、model、connectTimeoutMs、readTimeoutMs）
- [x] 1.2 在 `application.yml` 新增 gemini 設定區塊（預設 enabled=false）及 chat 設定（max-message-length、rate-limit-per-minute）
- [x] 1.3 新增 `ChatProperties` 設定類別（`wego.chat.*`：maxMessageLength=500、rateLimitPerMinute=5）

## 2. Gemini Client 層

- [x] 2.1 建立 `GeminiClient` 介面，定義 `String chat(String systemPrompt, String userMessage)` 方法
- [x] 2.2 實作 `GeminiClientImpl`（`@ConditionalOnProperty` enabled=true），使用 RestTemplate 呼叫 Gemini REST API
- [x] 2.3 實作 `MockGeminiClient`（`@ConditionalOnProperty` enabled=false），回傳模擬旅遊建議
- [x] 2.4 新增 `GeminiException` 例外類別，處理 API 呼叫失敗
- [x] 2.5 撰寫 `GeminiClientImplTest` 單元測試（mock RestTemplate，驗證請求格式、錯誤處理、逾時）
- [x] 2.6 撰寫 `MockGeminiClientTest` 單元測試（驗證回傳格式）

## 3. DTO 層

- [x] 3.1 建立 `ChatRequest` DTO（`message` 欄位，含 `@NotBlank` 及 `@Size(max=500)` 驗證）
- [x] 3.2 建立 `ChatResponse` DTO（`reply` 欄位）

## 4. Service 層

- [x] 4.1 實作 `ChatService`：組裝 System Prompt（身份、回覆規則、範圍限制、安全規則）
- [x] 4.2 實作今日行程上下文組裝邏輯：計算 day = today - trip.startDate + 1，查詢當日 Activity，格式化為 prompt 文字
- [x] 4.3 實作行程不在日期範圍內的降級邏輯：僅注入行程基本資訊
- [x] 4.4 整合 PermissionChecker 權限檢查、Rate Limit 檢查
- [x] 4.5 撰寫 `ChatServiceTest` 單元測試（mock GeminiClient + ActivityRepository + TripRepository，驗證 prompt 組裝、權限、Rate Limit、錯誤處理）

## 5. Controller 層

- [x] 5.1 實作 `ChatApiController`（`POST /api/trips/{tripId}/chat`），含 `@Valid` 驗證、權限檢查、呼叫 ChatService
- [x] 5.2 撰寫 `ChatApiControllerTest` WebMvcTest（驗證 200/400/403/429 回應、CSRF、認證）

## 6. 前端 UI（須參考 `/ui-ux-pro-max` skill）

- [x] 6.1 建立 `fragments/chat-widget.html` Thymeleaf fragment（浮動按鈕 + 聊天視窗 HTML 結構）
- [x] 6.2 在 Trip 相關頁面引入 chat-widget fragment
- [x] 6.3 建立 `chat.js`：聊天視窗開關、發送訊息（使用 `WeGo.fetchWithTimeout` + CSRF）、顯示使用者訊息
- [x] 6.4 實作 dots 等待動畫（`●●●`）
- [x] 6.5 實作逐字打字動畫（~30ms/字，完成後重新啟用輸入框）
- [x] 6.6 實作字數計數器（`42/500`，接近上限變色）
- [x] 6.7 實作前端錯誤處理（400/403/429/500 友善提示）
- [x] 6.8 更新 `SecurityConfig.java` CSP 設定（如有需要）

## 7. E2E 測試

- [x] 7.1 撰寫 `chat.spec.ts` E2E 測試（開關聊天視窗、發送問題、收到回覆、錯誤處理）
