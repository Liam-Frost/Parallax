# API 合同与变更记录

## 索引
- [1. 合同原则](#1-合同原则)
- [2. 现状与差异说明](#2-现状与差异说明)
- [3. Phase 1 目标合同（/api 保持）](#3-phase-1-目标合同api-保持)
- [4. 鉴权与通用 Header](#4-鉴权与通用-header)
- [5. 错误格式](#5-错误格式)
- [6. 变更记录](#6-变更记录)

## 1. 合同原则
- Phase 1：保持 `/api/...` 路径稳定
- 不再信任客户端传 `username`（从 JWT `sub` 获取当前用户）
- 管理员接口由 RBAC 控制

## 2. 现状与差异说明
当前仓库包含：
- `API.md`：历史/演示文档
- Java handler 实现：实际已存在部分 method/path 差异

以本文件为准，Phase 1 会同步修订 `API.md` 并以 Spring Boot 实现最终合同。

## 3. Phase 1 目标合同（/api 保持）
### 3.1 Auth
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/logout_all`

### 3.2 Account
- `GET /api/account/me`
- `POST /api/account/contact`
- `POST /api/account/password`
- `DELETE /api/account`

### 3.3 Vehicles
- `GET /api/vehicles`（USER：返回自己的；ADMIN：返回全量含 owner 信息）
- `POST /api/vehicles`
- `DELETE /api/vehicles`
- `POST /api/vehicles/blacklist`（ADMIN）
- `GET /api/vehicles/query?license=...`（公开查询，不返回 PII）
- `POST /api/vehicles/query-image`

## 4. 鉴权与通用 Header
- Access：`Authorization: Bearer <jwt>`
- CSRF：写操作需 `X-CSRF-Token: <value>`
- refresh 请求需 `credentials: include`

## 5. 错误格式
建议统一：
```json
{
  "success": false,
  "errorCode": "SOME_CODE",
  "message": "Human readable message"
}
```

## 6. 变更记录
| 日期 | 变更 | 说明 |
| --- | --- | --- |
| 2026-01-22 | 建立 Phase 1 目标合同 | 以本文件为准推进 Spring Boot/React 迁移 |
