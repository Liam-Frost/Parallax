# Parallax 项目开发进度文档

## 索引
- [1. 项目概览](#1-项目概览)
- [2. 架构与模块](#2-架构与模块)
- [3. 目录结构](#3-目录结构)
- [4. 已实现功能](#4-已实现功能)
- [5. 实现要点与特点](#5-实现要点与特点)
- [6. 当前状态与限制](#6-当前状态与限制)
- [7. 测试与质量保障](#7-测试与质量保障)
- [8. 进度记录](#8-进度记录)
- [9. 下一步计划](#9-下一步计划)
- [10. 关键入口与配置](#10-关键入口与配置)

## 1. 项目概览
Parallax 是一个停车场/校园车辆管理的端到端演示系统，包含静态前端与轻量级 Java HTTP 后端，支持用户注册登录、车辆登记、黑名单管理和车牌查询。当前版本重点在于快速验证业务流程与接口合同，持久化与安全机制仍处在规划阶段。

## 2. 架构与模块
### 2.1 总体架构
- 前端：纯 HTML/CSS/JS 的单页式体验，负责 UI、表单流程、状态管理与 API 调用。
- 后端：基于 `com.sun.net.httpserver.HttpServer` 的轻量服务，按 handler 模块拆分。
- 数据层：`UserRepository` / `VehicleRepository` 接口抽象，当前为内存实现，预留 SQLite 替换。
- 外部服务（已接入接口，部署可选）：Python 车牌识别服务，后端通过 HTTP 调用。

### 2.2 后端模块划分
- HTTP 层：`parallax.backend.http` 下的 Handler 分离登录、注册、账户、车辆、查询、健康检查。
- 配置层：`parallax.backend.config.AppConfig` 统一读取端口、管理员账号、外部服务地址。
- 数据层：`parallax.backend.db` 负责用户/车辆的 CRUD 与黑名单状态维护。

### 2.3 前端模块划分
- `index.html`：页面结构与各业务模块布局（登录、注册、账户、车辆、查询）。
- `styles.css`：Apple 风格 UI 主题、布局与动效。
- `app.js`：表单校验、状态机（登录阶段切换）、API 调用、车辆表格过滤、验证码等逻辑。
- `config.js`：前端 API_BASE 配置入口。

## 3. 目录结构
```
Parallax/
├─ index.html
├─ styles.css
├─ app.js
├─ config.js
├─ API.md
├─ README.md
├─ backend/
│  ├─ pom.xml
│  └─ src/main/java/parallax/backend/
│     ├─ config/
│     ├─ db/
│     ├─ http/
│     └─ model/
└─ resource/
   └─ img/Logo.png
```

## 4. 已实现功能
### 4.1 账户与认证
- 登录：支持邮箱或手机号登录，后端校验成功返回 `LoginResponse`，前端将 `ft_session` 写入 `localStorage`。
- 注册：前端包含多字段校验（地区、生日、邮箱、密码强度、手机号），后端拒绝重复邮箱/手机号。
- 账户管理：支持更新联系方式、修改密码、删除账号（管理员账号不可删除）。

### 4.2 车辆管理与黑名单
- 车辆登记：提交车牌、厂商、车型、年份，后端完成合法性校验并存储。
- 车辆列表：普通用户只读自己的车辆，管理员可查看所有车辆并带上车主信息。
- 车辆删除：普通用户删除自己的车辆，管理员按车牌全局删除。
- 黑名单：管理员可更新某车牌的黑名单状态。

### 4.3 查询能力
- 文字车牌查询：`GET /api/vehicles/query?license=...`，返回存在与黑名单状态，无车主信息。
- 图片识别查询：`POST /api/vehicles/query-image` 接收图片并转发到 Python 服务，返回识别车牌与黑名单状态。

### 4.4 系统健康
- 健康检查接口：`GET /api/health` 返回 `{ "status": "ok" }`。

## 5. 实现要点与特点
- 轻量 HTTP 服务：没有框架依赖，便于理解请求流与路由注册逻辑。
- 数据层可替换：以接口抽象隔离存储，实现 SQLite 迁移时只需替换实现类。
- 前端流程细化：登录分阶段、注册字段完整、UI 统一且强调可读性。
- 基本校验逻辑：前后端同时做邮箱/密码/车牌基本校验。
- 图像识别链路：Java 后端已包含对 Python OCR 服务的转发与结果解析流程。

## 6. 当前状态与限制
- 数据存储为内存实现，重启后数据丢失。
- 鉴权为简化逻辑，使用 `X-User`/`ft_session` 方式标识用户，尚未引入 JWT/Session。
- API.md 与实现存在小差异（例如账户接口当前使用 `POST /api/account/contact` 与 `POST /api/account/password`），需要在后续版本统一。
- 密码为明文存储，仅用于演示。

## 7. 测试与质量保障
- 已包含 JUnit 单元测试：覆盖登录/注册/账户/车辆/查询/健康检查和内存仓库。
- 集成测试：`HttpServerAppIntegrationTest` 验证服务启动和关键路由。

## 8. 进度记录
| 日期 | 事项 | 状态 | 备注 |
| --- | --- | --- | --- |
| 2026-01-22 | 当前文档创建，整理架构与功能现状 | 已完成 | 可持续追加 |

## 9. 下一步计划
- 替换数据层为 SQLite，实现持久化与迁移脚本。
- 引入 JWT 或服务器端会话，统一登录与鉴权逻辑。
- 统一 API 文档与当前实现路径/方法。
- 完善 OCR 服务对接的错误处理与重试策略。

## 10. 关键入口与配置
- 后端启动入口：`backend/src/main/java/parallax/backend/http/HttpServerApp.java`
- 后端配置：`backend/src/main/java/parallax/backend/config/AppConfig.java`
- 前端配置：`config.js`
- API 文档：`API.md`
- 项目概览：`README.md`
