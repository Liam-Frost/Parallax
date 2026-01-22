# Phase 1 开发计划：Spring Boot + PostgreSQL + React（含 /admin）

## 索引
- [1. 阶段目标](#1-阶段目标)
- [2. 范围与非目标](#2-范围与非目标)
- [3. 里程碑与交付物](#3-里程碑与交付物)
- [4. 技术方案（已敲定）](#4-技术方案已敲定)
- [5. 任务拆解（建议执行顺序）](#5-任务拆解建议执行顺序)
- [6. 验收标准](#6-验收标准)
- [7. 风险与对策](#7-风险与对策)
- [8. 变更记录](#8-变更记录)

## 1. 阶段目标
1) 后端从轻量 HttpServer 迁移到 Spring Boot（保留 `/api/...`）
2) 数据从内存仓库迁移到 PostgreSQL（Flyway 管 schema）
3) 引入安全基线：JWT Access + Refresh Cookie（rotation）+ CSRF
4) Nginx 作为 `api.<主域>` 入口：TLS 终止 + LB + 限流 + 上传限制
5) 前端用 React 重构现有功能，并在同一 SPA 中加入 `/admin` 管理路由
6) 同步更新 API 文档（`API.md` 与 `docs/api/api-contract.md`）

## 2. 范围与非目标
### 2.1 范围
- 登录/注册/账户信息维护/改密/注销
- 车辆 CRUD（车牌全局唯一）
- 黑名单（ADMIN）
- 车牌文字查询（不返回 PII）
- 图片查询（转发 OCR `/v1/detect-plate`，失败 502 + errorCode）
- Admin UI：车辆列表、黑名单开关、基础筛选

### 2.2 非目标（明确不做）
- Redis（Phase 2 再评估）
- `/api/v1` 版本化（稳定后再做）
- 复杂权限模型（Phase 1 仅 USER/ADMIN）
- 单点登录/第三方 OAuth

## 3. 里程碑与交付物
### M1：后端骨架
- Spring Boot 项目可启动
- `/api/health` 可用
- CORS allowlist + credentials 生效

### M2：数据库与基础实体
- Flyway V1：users/vehicles/refresh_sessions
- JPA 实体与 repository 可用
- BCrypt 密码存储

### M3：鉴权闭环
- `POST /api/auth/login` 签发 access + refresh cookie
- `POST /api/auth/refresh` rotation + 重放检测（撤销 sid）
- CSRF 双提交 cookie 基线跑通

### M4：核心业务接口迁移
- account/vehicles/query/query-image/blacklist 全部迁移到 Spring Boot
- 取消客户端传 username 鉴权模式

### M5：React 前端重构
- React SPA 覆盖现有用户端功能
- `/admin` 管理路由上线（同一 React 项目）
- 前端 access 仅存内存，刷新依赖 refresh cookie

### M6：Nginx LB 与部署基线
- `api.<主域>` Nginx upstream 到多实例
- `client_max_body_size`、forwarded headers、限流规则落地

## 4. 技术方案（已敲定）
- 域名：同主域不同子域（前后端分离）
- Auth：Access(Bearer+memory) + Refresh(cookie+rotation) + sessions(Postgres)
- Refresh cookie：SameSite=Lax；不设置 Domain；重放检测策略为撤销 sid 并要求重新登录
- CSRF：开启；Double Submit Cookie（`XSRF-TOKEN` + `X-CSRF-Token`）
- DB：PostgreSQL + Flyway；车牌全局唯一；phone 唯一；删除用户级联删除车辆
- Admin：Flyway seed；密码由 env 覆盖；RBAC 仅 USER/ADMIN；JPA
- OCR：`POST /v1/detect-plate`；失败返回 502 + errorCode；OCR client 统一封装（超时 + 有界重试）
- Redis：Phase 1 不引入

## 5. 任务拆解（建议执行顺序）
1) 新增 Spring Boot 模块（建议 `backend-spring/` 或迁移替换 `backend/`，二选一）
2) 引入 Flyway + PostgreSQL 配置（dev/test/prod 三套 profile）
3) 定义 JPA 实体：User、Vehicle、RefreshSession
4) 完成 Auth：login/refresh/logout/logout_all + rotation + 重放检测
5) 完成 CSRF：Double Submit Cookie + 前端 header 注入策略
6) 迁移 Vehicles/Account/Query/OCR endpoints
7) 替换 admin 逻辑：从“配置账号”迁移为 DB 角色
8) 建立 React 项目（Vite + React + TS），实现 API client（支持 credentials + bearer + csrf header）
9) 逐个页面迁移：Auth -> Vehicles -> Account -> Query -> Admin
10) Nginx 配置基线 + 多实例部署验证
11) 更新文档：`API.md`、`docs/api/api-contract.md`、`docs/ops/nginx.md`

## 6. 验收标准
- 功能：用户端与 admin 端核心功能可用（见 2.1）
- 安全：
  - access 不落 localStorage
  - refresh 为 HttpOnly cookie 且 rotation 生效
  - CSRF 对写接口生效
  - 不再使用“客户端传 username 作为身份依据”
- 稳定性：
  - Nginx LB 下多实例可用（无 sticky）
  - OCR 服务不可用时返回 502 + 明确 errorCode

## 7. 风险与对策
- 跨子域 cookie/cors：严格 allowlist + credentials；本地用 mkcert 做 HTTPS
- 并发 refresh：DB 原子更新/行锁，避免误判重放
- API 合同漂移：以 `docs/api/api-contract.md` 为准，变更必须记入 8

## 8. 变更记录
| 日期 | 变更 | 说明 |
| --- | --- | --- |
| 2026-01-22 | 建立 Phase 1 开发计划 | 依据 Decision Log 敲定方案 |
| 2026-01-22 | 创建 backend-spring 骨架 | Spring Boot 模块 + /api/health 可用 |
| 2026-01-22 | 增加 Flyway V1 迁移 | users/vehicles/refresh_sessions 初始化 |
| 2026-01-22 | 增加 JPA 实体与仓库 | User/Vehicle/RefreshSession 实体与 Repository |
| 2026-01-22 | 新增 Flyway V2 管理员种子 | admin 账号由环境变量 hash 覆盖 |
| 2026-01-22 | 完成 Auth 基础能力 | JWT + refresh rotation + CSRF/CORS 基线 |
| 2026-01-22 | 迁移账户与车辆接口 | account/vehicles/query/query-image 初版 |
| 2026-01-22 | 新增 OCR 客户端封装 | `/v1/detect-plate` 调用与 502 策略 |
| 2026-01-22 | React 迁移脚手架 | 新建 Vite React 项目 + /admin 路由占位 |
| 2026-01-22 | 补充 Nginx 示例配置 | LB/TLS/转发头配置示例 |
| 2026-01-22 | 增加后端单元测试 | Auth/Account/Vehicles/JWT 关键路径覆盖 |
| 2026-01-22 | 构建验证通过 | `backend-spring` Maven 测试+打包；`frontend-react` 构建通过 |
