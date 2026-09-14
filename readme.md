# Weichat_Finance · 微信支付接入项目

> 本项目使用 [wechatpay-payment-integration Skill](C:\Users\Administrator\.cursor\skills\wechatpay-payment-integration\SKILL.md) 作为唯一接入指引，所有接口、字段、示例代码均以 Skill 加载的官方文档为准。

---

## 一、项目目标

为自有业务系统打通微信支付能力，并预留升级为**服务商**的扩展空间，按阶段交付：

| 阶段 | 商户类型 | 目标能力 |
|---|---|---|
| **v1.0（当前）** | 直连商户 | JSAPI 支付、Native 支付、退款 / 退款查询、对账文件下载 |
| v1.1 | 直连商户 | 商家转账到零钱、营销券、消费者投诉 |
| v2.0 | **服务商** | 特约商户进件、分账、合单支付、二级商户资金管理 |
| v2.1 | 服务商 | 付款码支付（V2 通道，线下被扫）、委托代扣 |

> 付款码支付在 v1.0 暂不接入（仅 V2 通道），v2.1 升级服务商时统一接入。

---

## 二、技术选型

| 维度 | 选择 | 理由 |
|---|---|---|
| 语言 | **Java 17**（本机已装 1.8，启动时切换） | 与微信支付官方文档示例语言一致，便于对照 |
| 框架 | **Spring Boot 3.x** | 生态成熟，对 HTTP 客户端、验签、加签、回调处理支持完善 |
| HTTP 客户端 | **OkHttp 4** | 微信支付 V3 官方 Java SDK 推荐使用 |
| JSON | **Jackson** | Spring Boot 默认 |
| 加密 / 签名 | BouncyCastle（PKCS#12 证书处理）+ 自实现 V3 签名 | 见下方"签名机制" |
| 缓存 | Caffeine（本地）+ MySQL 持久化（v1.0）→ Redis（v2.0 服务商期引入） | 商户配置、AccessToken、平台证书走 DB |
| 持久化 | **MySQL 8.0+** + **MyBatis-Plus 3.5.x** | 单库多表；MyBatis-Plus 免写大量 SQL |
| 连接池 | HikariCP（Spring Boot 默认） | 高性能 |
| 迁移 | Flyway（v1.0 引入） | 版本化管理 schema，团队协作友好 |
| 日志 | Logback + MDC（traceId 串联） | 排查问题必需 |
| 配置中心 | `application.yml`（v1.0）→ Nacos（v2.0） | 跟随业务演进 |
| 构建 | Maven | 与多数团队习惯一致 |

### 关键依赖（Maven 草案）

