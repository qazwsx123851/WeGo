## ADDED Requirements

### Requirement: 浮動聊天按鈕
系統 SHALL 在所有 Trip 頁面 (`/trips/{tripId}/**`) 顯示浮動聊天按鈕，讓使用者快速開啟 AI 旅遊助手。

#### Scenario: Trip 頁面顯示聊天按鈕
- **WHEN** 使用者進入任何 Trip 頁面（行程詳情、活動、支出等）
- **THEN** 頁面右下角顯示浮動聊天按鈕圖示

#### Scenario: 非 Trip 頁面不顯示
- **WHEN** 使用者在 Dashboard、Profile 等非 Trip 頁面
- **THEN** 不顯示浮動聊天按鈕

### Requirement: 聊天視窗互動
系統 SHALL 提供聊天視窗讓使用者輸入問題並顯示 AI 回覆。

#### Scenario: 開啟聊天視窗
- **WHEN** 使用者點擊浮動聊天按鈕
- **THEN** 展開聊天視窗，顯示歡迎訊息和輸入框

#### Scenario: 關閉聊天視窗
- **WHEN** 使用者點擊關閉按鈕或再次點擊浮動按鈕
- **THEN** 聊天視窗收合，僅顯示浮動按鈕

#### Scenario: 發送問題
- **WHEN** 使用者在輸入框輸入文字並按 Enter 或點擊發送按鈕
- **THEN** 使用者的問題顯示在對話區域，輸入框清空並禁用，系統開始處理

### Requirement: 等待與打字動畫
系統 SHALL 在等待 AI 回覆期間顯示動畫效果，收到回覆後逐字顯示。

#### Scenario: 等待回覆動畫
- **WHEN** 使用者發送問題後等待 AI 回覆
- **THEN** 對話區域顯示 dots 動畫（`●●●`）表示正在處理

#### Scenario: 逐字打字動畫
- **WHEN** 系統收到 AI 的完整回覆
- **THEN** 回覆文字以逐字方式顯示在對話區域（約 30ms/字），完成後重新啟用輸入框

#### Scenario: API 錯誤處理
- **WHEN** API 回傳錯誤（400/403/429/500）
- **THEN** 在對話區域顯示友善錯誤提示，重新啟用輸入框

### Requirement: 輸入限制前端提示
系統 SHALL 在前端提供即時的輸入限制回饋。

#### Scenario: 字數計數器
- **WHEN** 使用者在輸入框中輸入文字
- **THEN** 顯示當前字數與上限（如 `42/500`），接近上限時變色提醒

#### Scenario: 前端阻擋空白發送
- **WHEN** 使用者嘗試發送空白訊息
- **THEN** 發送按鈕保持禁用狀態，不發送請求
