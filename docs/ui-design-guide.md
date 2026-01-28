# WeGo UI 設計指南

## 1. 設計原則

### 核心理念
- **Mobile-First**：優先設計手機體驗，再向上延伸至平板/桌機
- **協作感知**：讓用戶感受到「一起規劃」的氛圍
- **輕鬆愉快**：旅遊規劃應該是開心的，避免過於嚴肅的商務風格
- **資訊層次**：重要資訊一目了然，細節需要時再展開

### 設計關鍵字
```
輕盈 · 活潑 · 直覺 · 協作 · 信任
```

---

## 2. 色彩系統 (Color System)

### 主色調
```css
/* Primary - 活力藍綠（代表旅行、探索） */
--primary-50:  #E6FFFA;
--primary-100: #B2F5EA;
--primary-200: #81E6D9;
--primary-300: #4FD1C5;
--primary-400: #38B2AC;
--primary-500: #319795;  /* 主要按鈕、連結 */
--primary-600: #2C7A7B;
--primary-700: #285E61;
--primary-800: #234E52;
--primary-900: #1D4044;

/* Secondary - 珊瑚橘（代表熱情、活力） */
--secondary-50:  #FFF5F5;
--secondary-100: #FED7D7;
--secondary-200: #FEB2B2;
--secondary-300: #FC8181;
--secondary-400: #F56565;
--secondary-500: #E53E3E;  /* 強調、CTA */
--secondary-600: #C53030;
```

### 語意色彩
```css
/* 成功 - 完成任務、分帳結清 */
--success: #38A169;

/* 警告 - 提醒、截止日期接近 */
--warning: #D69E2E;

/* 錯誤 - 操作失敗、必填欄位 */
--error: #E53E3E;

/* 資訊 - 提示訊息 */
--info: #3182CE;
```

### 中性色
```css
/* 用於文字、背景、邊框 */
--gray-50:  #F7FAFC;  /* 頁面背景 */
--gray-100: #EDF2F7;  /* 卡片背景 */
--gray-200: #E2E8F0;  /* 邊框、分隔線 */
--gray-300: #CBD5E0;
--gray-400: #A0AEC0;  /* 次要文字、placeholder */
--gray-500: #718096;
--gray-600: #4A5568;  /* 次要文字 */
--gray-700: #2D3748;  /* 主要文字 */
--gray-800: #1A202C;
--gray-900: #171923;
```

### Tailwind 配置
```javascript
// tailwind.config.js
module.exports = {
  darkMode: 'class',  // 支援 dark mode
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#E6FFFA',
          100: '#B2F5EA',
          200: '#81E6D9',
          300: '#4FD1C5',
          400: '#38B2AC',
          500: '#319795',
          600: '#2C7A7B',
          700: '#285E61',
        },
        coral: {
          400: '#F56565',
          500: '#E53E3E',
        }
      }
    }
  }
}
```

### 深色模式 (Dark Mode) - Phase 4

> 深色模式預計在 Phase 4 實作，以下為色彩對照表。

#### 色彩對照

| 用途 | Light Mode | Dark Mode |
|------|------------|-----------|
| 頁面背景 | `gray-50` #F7FAFC | `gray-900` #171923 |
| 卡片背景 | `white` #FFFFFF | `gray-800` #1A202C |
| 邊框 | `gray-200` #E2E8F0 | `gray-700` #2D3748 |
| 主要文字 | `gray-700` #2D3748 | `gray-100` #EDF2F7 |
| 次要文字 | `gray-500` #718096 | `gray-400` #A0AEC0 |
| 主色調 | `primary-500` #319795 | `primary-400` #38B2AC |
| 強調色 | `coral-500` #E53E3E | `coral-400` #F56565 |

#### CSS 變數定義
```css
:root {
  --bg-primary: #F7FAFC;
  --bg-card: #FFFFFF;
  --border: #E2E8F0;
  --text-primary: #2D3748;
  --text-secondary: #718096;
}

.dark {
  --bg-primary: #171923;
  --bg-card: #1A202C;
  --border: #2D3748;
  --text-primary: #EDF2F7;
  --text-secondary: #A0AEC0;
}
```

