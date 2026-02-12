# WeGo UI 設計指南

## 文件資訊

| 項目 | 內容 |
|------|------|
| 版本 | 2.1.0 |
| 更新日期 | 2026-02-12 |
| 設計系統 | UI/UX Pro Max |

---

## 1. 設計原則

### 核心理念

| 原則 | 說明 |
|------|------|
| **Mobile-First** | 優先設計手機體驗，再向上延伸至平板/桌機 |
| **協作感知** | 讓用戶感受到「一起規劃」的氛圍 |
| **輕鬆愉快** | 旅遊規劃應該是開心的，避免過於嚴肅的商務風格 |
| **資訊層次** | 重要資訊一目了然，細節需要時再展開 |
| **無障礙優先** | 確保所有用戶都能順暢使用 |

### 設計關鍵字

輕盈、活潑、直覺、協作、信任、冒險

### 選用風格

| 風格 | 用途 |
|------|------|
| **Glassmorphism** | 卡片、Modal、浮動元件 |
| **Bento Grid** | Dashboard、功能展示 |
| **Soft UI Evolution** | 按鈕、表單元件 |

---

## 2. 色彩系統 (Color System)

### 主色調 - 旅遊冒險主題

**Primary - 天空藍**（代表旅行、探索、自由）

| 色階 | 色碼 | 用途 |
|------|------|------|
| primary-50 | #F0F9FF | 極淺背景 |
| primary-100 | #E0F2FE | 淺背景 |
| primary-200 | #BAE6FD | 邊框、標記 |
| primary-300 | #7DD3FC | 淺色強調 |
| primary-400 | #38BDF8 | Dark Mode 主色 |
| primary-500 | #0EA5E9 | 主要按鈕、連結 |
| primary-600 | #0284C7 | Hover 狀態 |
| primary-700 | #0369A1 | Active 狀態 |
| primary-800 | #075985 | 深色強調 |
| primary-900 | #0C4A6E | 主要文字 |

**Secondary - 冒險橘**（代表熱情、活力、CTA）

| 色階 | 色碼 | 用途 |
|------|------|------|
| adventure-50 | #FFF7ED | 極淺背景 |
| adventure-100 | #FFEDD5 | 淺背景 |
| adventure-200 | #FED7AA | 邊框、標記 |
| adventure-300 | #FDBA74 | 淺色強調 |
| adventure-400 | #FB923C | Dark Mode CTA |
| adventure-500 | #F97316 | CTA 按鈕 |
| adventure-600 | #EA580C | Hover 狀態 |
| adventure-700 | #C2410C | Active 狀態 |

### 語意色彩

| 類型 | 淺色 | 標準色 | 深色 | 用途 |
|------|------|--------|------|------|
| 成功 | #DCFCE7 | #22C55E | #166534 | 完成任務、分帳結清 |
| 警告 | #FEF9C3 | #EAB308 | #854D0E | 提醒、截止日期接近 |
| 錯誤 | #FEE2E2 | #EF4444 | #991B1B | 操作失敗、必填欄位 |
| 資訊 | #DBEAFE | #3B82F6 | #1E40AF | 提示訊息 |

### 中性色

| 色階 | 色碼 | 用途 |
|------|------|------|
| gray-50 | #F8FAFC | 頁面背景 |
| gray-100 | #F1F5F9 | 卡片背景 (Light) |
| gray-200 | #E2E8F0 | 邊框、分隔線 |
| gray-300 | #CBD5E1 | 一般裝飾 |
| gray-400 | #94A3B8 | Placeholder 文字 |
| gray-500 | #64748B | 次要文字 |
| gray-600 | #475569 | 次要文字（重要） |
| gray-700 | #334155 | 主要文字 |
| gray-800 | #1E293B | 標題文字 |
| gray-900 | #0F172A | 深色背景 |

### Glassmorphism 效果

| 屬性 | Light Mode | Dark Mode |
|------|------------|-----------|
| 玻璃背景 | rgba(255, 255, 255, 0.7) | rgba(15, 23, 42, 0.7) |
| 模糊半徑 | 12px | 12px |
| 邊框 | rgba(255, 255, 255, 0.2) | rgba(255, 255, 255, 0.2) |
| 陰影 | 0 8px 32px rgba(0, 0, 0, 0.1) | 0 8px 32px rgba(0, 0, 0, 0.1) |

### 深色模式 (Dark Mode) - Phase 4

