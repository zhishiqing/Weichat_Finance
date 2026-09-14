# Weichat_Finance · 开发文档

> 本文档面向**开发者**，描述项目的技术选型、架构设计、代码组织、关键依赖。  
> 如果你是第一次接触本项目，请先看 [readme.md](../readme.md) 了解整体定位，再到本文档。

---

## 一、技术选型

| 维度 | 选择 | 理由 |
|---|---|---|
| 语言 | **Java 17** | 与微信支付官方文档示例语言一致，便于对照 |
| 框架 | **Spring Boot 3.3.5** | 生态成熟，对 HTTP 客户端、验签、加签、回调处理支持完善 |
| ORM | **MyBatis-Plus 3.5.7** | 单表 CRUD 免写 SQL；3.5.9 当前镜像源未同步，锁 3.5.7 |
| 数据库 | **MySQL 8.0+** | 单库多表 |
| 连接池 | HikariCP（Spring Boot 默认） | 高性能 |
| 迁移 | **Flyway 10.20.1 + flyway-mysql** | 版本化管理 schema，团队协作友好 |
| HTTP | OkHttp 4 | 微信支付 V3 官方 Java SDK 推荐使用 |
| JSON | Jackson | Spring Boot 默认 |
| 加密 | BouncyCastle + 自实现 V3 签名 | 处理 PKCS#12 证书 |
| 缓存 | Caffeine（本地）+ MySQL 持久化（v1.0）→ Redis（v2.0） | 商户配置、AccessToken、平台证书走 DB |
| 日志 | Logback + MDC（traceId 串联） | 排查问题必需 |
| 构建 | Maven | 与多数团队习惯一致 |

---

## 二、Maven 依赖

`backend/pom.xml` 关键依赖：

```xml
<!-- Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- AOP（MyBatis-Plus 事务依赖） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- JDBC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.7</version>
</dependency>

<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>10.20.1</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <version>10.20.1</version>
</dependency>

<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.4.0</version>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

> 实际版本号在生成骨架时以 Maven Central 最新稳定版为准。**未来新增微信 SDK、OkHttp、BouncyCastle 时**：
> - `wechatpay-java`（V3 官方 SDK）
> - `okhttp 4.12.0`
> - `bcpkix-jdk18on 1.78`

---

## 三、项目结构

```
Weichat_Finance/
├── .gitignore                       # 仓库忽略规则
├── readme.md                        # 项目简介（入口）
├── backend/
│   ├── pom.xml                      # Spring Boot 3.3.5 + JDK 17 + MP 3.5.7 + Flyway 10
│   └── src/main/
│       ├── java/com/weichat/finance/
│       │   ├── WeichatFinanceApplication.java        # 主程序入口
│       │   ├── common/R.java                          # 统一响应封装
│       │   ├── config/MybatisPlusConfig.java          # 分页插件 + gmt_create/gmt_modified 自动填充
│       │   ├── controller/
│       │   │   ├── HealthController.java              # GET /api/health
│       │   │   └── MerchantConfigController.java      # GET /api/v1/merchant/{mchId}
│       │   ├── entity/MerchantConfig.java             # 商户配置表实体（含逻辑删除、字段填充）
│       │   ├── mapper/MerchantConfigMapper.java       # BaseMapper CRUD
│       │   └── service/
│       │       ├── MerchantConfigService.java         # 业务接口
│       │       └── impl/MerchantConfigServiceImpl.java # 业务实现
│       └── resources/
│           ├── application.yml                        # 数据源 + Flyway + MyBatis-Plus 配置
│           └── db/migration/V1__weichat_finance_init.sql  # 迁移脚本
└── docs/
    ├── development.md                # 本文件
    ├── api.md                        # 接口文档
    ├── operations.md                 # 运维手册
    ├── changelog.md                  # 变更日志
    └── db/schema/V1__weichat_finance_init.sql  # 数据库初始化脚本
```

### 当前架构（v1.0 直连商户）

```
┌─────────────────┐     ┌──────────────────────┐     ┌──────────────┐
│  业务系统        │────▶│  本服务（Spring Boot）│────▶│  微信支付 API │
│  （调用方）       │◀────│                      │◀────│  （V3 通道）  │
└─────────────────┘     └──────────────────────┘     └──────────────┘
                              │       ▲
                              │       │
                              ▼       │
                        ┌──────────────────┐
                        │  MySQL 8         │
                        │  （持久化）        │
                        └──────────────────┘
```

### 未来架构（v2.0 服务商）

```
                       ┌──────────────────────┐
                       │ MerchantProvider    │  ← 商户路由抽象
                       │ (Direct/Partner)    │
                       └──────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
        ┌──────────────┐            ┌──────────────┐
        │ DirectMerchant│            │PartnerMerchant│
        │ Provider      │            │ Provider      │
        └──────────────┘            └──────────────┘