#### 使用範例
```html
<!-- 自動切換的卡片元件 -->
<div class="
  bg-white dark:bg-gray-800
  border border-gray-200 dark:border-gray-700
  text-gray-700 dark:text-gray-100
  rounded-lg p-4
">
  <h3 class="font-semibold">行程名稱</h3>
  <p class="text-gray-500 dark:text-gray-400">描述文字</p>
</div>
```

#### 切換機制
```javascript
// 偵測系統偏好
if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
  document.documentElement.classList.add('dark');
}

// 手動切換
function toggleDarkMode() {
  document.documentElement.classList.toggle('dark');
  localStorage.setItem('theme',
    document.documentElement.classList.contains('dark') ? 'dark' : 'light'
  );
}

// 載入時恢復偏好
const theme = localStorage.getItem('theme');
if (theme === 'dark') {
  document.documentElement.classList.add('dark');
}
```

---

## 3. 字體排版 (Typography)

### 字體選擇
```css
/* 主要字體 - 中文優先 */
--font-sans: 'Noto Sans TC', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;

/* 數字/金額 - 等寬清晰 */
--font-mono: 'JetBrains Mono', 'Fira Code', monospace;
```

### 字級系統
| 用途 | 大小 | 行高 | 字重 | Tailwind |
|------|------|------|------|----------|
| 頁面標題 | 24px | 1.3 | 700 | `text-2xl font-bold` |
| 區塊標題 | 20px | 1.4 | 600 | `text-xl font-semibold` |
| 卡片標題 | 16px | 1.5 | 600 | `text-base font-semibold` |
| 內文 | 14px | 1.6 | 400 | `text-sm` |
| 輔助文字 | 12px | 1.5 | 400 | `text-xs text-gray-500` |
| 金額數字 | 18px | 1.4 | 600 | `text-lg font-semibold font-mono` |

### 文字顏色
```html
<!-- 主要文字 -->
<p class="text-gray-700">行程名稱</p>

<!-- 次要文字 -->
<p class="text-gray-500">3 天 2 夜 · 5 位成員</p>

<!-- 強調文字 -->
<p class="text-primary-600 font-medium">查看詳情</p>

<!-- 金額 -->
<span class="text-gray-800 font-mono font-semibold">$12,500</span>
```

---

## 4. 間距與佈局 (Spacing & Layout)

### 間距系統（8px 基準）
```css
--space-1: 4px;   /* 0.25rem - 極小間距 */
--space-2: 8px;   /* 0.5rem  - 緊湊間距 */
--space-3: 12px;  /* 0.75rem - 元素內間距 */
--space-4: 16px;  /* 1rem    - 標準間距 */
--space-5: 20px;  /* 1.25rem */
--space-6: 24px;  /* 1.5rem  - 區塊間距 */
--space-8: 32px;  /* 2rem    - 大區塊間距 */
```

### 響應式斷點
```css
/* Tailwind 預設斷點 */
sm: 640px   /* 大手機 */
md: 768px   /* 平板 */
lg: 1024px  /* 小筆電 */
xl: 1280px  /* 桌機 */
```

### 頁面佈局模式

#### 手機版（< 768px）
```
┌────────────────────────┐
│  Header (56px)         │  ← 固定頂部
├────────────────────────┤
│                        │
│  Content               │  ← 可捲動區域
│  (padding: 16px)       │
│                        │
├────────────────────────┤
│  Bottom Nav (64px)     │  ← 固定底部
└────────────────────────┘
```

#### 平板/桌機版（≥ 768px）
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

---

## 5. 元件設計 (Components)

### 5.1 按鈕 (Buttons)

```html
<!-- 主要按鈕 -->
<button class="
  bg-primary-500 hover:bg-primary-600
  text-white font-medium
  px-4 py-2 rounded-lg
  transition-colors duration-200
  active:scale-95
">
  建立行程
</button>

<!-- 次要按鈕 -->
<button class="
  bg-white hover:bg-gray-50
  text-gray-700 font-medium
  px-4 py-2 rounded-lg
  border border-gray-200
  transition-colors duration-200
">
  取消
</button>

<!-- 危險按鈕 -->
<button class="
  bg-red-500 hover:bg-red-600
  text-white font-medium
  px-4 py-2 rounded-lg
">
  刪除行程
</button>

<!-- 文字按鈕 -->
<button class="
  text-primary-600 hover:text-primary-700
  font-medium
  hover:underline
">
  查看更多
</button>

<!-- 圖示按鈕 -->
<button class="
  p-2 rounded-full
  hover:bg-gray-100
  transition-colors
">
  <svg>...</svg>
</button>
```

