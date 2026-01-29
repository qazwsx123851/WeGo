# WeGo UI 設計指南

## 文件資訊

| 項目 | 內容 |
|------|------|
| 版本 | 2.0.0 |
| 更新日期 | 2024-01 |
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

```
輕盈 · 活潑 · 直覺 · 協作 · 信任 · 冒險
```

### 選用風格

| 風格 | 用途 |
|------|------|
| **Glassmorphism** | 卡片、Modal、浮動元件 |
| **Bento Grid** | Dashboard、功能展示 |
| **Soft UI Evolution** | 按鈕、表單元件 |

---

## 2. 色彩系統 (Color System)

### 主色調 - 旅遊冒險主題

```css
/* Primary - 天空藍（代表旅行、探索、自由） */
--primary-50:  #F0F9FF;
--primary-100: #E0F2FE;
--primary-200: #BAE6FD;
--primary-300: #7DD3FC;
--primary-400: #38BDF8;
--primary-500: #0EA5E9;  /* 主要按鈕、連結 */
--primary-600: #0284C7;
--primary-700: #0369A1;
--primary-800: #075985;
--primary-900: #0C4A6E;  /* 主要文字 */

/* Secondary - 冒險橘（代表熱情、活力、CTA） */
--secondary-50:  #FFF7ED;
--secondary-100: #FFEDD5;
--secondary-200: #FED7AA;
--secondary-300: #FDBA74;
--secondary-400: #FB923C;
--secondary-500: #F97316;  /* CTA 按鈕 */
--secondary-600: #EA580C;
--secondary-700: #C2410C;
```

### 語意色彩

```css
/* 成功 - 完成任務、分帳結清 */
--success-light: #DCFCE7;
--success: #22C55E;
--success-dark: #166534;

/* 警告 - 提醒、截止日期接近 */
--warning-light: #FEF9C3;
--warning: #EAB308;
--warning-dark: #854D0E;

/* 錯誤 - 操作失敗、必填欄位 */
--error-light: #FEE2E2;
--error: #EF4444;
--error-dark: #991B1B;

/* 資訊 - 提示訊息 */
--info-light: #DBEAFE;
--info: #3B82F6;
--info-dark: #1E40AF;
```

### 中性色

```css
/* 用於文字、背景、邊框 */
--gray-50:  #F8FAFC;  /* 頁面背景 */
--gray-100: #F1F5F9;  /* 卡片背景 (Light) */
--gray-200: #E2E8F0;  /* 邊框、分隔線 */
--gray-300: #CBD5E1;
--gray-400: #94A3B8;  /* Placeholder 文字 */
--gray-500: #64748B;  /* 次要文字 */
--gray-600: #475569;  /* 次要文字 (重要) */
--gray-700: #334155;  /* 主要文字 */
--gray-800: #1E293B;  /* 標題文字 */
--gray-900: #0F172A;  /* 深色背景 */
```

### Tailwind 配置

```javascript
// tailwind.config.js
module.exports = {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#F0F9FF',
          100: '#E0F2FE',
          200: '#BAE6FD',
          300: '#7DD3FC',
          400: '#38BDF8',
          500: '#0EA5E9',
          600: '#0284C7',
          700: '#0369A1',
          800: '#075985',
          900: '#0C4A6E',
        },
        adventure: {
          50: '#FFF7ED',
          100: '#FFEDD5',
          200: '#FED7AA',
          300: '#FDBA74',
          400: '#FB923C',
          500: '#F97316',
          600: '#EA580C',
          700: '#C2410C',
        },
      },
      fontFamily: {
        sans: ['Noto Sans TC', 'Plus Jakarta Sans', 'sans-serif'],
        heading: ['Plus Jakarta Sans', 'Noto Sans TC', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
    },
  },
}
```

### Glassmorphism 效果變數

```css
/* 玻璃效果 */
--glass-bg-light: rgba(255, 255, 255, 0.7);
--glass-bg-dark: rgba(15, 23, 42, 0.7);
--glass-blur: 12px;
--glass-border: rgba(255, 255, 255, 0.2);
--glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
```

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

