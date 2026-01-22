# 决策记录（Decision Log）

## 索引
- [DL-0001：Phase 1 目标与技术栈](#dl-0001phase-1-目标与技术栈)
- [DL-0002：域名与部署边界](#dl-0002域名与部署边界)
- [DL-0003：鉴权与会话（Access + Refresh）](#dl-0003鉴权与会话access--refresh)
- [DL-0004：CORS/CSRF/本地 HTTPS 开发](#dl-0004corscsrf本地-https-开发)
- [DL-0005：API 版本策略与兼容性](#dl-0005api-版本策略与兼容性)
- [DL-0006：数据模型约束](#dl-0006数据模型约束)
- [DL-0007：管理员初始化与 RBAC](#dl-0007管理员初始化与-rbac)
- [DL-0008：Nginx LB 约定](#dl-0008nginx-lb-约定)
- [DL-0009：OCR 服务契约与失败策略](#dl-0009ocr-服务契约与失败策略)
- [DL-0010：Redis（阶段性不引入）](#dl-0010redis阶段性不引入)
- [DL-0011：Admin 管理面板形态](#dl-0011admin-管理面板形态)

## DL-0001：Phase 1 目标与技术栈
- 状态：已确定
- 决策：后端迁移 Spring Boot；数据库使用 PostgreSQL；前端用 React 重构；Nginx 负责 TLS 终止与负载均衡。

## DL-0002：域名与部署边界
- 状态：已确定
- 决策：前后端分离，不同子域同主域。
- 约定：
  - 前端：`https://app.<主域>`（同一 React 项目包含用户端与 `/admin`）
  - 后端 API：`https://api.<主域>`
  - OCR 服务：作为独立服务（可内网），对外契约版本化为 `/v1/detect-plate`

## DL-0003：鉴权与会话（Access + Refresh）
- 状态：已确定
- 决策：
  - Access Token：JWT，前端仅内存保存，通过 `Authorization: Bearer` 传递
  - Refresh Token：opaque 随机串，HttpOnly Cookie 保存，服务端仅存 hash
  - Refresh Rotation：开启
  - 会话存储：PostgreSQL `refresh_sessions` 表
  - 重放检测：发现旧 refresh 被复用时，撤销该 sid 并要求重新登录（不默认全设备下线）
- SameSite：同站点子域用 `Lax`；跨站才必须 `None`
- Cookie Domain：不设置（host-only）

## DL-0004：CORS/CSRF/本地 HTTPS 开发
- 状态：已确定
- 决策：
  - 允许携带 cookie（refresh 依赖）
  - 做 CSRF 防护
  - 本地 HTTPS：使用 mkcert 或等价方案
  - CORS：仅放行明确的前端 Origin 白名单；`Allow-Credentials: true`；允许 `Authorization, Content-Type`（以及 CSRF header）

## DL-0005：API 版本策略与兼容性
- 状态：已确定
- 决策：
  - Phase 1 保持 `/api/...` 不变，稳定后再引入 `/api/v1/...`
  - 不再信任客户端传 `username`，后端从 JWT 解析用户身份
  - 同步更新 `API.md` 与实现（以 `docs/api/api-contract.md` 为准）

## DL-0006：数据模型约束
- 状态：已确定
- 决策：
  - 车牌全局唯一
  - phone 唯一约束（按国家码+号码归一化）
  - 删除用户级联删除车辆
  - Flyway 管理 schema
  - 密码使用 BCrypt 存储

## DL-0007：管理员初始化与 RBAC
- 状态：已确定
- 决策：
  - RBAC：`USER/ADMIN` 足够
  - 管理员初始化：Flyway 插入基础记录；密码由环境变量生成/覆盖（部署时注入）
  - ORM：JPA

## DL-0008：Nginx LB 约定
- 状态：已确定
- 决策：
  - API 域名入口由 Nginx 承担 TLS 终止与负载均衡
  - 健康检查：沿用 `/api/health`
  - OCR 图片上传：配置 `client_max_body_size`
  - 头透传：`X-Forwarded-For` / `X-Forwarded-Proto`，Spring Boot 正确识别
  - 后端无状态；不需要 sticky

## DL-0009：OCR 服务契约与失败策略
- 状态：已确定
- 决策：
  - OCR 服务接口版本化 `/v1/detect-plate`
  - 后端失败策略：返回 502 + 可读错误码
  - 后端统一封装 OCR client（超时 + 有界重试 + 失败降级）

## DL-0010：Redis（阶段性不引入）
- 状态：已确定
- 决策：Phase 1 不引入 Redis；限流先用 Nginx；缓存先用本地 Caffeine；Phase 2 再评估。

## DL-0011：Admin 管理面板形态
- 状态：已确定
- 决策：同一个 React 项目做 `/admin` 路由（不拆独立 SPA）。
