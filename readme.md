# Weichat_Finance

> 微信支付（APIv3）财务对接系统 · Spring Boot + MySQL + Flyway

---

## 项目简介

为自有业务系统打通微信支付能力，并预留升级为**服务商**的扩展空间。

## 项目目标

| 阶段 | 商户类型 | 目标能力 |
|---|---|---|
| **v1.0（当前）** | 直连商户 | JSAPI 支付、Native 支付、退款 / 退款查询、对账文件下载 |
| v1.1 | 直连商户 | 商家转账到零钱、营销券、消费者投诉 |
| v2.0 | **服务商** | 特约商户进件、分账、合单支付、二级商户资金管理 |
| v2.1 | 服务商 | 付款码支付（V2 通道，线下被扫）、委托代扣 |

付款码支付在 v1.0 暂不接入（仅 V2 通道），v2.1 升级服务商时统一接入。

## 技术栈

| 维度 | 选择 |
|---|---|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.3.5 |
| 持久化 | MySQL 8.0+ + MyBatis-Plus 3.5.7 |
| 迁移 | Flyway 10.20.1 |
| HTTP | OkHttp 4（计划） |
| 加密 | BouncyCastle（计划） |

## 当前进度

**v0.8 · Phase 2.1 + 2.2 + 3 + 接入质量评估已完成**

- [x] Spring Boot 骨架（51 个 .java）
- [x] Flyway 自动迁移 V1（8 张表）
- [x] JSAPI 全套：下单 / 查单 / 关单
- [x] Native 下单
- [x] 退款申请 / 退款查询（含金额上限 + 累计校验）
- [x] 微信回调落库（headers + raw_body）+ SIGNTEST 探测识别
- [x] 幂等保护 t_pay_idempotent 基础设施就绪
- [x] Mock 模式 E2E 全打通（10 个接口全部 PASS）
- [x] 接入质量评估：按 Skill 通用清单扫一遍，🔴 致命问题全部修复

下一阶段：**Phase 2.x · 真实私钥补齐后激活 RealJsapiService/RealRefundService/RealNativeService 与 NotificationParser**

## 接口速查

### 业务接口

| 方法 | 路径 | 用途 | 状态 |
|---|---|---|---|
| `GET` | `/api/health` | 健康检查（含 DB 状态） | ✅ Phase 1 |
| `GET` | `/api/v1/merchant/{mchId}` | 查询商户配置 | ✅ Phase 1 |
| `POST` | `/api/v1/payment/jsapi/create` | 创建 JSAPI 支付订单 | ✅ Phase 2（Mock）/ ⏳ Real |
| `GET` | `/api/v1/payment/order/{outTradeNo}` | 查询支付订单 | ✅ Phase 2.1 |
| `POST` | `/api/v1/payment/order/{outTradeNo}/close` | 关单 | ✅ Phase 2.1 |
| `POST` | `/api/v1/payment/native/create` | 创建 Native 支付订单 | ✅ Phase 2.2（Mock） |
| `POST` | `/api/v1/payment/refund/create` | 申请退款 | ✅ Phase 3（Mock） |
| `GET` | `/api/v1/payment/refund/{outRefundNo}` | 查询退款 | ✅ Phase 3 |

### 微信回调

| 路径 | 用途 | 状态 |
|---|---|---|
| `POST` `/api/notify/v3/pay/success` | 支付成功通知 | ✅ 落库 ⏳ 验签 |
| `POST` `/api/notify/v3/refund/success` | 退款成功通知 | ✅ 落库 ⏳ 验签 |
| `/notify/v3/refund/success` | 退款结果通知 |

完整接口清单见 [docs/api.md](docs/api.md)。

## 项目结构

```
Weichat_Finance/
├── readme.md                  ← 项目简介（本文件）
├── .gitignore
├── backend/                   ← Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
└── docs/                      ← 项目文档
    ├── development.md         ← 开发文档（技术选型、架构、依赖、Phase 交付）
    ├── api.md                 ← 接口文档（HTTP API + 数据模型 + SQL 规范）
    ├── operations.md          ← 运维手册（启动、证书、上线 checklist）
    ├── changelog.md           ← 变更日志
    └── db/schema/V1__weichat_finance_init.sql  ← 数据库唯一脚本
```

## 文档导航

| 我想了解... | 看哪个文档 |
|---|---|
| 这个项目是什么、做什么、做到哪了 | 👉 [readme.md](readme.md)（本文件） |
| 技术选型、代码结构、Maven 依赖、Phase 交付记录 | 👉 [docs/development.md](docs/development.md) |
| 接口怎么调、数据怎么存、SQL 脚本怎么跑 | 👉 [docs/api.md](docs/api.md) |
| 怎么启动、怎么部署、怎么上线 | 👉 [docs/operations.md](docs/operations.md) |
| 每个版本改了什么 | 👉 [docs/changelog.md](docs/changelog.md) |

## 快速开始

```bash
# 1. 启动 Docker MySQL
docker start weichat-finance-mysql

# 2. 启动 Spring Boot（Flyway 会自动建表）
cd backend
mvn spring-boot:run

# 3. 验证
curl http://localhost:8080/api/health
```

完整步骤见 [docs/operations.md · 本地启动流程](docs/operations.md#一本地启动流程)。

## 安全声明

> 本项目处理**真实资金**，请严格遵守以下规则：

- ❌ **绝对不能**将商户私钥、API v3 密钥提交到 git 仓库
- ❌ **绝对不能**将 `backend/certs/` 目录下的任何文件上传到 GitHub
- ✅ 所有密钥通过环境变量注入
- ✅ 上线前必看 [docs/operations.md · 上线前 Checklist](docs/operations.md#五上线前-checklist)

## License

未指定（如需对外开源，建议 Apache 2.0）

## 变更记录

详见 [docs/changelog.md](docs/changelog.md)。

最新版本：**v0.6**（2026-09-14）

| 日期 | 版本 | 摘要 |
|---|---|---|
| 2026-09-14 | v0.7 | **Phase 2：JSAPI 统一下单骨架**：Mock 模式 E2E 打通，引入 wechatpay-java SDK 0.2.12 |
| 2026-09-14 | v0.6 | 仓库卫生：清理 target/ + 新增 .gitignore；readme 拆分为 readme + 4 个 docs |
| 2026-09-14 | v0.5 | Phase 1 骨架：Spring Boot + MyBatis-Plus + Flyway |
| 2026-09-14 | v0.4 | 数据库 SQL 统一为单脚本 V1 |
| 2026-09-14 | v0.3 | 修复 SQL 中文乱码 |
| 2026-09-14 | v0.2 | 持久化方案确定 |
| 2026-09-14 | v0.1 | 项目初稿 |