```css
/* 主要字體 - 中文優先 */
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+TC:wght@300;400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

--font-sans: 'Noto Sans TC', 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
--font-heading: 'Plus Jakarta Sans', 'Noto Sans TC', sans-serif;
--font-mono: 'JetBrains Mono', 'Fira Code', monospace;
```

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

```html
<!-- 主要文字 -->
<p class="text-gray-800 dark:text-gray-100">行程名稱</p>

<!-- 次要文字 -->
<p class="text-gray-500 dark:text-gray-400">3 天 2 夜 · 5 位成員</p>

<!-- 強調文字 -->
<p class="text-primary-600 dark:text-primary-400 font-medium">查看詳情</p>

<!-- 金額 -->
<span class="text-gray-800 dark:text-gray-100 font-mono font-semibold">$12,500</span>

<!-- 錯誤文字 -->
<span class="text-error dark:text-red-400">此欄位必填</span>
```

### 行長度限制

```css
/* 最佳閱讀體驗：65-75 字元 */
.prose {
  max-width: 65ch;
}
```

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

```css
/* Tailwind 斷點 */
sm: 640px   /* 大手機 */
md: 768px   /* 平板 */
lg: 1024px  /* 小筆電 */
xl: 1280px  /* 桌機 */
2xl: 1536px /* 大螢幕 */

/* 測試必須涵蓋 */
320px, 375px, 768px, 1024px, 1280px, 1440px
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
│  (pb: 80px for nav)    │
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

### 響應式 Padding

```html
<!-- 容器 padding -->
<div class="px-4 sm:px-6 lg:px-8">

<!-- 卡片 padding -->
<div class="p-4 sm:p-6">
```

---

## 5. 元件設計 (Components)

### 5.1 按鈕 (Buttons)

#### 主要按鈕 (CTA)

```html
<button class="
  bg-adventure-500 hover:bg-adventure-600 active:bg-adventure-700
  text-white font-medium
  px-4 py-2.5 min-h-[44px]
  rounded-xl
  shadow-md hover:shadow-lg
  transition-all duration-200
  active:scale-[0.98]
  focus:outline-none focus:ring-2 focus:ring-adventure-500/50
  cursor-pointer
">
  建立行程
</button>
```

#### 次要按鈕

```html
<button class="
  bg-white hover:bg-gray-50 active:bg-gray-100
  dark:bg-gray-800 dark:hover:bg-gray-700
  text-gray-700 dark:text-gray-200 font-medium
  px-4 py-2.5 min-h-[44px]
  rounded-xl
  border border-gray-200 dark:border-gray-600
  transition-all duration-200
  active:scale-[0.98]
  focus:outline-none focus:ring-2 focus:ring-primary-500/50
  cursor-pointer
">
  取消
</button>
```

#### 危險按鈕

```html
<button class="
  bg-error hover:bg-red-600 active:bg-red-700
  text-white font-medium
  px-4 py-2.5 min-h-[44px]
  rounded-xl
  transition-all duration-200
  active:scale-[0.98]
  cursor-pointer
">
  刪除行程
</button>
```

#### 文字按鈕

```html
<button class="
  text-primary-600 dark:text-primary-400
  hover:text-primary-700 dark:hover:text-primary-300
  font-medium
  px-2 py-1
  rounded-lg
  hover:bg-primary-50 dark:hover:bg-primary-900/20
  transition-colors duration-200
  cursor-pointer
">
  查看更多
</button>
```

#### 圖示按鈕

```html
<button
  aria-label="關閉"
  class="
    p-2.5 min-w-[44px] min-h-[44px]
    rounded-full
    text-gray-500 hover:text-gray-700
    hover:bg-gray-100 dark:hover:bg-gray-800
    transition-colors duration-200
    cursor-pointer
    focus:outline-none focus:ring-2 focus:ring-primary-500/50
  "
>
  <svg class="w-5 h-5"><!-- Heroicons/Lucide icon --></svg>