```xml
<!-- 微信支付官方 Java SDK（V3） -->
<dependency>
    <groupId>com.github.wechatpay-apiv3</groupId>
    <artifactId>wechatpay-java</artifactId>
    <version>最新稳定版</version>
</dependency>

<!-- HTTP -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<!-- 加解密 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78</version>
</dependency>

<!-- 持久化 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.7</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>10.x</version>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <version>10.x</version>
</dependency>

<!-- Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

> ⚠️ 实际版本号在生成骨架时以 Maven Central 最新稳定版为准。

---

## 三、签名与安全机制（要点速查）

### 3.1 V3 通道（v1.0 全部使用）

- **商户请求**：使用商户 API 证书（apiclient_cert.p12 / apiclient_cert.pem + apiclient_key.pem），用 SHA256-with-RSA 对方法 + URL + body + timestamp + nonce 签名，请求头 `Authorization: WECHATPAY2-SHA256-RSA2048 ...`
- **回调验签**：使用微信支付**平台证书**（平台公钥），验签 `Wechatpay-Signature` 头，**必须**校验 `Wechatpay-Timestamp`、`Wechatpay-Nonce` 防重放
- **敏感信息加密**：回调中的 `resource` 字段是用平台证书对应的公钥加密的 AES-256-GCM，需用本地存储的平台私钥解密

### 3.2 V2 通道（v2.1 付款码支付才用）

- MD5 / HMAC-SHA256 签名，签名串按字段拼接规则
- 回调用商户密钥校验，无证书概念

> 详细字段、示例代码以 Skill 同步下来的官方文档为准。

---

## 四、目录结构（草案，落地前再确认）

```
Weichat_Finance/
├── readme.md                          ← 本文件
├── pom.xml                            ← Maven 配置
├── docs/
│   ├── api/                           ← 接口文档（自动生成 + 手动补充）
│   ├── operations/                    ← 运维手册：证书申请、回调配置、上线 checklist
│   └── changelog.md                   ← 版本变更日志
├── weichat-finance-application/        ← 启动模块（Spring Boot 入口）
│   └── src/main/java/com/yourorg/finance/Application.java
├── weichat-finance-domain/             ← 领域模型：订单、退款单、对账记录
│   └── src/main/java/com/yourorg/finance/domain/
├── weichat-finance-infrastructure/     ← 持久化（v1.0 用 MyBatis-Plus + MySQL 8）
│   └── src/main/
│       ├── java/com/yourorg/finance/infra/
│       │   ├── config/                  ← MyBatis-Plus 配置（分页插件、MetaObjectHandler）
│       │   └── repository/              ← Mapper 接口（每个领域实体一个 Mapper）
│       └── resources/db/migration/      ← Flyway 迁移脚本
│           ├── V1__init_schema.sql      ← 初始化建表
│           ├── V2__seed_merchant_config.sql ← 商户配置种子数据
│           └── ...
├── weichat-finance-payment/            ← 支付核心模块 ⭐
│   ├── v3-channel/                     ← V3 通道（v1.0 全部能力）
│   │   ├── client/                     ← HTTP 客户端 + 签名
│   │   ├── cert/                       ← 商户证书 / 平台证书管理
│   │   ├── jsapi/                      ← JSAPI 支付
│   │   ├── native/                     ← Native 支付
│   │   ├── refund/                     ← 退款 / 退款查询
│   │   ├── notify/                     ← 回调验签 + 解密
│   │   └── config/                     ← 商户配置加载
│   ├── v2-channel/                     ← V2 通道（v2.1 付款码时启用）
│   └── provider/                       ← 多商户 / 服务商抽象
│       ├── MerchantProvider.java       ← 商户路由接口
│       ├── DirectMerchantProvider.java ← 直连商户实现
│       └── PartnerMerchantProvider.java← 服务商实现（v2.0）
└── weichat-finance-web/                ← 对外 HTTP 接口（REST + 回调）
```

### 模块边界原则

- **payment 模块**只依赖 `wechatpay-java` SDK + `okhttp`，不直接接触业务订单
- **domain 模块**定义业务订单 / 退款单实体，与支付状态解耦
- **web 模块**只做参数解析、权限校验、调用 payment 模块、返回结果

---

## 五、v1.0 接口清单

> 表中"对外"指业务系统调用的接口；"回调"指微信支付主动通知的接口；"内部"指本系统调用微信支付服务端接口。

### 5.1 业务接口（业务系统 → 本服务）

| 接口 | 方法 | 路径 | 用途 |
|---|---|---|---|
| 创建 JSAPI 订单 | POST | `/api/v1/payment/jsapi/create` | 返回 prepay_id，前端调起支付 |
| 创建 Native 订单 | POST | `/api/v1/payment/native/create` | 返回 code_url，前端生成二维码 |
| 查询订单 | GET | `/api/v1/payment/order/{out_trade_no}` | 主动查单（兜底轮询） |
| 申请退款 | POST | `/api/v1/payment/refund/create` | 同步发起退款 |
| 查询退款 | GET | `/api/v1/payment/refund/{out_refund_no}` | 查退款进度 |

### 5.2 微信回调（本服务 → 业务系统）

| 路径 | 触发时机 |
|---|---|
| `/notify/v3/pay/success` | 支付成功通知 |
| `/notify/v3/refund/success` | 退款结果通知 |

> 所有回调入口必须做：① 验签 ② 防重放（5 分钟内 timestamp + nonce） ③ 幂等处理（按 out_trade_no / out_refund_no 去重） ④ 立即返回 200/204，业务逻辑异步消费。

### 5.3 内部调用（本服务 → 微信支付）

直接使用 `wechatpay-java` SDK 调用，无需自行实现 HTTP 客户端。所有路径、参数、签名头由 SDK 处理。

---

## 六、数据持久化（MySQL 8 + MyBatis-Plus + Flyway）

### 6.1 设计原则

1. **单库多表**：库名 `weichat_finance`，v1.0 全部表都在该库下；v2.0 服务商期可拆 `weichat_finance_partner` 库，通过 datasource 路由隔离
2. **主键策略**：`BIGINT AUTO_INCREMENT` 做表内唯一主键（不对外暴露），业务单号（`out_trade_no` / `out_refund_no`）用 UUID v4 单独生成，加唯一索引
3. **必备字段**：每张业务表都有 `id`、`gmt_create`、`gmt_modified`、`is_deleted`（逻辑删除，MyBatis-Plus 支持），便于排查和恢复
4. **金额字段**：全部用 `BIGINT` 存**分**（避免浮点精度问题），严禁用 `DECIMAL` 存金额字段
5. **时间字段**：用 `DATETIME(3)`（毫秒精度）存业务时间，`TIMESTAMP` 仅用于审计字段
6. **JSON 字段**：MySQL 8 支持 JSON 类型，微信支付回调的扩展参数、原始报文用 `JSON` 列存
7. **迁移管理**：所有 DDL 通过 Flyway 脚本（`V1__xxx.sql`、`V2__xxx.sql`），**禁止应用启动时自动建表**

### 6.2 数据库连接配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/weichat_finance?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: WeichatFinanceHikariCP

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    table: flyway_schema_history

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.yourorg.finance.domain.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: AUTO                 # 主键自增
      logic-delete-field: is_deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      insert-strategy: NOT_NULL
      update-strategy: NOT_NULL
```