### 5.2 卡片 (Cards)

#### 行程卡片
```html
<div class="
  bg-white rounded-xl
  shadow-sm hover:shadow-md
  transition-shadow duration-200
  overflow-hidden
">
  <!-- 封面圖 -->
  <div class="aspect-[16/9] bg-gray-100">
    <img src="cover.jpg" class="w-full h-full object-cover" />
  </div>

  <!-- 內容 -->
  <div class="p-4">
    <h3 class="text-base font-semibold text-gray-800">
      東京五日遊
    </h3>
    <p class="text-sm text-gray-500 mt-1">
      2024/03/15 - 2024/03/19
    </p>

    <!-- 成員頭像 -->
    <div class="flex -space-x-2 mt-3">
      <img class="w-8 h-8 rounded-full ring-2 ring-white" />
      <img class="w-8 h-8 rounded-full ring-2 ring-white" />
      <span class="w-8 h-8 rounded-full bg-gray-200
                   flex items-center justify-center
                   text-xs text-gray-600 ring-2 ring-white">
        +3
      </span>
    </div>
  </div>
</div>
```

#### 景點卡片
```html
<div class="
  bg-white rounded-lg
  border border-gray-100
  p-3 flex gap-3
  active:bg-gray-50
  cursor-pointer
">
  <!-- 序號 -->
  <div class="
    w-8 h-8 rounded-full
    bg-primary-100 text-primary-700
    flex items-center justify-center
    font-semibold text-sm
    flex-shrink-0
  ">
    1
  </div>

  <!-- 內容 -->
  <div class="flex-1 min-w-0">
    <h4 class="font-medium text-gray-800 truncate">
      淺草寺
    </h4>
    <p class="text-xs text-gray-500 mt-0.5">
      10:00 - 12:00 · 2 小時
    </p>
  </div>

  <!-- 拖曳手把 -->
  <div class="text-gray-300 cursor-grab">
    <svg>⋮⋮</svg>
  </div>
</div>

<!-- 交通時間連接線 -->
<div class="flex items-center gap-2 py-2 pl-4">
  <div class="w-0.5 h-4 bg-gray-200"></div>
  <span class="text-xs text-gray-400 flex items-center gap-1">
    🚇 25 分鐘
  </span>
</div>
```

### 5.3 表單元件 (Form Elements)

```html
<!-- 輸入框 -->
<div>
  <label class="block text-sm font-medium text-gray-700 mb-1">
    行程名稱
  </label>
  <input
    type="text"
    class="
      w-full px-3 py-2
      border border-gray-200 rounded-lg
      focus:outline-none focus:ring-2 focus:ring-primary-500/20
      focus:border-primary-500
      placeholder:text-gray-400
    "
    placeholder="輸入行程名稱"
  />
</div>

<!-- 下拉選單 -->
<select class="
  w-full px-3 py-2
  border border-gray-200 rounded-lg
  bg-white
  focus:outline-none focus:ring-2 focus:ring-primary-500/20
">
  <option>開車</option>
  <option>大眾運輸</option>
  <option>步行</option>
</select>

<!-- 日期選擇器 -->
<input
  type="date"
  class="
    px-3 py-2
    border border-gray-200 rounded-lg
    focus:outline-none focus:ring-2 focus:ring-primary-500/20
  "
/>
```

### 5.4 底部抽屜 (Bottom Drawer)

```html
<!-- 背景遮罩 -->
<div class="
  fixed inset-0 bg-black/30
  transition-opacity duration-300
"></div>

<!-- 抽屜本體 -->
<div class="
  fixed bottom-0 left-0 right-0
  bg-white rounded-t-2xl
  max-h-[85vh] overflow-hidden
  transform transition-transform duration-300
">
  <!-- 拖曳指示條 -->
  <div class="flex justify-center py-3">
    <div class="w-10 h-1 bg-gray-300 rounded-full"></div>
  </div>

  <!-- 內容 -->
  <div class="px-4 pb-6 overflow-y-auto">
    <!-- ... -->
  </div>
</div>
```