</button>
```

### 5.2 卡片 (Cards)

#### 玻璃效果卡片 (Glassmorphism)

```html
<div class="
  bg-white/70 dark:bg-gray-800/70
  backdrop-blur-xl
  border border-white/20 dark:border-gray-700/50
  rounded-2xl
  shadow-lg
  p-6
  transition-all duration-300
  hover:shadow-xl hover:bg-white/80 dark:hover:bg-gray-800/80
  cursor-pointer
">
  <!-- 內容 -->
</div>
```

#### 行程卡片

```html
<div class="
  bg-white dark:bg-gray-800
  rounded-2xl
  shadow-md hover:shadow-xl
  transition-all duration-300
  overflow-hidden
  cursor-pointer
  group
">
  <!-- 封面圖 -->
  <div class="aspect-[16/9] bg-gray-100 dark:bg-gray-700 overflow-hidden">
    <img
      src="cover.jpg"
      alt="東京五日遊封面"
      class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
      loading="lazy"
    />
  </div>

  <!-- 內容 -->
  <div class="p-4 space-y-3">
    <h3 class="text-base font-semibold text-gray-800 dark:text-gray-100">
      東京五日遊
    </h3>
    <p class="text-sm text-gray-500 dark:text-gray-400">
      2024/03/15 - 2024/03/19
    </p>

    <!-- 成員頭像 -->
    <div class="flex -space-x-2">
      <img class="w-8 h-8 rounded-full ring-2 ring-white dark:ring-gray-800" alt="成員 1" />
      <img class="w-8 h-8 rounded-full ring-2 ring-white dark:ring-gray-800" alt="成員 2" />
      <span class="
        w-8 h-8 rounded-full
        bg-gray-100 dark:bg-gray-700
        flex items-center justify-center
        text-xs font-medium text-gray-600 dark:text-gray-300
        ring-2 ring-white dark:ring-gray-800
      ">
        +3
      </span>
    </div>
  </div>
</div>
```

#### 景點卡片

```html
<div class="
  bg-white dark:bg-gray-800
  rounded-xl
  border border-gray-100 dark:border-gray-700
  p-3
  flex gap-3
  hover:bg-gray-50 dark:hover:bg-gray-750
  active:bg-gray-100 dark:active:bg-gray-700
  transition-colors duration-200
  cursor-pointer
  touch-action-manipulation
">
  <!-- 序號 -->
  <div class="
    w-10 h-10
    rounded-full
    bg-primary-100 dark:bg-primary-900/30
    text-primary-600 dark:text-primary-400
    flex items-center justify-center
    font-semibold text-sm
    flex-shrink-0
  ">
    1
  </div>

  <!-- 內容 -->
  <div class="flex-1 min-w-0">
    <h4 class="font-medium text-gray-800 dark:text-gray-100 truncate">
      淺草寺
    </h4>
    <p class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
      10:00 - 12:00 · 2 小時
    </p>
  </div>

  <!-- 拖曳手把 -->
  <div class="text-gray-300 dark:text-gray-600 cursor-grab active:cursor-grabbing" aria-label="拖曳排序">
    <svg class="w-5 h-5"><!-- grip-vertical icon --></svg>
  </div>
</div>

<!-- 交通時間連接線 -->
<div class="flex items-center gap-2 py-2 pl-5">
  <div class="w-0.5 h-4 bg-gray-200 dark:bg-gray-700"></div>
  <span class="text-xs text-gray-400 dark:text-gray-500 flex items-center gap-1">
    <svg class="w-3.5 h-3.5"><!-- transit icon --></svg>
    25 分鐘
  </span>
</div>
```

### 5.3 表單元件 (Form Elements)

#### 輸入框

```html
<div class="space-y-1.5">
  <label for="trip-name" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
    行程名稱
  </label>
  <input
    id="trip-name"
    type="text"
    inputmode="text"
    autocomplete="off"
    class="
      w-full px-4 py-3
      bg-white dark:bg-gray-800
      border border-gray-200 dark:border-gray-700
      rounded-xl
      text-gray-800 dark:text-gray-100
      placeholder:text-gray-400 dark:placeholder:text-gray-500
      focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500
      transition-all duration-200
    "
    placeholder="輸入行程名稱"
  />
  <!-- 錯誤訊息 -->
  <p class="text-xs text-error hidden" role="alert">此欄位必填</p>