| 用途 | Light Mode | Dark Mode |
|------|------------|-----------|
| 頁面背景 | `gray-50` #F8FAFC | `gray-900` #0F172A |
| 卡片背景 | `white` #FFFFFF | `gray-800` #1E293B |
| 玻璃背景 | `white/70` | `gray-900/70` |
| 邊框 | `gray-200` #E2E8F0 | `gray-700` #334155 |
| 主要文字 | `gray-800` #1E293B | `gray-100` #F1F5F9 |
| 次要文字 | `gray-500` #64748B | `gray-400` #94A3B8 |
| 主色調 | `primary-500` | `primary-400` |
| CTA | `adventure-500` | `adventure-400` |

---

## 3. 字體排版 (Typography)

### 字體選擇

| 用途 | 字體家族 |
|------|----------|
| 主要字體 | Noto Sans TC, Plus Jakarta Sans, 系統字體 |
| 標題字體 | Plus Jakarta Sans, Noto Sans TC, sans-serif |
| 等寬字體 | JetBrains Mono, Fira Code, monospace |

### 字級系統

| 用途 | 大小 | 行高 | 字重 | Tailwind |
|------|------|------|------|----------|
| 頁面標題 | 24px | 1.3 | 700 | `text-2xl font-bold leading-tight` |
| 區塊標題 | 20px | 1.4 | 600 | `text-xl font-semibold` |
| 卡片標題 | 16px | 1.5 | 600 | `text-base font-semibold` |
| 內文 | 14px | 1.6 | 400 | `text-sm leading-relaxed` |
| 輔助文字 | 12px | 1.5 | 400 | `text-xs text-gray-500` |
| 金額數字 | 18px | 1.4 | 600 | `text-lg font-semibold font-mono` |
| 按鈕文字 | 14px | 1.5 | 500 | `text-sm font-medium` |

### 文字顏色使用

| 類型 | Light Mode | Dark Mode |
|------|------------|-----------|
| 主要文字 | gray-800 | gray-100 |
| 次要文字 | gray-500 | gray-400 |
| 強調文字 | primary-600, font-medium | primary-400, font-medium |
| 金額文字 | gray-800, font-mono, font-semibold | gray-100, font-mono, font-semibold |
| 錯誤文字 | error | red-400 |

### 行長度限制

最佳閱讀體驗為 65-75 字元寬度，使用 `max-width: 65ch` 限制。

---

## 4. 間距與佈局 (Spacing & Layout)

### 間距系統（8px 基準）

| Token | 值 | 用途 |
|-------|-----|------|
| `space-1` | 4px | 極小間距 (icon 與文字) |
| `space-2` | 8px | 緊湊間距 (inline 元素) |
| `space-3` | 12px | 元素內間距 |
| `space-4` | 16px | 標準間距 |
| `space-5` | 20px | 中等間距 |
| `space-6` | 24px | 區塊間距 |
| `space-8` | 32px | 大區塊間距 |
| `space-10` | 40px | Section 間距 |
| `space-12` | 48px | 頁面區塊 |

### 響應式斷點

| 斷點 | 寬度 | 用途 |
|------|------|------|
| sm | 640px | 大手機 |
| md | 768px | 平板 |
| lg | 1024px | 小筆電 |
| xl | 1280px | 桌機 |
| 2xl | 1536px | 大螢幕 |

測試必須涵蓋：320px, 375px, 768px, 1024px, 1280px, 1440px

### 頁面佈局模式

#### 手機版（< 768px）

```
┌────────────────────────┐
│  Header (56px)         │  ← 固定頂部
├────────────────────────┤
│                        │
│  Content               │  ← 可捲動區域
│  (padding: 16px)       │
│  (pb: 80px for nav)    │
│                        │
├────────────────────────┤
│  Bottom Nav (64px)     │  ← 固定底部
└────────────────────────┘
```

#### 平板/桌機版（>= 768px）

```
┌──────────────────────────────────────┐
│  Header (64px)                       │
├──────────────────┬───────────────────┤
│                  │                   │
│  Sidebar/List    │  Main Content     │
│  (320px)         │  (flex-1)         │
│                  │                   │
│                  │                   │
└──────────────────┴───────────────────┘
```

### 響應式 Padding

- 容器 padding：手機 16px / 平板 24px / 桌機 32px（Tailwind: `px-4 sm:px-6 lg:px-8`）
- 卡片 padding：手機 16px / 平板以上 24px（Tailwind: `p-4 sm:p-6`）

---

## 5. 元件設計 (Components)

### 5.1 按鈕 (Buttons)

所有按鈕最小觸控高度為 44px，圓角使用 `rounded-xl`，過渡動畫 200ms。

