## 開發規範提醒

1. **遵循現有模式** - 參考 WeatherClient/OpenWeatherMapClient/MockWeatherClient 的介面抽象化模式
2. **TDD 流程** - 先寫測試 (RED)，再寫實作 (GREEN)，最後重構 (REFACTOR)
3. **契約註解** - 所有 public 方法須包含 `@contract` 註解
4. **授權檢查** - 統計 API 必須在資料存取前檢查權限
5. **覆蓋率目標** - Service >= 80%, Domain >= 90%
6. **BigDecimal** - 金額計算禁止使用 double/float

---

## 進度追蹤

| 日期 | 完成項目 | 備註 |
|------|----------|------|
| 2026-02-03 | 建立 task.md v1.0 | 開始 Phase 3 規劃 |
| 2026-02-03 | 更新 task.md v2.0 | 經 agent 審查，細化任務、加入安全檢查清單 |
| 2026-02-03 | Phase 3.1 完成 (10/11) | ExchangeRate 基礎建設完成，P3-004f 跳過 |
| 2026-02-03 | Phase 3.2 完成 | 多幣別結算、即時匯率預覽，全部 714 tests 通過 |
| 2026-02-03 | Phase 3.3 完成 | 統計功能完成，分類/趨勢/成員統計，Chart.js 整合，725 tests 通過 |
| 2026-02-03 | Phase 3.4 完成 | CacheConfig + Caffeine 設定，資料庫索引 migration，725 tests 通過 |
| 2026-02-03 | Phase 3.5 完成 | StatisticsService + API Controller 測試，756 tests 通過，**Phase 3 全部完成** |
| 2026-02-03 | P3-T-001 補齊 | 新增 ExchangeRateServiceTest.java (30 tests)，總共 786 tests 通過 |
| 2026-02-04 | Phase 4 規劃完成 | 安全修復 + E2E + 深色模式 |
| 2026-02-04 | Phase 4 審核完成 | 經 agent 審查，移除誤報、調整順序、移除 PWA |
| 2026-02-04 | Phase 4.1 CRITICAL/HIGH 完成 | 5 個安全漏洞修復，786 tests 通過 |
| 2026-02-04 | Phase 4.1 MEDIUM 部分完成 | OAuth Mock 機制、CSRF 強化、SpEL null-safety、bucket 修復 |
| 2026-02-04 | Phase 4.1 完成 | Rate Limiting、前端 TTL、N+1 修復，786 tests 通過 |
| 2026-02-04 | Phase 4.2 完成 | 深色模式實作：Navbar 切換、FOUC 防護、Chart.js 主題響應，786 tests 通過 |
| 2026-02-04 | Phase 4.3 完成 | E2E 測試全部完成，89 passed (chromium)，786 unit tests 通過 |
| 2026-02-04 | Phase 4.4 完成 | 安全強化、無障礙修復、文件更新，**Phase 4 全部完成** |
| 2026-02-12 | 文件更新 | 測試統計更新為 ~864 unit tests + ~118 E2E tests |
| - | - | - |

---

## Phase 4 - 安全修復 + 品質提升

> 規劃日期: 2026-02-04
> 審核日期: 2026-02-04
> 預估週期: **5 週** (原 7 週，移除 PWA)
> 開發順序: 安全優先

### Phase 4.1: 安全修復 + E2E 基礎 (Week 1-2) 🔴 CRITICAL

#### 後端 CRITICAL (立即修復)

| ID | 問題 | 檔案 | 狀態 |
|----|------|------|:----:|
| BE-SEC-002 | 認證繞過 (**4** Controllers) | `Activity/Todo/Document/ExpenseApiController.java` | ✅ |
| BE-SEC-001 | IDOR 跨行程文件洩漏 | `DocumentService.java:342` | ✅ |

#### 前端 HIGH

| ID | 問題 | 檔案 | 狀態 |
|----|------|------|:----:|
| SEC-007 | DOM XSS (innerHTML) | `document/list.html:609` | ✅ |

#### 後端 HIGH

| ID | 問題 | 檔案 | 狀態 |
|----|------|------|:----:|
| BE-SEC-003 | 刪除行程資料殘留 | `TripService.java:260-295` | ✅ |
| BE-SEC-004 | Splits 驗證缺失 | `ExpenseService.java:301-465` | ✅ |

#### E2E 基礎 (Week 1)

| ID | 項目 | 狀態 |
|----|------|:----:|
| P4-E2E-001 | Playwright 環境建置 | ✅ 已存在 |
| P4-E2E-002 | OAuth Mock 機制 | ✅ |

#### MEDIUM

| ID | 問題 | 狀態 |
|----|------|:----:|
| SEC-004 | JS 語法錯誤 (todo) | ✅ (CSRF 驗證強化) |
| SEC-005 | SpEL null (components.html:128) | ✅ |
| BE-SEC-005 | Cover image bucket 錯誤 | ✅ |
| BE-SEC-006 | 成員驗證缺失 | ✅ 已正確實作 |
| CR-HIGH-001 | API 認證缺失 | ✅ 已正確配置 |
| CR-HIGH-002 | Rate Limiting | ✅ 已實作 (Bucket4j) |
| CR-HIGH-003 | 匯率轉換失敗處理 | ✅ 已實作 fallback |
| CR-MEDIUM-001 | 前端快取無 TTL | ✅ (30 分鐘 TTL) |
| CR-MEDIUM-002 | 貨幣代碼驗證 | ✅ 已正確實作 |
| CR-MEDIUM-004 | 測試覆蓋不足 | ⏳ 持續改善 |
| CR-MEDIUM-005 | 契約註解不完整 | ⏳ 持續改善 |
| NEW-002 | CSRF token fallback 為空字串 | ✅ |
| NEW-003 | 用戶枚舉 (不同錯誤訊息) | ✅ N/A (OAuth2) |