### 5.5 底部導覽列 (Bottom Navigation)

```html
<nav class="
  fixed bottom-0 left-0 right-0
  bg-white border-t border-gray-100
  px-2 pb-safe
">
  <div class="flex justify-around">
    <!-- 導覽項目 -->
    <a href="#" class="
      flex flex-col items-center
      py-2 px-3
      text-primary-600
    ">
      <svg class="w-6 h-6"><!-- icon --></svg>
      <span class="text-xs mt-1 font-medium">行程</span>
    </a>

    <a href="#" class="
      flex flex-col items-center
      py-2 px-3
      text-gray-400
    ">
      <svg class="w-6 h-6"><!-- icon --></svg>
      <span class="text-xs mt-1">分帳</span>
    </a>

    <!-- 中央新增按鈕 -->
    <button class="
      -mt-4 w-14 h-14
      bg-primary-500 rounded-full
      text-white shadow-lg
      flex items-center justify-center
    ">
      <svg class="w-7 h-7"><!-- + icon --></svg>
    </button>

    <a href="#" class="...">檔案</a>
    <a href="#" class="...">設定</a>
  </div>
</nav>
```

### 5.6 Toast 訊息通知

#### 成功 Toast
```html
<div class="
  fixed top-4 right-4 z-50
  flex items-center gap-3
  bg-white rounded-lg shadow-lg
  border-l-4 border-green-500
  px-4 py-3
  animate-slide-in
">
  <svg class="w-5 h-5 text-green-500"><!-- check icon --></svg>
  <div>
    <p class="font-medium text-gray-800">儲存成功</p>
    <p class="text-sm text-gray-500">行程已更新</p>
  </div>
  <button class="text-gray-400 hover:text-gray-600">
    <svg class="w-4 h-4"><!-- x icon --></svg>
  </button>
</div>
```

#### 錯誤 Toast
```html
<div class="
  fixed top-4 right-4 z-50
  flex items-center gap-3
  bg-white rounded-lg shadow-lg
  border-l-4 border-red-500
  px-4 py-3
">
  <svg class="w-5 h-5 text-red-500"><!-- alert icon --></svg>
  <div>
    <p class="font-medium text-gray-800">操作失敗</p>
    <p class="text-sm text-gray-500">請稍後再試</p>
  </div>
</div>
```

#### 警告 Toast
```html
<div class="
  fixed top-4 right-4 z-50
  flex items-center gap-3
  bg-white rounded-lg shadow-lg
  border-l-4 border-yellow-500
  px-4 py-3
">
  <svg class="w-5 h-5 text-yellow-500"><!-- warning icon --></svg>
  <div>
    <p class="font-medium text-gray-800">注意</p>
    <p class="text-sm text-gray-500">連結將於 24 小時後過期</p>
  </div>
</div>
```

#### Toast 動畫
```css
@keyframes slide-in {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.animate-slide-in {
  animation: slide-in 0.3s ease-out;
}

/* 自動消失 */
.toast-auto-dismiss {
  animation: slide-in 0.3s ease-out, fade-out 0.3s ease-in 2.7s forwards;
}
```

### 5.7 錯誤狀態頁面

#### 404 頁面
```html
<div class="
  min-h-screen flex flex-col items-center justify-center
  px-4 text-center
">
  <!-- Lottie 動畫：迷路的旅人 -->
  <div class="w-48 h-48 mb-6">
    <lottie-player src="lost-traveler.json" loop autoplay></lottie-player>
  </div>

  <h1 class="text-2xl font-bold text-gray-800 mb-2">
    找不到這個頁面
  </h1>
  <p class="text-gray-500 mb-6">
    這個行程可能已被刪除或連結有誤
  </p>

  <a href="/" class="
    bg-primary-500 hover:bg-primary-600
    text-white font-medium
    px-6 py-2 rounded-lg
  ">
    回到首頁
  </a>
</div>
```

#### 網路錯誤頁面
```html
<div class="
  min-h-screen flex flex-col items-center justify-center
  px-4 text-center
">
  <!-- Lottie 動畫：斷線 -->
  <div class="w-48 h-48 mb-6">
    <lottie-player src="no-connection.json" loop autoplay></lottie-player>
  </div>

  <h1 class="text-2xl font-bold text-gray-800 mb-2">
    網路連線異常
  </h1>
  <p class="text-gray-500 mb-6">
    請檢查網路連線後再試一次
  </p>

  <button onclick="location.reload()" class="
    bg-primary-500 hover:bg-primary-600
    text-white font-medium
    px-6 py-2 rounded-lg
  ">
    重新整理
  </button>
</div>
```