### 6.3 库表清单（v1.0）

| # | 表名 | 用途 | 关键索引 |
|---|---|---|---|
| 1 | `t_pay_order` | 支付订单（业务订单） | UK(`out_trade_no`)、IDX(`mch_id`, `status`)、IDX(`gmt_create`) |
| 2 | `t_pay_transaction` | 微信支付交易流水（一次支付 = 一条） | UK(`transaction_id`)、UK(`out_trade_no`)、IDX(`mch_id`, `pay_status`) |
| 3 | `t_pay_refund` | 退款单 | UK(`out_refund_no`)、IDX(`transaction_id`)、IDX(`mch_id`, `refund_status`) |
| 4 | `t_pay_notify_log` | 回调原始报文（调试 + 重放） | IDX(`out_trade_no`)、IDX(`gmt_create`) |
| 5 | `t_pay_idempotent` | 幂等记录（按 out_trade_no / out_refund_no） | UK(`idempotent_key`) |
| 6 | `t_merchant_config` | 商户配置（多商户预留，v1.0 1 条） | UK(`mch_id`) |
| 7 | `t_platform_cert` | 微信平台证书缓存 | UK(`serial_no`) |
| 8 | `t_pay_reconciliation` | 对账记录（v1.1 启用） | IDX(`bill_date`, `mch_id`) |

### 6.4 关键表 schema（草案）

#### `t_pay_order`（业务订单）