#### LOW

| ID | 問題 | 狀態 |
|----|------|:----:|
| BE-SEC-007 | N+1 查詢 (TodoService) | ✅ |
| NEW-001 | 敏感資料 (UUID) 在 debug logs | ⏳ 低優先 |

#### 已修復/誤報 (移除)

| ID | 原因 |
|----|------|
| ~~SEC-002~~ | 已正確使用 `th:data-*` + `this.dataset.*` |
| ~~SEC-003~~ | 已正確使用 `th:data-*` + `this.dataset.*` |
| ~~SEC-001~~ | `error.html` 無 `th:utext`，已修復或誤判 |
| ~~SEC-006~~ | `global-overview.html` 已使用 null-safe (`?.`, `?:`) |

### Phase 4.2: 深色模式 (Week 3)

> 架構已準備就緒：Tailwind `darkMode: 'class'` 已配置，`DarkMode` 控制器已存在於 `app.js`

| ID | 項目 | 狀態 |
|----|------|:----:|
| P4-DARK-001 | dark-mode.js 控制器 | ✅ 已存在 |
| P4-DARK-002 | Navbar 切換按鈕元件 | ✅ |
| P4-DARK-003a | Core layouts (3 templates) | ✅ (dashboard, login, index) |
| P4-DARK-003b | Trip templates (4) | ✅ (FOUC + dark classes) |
| P4-DARK-003c | Activity templates (4) | ✅ (list, detail: FOUC; create: via head fragment) |
| P4-DARK-003d | Expense templates (5) | ✅ (all via head fragment; global-overview: FOUC) |
| P4-DARK-003e | Document templates (3) | ✅ (list: via head fragment; upload, global-overview: FOUC) |
| P4-DARK-003f | Error templates (4) | ✅ |
| P4-DARK-003g | Profile/Other templates (4) | ✅ (index, edit: FOUC; todo: via head fragment) |
| P4-DARK-004 | CSS 變數補全 + Chart.js 配色 | ✅ (themechange 事件 + 圖表重繪) |
| P4-DARK-005 | FOUC 防護 (head 內嵌 JS) | ✅ (15 templates + fragments/head) |

### Phase 4.3: E2E 測試 (Week 4)

> 測試框架: Playwright + e2e profile
> 測試結果: **~118 passed** (chromium)

| ID | 項目 | 狀態 |
|----|------|:----:|
| P4-E2E-003 | auth.spec.ts (登入/登出) | ✅ (26 tests) |
| P4-E2E-004 | trip.spec.ts (行程 CRUD) | ✅ (18 tests) |
| P4-E2E-005 | activity.spec.ts | ✅ (18 tests) |
| P4-E2E-006 | expense.spec.ts | ✅ (12 tests) |
| P4-E2E-007 | document.spec.ts | ✅ (15 tests) |
| P4-E2E-008 | CI 整合 (Maven exec) | ⬜ |
| P4-E2E-009 | todo.spec.ts | ✅ (14 tests) |
| P4-E2E-010 | statistics.spec.ts | ⏳ 已含於 expense.spec.ts |
| P4-E2E-011 | settlement.spec.ts | ✅ (6 tests) |
| P4-E2E-012 | dark-mode.spec.ts | ✅ (28 tests) |
| P4-E2E-013 | member.spec.ts | ✅ (8 tests) |
| P4-E2E-014 | profile.spec.ts | ✅ (5 tests) |

### ~~Phase 4.4: PWA 離線支援~~ (已移除)

> 用戶決定不開發 PWA 離線功能

### Phase 4.4: 收尾 (Week 5)

| ID | 項目 | 狀態 |
|----|------|:----:|
| P4-FIN-001 | 全面回歸測試 | ✅ (~864 unit + ~118 E2E) |
| P4-FIN-002 | 文件更新 | ✅ (README.md 更新) |
| P4-FIN-003 | Lighthouse 審計 (Performance >= 90) | ⬜ |
| P4-FIN-004 | 安全滲透測試 | ✅ (Rate Limit 記憶體洩漏修復) |
| P4-FIN-005 | Accessibility 審計 (WCAG 2.1) | ✅ (HIGH issues fixed) |
| P4-FIN-006 | 跨瀏覽器測試 | ✅ (chromium + mobile-chrome) |

---

## Phase 4 技術決策

### 深色模式色彩系統 (Tailwind)

> 保留現有 gray 色系，不引入 slate

Light Mode 使用 gray-50 背景、white 表面、gray-900 文字、gray-600 輔助文字、gray-200 邊框。

Dark Mode 使用 gray-900 背景、gray-800 表面、gray-100 文字、gray-400 輔助文字、gray-700 邊框。CTA 按鈕使用 adventure-500 保持品牌一致性。

### 安全修復模式

前端安全修復統一使用 `th:data-*` 屬性搭配 `this.dataset.*` 存取，取代直接在 `th:onclick` 中拼接值。

後端認證修復在所有 API Controller 方法中加入 principal null 檢查，null 時拋出 UnauthorizedException。建議後續可使用 AOP 攔截器（`@RequiresTripMembership`）統一處理。

---

## 關鍵檔案路徑

| 用途 | 路徑 |
|------|------|
| 安全配置 | `src/main/java/com/wego/config/SecurityConfig.java` |
| API Controllers | `src/main/java/com/wego/controller/*ApiController.java` |
| Tailwind 配置 | `src/main/frontend/tailwind.config.js` |
| DarkMode 控制器 | `src/main/resources/static/js/app.js` |
| E2E 測試 | `e2e/tests/*.spec.ts` |