| 類型 | 背景色 | 文字色 | 特殊效果 | 用途 |
|------|--------|--------|----------|------|
| 主要按鈕 (CTA) | adventure-500 | white | 陰影 + hover 加深 + active 縮放 | 建立行程等主要操作 |
| 次要按鈕 | white / gray-800 (dark) | gray-700 / gray-200 (dark) | 邊框 + hover 變色 | 取消等次要操作 |
| 危險按鈕 | error (red) | white | active 縮放 | 刪除等破壞性操作 |
| 文字按鈕 | 透明 | primary-600 / primary-400 (dark) | hover 加淺背景 | 查看更多等輕量操作 |
| 圖示按鈕 | 透明 | gray-500 | 圓形 44x44px、hover 加背景 | 關閉、選單等 |

### 5.2 卡片 (Cards)

**玻璃效果卡片 (Glassmorphism)**：使用半透明白色背景 + 模糊效果 + 白色半透明邊框，圓角 `rounded-2xl`，hover 時加深陰影和背景不透明度。

**行程卡片**：包含 16:9 封面圖（支援 hover 放大效果、lazy loading）、標題、日期、成員頭像堆疊。

**景點卡片**：水平排列，包含圓形序號、景點名稱與時段、拖曳手把。卡片之間以交通時間連接線串聯。

### 5.3 表單元件 (Form Elements)

所有表單元件共用特徵：全寬、padding 16px 水平 / 12px 垂直、`rounded-xl` 圓角、`gray-200` 邊框、聚焦時顯示 `primary-500` 的 ring。

| 元件 | 特殊設定 |
|------|----------|
| 輸入框 | 搭配 label、placeholder、錯誤訊息（role="alert"） |
| 下拉選單 | 加 `cursor-pointer` |
| 日期選擇器 | 原生 `type="date"` |

### 5.4 底部抽屜 (Bottom Drawer)

底部抽屜由背景遮罩 + 抽屜本體組成：

- 背景遮罩：黑色 40% 透明度 + 模糊效果，z-index 40
- 抽屜本體：白色背景、`rounded-t-3xl` 頂部圓角、最大高度 85vh、z-index 50
- 頂部拖曳指示條（12px 寬灰色圓條）
- 標題區、可捲動內容區（最大高度 60vh）、底部按鈕區（包含 safe-area padding）
- 使用 `role="dialog"` 和 `aria-modal="true"` 支援無障礙

### 5.5 底部導覽列 (Bottom Navigation)

固定於底部，使用玻璃效果背景（白色 80% 透明度 + 模糊），z-index 30。

包含 5 個項目：行程、分帳、中央新增按鈕、檔案、設定。選中項目使用 primary 色，未選中使用 gray-400。中央新增按鈕為圓形浮起按鈕（adventure-500 背景、-mt-5 偏移、陰影）。每個項目最小寬度 64px、高度 56px。

### 5.6 Toast 訊息通知

固定於右上角（top-4 right-4），z-index 50。包含左側色條（語意色）、圖示、標題與描述、關閉按鈕。使用 slide-in 動畫進入。最小寬度 280px。使用 `role="status"` 和 `aria-live="polite"` 支援無障礙。

### 5.7 確認對話框 (Confirm Dialog)

置中彈出，背景遮罩黑色 50% + 模糊，z-index 50。對話框最大寬度 `max-w-sm`、`rounded-2xl`。包含標題、描述、取消與確認按鈕（並排 flex）。使用 `role="alertdialog"` 和 `aria-modal="true"` 支援無障礙。

### 5.8 錯誤狀態頁面

全螢幕置中佈局，包含 Lottie 動畫（如迷路旅人）、錯誤標題、說明文字、回到首頁按鈕。

---

## 6. UX 指南 (UX Guidelines)

### 6.1 觸控與互動 (Critical)

| 規則 | 做法 | 避免 |
|------|------|------|
| **觸控目標大小** | 最小 44x44px | 小於 44px 的按鈕 |
| **觸控間距** | 按鈕間最少 8px 間距 | 緊貼的可點擊元素 |
| **Hover vs Tap** | 使用 click/tap 為主要互動 | 僅依賴 hover 效果 |
| **點擊反饋** | 所有可點擊元素加 cursor-pointer | 無游標變化 |
| **按鈕狀態** | 異步操作時 disable 按鈕 | 允許重複點擊 |
| **300ms 延遲** | 使用 touch-action: manipulation | 預設觸控處理 |

### 6.2 無障礙 (Critical)

