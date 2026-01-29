# WeGo 專案檢視與 E2E 測試實施計畫

## 專案現況摘要

### 已完成功能

| 模組 | 狀態 | 測試覆蓋 |
|------|:----:|:--------:|
| User Module (OAuth 登入) | ✅ 100% | 68+ tests |
| Trip Module (行程管理) | ✅ 100% | 100+ tests |
| Activity Module (景點/活動) | ✅ 100% | 40+ tests |
| Expense Module (分帳) | ✅ 100% | 60+ tests |
| Settlement Module (結算演算法) | ✅ 100% | 30+ tests |
| Document Module (檔案上傳) | ⚠️ 85% | Entity + Repository only |
| Google Maps Integration | ❌ 0% | Mock only |

### 測試狀態

```
單元/整合測試: 349 tests, 100% 通過 ✅
E2E 測試: 尚未建立 ❌
```

### 待完成項目 (Phase 1 剩餘 10%)

1. **P1-F-003**: DocumentService (Supabase Storage 整合)
2. **P1-A-005**: GoogleMapsService (可使用 Mock)
3. **E2E 測試**: 關鍵用戶流程測試

---

## 實施計畫

### Phase A: E2E 測試基礎設施 (優先)

#### A.1 Playwright 環境設定
- [ ] 新增 Playwright Maven 依賴或獨立 Node.js 測試專案
- [ ] 設定測試環境配置
- [ ] 建立測試用帳號與資料

#### A.2 測試架構
```
src/test/e2e/
├── playwright.config.ts
├── tests/
│   ├── auth.spec.ts          # 登入/登出流程
│   ├── trip-crud.spec.ts     # 行程 CRUD
│   ├── activity.spec.ts      # 景點管理
│   ├── expense.spec.ts       # 分帳功能
│   └── settlement.spec.ts    # 結算功能
├── fixtures/
│   └── test-data.ts          # 測試資料
└── utils/
    └── helpers.ts            # 輔助函數
```

### Phase B: 關鍵用戶流程 E2E 測試

根據 `docs/test-cases.md` 優先測試以下流程：

#### B.1 認證流程 (優先級: HIGH)
- [ ] **E2E-AUTH-001**: Google OAuth 登入流程
- [ ] **E2E-AUTH-002**: Session 持久化驗證
- [ ] **E2E-AUTH-003**: 登出流程

#### B.2 行程管理流程 (優先級: HIGH)
- [ ] **E2E-TRIP-001**: 建立新行程
- [ ] **E2E-TRIP-002**: 編輯行程資訊
- [ ] **E2E-TRIP-003**: 邀請成員加入
- [ ] **E2E-TRIP-004**: 角色權限驗證

#### B.3 景點管理流程 (優先級: MEDIUM)
- [ ] **E2E-ACT-001**: 新增景點
- [ ] **E2E-ACT-002**: 編輯景點
- [ ] **E2E-ACT-003**: 拖拽排序景點
- [ ] **E2E-ACT-004**: 刪除景點

#### B.4 分帳流程 (優先級: HIGH)
- [ ] **E2E-EXP-001**: 新增支出 (均分)
- [ ] **E2E-EXP-002**: 新增支出 (指定分帳)
- [ ] **E2E-EXP-003**: 查看結算結果
- [ ] **E2E-EXP-004**: 標記已結清

### Phase C: 缺失功能補完

#### C.1 DocumentService 實作
- [ ] **P1-F-003**: 建立 DocumentService
  - Supabase Storage API 整合
  - 檔案上傳/下載功能
  - 檔案類型驗證
  - 大小限制檢查

#### C.2 GoogleMapsService (Mock 版本)
- [ ] **P1-A-005**: 建立 MockGoogleMapsService
  - 模擬距離計算
  - 模擬交通時間估算
  - 為正式 API 整合預留介面

---

## 實施順序與時程

### 第一步: E2E 測試基礎設施 (A.1)
**預估工作量**: 中等
**依賴**: 無

### 第二步: 認證與行程 E2E 測試 (B.1, B.2)
**預估工作量**: 中等
**依賴**: A.1 完成

### 第三步: 分帳 E2E 測試 (B.4)
**預估工作量**: 中等
**依賴**: A.1 完成

### 第四步: 景點 E2E 測試 (B.3)
**預估工作量**: 低
**依賴**: A.1 完成

### 第五步: 功能補完 (C.1, C.2)
**預估工作量**: 低
**依賴**: 無

---

## 風險評估

| 風險 | 影響 | 緩解策略 |
|------|:----:|----------|
| OAuth Mock 困難 | 高 | 使用測試帳號或 Mock OAuth Provider |
| Supabase 測試資料隔離 | 中 | 使用專用測試 Schema 或 Branch |
| E2E 測試不穩定 | 中 | 加入重試機制與等待策略 |

---

## 建議使用的 Agent

| 階段 | Agent | 用途 |
|------|-------|------|
| E2E 測試開發 | `e2e-runner` | Playwright 測試生成與執行 |
| 功能補完 | `tdd-guide` | TDD 流程實作 |
| 程式碼審查 | `code-reviewer` | 品質檢查 |
| 安全審查 | `security-reviewer` | 檔案上傳安全性 |

---

## 下一步行動

**建議優先執行**: 設定 Playwright E2E 測試環境並建立第一個認證測試

等待確認後，我將使用 `e2e-runner` agent 開始建立 E2E 測試基礎設施。
