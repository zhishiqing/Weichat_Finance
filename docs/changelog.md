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

## v1.2 · 2026-09-14

### Added · 定时查单兜底（解决回调丢失/失败导致的订单悬挂）

**问题场景**：
- 微信支付回调可能因网络/系统问题丢失或失败
- 历史上发生过订单已支付但 t_pay_order.status 永远停留在 CREATED，导致用户看不到支付成功
- 主动查单兜底是微信官方推荐的兜底方案（每 30 分钟调用一次 `GET /v3/pay/transactions/out-trade-no/{out_trade_no}`）

**实现**：
- 主程序入口加 `@EnableScheduling`
- 新增 `PayOrderQueryScheduler`（`com.weichat.finance.job`），默认每 30 分钟执行一次
- 新增 Flyway V2 迁移脚本：
  - `t_pay_order.last_query_time`：最近一次查单时间（避免同批次重复查）
  - `t_pay_order_query_log`：每次批次的执行日志（笔数/耗时/异常统计）
- 新增 `PayOrderQueryLog` 实体 + Mapper + Service
- `PayOrderService.listHangingOrders()`：扫描 status IN (CREATED, SUBMITTING) 且 last_query_time 已过期/为空的订单
- `PayTransactionService.updateByOutTradeNo()`：按 out_trade_no 更新交易流水
- `application.yml` 加 `scheduler.pay-order-query.interval-ms / initial-delay-ms` 配置项

**执行流程**：
1. 扫描 t_pay_order 中悬挂的未支付订单
2. 调用 JsapiService.queryByOutTradeNo() 查微信侧状态
3. 状态变化时同步更新 t_pay_order + t_pay_transaction
4. 无论状态是否变化，都更新 last_query_time（防止本批次重复查）
5. 记录执行日志到 t_pay_order_query_log

**健壮性**：
- 指数退避重试：查单失败 1s → 2s → 4s 最多 3 次
- 分批处理：每批 100 条，避免长时间锁表
- MOCK 模式：只打日志 + 写日志表，不调用微信，便于本地验证

**配置项**（生产 30 分钟 / 本地验证 10 秒）：

```yaml
scheduler:
  pay-order-query:
    interval-ms: 1800000        # 30 分钟
    initial-delay-ms: 60000     # 60 秒
```

### Changed

- `PayOrder` 实体增加 `lastQueryTime` 字段
- `PayOrderService` 接口增加 `listHangingOrders(...)` 方法

### 验证

- Flyway V2 自动迁移成功（V2 记录已写入 flyway_schema_history）
- MOCK 模式调度任务运行：扫描到 4 笔历史 CREATED 订单，正确写入 t_pay_order_query_log
- 编译通过：65 个 .java
- E2E 全 PASS：健康检查 + 商户配置

---

## v1.1 · 2026-09-14

### Added · 集成 Knife4j 4.5

- 引入 `knife4j-openapi3-jakarta-spring-boot-starter`（Spring Boot 3 专用）
- 访问路径：
  - Knife4j UI：http://localhost:8080/api/doc.html
  - OpenAPI JSON：http://localhost:8080/api/v3/api-docs
  - 原生 Swagger UI：http://localhost:8080/api/swagger-ui.html
- 自动生成 5 个接口分组：JSAPI 支付 / Native 支付 / 退款管理 / 商户配置 / 健康检查
- 中文界面 + 多版本切换 + 实体类 Model 列表
- **生产环境**：通过 `knife4j.enable: false` 关闭 UI

### Changed

- 5 个 Controller 添加 `@Tag` / `@Operation` / `@Parameter` 注解

### 验证

- 编译通过：62 个文件
- OpenAPI JSON 含 10 个接口 + 完整 Schema
- E2E 全 PASS：JSAPI 下单、健康检查

---

## v1.0 · 2026-09-14

### Added · 实体字段文档化 + 枚举常量

- **6 个实体每个字段添加 javadoc 说明**，含枚举值引用（`@see OrderStatus` 等）
- **新增 9 个枚举常量类**（位于 `entity/enums/`）：
  - `OrderStatus`（包含状态机图）
  - `ProductType`
  - `PayStatus`
  - `RefundStatus`（包含状态机图）
  - `NotifyType`
  - `NotifyResult`
  - `IdempotentOperation`
  - `IdempotentResult`
  - `MerchantMode`
  - `CommonFlag`（启用/禁用、逻辑删除）
- 所有 Controller / Service 中的硬编码字符串替换为枚举引用
- **好处**：IDE 自动补全、重命名安全、消除拼写错误、文档化业务规则

### 验证