#### 伺服器錯誤頁面
```html
<div class="
  min-h-screen flex flex-col items-center justify-center
  px-4 text-center
">
  <!-- Lottie 動畫：工程師修理中 -->
  <div class="w-48 h-48 mb-6">
    <lottie-player src="server-error.json" loop autoplay></lottie-player>
  </div>

  <h1 class="text-2xl font-bold text-gray-800 mb-2">
    系統忙碌中
  </h1>
  <p class="text-gray-500 mb-6">
    請稍後再試，或聯繫管理員
  </p>

  <button onclick="location.reload()" class="
    bg-primary-500 hover:bg-primary-600
    text-white font-medium
    px-6 py-2 rounded-lg
  ">
    重新整理
  </button>
</div>
```

#### 權限不足提示
```html
<div class="
  bg-yellow-50 border border-yellow-200 rounded-lg
  p-4 flex items-start gap-3
">
  <svg class="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5">
    <!-- lock icon -->
  </svg>
  <div>
    <p class="font-medium text-yellow-800">僅供檢視</p>
    <p class="text-sm text-yellow-700 mt-1">
      您是此行程的 Viewer，如需編輯請聯繫 Owner 變更權限
    </p>
  </div>
</div>
```

### 5.8 確認對話框 (Confirm Dialog)

```html
<!-- 背景遮罩 -->
<div class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center">

  <!-- 對話框 -->
  <div class="
    bg-white rounded-xl shadow-xl
    w-[90%] max-w-sm
    overflow-hidden
  ">
    <!-- 標題 -->
    <div class="px-6 pt-6 pb-4">
      <h3 class="text-lg font-semibold text-gray-800">
        確定要刪除此景點？
      </h3>
      <p class="text-sm text-gray-500 mt-2">
        刪除後將無法復原，關聯的檔案也會解除關聯。
      </p>
    </div>

    <!-- 按鈕 -->
    <div class="px-6 pb-6 flex gap-3">
      <button class="
        flex-1 px-4 py-2 rounded-lg
        border border-gray-200
        text-gray-700 font-medium
        hover:bg-gray-50
      ">
        取消
      </button>
      <button class="
        flex-1 px-4 py-2 rounded-lg
        bg-red-500 hover:bg-red-600
        text-white font-medium
      ">
        刪除
      </button>
    </div>
  </div>
</div>
```

---

## 6. 頁面設計建議

### 6.1 登入頁

```
┌─────────────────────────┐
│                         │
│      [WeGo Logo]        │
│                         │
│   一起規劃，說走就走     │
│                         │
│  ┌─────────────────┐    │
│  │  G  以 Google 登入 │   │
│  └─────────────────┘    │
│                         │
│   登入即表示同意服務條款  │
│                         │
└─────────────────────────┘
```

**設計重點：**
- Logo 使用品牌主色 + 旅行相關圖示（飛機、地圖標記）
- 背景可使用淡漠的旅遊插圖或漸層
- 按鈕使用各平台官方配色

### 6.2 行程總覽 (Dashboard)

```
┌─────────────────────────┐
│ 我的行程          [+]   │  ← Header
├─────────────────────────┤
│                         │
│ 進行中                  │  ← Section
│ ┌─────────────────────┐ │
│ │ 🏔️ 東京五日遊        │ │
│ │ 03/15 - 03/19       │ │
│ │ 👤👤👤 +2            │ │
│ └─────────────────────┘ │
│                         │
│ 即將出發                │
│ ┌─────────────────────┐ │
│ │ 🌊 沖繩三日遊        │ │
│ │ 04/01 - 04/03       │ │
│ └─────────────────────┘ │
│                         │
│ 過去行程                │
│ ┌─────────────────────┐ │
│ │ 🗼 大阪自由行        │ │
│ └─────────────────────┘ │
│                         │
├─────────────────────────┤
│ [首頁] [探索] [+] [通知] [我] │
└─────────────────────────┘
```

