# Parallax 开发文档

## 索引
- [01. 当前阶段开发计划（Phase 1）](plan/phase-1-migration.md)
- [02. 系统架构](architecture/system-architecture.md)
- [03. 鉴权与会话设计](architecture/auth-design.md)
- [04. 数据库与表结构（PostgreSQL + Flyway）](db/schema.md)
- [05. API 合同与变更记录](api/api-contract.md)
- [06. Nginx 与部署/LB 约定](ops/nginx.md)
- [07. 决策记录（ADR/Decision Log）](decisions/decision-log.md)

## 文档维护约定
- 本目录是“开发文档单一事实来源（SSOT）”。涉及架构/接口/部署/数据库的变更，必须同步更新对应文档。
- 每次做重大技术选择、约束变更、接口破坏性变更：追加一条到 `docs/decisions/decision-log.md`。
- 每个阶段（Phase）以 `docs/plan/phase-*.md` 维护，阶段完成后在该文件末尾标记“完成标准达成情况”。

## 快速命令
- 后端单元测试：`cd backend-spring && mvn test`
- 前端构建：`cd frontend-react && npm run build`