</div>
```

#### 下拉選單

```html
<div class="space-y-1.5">
  <label for="transport" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
    交通方式
  </label>
  <select
    id="transport"
    class="
      w-full px-4 py-3
      bg-white dark:bg-gray-800
      border border-gray-200 dark:border-gray-700
      rounded-xl
      text-gray-800 dark:text-gray-100
      focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500
      cursor-pointer
    "
  >
    <option value="driving">開車</option>
    <option value="transit">大眾運輸</option>
    <option value="walking">步行</option>
  </select>
</div>
```

#### 日期選擇器

```html
<div class="space-y-1.5">
  <label for="start-date" class="block text-sm font-medium text-gray-700 dark:text-gray-300">
    開始日期
  </label>
  <input
    id="start-date"
    type="date"
    class="
      w-full px-4 py-3
      bg-white dark:bg-gray-800
      border border-gray-200 dark:border-gray-700
      rounded-xl
      text-gray-800 dark:text-gray-100
      focus:outline-none focus:ring-2 focus:ring-primary-500/50 focus:border-primary-500
    "
  />
</div>
```

### 5.4 底部抽屜 (Bottom Drawer)

```html
<!-- 背景遮罩 -->
<div
  class="
    fixed inset-0 bg-black/40 backdrop-blur-sm
    z-40
    transition-opacity duration-300
  "
  aria-hidden="true"
></div>

<!-- 抽屜本體 -->
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="drawer-title"
  class="
    fixed bottom-0 left-0 right-0
    bg-white dark:bg-gray-900
    rounded-t-3xl
    max-h-[85vh]
    z-50
    overflow-hidden
    transform transition-transform duration-300 ease-out
    shadow-2xl
  "
>
  <!-- 拖曳指示條 -->
  <div class="flex justify-center py-3">
    <div class="w-12 h-1.5 bg-gray-300 dark:bg-gray-600 rounded-full"></div>
  </div>

  <!-- 標題 -->
  <div class="px-6 pb-4 border-b border-gray-100 dark:border-gray-800">
    <h2 id="drawer-title" class="text-lg font-semibold text-gray-800 dark:text-gray-100">
      景點詳情
    </h2>
  </div>

  <!-- 內容 -->
  <div class="px-6 py-4 overflow-y-auto max-h-[60vh]">
    <!-- ... -->
  </div>

  <!-- 底部按鈕 -->
  <div class="px-6 py-4 border-t border-gray-100 dark:border-gray-800 pb-safe">
    <button class="w-full bg-primary-500 text-white py-3 rounded-xl font-medium">
      確認
    </button>
  </div>
</div>
```

### 5.5 底部導覽列 (Bottom Navigation)

```html
<nav class="
  fixed bottom-0 left-0 right-0
  bg-white/80 dark:bg-gray-900/80
  backdrop-blur-xl
  border-t border-gray-100 dark:border-gray-800
  px-2 pb-safe
  z-30
