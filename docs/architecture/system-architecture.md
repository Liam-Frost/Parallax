# 系统架构

## 索引
- [1. 现状（当前仓库）](#1-现状当前仓库)
- [2. 目标架构（Phase 1 后）](#2-目标架构phase-1-后)
- [3. 模块边界与职责](#3-模块边界与职责)
- [4. 关键数据流](#4-关键数据流)
- [5. 迁移原则](#5-迁移原则)

## 1. 现状（当前仓库）
- 前端：仓库根目录下的静态页面（`index.html` + `styles.css` + `app.js` + `config.js`）。
- 后端：`backend/` 内的 Java 轻量 HTTP Server（`com.sun.net.httpserver.HttpServer`），按 `*Handler` 拆分路由。
- 数据：通过 `UserRepository` / `VehicleRepository` 抽象，当前为内存实现（`InMemory*Repository`）。
- OCR：后端已具备将上传图片转发到 Python 服务并解析结果的链路（`PlateImageQueryHandler`）。

## 2. 目标架构（Phase 1 后）
### 2.1 组件
- 前端（React SPA）：
  - 用户端路由：`/`、`/account`、`/vehicles`、`/query`...
  - 管理端路由：`/admin`（同一 React 项目）
- API（Spring Boot）：
  - 统一 `/api/...` 入口
  - Spring Security + JWT（Access）+ Refresh Cookie（rotation）
- PostgreSQL：
  - 用户、车辆、黑名单状态、refresh_sessions
- OCR 服务（Python）：
  - 对外契约：`POST /v1/detect-plate`

### 2.2 部署拓扑（不同子域同主域）
- `app.<主域>`：静态资源（React build）
- `api.<主域>`：Nginx TLS 终止与 upstream LB 到多个 Spring Boot 实例
- OCR：可内网（推荐）或单独子域；API 通过内网地址访问

## 3. 模块边界与职责
- 前端：只负责 UI/交互/表单校验/路由与 API 调用；不承载安全敏感的身份判定逻辑。
- API：
  - 身份认证/授权（RBAC）
  - 车辆与黑名单业务规则（车牌全局唯一等）
  - OCR 调用封装、超时/重试/降级
- DB：系统真相来源（用户、车辆、会话）；Flyway 管理 schema。

## 4. 关键数据流
### 4.1 登录与保持登录
1. `POST /api/auth/login` -> 返回 access（body）+ refresh（HttpOnly cookie）
2. 前端内存保存 access，所有 API 带 `Authorization: Bearer`
3. access 过期：`POST /api/auth/refresh`（带 cookie）-> rotation -> 下发新的 access + refresh

### 4.2 管理员操作
- RBAC：`ROLE_ADMIN` 才可调用黑名单/全量车辆列表等接口。

### 4.3 OCR 查询
1. 前端上传图片到 `POST /api/vehicles/query-image`
2. API 转发到 OCR `POST /v1/detect-plate`
3. API 查询 DB（车牌全局唯一）返回是否存在/是否黑名单

## 5. 迁移原则
- 兼容优先：Phase 1 保持 `/api/...` 路径，避免前端与部署同时大改。
- 安全优先：不再信任客户端传 username；统一由 JWT 决定当前用户。
- 可演进：Redis、/api/v1、分离 admin SPA 都属于 Phase 2+ 可选项。