```sql
CREATE TABLE `t_pay_order` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `out_trade_no`    VARCHAR(64)  NOT NULL                COMMENT '商户订单号（业务侧生成，UUID）',
  `mch_id`          VARCHAR(32)  NOT NULL                COMMENT '商户号',
  `app_id`          VARCHAR(32)  NOT NULL                COMMENT '公众号/小程序 AppID',
  `description`     VARCHAR(128) NOT NULL                COMMENT '订单描述',
  `amount_total`    BIGINT       NOT NULL                COMMENT '订单金额（分）',
  `currency`        VARCHAR(8)   NOT NULL DEFAULT 'CNY'   COMMENT '货币类型',
  `openid`          VARCHAR(64)           DEFAULT NULL    COMMENT '用户标识（JSAPI 必填，Native 为空）',
  `product_type`    VARCHAR(16)  NOT NULL                COMMENT '支付产品：JSAPI / NATIVE',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'CREATED' COMMENT '订单状态：CREATED/SUBMITTING/SUCCESS/CLOSED/REFUNDING/REFUNDED',
  `time_expire`     DATETIME(3)           DEFAULT NULL    COMMENT '订单失效时间',
  `success_time`    DATETIME(3)           DEFAULT NULL    COMMENT '支付成功时间（微信回传）',
  `notify_url`      VARCHAR(512)          DEFAULT NULL    COMMENT '回调地址',
  `attach`          VARCHAR(128)          DEFAULT NULL    COMMENT '附加数据，原样回传',
  `ext`             JSON                  DEFAULT NULL    COMMENT '扩展参数',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_mch_status` (`mch_id`, `status`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务订单';
```

#### `t_pay_transaction`（微信交易流水）

```sql
CREATE TABLE `t_pay_transaction` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
  `out_trade_no`        VARCHAR(64)  NOT NULL                COMMENT '商户订单号',
  `transaction_id`      VARCHAR(64)           DEFAULT NULL    COMMENT '微信支付订单号',
  `mch_id`              VARCHAR(32)  NOT NULL,
  `pay_status`          VARCHAR(16)  NOT NULL DEFAULT 'NOTPAY' COMMENT 'NOTPAY/SUCCESS/CLOSED/REVOKED/REFUNDED',
  `amount_payer_total`  BIGINT       NOT NULL                COMMENT '用户实际支付金额（分，应收 - 优惠）',
  `bank_type`           VARCHAR(32)           DEFAULT NULL    COMMENT '付款银行',
  `success_time`        DATETIME(3)           DEFAULT NULL,
  `raw_response`        JSON                  DEFAULT NULL    COMMENT '下单接口原始响应',
  `gmt_create`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted`          TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_id` (`transaction_id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_mch_pay_status` (`mch_id`, `pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微信支付交易流水';
```

#### `t_pay_idempotent`（幂等记录）

```sql
CREATE TABLE `t_pay_idempotent` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `idempotent_key`  VARCHAR(128) NOT NULL                COMMENT '幂等键，如 out_trade_no:CREATE 或 out_refund_no:REFUND',
  `operation`       VARCHAR(32)  NOT NULL                COMMENT '操作类型：CREATE_ORDER / REFUND / NOTIFY',
  `result_code`     VARCHAR(16)  NOT NULL                COMMENT 'SUCCESS / FAILED',
  `result_body`     JSON                  DEFAULT NULL    COMMENT '操作结果摘要（用于重放）',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent_key` (`idempotent_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等记录';
```

### 6.5 持久层约束

- 所有 Mapper **继承** `BaseMapper<T>`，禁止裸写 SQL
- 复杂查询用 `@Select` 注解或写在 `src/main/resources/mapper/` 下的 XML
- 事务：服务层 `@Transactional(rollbackFor = Exception.class)`，**禁用** `noRollbackFor` 偷懒写法
- 任何 DB 写操作必须有日志（操作人 / IP / 前后值），v1.0 至少实现 `gmt_modified` 自动填充

### 6.6 SQL 脚本执行规范（防止中文乱码）

**文件位置（**唯一**）**：`docs/db/schema/V1__weichat_finance_init.sql`

**原则**：数据库只有一份脚本，就是真相。**不允许多版本（V1、V2、V3...）存在**——避免开发者不知道以哪份为准。建表语句 + 中文注释 + 乱码自检全部写在这一个文件里。

**根因**：PowerShell 管道默认用 GBK 解码 UTF-8 文件，docker MySQL 客户端默认 `character_set_client=latin1`，两段叠加导致中文存成 `?`（0x3F）。

**已修复**：

1. MySQL 客户端默认 utf8mb4（`/etc/mysql/conf.d/99-client-utf8mb4.cnf`）
2. 数据 `merchant_name` 已从 `??????` 修复为 `默认直连商户`
3. 8 张表的表注释 + 全部字段注释已全部恢复中文

**强制规范**（任何 SQL 脚本都必须遵守）：

```bash
# ✅ 标准流程（4 步走）
# 1. 编辑 SQL 文件（路径用 / 不用 \）
"D:/dev/panhw/Weichat_Finance/docs/db/schema/V1__weichat_finance_init.sql"

# 2. docker cp 到容器内（用前向斜杠路径，避免 \ 转义问题）
docker cp "D:/dev/panhw/Weichat_Finance/docs/db/schema/V1__weichat_finance_init.sql" \
           weichat-finance-mysql:/tmp/V1__weichat_finance_init.sql

# 3. 在容器内执行（绕开 PowerShell 管道编码）
docker exec weichat-finance-mysql \
    bash -c "mysql -uroot -proot --default-character-set=utf8mb4 weichat_finance < /tmp/V1__weichat_finance_init.sql"

# 4. 校验无乱码（必须返回 0 行）
docker exec weichat-finance-mysql mysql -uroot -proot --default-character-set=utf8mb4 -e \
    "SELECT COUNT(*) AS bad_columns FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA='weichat_finance' AND COLUMN_COMMENT LIKE '%?%';"
```

**禁止写法**（会再次造成乱码）：

- ❌ `Get-Content -Raw xxx.sql | docker exec -i ... mysql ...`（PowerShell 管道 GBK 解码）
- ❌ `docker exec -i ... mysql ... < xxx.sql`（PowerShell 不支持重定向输入）
- ❌ `mysql ... < /tmp/xxx.sql` 不加 `--default-character-set=utf8mb4`（客户端字符集兜底）
- ❌ **新建 V2、V3 等多版本 SQL 脚本**（杜绝多版本造成歧义；新需求直接改 V1）

**重置数据库**（需要彻底清库时）：

```bash
# Drop + 重建 + 重跑唯一脚本（必须执行第 4 步校验）
docker exec weichat-finance-mysql mysql -uroot -proot -e \
    "DROP DATABASE IF EXISTS weichat_finance;
     CREATE DATABASE weichat_finance CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
# 然后重新跑上面的 4 步流程
```

**Flyway 集成**：

- 在 `src/main/resources/db/migration/` 下软链接或复制 `V1__weichat_finance_init.sql`
- Spring Boot 启动时 Flyway 自动执行，应用 `spring.flyway.encoding=utf-8` 确保客户端字符集
- 数据库手动维护只在 docker 容器内做（重置/调试），不直接动 Flyway 历史

---

## 七、商户配置（application.yml 草案）

```yaml
wechatpay:
  # 直连商户模式（v1.0）
  mode: DIRECT                # DIRECT / PARTNER
  merchant:
    mch-id: ${WX_MCH_ID}                  # 商户号
    app-id: ${WX_APP_ID}                  # 公众号 / 小程序 AppID
    api-v3-key: ${WX_API_V3_KEY}          # V3 密钥（32 位，用于回调解密）
    cert:
      serial-no: ${WX_CERT_SERIAL_NO}     # 商户证书序列号
      private-key-path: certs/apiclient_key.pem
  # V2 密钥（v2.1 付款码时启用，v1.0 不必填）
  v2-key: ${WX_V2_KEY:}
  notify-url-base: https://your-domain.com
```

> 真实值通过环境变量注入，**禁止写入代码或 git 仓库**。商户配置默认从 `t_merchant_config` 表读，`application.yml` 仅做启动兜底。

---

## 八、证书管理流程

1. **商户 API 证书**：商户平台 → API 安全 → 申请 API 证书，下载得到 `apiclient_cert.pem`、`apiclient_key.pem`、`apiclient_cert.p12`
2. **平台证书**：用于回调验签 + 回调资源解密，**不能**静态写死 —— 需在启动时调用 `/v3/certificates` 接口拉取并缓存，启动时若缓存为空则同步拉取一次
3. **证书序列号**：每次 HTTP 请求头 `Wechatpay-Serial` 都需带上对应签名证书的序列号
4. **证书轮换**：监听回调头里的证书变更通知（`Wechatpay-Serial` 与本地不一致时），异步刷新本地缓存

> 详细字段、示例代码以 Skill 同步下来的官方文档为准。

---

## 九、开发路线图

### Phase 0 · 准备（已完成）

- [x] 安装 wechatpay-payment-integration Skill
- [x] 同步官方文档知识库
- [x] 确认技术栈：Java + Spring Boot
- [x] 确认支付能力：JSAPI + Native
- [x] 写 readme.md（本文件）

### Phase 1 · v1.0 骨架（下一步，待你确认）

- [ ] 创建 Maven 多模块项目骨架
- [ ] 编写 `pom.xml` 公共依赖
- [ ] 编写 Flyway 迁移脚本 `V1__init_schema.sql`（包含全部 8 张表）
- [ ] 实现 `WechatPayConfig` 配置类
- [ ] 实现签名拦截器 / HTTP 客户端
- [ ] 实现商户证书 / 平台证书管理器
- [ ] JSAPI 支付下单
- [ ] Native 支付下单
- [ ] 统一回调验签 + 解密 + 防重放 + 幂等处理
- [ ] 退款 / 退款查询
- [ ] 单元测试 + 接入质量自检（用 Skill 「能力3」）

### Phase 2 · v1.1 直连商户增强

- [ ] 商家转账到零钱
- [ ] 营销券（代金券 / 商家券）
- [ ] 消费者投诉 2.0
- [ ] 对账文件下载 + 自动对账

### Phase 3 · v2.0 升级服务商

- [ ] 服务商 / 子商户配置体系（MerchantProvider 抽象落地）
- [ ] 特约商户进件（二级商户开户）
- [ ] 合单支付（JSAPI / Native 合单）
- [ ] 分账（订单分账 / 查询 / 退分账）
- [ ] 二级商户资金管理

### Phase 4 · v2.1 服务商扩展

- [ ] 付款码支付（V2 通道并行接入）
- [ ] 委托代扣
- [ ] 微信支付分 / 停车服务

---

## 十、上线前 Checklist（上线前必看）

> 全部用 Skill 「能力3：接入质量评估」自检

- [ ] 所有签名串打印脱敏（不含完整签名）
- [ ] 回调 URL 强制 HTTPS + IP 白名单
- [ ] 商户私钥不入库、不提交 git
- [ ] 平台证书每 12 小时自动刷新
- [ ] DB 所有金额字段用 BIGINT 存分，禁止 DECIMAL / double
- [ ] `out_trade_no` / `out_refund_no` 全局唯一且有唯一索引
- [ ] 业务订单与支付订单 1:1 且状态机完整
- [ ] 退款申请有金额校验（不能超单笔实付）
- [ ] 幂等键设计（out_trade_no / out_refund_no 全局唯一）
- [ ] 异常重试有指数退避 + 最大次数
- [ ] 监控告警：下单失败率、回调延迟、退款异常
- [ ] 灰度开关：单商户灰度 → 全量

---

## 十一、变更记录

| 日期 | 版本 | 内容 |
|---|---|---|
| 2026-09-14 | v0.6 | 仓库卫生：停掉 Spring Boot 后台进程；清理 `backend/target/` 编译产物（释放 ~60KB）和 `backend/run.log`；新增根 `.gitignore`（95 行，覆盖 Java/Maven/IDE/OS/敏感文件）；readme 增加第十三章仓库卫生规范 |
| 2026-09-14 | v0.5 | Phase 1 骨架完成：Spring Boot 3.3.5 + JDK 17 + MyBatis-Plus 3.5.7 + Flyway 10.20.1；本地连 Docker MySQL（3306）；Flyway 启动自动迁移 V1；示例接口 `/api/health`、`/api/v1/merchant/{mchId}` 验证中文全链路正常 |
| 2026-09-14 | v0.4 | **数据库 SQL 统一为单脚本**：`docs/db/schema/V1__weichat_finance_init.sql`（含建表 + 中文注释 + 乱码自检）；删除 V1/V2 历史脚本；Flyway 启动时自动执行该脚本，乱码问题根治 |
| 2026-09-14 | v0.3 | 修复 V1 建表脚本中表/字段注释因客户端 latin1 导致的 `?` 乱码（V2 迁移脚本修复，9 张表的注释全部恢复中文）；锁定 SQL 脚本执行标准流程（`docker cp` + 容器内 mysql + `default-character-set=utf8mb4`） |
| 2026-09-14 | v0.2 | 持久化方案确定：MySQL 8 + MyBatis-Plus + Flyway；单库多表；新增数据持久化章节（库表清单、schema 草案、Flyway 配置） |

---

## 十二、Phase 1 骨架交付（v0.5）

### 12.1 项目结构

```
backend/
├── pom.xml                                              # Spring Boot 3.3.5 + JDK 17 + MP 3.5.7 + Flyway 10
└── src/main/
    ├── java/com/weichat/finance/
    │   ├── WeichatFinanceApplication.java               # 主程序入口
    │   ├── common/R.java                                # 统一响应封装
    │   ├── config/MybatisPlusConfig.java                # 分页插件 + gmt_create/gmt_modified 自动填充
    │   ├── controller/
    │   │   ├── HealthController.java                    # GET /api/health
    │   │   └── MerchantConfigController.java            # GET /api/v1/merchant/{mchId}
    │   ├── entity/MerchantConfig.java                   # 商户配置表实体（含逻辑删除、字段填充）
    │   ├── mapper/MerchantConfigMapper.java             # BaseMapper CRUD
    │   └── service/
    │       ├── MerchantConfigService.java               # 业务接口
    │       └── impl/MerchantConfigServiceImpl.java      # 业务实现（按 mchId 查询）
    └── resources/
        ├── application.yml                              # 数据源 + Flyway + MyBatis-Plus 配置
        └── db/migration/V1__weichat_finance_init.sql    # 迁移脚本（软链接 docs/db/schema/）
```

### 12.2 启动流程（已验证）

1. 启动 Docker MySQL：`docker start weichat-finance-mysql`
2. 启动 Spring Boot：`cd backend && mvn spring-boot:run`
3. Flyway 启动时自动执行 V1，**无需手动建表**
4. 验证接口：
   - `GET http://localhost:8080/api/health` → 看到 `db: UP`
   - `GET http://localhost:8080/api/v1/merchant/PLACEHOLDER_MCH_ID` → 中文"默认直连商户"完整链路正常

### 12.3 已知选型与约束

- **MyBatis-Plus 锁版本 3.5.7**（3.5.9 当前镜像源未同步；升级前先确认本地缓存）
- **MySQL Connector/J 8.4.0**
- **Flyway 10.20.1 + flyway-mysql**（必须加 flyway-mysql 模块，Flyway 10 拆分了 DB 支持）
- **数据库字符集**：`character_set_client/connection/results = utf8mb4`（已写入 `/etc/mysql/conf.d/99-client-utf8mb4.cnf`）
- **JDBC URL**：`useUnicode=true&characterEncoding=utf8`（关键，否则连接字符集兜底）

### 12.4 后续 Phase（待启动）

- Phase 2：JSAPI/Native 统一下单 + 回调验签解密
- Phase 3：退款流程 + 幂等保护
- Phase 4：对账单下载 + 差异对账
- Phase 5：Dockerfile + docker-compose 全栈部署

---

## 十三、仓库卫生（v0.6）

### 13.1 仓库状态

- 当前**不是 git 仓库**（环境判断 `Is directory a git repo: No`），本节作为后续初始化 git 时的最佳实践文档
- 根 `.gitignore`（95 行）已就位，覆盖 Java/Maven/IDE/OS/敏感文件

### 13.2 .gitignore 覆盖范围

| 类别 | 忽略项 |
|---|---|
| Java/Maven | `target/`、`*.class`、`*.jar`、`*.log` |
| IntelliJ IDEA | `.idea/`（除项目必要配置外的运行时数据） |
| VS Code/Cursor | `.vscode/`、`.cursor/` |
| Eclipse | `.classpath`、`.project`、`.settings/` |
| OS | `Thumbs.db`、`.DS_Store`、`$RECYCLE.BIN/` |
| 项目特定 | `backend/run.log`、`backend/certs/`、`.env`、`*.pem`、`*.key`、`*.p12` |
| 构建产物 | `*.tar.gz`、`*.zip` |
| MySQL | `docker/mysql/data/` |

### 13.3 安全红线（**绝对不能进仓库**）

| 类型 | 风险 |
|---|---|
| `*.pem`、`*.key`、`*.p12`、`*.jks` | 私钥 / 证书泄露 = 资金风险 |
| `api_v3_key` | 解密所有微信回调的密钥 |
| `.env`、`*.local` | 数据库密码、API 密钥 |
| 商户证书私钥 | 微信 API v3 签名私钥 |

### 13.4 Phase 1 交付物快照（清理后）

```
Weichat_Finance/
├── .gitignore                                          # 1247 字节 / 95 行
├── .idea/                                              # Cursor IDE 配置（保留）
├── backend/
│   ├── pom.xml
│   └── src/main/                                       # 9 个 .java + application.yml + V1.sql
└── docs/
    ├── db/schema/V1__weichat_finance_init.sql          # 16619 字节 / 唯一脚本
    └── ... (其他文档)
```

后端编译产物 `target/` 与运行日志 `run.log` **不在仓库内**（已加入 `.gitignore`），每次 `mvn clean compile` 自动重建。

---
| 2026-09-14 | v0.1 | 项目初稿：明确 v1.0 范围（JSAPI + Native），预留服务商升级路径 |
