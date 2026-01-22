# Nginx 与部署/LB 约定

## 索引
- [1. 目标](#1-目标)
- [2. API 入口与负载均衡](#2-api-入口与负载均衡)
- [3. 关键 Nginx 参数](#3-关键-nginx-参数)
- [4. 头透传与真实客户端 IP](#4-头透传与真实客户端-ip)
- [5. 限流建议（Phase 1）](#5-限流建议phase-1)
- [6. 开发环境 HTTPS](#6-开发环境-https)

## 1. 目标
- `api.<主域>`：TLS 终止 + upstream LB 到多个 Spring Boot 实例
- 健康检查：`/api/health`
- 支持 OCR 图片上传（控制最大 body）

## 2. API 入口与负载均衡
- upstream：多个 `backend` 实例（容器或进程）
- 不使用 sticky；后端按设计无状态

示例配置：`docs/ops/nginx.example.conf`

## 3. 关键 Nginx 参数
- `client_max_body_size`：支持图片上传（按业务需求设置，例如 5m/10m）
- 超时：`proxy_read_timeout`、`proxy_connect_timeout`（与 OCR 调用链路匹配）

## 4. 头透传与真实客户端 IP
必须透传：
- `X-Forwarded-For`
- `X-Forwarded-Proto`

Spring Boot 侧需启用正确的 forwarded header 解析（避免生成错误的回跳 URL、错误识别 https）。

## 5. 限流建议（Phase 1）
优先在 Nginx 层对以下接口限流：
- `POST /api/auth/login`
- `POST /api/auth/refresh`

## 6. 开发环境 HTTPS
- 使用 mkcert 或等价工具生成开发证书
- 目标：本地也能验证 `Secure` cookie 行为