| 規則 | 做法 | 避免 |
|------|------|------|
| **色彩對比** | 最少 4.5:1 對比度 | 低對比文字 |
| **Focus 狀態** | 明顯的 focus ring | 移除 outline |
| **Alt 文字** | 有意義的圖片加 alt | 空白或無意義的 alt |
| **ARIA 標籤** | 圖示按鈕加 aria-label | 無標籤的圖示按鈕 |
| **鍵盤導航** | Tab 順序符合視覺順序 | tabindex 亂跳 |
| **表單標籤** | label 搭配 for 屬性 | 無標籤的輸入框 |
| **Reduced Motion** | 檢查 prefers-reduced-motion | 強制動畫 |

### 6.3 效能 (High)

| 規則 | 做法 | 避免 |
|------|------|------|
| **圖片優化** | WebP 格式、srcset、lazy loading | 未壓縮的大圖 |
| **動畫效能** | 使用 transform/opacity | 動畫 width/height |
| **內容跳動** | 預留異步內容空間 | 內容載入造成版面跳動 |
| **Skeleton Screen** | 載入時顯示骨架屏 | 空白或僅顯示 spinner |

### 6.4 動畫 (Medium)

| 規則 | 做法 | 避免 |
|------|------|------|
| **微互動時長** | 150-300ms | 過慢 (>500ms) 或無過渡 |
| **連續動畫** | 僅用於載入指示器 | 裝飾性無限動畫 |
| **緩動函數** | ease-out 用於進入, ease-in 用於離開 | linear 或不自然的緩動 |

---

## 7. 動效設計 (Animation)

### 動畫規格

| 動畫名稱 | 效果 | 時長 | 緩動函數 |
|----------|------|------|----------|
| 基礎過渡 | 所有屬性過渡 | 200ms | ease-out |
| Toast 滑入 | 從右側滑入 + 淡入 | 300ms | ease-out |
| 抽屜展開 | 從底部滑入 | 300ms | cubic-bezier(0.32, 0.72, 0, 1) |
| 骨架屏閃爍 | 漸層左右移動 | 1.5s | infinite |

所有動畫須尊重 `prefers-reduced-motion` 系統設定，在 reduced motion 模式下動畫時長降至接近 0。

### Lottie 動畫使用場景

| 場景 | 動畫 | 時機 |
|------|------|------|
| 頁面載入 | 飛機飛行 | 全頁載入時 |
| 空狀態 | 跳動行李箱 | 無資料時 |
| 任務完成 | 打勾 + 撒花 | Todo 完成時 |
| 分帳結清 | 撒紙花 | 所有帳結清時 |
| 路線優化 | 旋轉齒輪 | 計算中 |
| 上傳成功 | 綠色打勾 | 檔案上傳完成 |
| 404 | 迷路旅人 | 頁面不存在 |
| 網路錯誤 | 斷線雲朵 | 無網路連線 |

