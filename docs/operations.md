# Weichat_Finance · 运维手册

> 本文档面向**运维 / 部署 / 上线**人员，描述本地启动、证书管理、上线 checklist 等操作流程。

---

## 一、本地启动流程

### 1.1 前置条件

| 依赖 | 版本 | 验证命令 |
|---|---|---|
| JDK | 17 | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Docker | 任意 | `docker --version` |
| MySQL 容器 | 8.x | `docker ps \| grep weichat-finance-mysql` |

### 1.2 启动顺序

```bash
# 1. 启动 Docker MySQL（首次创建容器）
docker run -d --name weichat-finance-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=weichat_finance \
  -v /etc/mysql/conf.d:/etc/mysql/conf.d \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_0900_ai_ci

# 2. 验证字符集
docker exec weichat-finance-mysql mysql -uroot -proot -e \
  "SHOW VARIABLES WHERE Variable_name LIKE 'character_set%';"

# 3. 启动 Spring Boot
cd backend
mvn spring-boot:run
```

启动日志应包含：
- `Flyway Community Edition X.X.X by Redgate`
- `Successfully applied X migrations to schema`
- `Started WeichatFinanceApplication in X seconds`

### 1.3 验证

```bash
# 健康检查
curl http://localhost:8080/api/health

# 查询商户配置
curl http://localhost:8080/api/v1/merchant/PLACEHOLDER_MCH_ID
```

中文 `默认直连商户` 应完整显示。

---

## 二、仓库卫生

### 2.1 状态

- 当前**已是 git 仓库**（push 到 GitHub 公开仓库）
- 根 `.gitignore`（95 行）已就位，覆盖 Java/Maven/IDE/OS/敏感文件

### 2.2 .gitignore 覆盖范围

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

### 2.3 安全红线（**绝对不能进仓库**）

| 类型 | 风险 |
|---|---|
| `*.pem`、`*.key`、`*.p12`、`*.jks` | 私钥 / 证书泄露 = 资金风险 |
| `api_v3_key` | 解密所有微信回调的密钥 |
| `.env`、`*.local` | 数据库密码、API 密钥 |
| 商户证书私钥 | 微信 API v3 签名私钥 |

---

## 三、证书管理流程

### 3.1 商户 API 证书

**来源**：商户平台 → API 安全 → 申请 API 证书

**下载得到**：
- `apiclient_cert.pem`（证书）
- `apiclient_key.pem`（私钥）
- `apiclient_cert.p12`（PKCS#12 格式，可选）

**部署位置**：`backend/certs/`（已加入 `.gitignore`）

**注入方式**：通过环境变量指定路径：

```yaml
wechatpay:
  merchant:
    cert:
      serial-no: ${WX_CERT_SERIAL_NO}
      private-key-path: ${WX_CERT_PATH:backend/certs/apiclient_key.pem}
```

### 3.2 平台证书

**用途**：回调验签 + 回调资源解密

**获取方式**：调用 `/v3/certificates` 接口

**关键约束**：
- **不能**静态写死 —— 启动时若缓存为空则同步拉取一次
- 每次 HTTP 请求头 `Wechatpay-Serial` 需带上对应签名证书的序列号
- 监听回调头里的证书变更通知（`Wechatpay-Serial` 与本地不一致时），异步刷新本地缓存
- 推荐缓存到 MySQL 表 `t_platform_cert`，**定期刷新**（如每 12 小时）

### 3.3 证书轮换

| 触发条件 | 处理动作 |
|---|---|
| 启动时缓存为空 | 同步拉取一次 |
| 回调头 `Wechatpay-Serial` 与本地不一致 | 异步拉取新证书 |
| 定期任务（建议 12h） | 拉取一次，更新缓存 |
| 微信支付侧下架老证书 | 拉取接口返回 404 → 删除本地老证书 |

---

## 四、配置管理

### 4.1 application.yml 草案

```yaml
wechatpay:
  mode: DIRECT                # DIRECT / PARTNER
  merchant:
    mch-id: ${WX_MCH_ID}
    app-id: ${WX_APP_ID}
    api-v3-key: ${WX_API_V3_KEY}
    cert:
      serial-no: ${WX_CERT_SERIAL_NO}
      private-key-path: certs/apiclient_key.pem
  v2-key: ${WX_V2_KEY:}
  notify-url-base: https://your-domain.com
```

### 4.2 配置优先级

1. **运行时**：从 `t_merchant_config` 表读取（多商户场景）
2. **启动兜底**：`application.yml` + 环境变量
3. **覆盖关系**：DB > yml

> 真实值通过环境变量注入，**禁止写入代码或 git 仓库**。

### 4.3 数据库配置

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
  type-aliases-package: com.weichat.finance.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: is_deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      insert-strategy: NOT_NULL
      update-strategy: NOT_NULL
```

---

## 五、上线前 Checklist

> 全部用 Skill 「接入质量评估」自检

### 5.1 安全

- [ ] 所有签名串打印脱敏（不含完整签名）
- [ ] 回调 URL 强制 HTTPS + IP 白名单
- [ ] 商户私钥不入库、不提交 git
- [ ] 平台证书每 12 小时自动刷新
- [ ] 数据库密码、API v3 密钥使用环境变量，不在代码中明文

### 5.2 数据

- [ ] DB 所有金额字段用 BIGINT 存分，**禁止** DECIMAL / double
- [ ] `out_trade_no` / `out_refund_no` 全局唯一且有唯一索引
- [ ] 业务订单与支付订单 1:1 且状态机完整
- [ ] 数据库字符集 `utf8mb4`，JDBC URL 含 `useUnicode=true&characterEncoding=UTF-8`

### 5.3 业务

- [ ] 退款申请有金额校验（不能超单笔实付）
- [ ] 幂等键设计（`out_trade_no` / `out_refund_no` 全局唯一）
- [ ] 异常重试有指数退避 + 最大次数
- [ ] 监控告警：下单失败率、回调延迟、退款异常
- [ ] 灰度开关：单商户灰度 → 全量

### 5.4 部署

- [ ] Spring Boot 应用配置 JVM 参数（`-Xms` `-Xmx`）
- [ ] 日志输出到文件 + 集中收集（ELK / Loki）
- [ ] 健康检查端点 `/api/health` 接入了 LB 探活
- [ ] 数据库迁移通过 Flyway 自动执行，不依赖手工脚本

---

## 六、监控告警（待实施）

| 指标 | 阈值 | 告警方式 |
|---|---|---|
| 下单失败率 | > 5% / 5min | 企业微信机器人 |
| 回调延迟 | P99 > 30s | 企业微信机器人 |
| 退款异常率 | > 1% / 5min | 企业微信机器人 |
| 数据库连接池使用率 | > 80% | 企业微信机器人 |
| Flyway 迁移失败 | 启动失败 | 直接告警 |
| JVM GC 时间 | > 5% / 1min | Prometheus + Grafana |

---

## 七、回滚流程

### 7.1 代码回滚

```bash
git revert <commit-hash>
git push origin main
```

或：
```bash
git reset --hard <previous-commit-hash>
git push -f origin main  # 仅限个人分支，main 分支禁用
```

### 7.2 数据库回滚

Flyway 不支持自动 downgrade。回滚原则：

1. **新增表 / 加列**：直接 drop 即可（数据可丢弃）
2. **删除表 / 删列**：通过新增列 + 数据迁移完成"反向"逻辑
3. **线上事故**：先回滚代码，再手动回滚数据，**不要**反向 Flyway 脚本

### 7.3 配置回滚

应用配置通过环境变量注入，回滚只需修改 K8s ConfigMap / Nacos，无需重新打包。