**設計重點：**
- 卡片依狀態分組（進行中 > 即將出發 > 過去）
- 進行中的行程卡片稍大、加強視覺
- 使用 emoji 或圖示增加辨識度
- 空狀態顯示引導動畫與 CTA

### 6.3 行程主頁 (Editor)

#### 手機版
```
┌─────────────────────────┐
│ ← 東京五日遊    ⋮       │
├─────────────────────────┤
│ [Day1] [Day2] [Day3]... │  ← 日期 Tabs
├─────────────────────────┤
│ [列表🔘] [地圖○]        │  ← 視圖切換
├─────────────────────────┤
│                         │
│ ① 淺草寺                │
│    10:00-12:00          │
│    ↓ 🚇 25分            │
│ ② 東京晴空塔            │
│    12:30-14:00          │
│    ↓ 🚶 10分            │
│ ③ ...                   │
│                         │
│ ┌─────────────────────┐ │
│ │  + 新增景點          │ │
│ └─────────────────────┘ │
│                         │
├─────────────────────────┤
│  🏠   📍   [+]   💰   ⚙️  │
└─────────────────────────┘
```

#### 平板/桌機版
```
┌────────────────────────────────────────────┐
│ ← 東京五日遊                    👤👤👤 [邀請] │
├────────────────────────────────────────────┤
│ [Day1] [Day2] [Day3] [Day4] [Day5]        │
├──────────────────┬─────────────────────────┤
│                  │                         │
│ ① 淺草寺         │      [Google Map]       │
│    ↓ 🚇 25分     │                         │
│ ② 東京晴空塔     │        📍 1            │
│    ↓ 🚶 10分     │              📍 2       │
│ ③ 上野公園       │                    📍 3 │
│                  │                         │
│ [+ 新增景點]     │      [優化路線]         │
│                  │                         │
└──────────────────┴─────────────────────────┘
```

**設計重點：**
- 日期 Tabs 可左右滑動
- 景點卡片間的交通連接線清楚顯示時間
- 地圖標記與左側列表序號對應
- 拖拽排序時有明顯的視覺反饋

### 6.4 分帳頁面

```
┌─────────────────────────┐
│ ← 分帳           [新增]  │
├─────────────────────────┤
│                         │
│ 總支出  $45,680         │
│ ─────────────────────── │
│ 🍽️ 餐飲 35%  🚗 交通 25% │  ← 圓餅圖
│ 🏨 住宿 30%  🎫 其他 10% │
│                         │
├─────────────────────────┤
│ 支出紀錄                │
│ ┌─────────────────────┐ │
│ │ 🍜 一蘭拉麵          │ │
│ │ Mark 代墊 · ¥3,200  │ │
│ │ 3人均分             │ │
│ └─────────────────────┘ │
│ ┌─────────────────────┐ │
│ │ 🚃 JR Pass          │ │
│ │ Amy 代墊 · ¥29,110  │ │
│ └─────────────────────┘ │
│                         │
├─────────────────────────┤
│ [查看結算] ← 醒目按鈕   │
└─────────────────────────┘
```

#### 結算頁面
```
┌─────────────────────────┐
│ ← 結算結果              │
├─────────────────────────┤
│                         │
│ 最簡化的付款方式：       │
│                         │
│ ┌─────────────────────┐ │
│ │ 👤 Mark             │ │
│ │       ──$2,350→     │ │
│ │              👤 Amy │ │
│ │ [標記已結清]        │ │
│ └─────────────────────┘ │
│                         │
│ ┌─────────────────────┐ │
│ │ 👤 John             │ │
│ │       ──$1,200→     │ │
│ │              👤 Amy │ │
│ │ [標記已結清]        │ │
│ └─────────────────────┘ │
│                         │
│ ✅ 已結清 2 筆          │
│                         │
└─────────────────────────┘
```

---

## 7. 動效設計 (Animation)

### 轉場動效
```css
/* 頁面切換 - 滑入 */
.page-enter {
  transform: translateX(100%);
}
.page-enter-active {
  transform: translateX(0);
  transition: transform 300ms ease-out;
}

/* 抽屜展開 */
.drawer-enter {
  transform: translateY(100%);
}
.drawer-enter-active {
  transform: translateY(0);
  transition: transform 300ms cubic-bezier(0.32, 0.72, 0, 1);
}
```