">
  <div class="flex justify-around max-w-md mx-auto">
    <!-- 導覽項目 - 選中 -->
    <a
      href="#"
      class="
        flex flex-col items-center
        py-2 px-4
        text-primary-600 dark:text-primary-400
        min-w-[64px] min-h-[56px]
      "
      aria-current="page"
    >
      <svg class="w-6 h-6" aria-hidden="true"><!-- icon --></svg>
      <span class="text-xs mt-1 font-medium">行程</span>
    </a>

    <!-- 導覽項目 - 未選中 -->
    <a
      href="#"
      class="
        flex flex-col items-center
        py-2 px-4
        text-gray-400 dark:text-gray-500
        hover:text-gray-600 dark:hover:text-gray-300
        transition-colors duration-200
        min-w-[64px] min-h-[56px]
      "
    >
      <svg class="w-6 h-6" aria-hidden="true"><!-- icon --></svg>
      <span class="text-xs mt-1">分帳</span>
    </a>

    <!-- 中央新增按鈕 -->
    <button
      aria-label="新增"
      class="
        -mt-5
        w-14 h-14
        bg-adventure-500 hover:bg-adventure-600
        rounded-full
        text-white
        shadow-lg shadow-adventure-500/30
        flex items-center justify-center
        transition-all duration-200
        active:scale-95
        cursor-pointer
      "
    >
      <svg class="w-7 h-7" aria-hidden="true"><!-- plus icon --></svg>
    </button>

    <a href="#" class="...">檔案</a>
    <a href="#" class="...">設定</a>
  </div>
</nav>
```

### 5.6 Toast 訊息通知

```html
<!-- Toast 容器 -->
<div
  role="status"
  aria-live="polite"
  class="fixed top-4 right-4 z-50 space-y-3"
>
  <!-- 成功 Toast -->
  <div class="
    flex items-center gap-3
    bg-white dark:bg-gray-800
    rounded-xl shadow-lg
    border-l-4 border-success
    px-4 py-3
    min-w-[280px]
    animate-slide-in
  ">
    <svg class="w-5 h-5 text-success flex-shrink-0" aria-hidden="true"><!-- check icon --></svg>
    <div class="flex-1">
      <p class="font-medium text-gray-800 dark:text-gray-100">儲存成功</p>
      <p class="text-sm text-gray-500 dark:text-gray-400">行程已更新</p>
    </div>
    <button
      aria-label="關閉通知"
      class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 p-1 cursor-pointer"
    >
      <svg class="w-4 h-4"><!-- x icon --></svg>
    </button>
  </div>
</div>
```

### 5.7 確認對話框 (Confirm Dialog)

```html
<!-- 背景遮罩 -->
<div class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">

  <!-- 對話框 -->
  <div
    role="alertdialog"
    aria-modal="true"
    aria-labelledby="dialog-title"
    aria-describedby="dialog-desc"
    class="
      bg-white dark:bg-gray-800
      rounded-2xl shadow-2xl
      w-full max-w-sm
      overflow-hidden
    "
  >
    <!-- 標題與內容 -->
    <div class="px-6 pt-6 pb-4">
      <h3 id="dialog-title" class="text-lg font-semibold text-gray-800 dark:text-gray-100">
        確定要刪除此景點？
      </h3>
      <p id="dialog-desc" class="text-sm text-gray-500 dark:text-gray-400 mt-2">
        刪除後將無法復原，關聯的檔案也會解除關聯。
      </p>
    </div>

    <!-- 按鈕 -->
    <div class="px-6 pb-6 flex gap-3">
      <button class="
        flex-1 px-4 py-2.5
        rounded-xl
        border border-gray-200 dark:border-gray-600
        text-gray-700 dark:text-gray-200 font-medium
        hover:bg-gray-50 dark:hover:bg-gray-700
        transition-colors duration-200
        cursor-pointer
      ">
        取消
      </button>
      <button class="
        flex-1 px-4 py-2.5
        rounded-xl
        bg-error hover:bg-red-600
        text-white font-medium
        transition-colors duration-200
        cursor-pointer
      ">
        刪除
      </button>
    </div>
  </div>
</div>
```

### 5.8 錯誤狀態頁面

```html
<div class="
  min-h-screen flex flex-col items-center justify-center
  px-4 text-center
  bg-gray-50 dark:bg-gray-900
">
  <!-- Lottie 動畫：迷路的旅人 -->
  <div class="w-48 h-48 mb-6">
    <lottie-player src="/animations/lost-traveler.json" loop autoplay></lottie-player>
  </div>

  <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100 mb-2">
    找不到這個頁面
  </h1>
  <p class="text-gray-500 dark:text-gray-400 mb-6 max-w-xs">
    這個行程可能已被刪除或連結有誤
  </p>

  <a
    href="/"
    class="
      bg-primary-500 hover:bg-primary-600
      text-white font-medium
      px-6 py-3 rounded-xl
      transition-colors duration-200
    "
  >
    回到首頁
  </a>