- 编译通过：61 个文件
- E2E 全 PASS：JSAPI 下单、退款、回调落库
- 数据库枚举值正确落库（status=REFUNDING / product_type=JSAPI / pay_status=NOTPAY 等）

---

## v0.9 · 2026-09-14

### Refactored · Lombok 修复（实体回归 @Data）

- 移除所有实体类手写的 getter/setter（共 -190 行），统一用 `@Data`
- **根因**：之前使用 `<source>17` + `<target>17` 导致 Lombok 注解处理器在 Spring Boot 3 + JDK 17 下不生效
- **修复**：`pom.xml` 改用 `<release>17`（Spring Boot 3 官方推荐）+ 显式 `annotationProcessorPaths`
- 受影响：6 个实体类（MerchantConfig / PayOrder / PayTransaction / PayRefund / PayNotifyLog / PayIdempotent）
- **验证**：编译通过 + E2E（健康检查 / JSAPI / 退款）全 PASS

---

## v0.8 · 2026-09-14

### Added · Phase 2.1 + Phase 2.2 + Phase 3 + Phase 3.4

- **JSAPI 查单接口**：`GET /api/v1/payment/order/{outTradeNo}`
- **JSAPI 关单接口**：`POST /api/v1/payment/order/{outTradeNo}/close`
- **Native 支付下单**：`POST /api/v1/payment/native/create`，返回 code_url（Mock 默认）
- **退款申请**：`POST /api/v1/payment/refund/create`，Mock + Real 双模式
- **退款查询**：`GET /api/v1/payment/refund/{outRefundNo}`
- **微信支付回调**：`POST /api/notify/v3/pay/success` 与 `/api/notify/v3/refund/success`
  - 落库 t_pay_notify_log（原始报文 + 请求头）
  - **SIGNTEST 探测流量正确处理**（返回 200，不进入业务）
- **新增实体 / Mapper / Service**：t_pay_transaction / t_pay_refund / t_pay_notify_log / t_pay_idempotent
- **接入质量评估**：按 Skill 通用清单扫描当前代码，发现并修复以下 🔴 致命问题

### Fixed · 接入质量评估阶段

- 🔴 致命：退款金额上限校验缺失 + 累计已退金额未校验 → 已修复（`RefundController#createRefund`）
- 🔴 致命：SIGNTEST 探测流量未识别 → 已修复（`WechatNotifyController#isSignTestTraffic`）
- 🟠 建议：JSAPI/Native 下单"金额字段以后端为准"暂为 TODO Phase 4（当前业务订单=支付订单）
- 退款请求 DTO 的 `@JsonProperty` 误用 getter，导致 `amountRefund` 解析失败 → 改为字段直接映射

### Known Limitations · 待真实私钥补齐

- 🔴 `WechatPayClientConfig` 暂未激活（v0.2.12 的 RSAAutoCertificateConfig / NotificationConfig）
- 🔴 `RealJsapiService` / `RealRefundService` / `RealNativeService` 仍是占位
- 🔴 微信回调验签 + 解密（`NotificationParser`）未激活，目前回调仅落库
- 🟡 主动查询兜底（定时任务查 NOTPAY 订单）未实施

### 验证情况（Mock 模式 E2E）

| 场景 | 结果 |
| --- | --- |
| 健康检查 `/api/health` | ✅ PASS |
| JSAPI 下单 | ✅ PASS（返回 MOCK_prepay_xxx） |
| JSAPI 重复下单 | ✅ 409 Conflict（幂等拦截） |
| JSAPI 查单 | ✅ PASS |
| JSAPI 关单 | ✅ PASS（状态→CLOSED） |
| Native 下单 | ✅ PASS（返回 code_url） |
| 退款申请 | ✅ PASS（落库 + 更新订单状态→REFUNDING） |
| 退款超额（99999/100） | ✅ 400 + 拦截（致命修复已生效） |
| 退款查询 | ✅ PASS |
| 微信 SIGNTEST 探测流量 | ✅ 直接返回 200，不进入业务 |
| 微信正常回调 | ✅ 落库 t_pay_notify_log，含 headers + body |

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

- v0.9 · Phase 2.x：激活 RealJsapiService/RealRefundService/RealNativeService + NotificationParser（真实私钥拿到后）
- v1.0 · Phase 4：业务订单表 t_business_order + 金额以后端为准
- v1.1 · Phase 5：对账单下载 + 差异对账 + Dockerfile 全栈部署
- v1.2 · 商家转账、营销券、消费者投诉
- v2.0 · 升级服务商（特约商户进件、合单支付、分账）
- v2.1 · 付款码支付（V2 通道）、委托代扣
