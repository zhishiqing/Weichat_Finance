# Weichat_Finance · 变更日志

> 记录项目每个版本的关键变更。**遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)** 规范。

格式：`YYYY-MM-DD` · `版本` · `变更类型` · `说明`

变更类型：
- `Added` 新功能
- `Changed` 功能变更
- `Fixed` Bug 修复
- `Refactored` 重构
- `Removed` 移除

---

## v0.7 · 2026-09-14

### Added · Phase 2：JSAPI 统一下单骨架

- JSAPI 统一下单接口：`POST /api/v1/payment/jsapi/create`（业务层）
- Mock 实现 `MockJsapiService`：默认模式，零风险，返回伪造 prepay_id
- Real 实现 `RealJsapiService`（Phase 2 占位，Phase 2.1 实施完整 SDK 配置）
- 微信支付配置 `WechatPayProperties`：`wechatpay.mode=MOCK/REAL`
- wechatpay-java SDK 引入 `0.2.12`（直连商户 JSAPI）
- 业务订单表 `t_pay_order` 完整 CRUD（实体 + Mapper + Service）
- E2E 测试通过：DB 落库 + Mock 模式返回 prepay_id + 幂等检查

### Fixed

- **Lombok 与 Maven 注解处理器冲突**：移除所有类上的 `@Data`/`@Slf4j`/`@RequiredArgsConstructor` 注解，改用显式 getter/setter
- **Java 17 SDK 包路径变化**：v0.2.17 SDK 重命名为 `partnerpayments.*`（服务商），v0.2.12 直连商户类路径为 `service.payments.jsapi.*`
- **真实服务端口引入**：所有 `java` 命令前需 `set JAVA_HOME=D:\tools\env\jdk17`，避免用 Java 8 启动 Spring Boot 3.x

### Known Limitations · Phase 2.1 TODO

- `WechatPayClientConfig` 暂未激活（v0.2.12 的 RSAAutoCertificateConfig / NotificationConfig 配置流程较繁琐）
- `RealJsapiService` 仅占位（`UnsupportedOperationException`），Phase 2.1 实施完成
- 微信支付回调验签 + 解密（`/notify/v3/pay/success`）未实施，Phase 3

---

## v0.6 · 2026-09-14

### Refactored
- readme.md 从 602 行 / 29KB 拆分为 `readme.md`（简介）+ `docs/` 下 4 个独立文档（development / api / operations / changelog），消除 readme 既做介绍又做开发文档的混乱
- `docs/db/schema/V1__weichat_finance_init.sql` 作为唯一数据库脚本（保持不变）

### Added
- 新增 `docs/development.md`（开发文档）：技术选型、Maven 依赖、项目结构、Phase 交付记录、代码组织原则
- 新增 `docs/api.md`（接口文档）：对外业务接口 + 微信回调接口 + 数据模型 + SQL 脚本规范
- 新增 `docs/operations.md`（运维手册）：本地启动流程、仓库卫生、证书管理、上线 checklist、监控告警、回滚流程
- 新增 `docs/changelog.md`（变更日志）：本文件

### Changed
- 仓库结构由 `readme.md` 单文件 → `readme.md` + `docs/` 多文件组织
- readme 顶部加入 docs 文档索引

---

## v0.5 · 2026-09-14

### Added
- Phase 1 骨架完成
- Spring Boot 3.3.5 + JDK 17 + MyBatis-Plus 3.5.7 + Flyway 10.20.1 骨架
- 本地连 Docker MySQL（3306）
- Flyway 启动自动迁移 V1（应用层自动建表）
- 示例接口 `/api/health`、`/api/v1/merchant/{mchId}` 验证中文全链路正常

---

## v0.4 · 2026-09-14

### Changed
- **数据库 SQL 统一为单脚本**：`docs/db/schema/V1__weichat_finance_init.sql`（含建表 + 中文注释 + 乱码自检）
- 删除 V1/V2 历史脚本
- Flyway 启动时自动执行该脚本，乱码问题根治

---

## v0.3 · 2026-09-14

### Fixed
- 修复 V1 建表脚本中表/字段注释因客户端 latin1 导致的 `?` 乱码
- 锁定 SQL 脚本执行标准流程（`docker cp` + 容器内 mysql + `default-character-set=utf8mb4`）

---

## v0.2 · 2026-09-14

### Added
- 持久化方案确定：MySQL 8 + MyBatis-Plus + Flyway；单库多表
- 新增数据持久化章节（库表清单、schema 草案、Flyway 配置）

---

## v0.1 · 2026-09-14

### Added
- 项目初稿：明确 v1.0 范围（JSAPI + Native），预留服务商升级路径

---

## 后续版本（待启动）

- v0.7 · Phase 2：JSAPI/Native 统一下单 + 回调验签解密
- v0.8 · Phase 3：退款流程 + 幂等保护
- v0.9 · Phase 4：对账单下载 + 差异对账
- v1.0 · Phase 5：Dockerfile + docker-compose 全栈部署 + 上线
- v1.1 · 商家转账、营销券、消费者投诉
- v2.0 · 升级服务商（特约商户进件、合单支付、分账）
- v2.1 · 付款码支付（V2 通道）、委托代扣