</div>
```

---

## 6. UX 指南 (UX Guidelines)

### 6.1 觸控與互動 (Critical)

| 規則 | 做法 | 避免 |
|------|------|------|
| **觸控目標大小** | 最小 44x44px (`min-h-[44px] min-w-[44px]`) | 小於 44px 的按鈕 |
| **觸控間距** | 按鈕間最少 8px 間距 (`gap-2`) | 緊貼的可點擊元素 |
| **Hover vs Tap** | 使用 click/tap 為主要互動 | 僅依賴 hover 效果 |
| **點擊反饋** | 所有可點擊元素加 `cursor-pointer` | 無游標變化 |
| **按鈕狀態** | 異步操作時 disable 按鈕 | 允許重複點擊 |
| **300ms 延遲** | 使用 `touch-action: manipulation` | 預設觸控處理 |

### 6.2 無障礙 (Critical)

| 規則 | 做法 | 避免 |
|------|------|------|
| **色彩對比** | 最少 4.5:1 對比度 | 低對比文字 |
| **Focus 狀態** | 明顯的 focus ring | 移除 outline |
| **Alt 文字** | 有意義的圖片加 alt | 空白或無意義的 alt |
| **ARIA 標籤** | 圖示按鈕加 `aria-label` | 無標籤的圖示按鈕 |
| **鍵盤導航** | Tab 順序符合視覺順序 | tabindex 亂跳 |
| **表單標籤** | label 搭配 for 屬性 | 無標籤的輸入框 |
| **Reduced Motion** | 檢查 `prefers-reduced-motion` | 強制動畫 |

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
| **緩動函數** | `ease-out` 用於進入, `ease-in` 用於離開 | linear 或不自然的緩動 |

---

## 7. 動效設計 (Animation)

### CSS 動畫

```css
/* 轉場動效 */
.transition-base {
  transition: all 200ms ease-out;
}

/* Toast 滑入 */
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
  animation: slide-in 300ms ease-out;
}

/* 抽屜展開 */
@keyframes drawer-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}
.animate-drawer-up {
  animation: drawer-up 300ms cubic-bezier(0.32, 0.72, 0, 1);
}

/* 骨架屏閃爍 */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}
.animate-shimmer {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

/* Reduced Motion */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
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

```html
<!-- 正確：使用 SVG 圖示 -->
<svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor">
  <!-- path -->
</svg>

<!-- 錯誤：使用 Emoji -->
<span>🎨</span> <!-- 不要這樣做 -->
```

---

## 9. 交付前檢查清單 (Pre-Delivery Checklist)

### 視覺品質

- [ ] **無 Emoji 作為圖示** - 使用 SVG (Heroicons/Lucide)
- [ ] **圖示一致** - 全部來自同一圖示庫
- [ ] **品牌 Logo 正確** - 從 Simple Icons 驗證
- [ ] **Hover 狀態穩定** - 不造成版面位移
- [ ] **使用主題色彩** - 直接用 `bg-primary-500` 而非 `var()`

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

## 附錄：快速參考卡片

### Tailwind 常用 Class

```html
<!-- 容器 -->
<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

<!-- 卡片 -->
<div class="bg-white dark:bg-gray-800 rounded-2xl shadow-md p-6">

<!-- 按鈕 -->
<button class="bg-primary-500 text-white px-4 py-2.5 rounded-xl font-medium min-h-[44px]">

<!-- 輸入框 -->
<input class="w-full px-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 focus:ring-2 focus:ring-primary-500/50">

<!-- 文字 -->
<h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100">
<p class="text-sm text-gray-500 dark:text-gray-400">

<!-- Glassmorphism -->
<div class="bg-white/70 dark:bg-gray-800/70 backdrop-blur-xl border border-white/20 rounded-2xl">
```

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