### 微互動
```css
/* 按鈕點擊 */
.btn:active {
  transform: scale(0.95);
}

/* 卡片 hover */
.card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

/* 載入骨架屏 */
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
```

### Lottie 動畫使用場景

| 場景 | 動畫 | 時機 |
|------|------|------|
| 頁面載入 | 飛機飛行 | 全頁載入時 |
| 空狀態 | 跳動行李箱 | 無資料時 |
| 任務完成 | 打勾 + 撒花 | Todo 完成時 |
| 分帳結清 | 撒紙花 | 所有帳結清時 |
| 路線優化 | 旋轉齒輪 | 計算中 |
| 上傳成功 | 綠色打勾 | 檔案上傳完成 |

**推薦 Lottie 資源：**
- [LottieFiles](https://lottiefiles.com/) - 搜尋 travel, success, loading
- [Lordicon](https://lordicon.com/) - 精緻圖示動畫

---

## 8. 圖示系統 (Icons)

### 推薦圖示庫
- **Heroicons** (推薦) - Tailwind 官方推薦，風格一致
- **Lucide** - 輕量、清晰
- **Phosphor** - 多樣風格可選

### 常用圖示對照
| 功能 | 圖示名稱 | 樣式 |
|------|----------|------|
| 首頁 | `home` | outline / solid |
| 行程 | `map-pin` | outline |
| 分帳 | `banknotes` | outline |
| 檔案 | `document` | outline |
| 設定 | `cog-6-tooth` | outline |
| 新增 | `plus` | solid |
| 編輯 | `pencil` | outline |
| 刪除 | `trash` | outline |
| 分享 | `share` | outline |
| 更多 | `ellipsis-vertical` | solid |

### 交通圖示
```
🚗 開車     → car / truck
🚇 地鐵     → building-office (或自訂)
🚌 公車     → truck
🚶 步行     → user
✈️ 飛機     → paper-airplane
🚄 高鐵     → rocket-launch
```

---

## 9. 無障礙設計 (Accessibility)

### 基本要求
- 色彩對比度符合 WCAG 2.1 AA 標準（4.5:1）
- 所有互動元素可被鍵盤操作
- 圖片與圖示提供 alt text
- 表單元素關聯 label

### 實作建議
```html
<!-- 圖示按鈕需要 aria-label -->
<button aria-label="刪除景點" class="...">
  <svg>...</svg>
</button>

<!-- 互動提示 -->
<div role="status" aria-live="polite">
  行程已儲存
</div>

<!-- 載入狀態 -->
<div aria-busy="true" aria-label="載入中">
  <Spinner />
</div>
```

---

## 10. 設計資源

### Figma 元件庫建議結構
```
WeGo Design System
├── 🎨 Foundations
│   ├── Colors
│   ├── Typography
│   ├── Spacing
│   └── Effects (shadows, radius)
├── 🧩 Components
│   ├── Buttons
│   ├── Cards
│   ├── Forms
│   ├── Navigation
│   └── Modals & Drawers
├── 📱 Templates
│   ├── Login
│   ├── Dashboard
│   ├── Trip Editor
│   ├── Expense
│   └── Settings
└── 📐 Layouts
    ├── Mobile
    ├── Tablet
    └── Desktop
```

### 參考設計
- **TripIt** - 行程管理 UI 參考
- **Splitwise** - 分帳 UI 參考
- **Google Maps** - 地圖互動參考
- **Notion** - 協作編輯體驗參考

### 免費素材資源
- **Unsplash** - 旅遊照片（封面圖）
- **unDraw** - 插圖（空狀態、onboarding）
- **LottieFiles** - 動畫

---

## 11. 設計 Checklist

### 開發前確認
- [ ] 色彩系統定義完成
- [ ] 字體引入與排版規範
- [ ] 元件庫 Figma 完成
- [ ] RWD 斷點確認
- [ ] 動效規範確認
- [ ] 圖示庫選定

### 各頁面檢查
- [ ] 載入狀態（Skeleton / Spinner）
- [ ] 空狀態設計
- [ ] 錯誤狀態設計
- [ ] 成功回饋動效
- [ ] 手機版 / 平板版 / 桌機版
- [ ] 深色模式（Phase 4）