**推薦資源：**
- [LottieFiles](https://lottiefiles.com/) - 搜尋 travel, success, loading
- [Lordicon](https://lordicon.com/) - 精緻圖示動畫

---

## 8. 圖示系統 (Icons)

### 圖示庫選擇

| 庫 | 用途 | CDN |
|------|------|-----|
| **Heroicons** (主要) | UI 圖示 | `@heroicons/vue` 或 `heroicons.com` |
| **Lucide** (備選) | UI 圖示 | `lucide.dev` |
| **Simple Icons** | 品牌 Logo | `simpleicons.org` |

### 常用圖示對照

| 功能 | 圖示名稱 | 變體 |
|------|----------|------|
| 首頁 | `home` | outline / solid |
| 行程 | `map-pin` | outline |
| 分帳 | `banknotes` | outline |
| 檔案 | `document-text` | outline |
| 設定 | `cog-6-tooth` | outline |
| 新增 | `plus` | solid |
| 編輯 | `pencil` | outline |
| 刪除 | `trash` | outline |
| 分享 | `share` | outline |
| 更多 | `ellipsis-vertical` | solid |
| 關閉 | `x-mark` | solid |
| 返回 | `arrow-left` | outline |
| 勾選 | `check` | solid |

### 交通圖示

| 類型 | 圖示 |
|------|------|
| 開車 | `truck` 或自訂 car |
| 大眾運輸 | `building-office` 或自訂 train |
| 步行 | `user` 或自訂 walking |
| 飛機 | `paper-airplane` |

### 圖示使用規範

所有圖示統一使用 SVG 格式（Heroicons 或 Lucide），禁止使用 Emoji 替代。

---

## 9. 交付前檢查清單 (Pre-Delivery Checklist)

### 視覺品質

- [ ] **無 Emoji 作為圖示** - 使用 SVG (Heroicons/Lucide)
- [ ] **圖示一致** - 全部來自同一圖示庫
- [ ] **品牌 Logo 正確** - 從 Simple Icons 驗證
- [ ] **Hover 狀態穩定** - 不造成版面位移
- [ ] **使用主題色彩** - 直接用 Tailwind class（如 `bg-primary-500`）

### 互動

- [ ] **cursor-pointer** - 所有可點擊元素
- [ ] **Hover 反饋** - 顏色/陰影/透明度變化
- [ ] **過渡平滑** - 150-300ms duration
- [ ] **Focus 可見** - 鍵盤導航有明顯 ring
- [ ] **觸控目標** - 最小 44x44px

### 明暗模式

- [ ] **Light 文字對比** - 至少 4.5:1
- [ ] **Glass 元件可見** - Light mode 背景夠深
- [ ] **邊框可見** - 兩種模式都能看到
- [ ] **測試兩種模式** - 交付前雙重確認

### 佈局

- [ ] **浮動元素間距** - navbar/fab 有適當邊距
- [ ] **內容不被遮擋** - 固定導覽列下方有足夠 padding
- [ ] **響應式測試** - 375px, 768px, 1024px, 1440px
- [ ] **無水平捲動** - 手機版無橫向 overflow

### 無障礙

- [ ] **圖片有 alt** - 有意義的描述
- [ ] **表單有 label** - 關聯 for 屬性
- [ ] **ARIA 標籤** - 圖示按鈕有 aria-label
- [ ] **prefers-reduced-motion** - 動畫尊重系統設定
- [ ] **語意化 HTML** - 使用正確的標籤 (nav, main, section, article)

---

## 10. 相關資源

### 設計工具

- [Figma](https://figma.com/) - UI 設計
- [LottieFiles](https://lottiefiles.com/) - 動畫資源
- [Heroicons](https://heroicons.com/) - 圖示庫
- [Tailwind CSS](https://tailwindcss.com/docs) - CSS 框架

### 色彩工具

- [Tailwind Color Generator](https://uicolors.app/) - 產生 Tailwind 色彩
- [Contrast Checker](https://webaim.org/resources/contrastchecker/) - 對比度檢查
- [Coolors](https://coolors.co/) - 調色板生成

### 參考設計

- **TripIt** - 行程管理 UI 參考
- **Splitwise** - 分帳 UI 參考
- **Google Maps** - 地圖互動參考
- **Notion** - 協作編輯體驗參考

### 免費素材

- **Unsplash** - 旅遊照片 (封面圖)
- **unDraw** - 插圖 (空狀態、onboarding)
- **LottieFiles** - 動畫

---

## 附錄：快速參考

### Z-Index 規範

| 層級 | 值 | 用途 |
|------|-----|------|
| Base | 0 | 一般內容 |
| Dropdown | 10 | 下拉選單 |
| Sticky | 20 | 固定 header |
| Fixed | 30 | 固定導覽列 |
| Modal Backdrop | 40 | Modal 背景 |
| Modal | 50 | Modal 本體 |
| Toast | 60 | 通知訊息 |

### Tailwind CSS 建構與 JIT 模式

**建構指令**：在 `src/main/frontend` 目錄下執行 `npm run watch`（開發模式）或 `npm run build`（生產版本），建構後將 `dist/styles.css` 複製至 `src/main/resources/static/css/styles.css`。

**JIT 掃描範圍**：Tailwind JIT 掃描 Thymeleaf 模板（`templates/**/*.html`）和 JavaScript 檔案（`static/js/**/*.js`）中出現的 class 來生成 CSS。

**動態類別的問題**：在 JavaScript 中動態組合的 Tailwind class，若建構時不存在於原始碼中，CSS 就不會生成。

| 解決方案 | 適用場景 | 優缺點 |
|----------|----------|--------|
| **組件類別** | 複雜響應式元件 | 可維護性高、可重用 |
| **Safelist** | 少量特定 class | 簡單，但增加 CSS 大小 |
| **模板佔位** | 偶爾使用的 class | 無需修改配置，但不直觀 |

推薦使用組件類別方案：在 `src/main/frontend/src/input.css` 中以 `@layer components` 定義可重用的元件樣式。

### 現有組件類別

| 類別 | 用途 | 響應式 |
|------|------|--------|
| `.glass-card` | 玻璃卡片容器 | - |
| `.btn-primary` | 主要按鈕 | - |
| `.btn-cta` | CTA 按鈕（橘色） | - |
| `.btn-secondary` | 次要按鈕 | - |
| `.weather-card` | 天氣預報卡片 | 手機固定寬度，桌面等比填滿 |
| `.bottom-nav` | 底部導覽 | - |
| `.toast` | 通知訊息 | - |

詳見 `src/main/frontend/src/input.css`
