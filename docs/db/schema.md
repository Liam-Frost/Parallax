# 数据库与表结构（PostgreSQL + Flyway）

## 索引
- [1. 设计原则](#1-设计原则)
- [2. 命名与字段规范](#2-命名与字段规范)
- [3. DDL（建议初版）](#3-ddl建议初版)
- [4. 约束与索引](#4-约束与索引)
- [5. 数据迁移与种子数据](#5-数据迁移与种子数据)

## 1. 设计原则
- 以 DB 为真相来源（SSOT）
- 车牌全局唯一
- 删除用户级联删除车辆与会话
- Refresh token 仅存 hash
- 所有时间字段使用 `timestamptz`

## 2. 命名与字段规范
- 用户主键：`users.id` 为 UUID（不使用 email 作为主键）
- email：统一小写保存
- phone：拆为 `phone_country` 与 `phone_digits`，digits 仅保存数字
- license_number：统一大写保存

## 3. DDL（建议初版）
以下为建议的 Flyway V1 版本 DDL（可按 JPA 实体微调，但约束保持一致）：

```sql
-- users
create table if not exists users (
  id uuid primary key,
  email text not null,
  password_hash text not null,
  display_name text,
  first_name text,
  last_name text,
  country text,
  birth_year int,
  birth_month int,
  birth_day int,
  phone_country text,
  phone_digits text,
  contact_method text,
  role text not null default 'USER',
  status text not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists ux_users_email on users ((lower(email)));

-- phone 唯一（允许 phone 为空）
create unique index if not exists ux_users_phone
  on users (phone_country, phone_digits)
  where phone_country is not null and phone_digits is not null;

-- vehicles
create table if not exists vehicles (
  id uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  license_number text not null,
  make text not null,
  model text not null,
  year int,
  blacklisted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists ux_vehicles_license on vehicles ((upper(license_number)));
create index if not exists ix_vehicles_user on vehicles (user_id);

-- refresh sessions
create table if not exists refresh_sessions (
  sid uuid primary key,
  user_id uuid not null references users(id) on delete cascade,
  refresh_token_hash bytea not null,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  last_used_at timestamptz,
  revoked_at timestamptz,
  revoked_reason text,
  replaced_by_sid uuid,
  ip_last inet,
  ua_last text
);

create unique index if not exists ux_refresh_sessions_token_hash on refresh_sessions (refresh_token_hash);
create index if not exists ix_refresh_sessions_user on refresh_sessions (user_id);
create index if not exists ix_refresh_sessions_expires on refresh_sessions (expires_at);
```

说明：
- `refresh_token_hash` 使用 `bytea` 存储 SHA-256 原始 bytes；若实现更偏好 hex 字符串，也可改为 `char(64)`。
- `role/status` 为未来 admin/封禁预留。

## 4. 约束与索引
- email：唯一（大小写不敏感）
- phone：唯一（允许空）
- license_number：唯一（大小写不敏感）
- refresh_token_hash：唯一

## 5. 数据迁移与种子数据
- Flyway：
  - V1：建表 + 索引
  - V2：插入管理员用户（email 固定，密码 hash 由部署时覆盖或通过启动脚本更新）