```

---

## 四、Phase 交付记录

### Phase 1 · v0.5/v0.6 · 骨架（已完成）

**目标**：能跑 + 连 MySQL + Flyway 自动执行 + 一个示例 API

**选型**：
- Spring Boot 3.3.5 + JDK 17 + Maven
- MyBatis-Plus 3.5.7（锁版本，本地缓存已就绪）
- Flyway 10.20.1 + flyway-mysql 模块
- 部署模式：Spring Boot 装在 Windows 本地，连 localhost:3306 的 Docker MySQL

**交付物**：

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/weichat/finance/         (9 个 .java)
    │   ├── WeichatFinanceApplication.java
    │   ├── common/R.java                 (统一响应 R<T>)
    │   ├── config/MybatisPlusConfig.java (分页插件 + MetaObjectHandler)
    │   ├── controller/{Health, MerchantConfig}Controller.java
    │   ├── entity/MerchantConfig.java    (含 @TableLogic 逻辑删除)
    │   ├── mapper/MerchantConfigMapper.java (BaseMapper CRUD)
    │   └── service/{MerchantConfigService, impl/MerchantConfigServiceImpl}.java
    └── resources/
        ├── application.yml
        └── db/migration/V1__weichat_finance_init.sql
```

**已知约束**：
- MyBatis-Plus 3.5.9 当前阿里云镜像未同步，**锁 3.5.7**
- Flyway 10 拆分了 DB 支持，**必须加 flyway-mysql 模块**
- JDBC URL 必须带 `useUnicode=true&characterEncoding=utf8`（连接字符集兜底）

### Phase 2 · v0.7（已完成）+ v0.8（已完成）

**v0.7 目标**：JSAPI 统一下单骨架（Mock 模式）

**v0.8 目标**：完整业务闭环（查单/关单/Native/退款/回调/幂等/质量评估）

**已交付接口**（v0.8）：

| 接口         | 方法   | 路径                                       | 模式          |
| ---------- | ---- | ---------------------------------------- | ----------- |
| 创建 JSAPI 订单 | POST | `/api/v1/payment/jsapi/create`           | Mock ✅ Real ⏳ |
| 查询 JSAPI 订单 | GET  | `/api/v1/payment/order/{outTradeNo}`     | Mock ✅ Real ⏳ |
| 关单         | POST | `/api/v1/payment/order/{outTradeNo}/close` | Mock ✅ Real ⏳ |
| 创建 Native 订单 | POST | `/api/v1/payment/native/create`          | Mock ✅ Real ⏳ |
| 申请退款     | POST | `/api/v1/payment/refund/create`          | Mock ✅ Real ⏳ |
| 查询退款     | GET  | `/api/v1/payment/refund/{outRefundNo}`   | Mock ✅ Real ⏳ |
| 微信支付回调 | POST | `/api/notify/v3/pay/success`             | 落库 ✅ 验签 ⏳  |
| 微信退款回调 | POST | `/api/notify/v3/refund/success`          | 落库 ✅ 验签 ⏳  |

**接入质量评估（v0.8）**：按 Skill 通用清单扫描，按 🔴🟡🟠 分级修复。

| 问题 | 等级 | 状态 |
| --- | --- | --- |
| 退款金额上限校验缺失 | 🔴 致命 | ✅ 已修 |
| SIGNTEST 探测流量未识别 | 🔴 致命 | ✅ 已修 |
| 退款金额累计校验 | 🔴 致命 | ✅ 已修 |
| 前端传值不可直接入金额（业务订单设计） | 🟠 TODO Phase 4 | 标 TODO |
| 微信回调验签 + 解密 | 🔴 致命 | ⏳ 待真实私钥 |
| 主动查询兜底（定时任务） | 🟡 必须 | ⏳ Phase 4.x |
| Request-Id 日志串联 | 🟠 建议 | ⏳ Phase 2.1 SDK 激活后 |

### Phase 3 · 待启动

- 退款流程
- 幂等保护（`t_pay_idempotent`）
- 退款查询

### Phase 4 · 待启动

- 对账单下载
- 差异对账

### Phase 5 · 待启动

- Dockerfile + docker-compose 全栈部署

---

## 五、代码组织原则

### 5.1 持久层约束

- 所有 Mapper **继承** `BaseMapper<T>`，禁止裸写 SQL
- 复杂查询用 `@Select` 注解或写在 `src/main/resources/mapper/` 下的 XML
- 事务：服务层 `@Transactional(rollbackFor = Exception.class)`，**禁用** `noRollbackFor` 偷懒写法
- 任何 DB 写操作必须有日志（操作人 / IP / 前后值），v1.0 至少实现 `gmt_modified` 自动填充

### 5.2 自动填充机制

`MybatisPlusConfig#metaObjectHandler` 统一处理：

- `insertFill`：填 `gmt_create` + `gmt_modified`
- `updateFill`：填 `gmt_modified`

**新增 Entity 时**：
```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime gmtCreate;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime gmtModified;
```

### 5.3 逻辑删除

全局配置：
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: is_deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**新增 Entity 时**：
```java
@TableLogic
private Integer isDeleted;
```

### 5.4 响应封装

所有 Controller 返回 `R<T>`：
```java
return R.ok(data);          // 成功
return R.fail("message");   // 失败
return R.fail(404, "not found");
```

---

## 六、调试与日志

### 6.1 开发期 SQL 打印

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 6.2 日志格式

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"
```

### 6.3 traceId（MDC）

v1.0 暂未启用，预留接口。Phase 2 接入微信回调时统一加 traceId 串联日志。
