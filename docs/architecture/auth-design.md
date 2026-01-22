# 鉴权与会话设计（JWT Access + Refresh Cookie）

## 索引
- [1. 目标](#1-目标)
- [2. Token 体系](#2-token-体系)
- [3. Cookie 与跨子域约束](#3-cookie-与跨子域约束)
- [4. CSRF 策略（已决定要做）](#4-csrf-策略已决定要做)
- [5. Endpoint 合同](#5-endpoint-合同)
- [6. Refresh Rotation 与重放检测](#6-refresh-rotation-与重放检测)
- [7. RBAC 与管理员](#7-rbac-与管理员)
- [8. 审计与限流（Phase 1 基线）](#8-审计与限流phase-1-基线)

## 1. 目标
- 用户无感保持登录
- Access 泄露损失可控（短 TTL）
- 支持单设备强制下线、改密失效、封禁
- 默认降低 XSS 盗取 token 的收益
- 支持多会话（session-level）可追踪

## 2. Token 体系
### 2.1 Access Token（JWT）
- TTL：15 分钟
- 传递：`Authorization: Bearer <access>`（前端仅存内存，不落 localStorage）
- 建议 claims：`iss/aud/sub/exp/iat/nbf/sid/roles`
- 签名算法：RS256（带 `kid`，支持轮换）

### 2.2 Refresh Token（opaque）
- TTL：30 天
- 形态：高熵随机串（非 JWT）
- 服务端存储：仅存 hash（例如 SHA-256）
- Rotation：每次 refresh 都换新 refresh

## 3. Cookie 与跨子域约束
前后端为同主域不同子域（同 site）。Refresh token 使用 HttpOnly cookie：
- `HttpOnly; Secure; SameSite=Lax`
- `Path=/api/auth/refresh`
- Cookie `Domain`：不设置（host-only）

前端请求：
- refresh / logout：`credentials: "include"`
- 其余 API：同样可带 credentials（推荐统一开启），但身份最终以 `Authorization` 为准

## 4. CSRF 策略（已决定要做）
采用 Double Submit Cookie（Spring Security 常用 SPA 方案）：
- 服务端下发 `XSRF-TOKEN`（非 HttpOnly cookie）
- 前端对所有写操作（POST/PUT/PATCH/DELETE）添加 header：`X-CSRF-Token: <cookie-value>`
- 后端校验 cookie 与 header 一致

说明：即使 Access 走 Bearer，refresh 走 cookie，仍建议 CSRF 全局启用，形成统一安全基线。

## 5. Endpoint 合同
（Phase 1 保持 `/api/...`）

- `POST /api/auth/login`
  - 入参：identifier + password
  - 出参：access token（JSON body）
  - 副作用：设置 refresh cookie；写入 refresh_sessions

- `POST /api/auth/refresh`
  - 入参：refresh cookie
  - 出参：新的 access token（JSON body）
  - 副作用：rotation refresh cookie；更新/写入 refresh_sessions

- `POST /api/auth/logout`
  - 入参：access（Bearer）
  - 副作用：撤销当前 sid；清理 refresh cookie（cookie Path 限制为 `/api/auth/refresh`）

- `POST /api/auth/logout_all`
  - 入参：access（Bearer）
  - 副作用：撤销该 user 下所有 sessions；清理 refresh cookie

## 6. Refresh Rotation 与重放检测
- Rotation：refresh 成功后生成新 refresh，旧 refresh 立即失效
- 重放检测：若收到“已被替换/已撤销”的 refresh
  - Phase 1 策略：撤销该 sid 并要求重新登录（不默认全设备下线）
  - 需要处理并发刷新：refresh endpoint 需具备幂等/互斥（通过 DB 行锁或条件更新实现）

## 7. RBAC 与管理员
- 角色：`USER` / `ADMIN`
- 资源鉴权：
  - 普通接口从 JWT 的 `sub` 取得当前用户
  - 管理接口要求 `ROLE_ADMIN`
- 管理员初始化：Flyway seed 插入，密码由环境变量覆盖

## 8. 审计与限流（Phase 1 基线）
- 审计：登录、刷新、登出、刷新失败、疑似重放事件需要记录
- 限流：优先由 Nginx 对 `/api/auth/login`、`/api/auth/refresh` 做限流（IP + burst）
