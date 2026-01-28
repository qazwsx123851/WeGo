# Changelog

所有專案的重要變更都會記錄在這個檔案中。

格式基於 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.0.0/)，
版本號遵循 [Semantic Versioning](https://semver.org/lang/zh-TW/)。

## [Unreleased]

### 待開發
- Phase 1 MVP 功能

---

## [0.1.0] - 2024-XX-XX (預計)

### Added - 新增功能
- OAuth 登入（Google）
- 建立/編輯/刪除行程
- 新增/編輯/刪除景點
- 拖拽排序景點
- 交通時間預估（Google Maps API）
- 邀請連結（基本權限）
- 基本分帳（單幣別、均分）
- 檔案上傳（PDF、JPG、PNG）

### Security - 安全性
- HTTPS 強制啟用
- CSRF 防護
- XSS 防護（Thymeleaf 自動 escape）

---

## 版本說明

### 版本號規則
- **主版本 (Major)**：不相容的 API 變更
- **次版本 (Minor)**：向下相容的功能新增
- **修訂版本 (Patch)**：向下相容的錯誤修正

### 變更類型
- **Added**：新增功能
- **Changed**：既有功能的變更
- **Deprecated**：即將移除的功能
- **Removed**：已移除的功能
- **Fixed**：錯誤修正
- **Security**：安全性修正

---

## 開發里程碑

| 版本 | 階段 | 主要功能 |
|------|------|----------|
| 0.1.0 | Phase 1 MVP | 核心功能 |
| 0.2.0 | Phase 2 | 協作強化 |
| 0.3.0 | Phase 3 | 分帳進階 |
| 0.4.0 | Phase 4 | 體驗優化 |
| 1.0.0 | 正式版 | 完整功能 |
