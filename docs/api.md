# Weichat_Finance · 接口文档

> 本文档描述本服务**对外暴露**的 HTTP 接口和**微信支付回调**的接口。
> 内部调用的微信支付 API（统一下单、退款、对账等）由 `wechatpay-java` SDK 直接处理，无需在本服务封装。

---

## 一、接口分类


| 类型         | 说明                        |
| ---------- | ------------------------- |
| **对外业务接口** | 业务系统 → 本服务（创建订单、查询、退款）    |
| **微信回调接口** | 微信支付 → 本服务（支付成功通知、退款结果通知） |
| **内部调用**   | 本服务 → 微信支付（无需在本服务封装）      |


---

## 二、对外业务接口

### 2.1 健康检查


| 项   | 值             |
| --- | ------------- |
| 方法  | `GET`         |
| 路径  | `/api/health` |
| 鉴权  | 无             |
| 用途  | 探活 + DB 连通性验证 |


**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "db": "UP"
  }
}
```

**字段说明**：

- `status`：`UP` / `DOWN`，整个应用状态
- `db`：`UP` / `DOWN`，数据库连通性

**当前状态**：✅ 已实现（Phase 1）

---



### 2.2 查询商户配置


| 项   | 值                          |
| --- | -------------------------- |
| 方法  | `GET`                      |
| 路径  | `/api/v1/merchant/{mchId}` |
| 鉴权  | 内部调用，无 token               |
| 用途  | 按商户号读取配置（v1.0 仅有 1 条默认数据）  |


**路径参数**：


| 名称      | 类型     | 说明  |
| ------- | ------ | --- |
| `mchId` | string | 商户号 |


**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "mchId": "PLACEHOLDER_MCH_ID",
    "merchantName": "默认直连商户",
    "appId": "wx0000000000000000",
    "apiV3Key": "********************************",
    "certSerialNo": null,
    "notifyUrlBase": null,
    "已启用": 1,
    "gmtCreate": "2026-09-14T10:00:00",
    "gmtModified": "2026-09-14T10:00:00"
  }
}
```

**当前状态**：✅ 已实现（Phase 1）

---



### 2.3 创建 JSAPI 支付订单（Phase 2 · Mock 模式可用）

| 项 | 值 |
|---|---|
| 方法 | `POST` |
| 路径 | `/api/v1/payment/jsapi/create` |
| 鉴权 | 内部调用 |
| 用途 | 创建 JSAPI 支付订单，返回 prepay_id 给前端调起支付 |

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `outTradeNo` | string(64) | 是 | 商户订单号（业务侧生成，UUID） |
| `description` | string(127) | 是 | 订单描述 |
| `amountTotal` | long | 是 | 订单金额（分），必须 > 0 |
| `currency` | string | 否 | 货币类型，默认 `CNY` |
| `openid` | string(64) | 是 | 用户标识（JSAPI 必传） |
| `attach` | string(128) | 否 | 附加数据 |
| `timeExpire` | string | 否 | 订单失效时间（ISO8601） |

**请求示例**：
```json
{
  "outTradeNo": "ORDER202609140002",
  "description": "测试商品描述",
  "amountTotal": 100,
  "openid": "oUpF8uMuAJLq5EQxS6Nk-2X0xxxx"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "prepayId": "MOCK_prepay_911c4e3f05f45759282bfcc4b87750e2",
    "source": "MOCK"
  }
}
```

**状态码**：

| code | 含义 |
|---|---|
| 200 | 下单成功（含 Mock 和 Real） |
| 400 | 参数校验失败（缺 openid、amountTotal ≤ 0 等） |
| 409 | 商户订单号已存在（幂等拦截） |
| 500 | 服务异常（商户配置缺失、SDK 异常等） |

**当前状态**：

- ✅ Mock 模式（默认）：本地零风险，DB 落库后返回伪造 prepay_id
- ⏳ Real 模式（Phase 2.1）：依赖 wechatpay-java SDK v0.2.12 + 商户私钥

**数据流**：

```
Client
  ↓ POST /api/v1/payment/jsapi/create
JsapiController
  ↓ 幂等检查（按 out_trade_no）
  ↓ 加载商户配置（mchId=PLACEHOLDER_MCH_ID）
  ↓ 业务订单落库（status=SUBMITTING）
JsapiService (Mock 或 Real)
  ↓ Real: 调用 wechatpay-java SDK → POST /v3/pay/transactions/jsapi
  ↓ Mock: 生成伪造 prepay_id
  ↓ 返回 prepay_id
Controller
  ↓ 更新订单状态（status=CREATED）
  ↓ 返回 prepay_id 给前端
```

---

### 2.4 v0.8 已实现 · 完整接口清单

> Phase 2.1/2.2/3 已实现以下接口。详见各小节。

| 接口           | 方法   | 路径                                         | 用途                  | 状态          |
| ------------ | ---- | ------------------------------------------ | ------------------- | ----------- |
| 创建 JSAPI 订单  | POST | `/api/v1/payment/jsapi/create`             | 返回 prepay_id，调起支付 | ✅ Phase 2   |
| 查询 JSAPI 订单  | GET  | `/api/v1/payment/order/{outTradeNo}`       | 主动查单                | ✅ Phase 2.1 |
| 关单           | POST | `/api/v1/payment/order/{outTradeNo}/close` | 关闭未支付订单             | ✅ Phase 2.1 |
| 创建 Native 订单 | POST | `/api/v1/payment/native/create`            | 返回 code_url          | ✅ Phase 2.2 |
| 申请退款         | POST | `/api/v1/payment/refund/create`            | 同步发起退款              | ✅ Phase 3   |
| 查询退款         | GET  | `/api/v1/payment/refund/{outRefundNo}`     | 查退款进度               | ✅ Phase 3   |
| 微信支付成功回调     | POST | `/api/notify/v3/pay/success`               | 微信支付通知入口            | ✅ Phase 3   |
| 微信退款成功回调     | POST | `/api/notify/v3/refund/success`            | 微信退款通知入口            | ✅ Phase 3   |

#### 2.4.1 查询 JSAPI 订单

| 项   | 值                                  |
| --- | ---------------------------------- |
| 方法  | `GET`                              |
| 路径  | `/api/v1/payment/order/{outTradeNo}` |
| 鉴权  | 内部                                |
| 用途  | 按商户订单号查支付订单（Mock 返回 NOTPAY）       |

#### 2.4.2 关单

| 项   | 值                                            |
| --- | -------------------------------------------- |
| 方法  | `POST`                                       |
| 路径  | `/api/v1/payment/order/{outTradeNo}/close`   |
| 用途  | 按商户订单号关单（Mock 仅日志记录），更新订单状态为 CLOSED |

#### 2.4.3 创建 Native 订单

| 项   | 值                                  |
| --- | ---------------------------------- |
| 方法  | `POST`                             |
| 路径  | `/api/v1/payment/native/create`    |
| 鉴权  | 内部                                |
| 用途  | 创建 Native（扫码）订单，返回 code_url |

**请求体**：

```json
{
  "outTradeNo": "TEST_NATIVE_001",
  "description": "测试Native订单",
  "amountTotal": 200,
  "currency": "CNY",
  "attach": "可选附加数据"
}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "codeUrl": "weixin://wxpay/bizpayurl?pr=MOCK_xxx",
    "source": "MOCK"
  }
}
```

#### 2.4.4 申请退款

| 项   | 值                                  |
| --- | ---------------------------------- |
| 方法  | `POST`                             |
| 路径  | `/api/v1/payment/refund/create`    |
| 鉴权  | 内部                                |
| 幂等  | 按 `outRefundNo` 判重               |
| 用途  | 申请退款（含🔴金额上限校验，已修致命问题）       |

**请求体**：

```json
{
  "outRefundNo": "TEST_REFUND_001",
  "outTradeNo": "TEST_JSAPI_001",
  "amountRefund": 50,
  "amountTotal": 100,
  "reason": "用户申请退款"
}
```

**字段说明**：

- `outRefundNo` · 商户退款单号（必填，幂等键）
- `outTradeNo` · 原商户订单号（必填）
- `amountRefund` · 退款金额（分，必填，必须 ≤ 原订单金额 - 累计已退金额）
- `amountTotal` · 原订单金额（分，必填，服务端会**用数据库订单金额覆盖**防止前端篡改）
- `reason` · 退款原因（可选）

**错误码**：

- 400 · 退款金额超限 / 累计退款金额超限
- 404 · 原商户订单号不存在
- 409 · 商户退款单号已存在（重放）

#### 2.4.5 查询退款

| 项   | 值                                      |
| --- | -------------------------------------- |
| 方法  | `GET`                                  |
| 路径  | `/api/v1/payment/refund/{outRefundNo}` |
| 用途  | 查询退款进度（Mock 返回 SUCCESS 状态）           |

---



## 三、微信回调接口



### 3.1 支付成功通知


| 项    | 值                                  |
| ---- | ---------------------------------- |
| 路径   | `/notify/v3/pay/success`           |
| 触发时机 | 用户支付成功后                            |
| 鉴权   | 微信签名（验签在请求头 `Wechatpay-Signature`） |


**请求头**（验签所需）：

- `Wechatpay-Timestamp`
- `Wechatpay-Nonce`
- `Wechatpay-Signature`
- `Wechatpay-Serial`
- `Wechatpay-Signature-Type`

**请求体**（已加密的资源）：

```json
{
  "id": "EV-2018022511223320873",
  "create_time": "2026-09-14T10:30:00+08:00",
  "resource_type": "encrypt-resource",
  "event_type": "TRANSACTION.SUCCESS",
  "summary": "支付成功",
  "resource": {
    "algorithm": "AEAD_AES_256_GCM",
    "ciphertext": "...",
    "nonce": "...",
    "associated_data": ""
  }
}
```

**处理流程**：

1. 验签（用平台证书验 `Wechatpay-Signature`）
2. 防重放（5 分钟内 timestamp + nonce 去重）
3. 解密 `resource`（用 `api_v3_key`）
4. 幂等（按 `out_trade_no`）
5. **立即返回 200/204**，业务逻辑异步消费



### 3.2 退款结果通知


| 项    | 值                           |
| ---- | --------------------------- |
| 路径   | `/notify/v3/refund/success` |
| 触发时机 | 退款状态变化时                     |


**处理流程**：同 3.1，幂等键为 `out_refund_no`

---



## 四、统一响应格式

所有 Controller 返回 `R<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**失败响应**：

```json
{
  "code": 500,
  "message": "internal error",
  "data": null
}
```

**错误码规范**：


| code | 含义        |
| ---- | --------- |
| 200  | 成功        |
| 400  | 参数错误      |
| 401  | 未授权       |
| 404  | 资源不存在     |
| 500  | 内部错误      |
| 502  | 微信支付服务端错误 |


---



## 五、数据模型



### 5.1 数据库表清单（v1.0）


| #   | 表名                     | 用途            | 关键索引                                                                     |
| --- | ---------------------- | ------------- | ------------------------------------------------------------------------ |
| 1   | `t_pay_order`          | 支付订单（业务订单）    | UK(`out_trade_no`)、IDX(`mch_id`, `status`)、IDX(`gmt_create`)             |
| 2   | `t_pay_transaction`    | 微信支付交易流水      | UK(`transaction_id`)、UK(`out_trade_no`)、IDX(`mch_id`, `pay_status`)      |
| 3   | `t_pay_refund`         | 退款单           | UK(`out_refund_no`)、IDX(`transaction_id`)、IDX(`mch_id`, `refund_status`) |
| 4   | `t_pay_notify_log`     | 回调原始报文        | IDX(`out_trade_no`)、IDX(`gmt_create`)                                    |
| 5   | `t_pay_idempotent`     | 幂等记录          | UK(`idempotent_key`)                                                     |
| 6   | `t_merchant_config`    | 商户配置          | UK(`mch_id`)                                                             |
| 7   | `t_platform_cert`      | 微信平台证书缓存      | UK(`serial_no`)                                                          |
| 8   | `t_pay_reconciliation` | 对账记录（v1.1 启用） | IDX(`bill_date`, `mch_id`)                                               |




### 5.2 设计原则

1. **主键策略**：`BIGINT AUTO_INCREMENT` 做表内唯一主键（不对外暴露），业务单号（`out_trade_no` / `out_refund_no`）用 UUID v4 单独生成，加唯一索引
2. **必备字段**：每张业务表都有 `id`、`gmt_create`、`gmt_modified`、`is_deleted`
3. **金额字段**：全部用 `BIGINT` 存**分**（严禁 `DECIMAL`）
4. **时间字段**：业务时间用 `DATETIME(3)`（毫秒精度）
5. **JSON 字段**：微信回调的扩展参数、原始报文用 MySQL 8 的 `JSON` 列
6. **迁移管理**：所有 DDL 通过 Flyway 脚本，**禁止应用启动时自动建表**



### 5.3 关键表 schema（草案）

> 完整 schema 见 `db/schema/V1__weichat_finance_init.sql`。



#### `t_merchant_config`（当前已实现）

```sql
CREATE TABLE `t_merchant_config` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `mch_id`         VARCHAR(32)  NOT NULL                  COMMENT '商户号',
  `merchant_name`  VARCHAR(128) NOT NULL                  COMMENT '商户名称',
  `app_id`         VARCHAR(32)  NOT NULL                  COMMENT 'AppID',
  `api_v3_key`     VARCHAR(64)  NOT NULL                  COMMENT 'V3 密钥（用于回调解密）',
  `cert_serial_no` VARCHAR(64)           DEFAULT NULL     COMMENT '商户证书序列号',
  `notify_url_base` VARCHAR(512)         DEFAULT NULL     COMMENT '回调 URL 前缀',
  `已启用`        TINYINT      NOT NULL DEFAULT 1       COMMENT '启用：1=启用，0=禁用',
  `gmt_create`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mch_id` (`mch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户配置';
```

---



## 六、SQL 脚本规范（防乱码）



### 6.1 文件位置（**唯一**）

`docs/db/schema/V1__weichat_finance_init.sql`

**原则**：数据库只有一份脚本，就是真相。**不允许多版本（V1、V2、V3...）存在**——避免开发者不知道以哪份为准。

### 6.2 根因（历史教训）

PowerShell 管道默认用 GBK 解码 UTF-8 文件，docker MySQL 客户端默认 `character_set_client=latin1`，两段叠加导致中文存成 `?`（0x3F）。

### 6.3 已修复

1. MySQL 客户端默认 utf8mb4（`/etc/mysql/conf.d/99-client-utf8mb4.cnf`）
2. 数据 `merchant_name` 已从 `??????` 修复为 `默认直连商户`
3. 全部表的表注释 + 字段注释已恢复中文



### 6.4 强制规范

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



### 6.5 禁止写法

- ❌ `Get-Content -Raw xxx.sql | docker exec -i ... mysql ...`（PowerShell 管道 GBK 解码）
- ❌ `docker exec -i ... mysql ... < xxx.sql`（PowerShell 不支持重定向输入）
- ❌ `mysql ... < /tmp/xxx.sql` 不加 `--default-character-set=utf8mb4`（客户端字符集兜底）
- ❌ **新建 V2、V3 等多版本 SQL 脚本**（杜绝多版本造成歧义；新需求直接改 V1）



### 6.6 Flyway 集成

- Spring Boot 启动时 Flyway 自动执行 `src/main/resources/db/migration/V1__weichat_finance_init.sql`
- 应用 `spring.flyway.encoding=utf-8` 确保客户端字符集
- 数据库手动维护只在 docker 容器内做（重置/调试），不直接动 Flyway 历史



### 6.7 重置数据库（需要彻底清库时）

```bash
docker exec weichat-finance-mysql mysql -uroot -proot -e \
    "DROP DATABASE IF EXISTS weichat_finance;
     CREATE DATABASE weichat_finance CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
# 然后重新跑上面的 4 步流程
```

---

## 七、API 文档（Knife4j）

### 访问路径

| 类型 | 路径 | 说明 |
| --- | --- | --- |
| Knife4j UI（推荐） | `http://localhost:8080/api/doc.html` | 中文界面，支持调试、下载 OpenAPI、搜索 |
| OpenAPI JSON | `http://localhost:8080/api/v3/api-docs` | 标准 OpenAPI 3 规范 JSON |
| 原生 Swagger UI | `http://localhost:8080/api/swagger-ui.html` | Knife4j 之外的备用入口 |

### 接口分组（自动生成）

| 分组 | 接口 |
| --- | --- |
| **JSAPI 支付** | `/v1/payment/jsapi/create`、`/v1/payment/order/{outTradeNo}`、`/v1/payment/order/{outTradeNo}/close` |
| **Native 支付** | `/v1/payment/native/create` |
| **退款管理** | `/v1/payment/refund/create`、`/v1/payment/refund/{outRefundNo}` |
| **商户配置** | `/v1/merchant/{mchId}` |
| **健康检查** | `/health` |
| **微信支付回调** | `/notify/v3/pay/success`、`/notify/v3/refund/success` |

### 调试说明

- 默认 Mock 模式：所有接口调用都会走本地 Mock 实现，零风险
- 切换 Real 模式：设置环境变量 `WX_PAY_MODE=REAL`，并填入真实商户凭证
- Knife4j 调试功能：点开任意接口 → 「调试」按钮 → 填入请求参数 → 即可在线发送请求并查看响应

### 生产环境建议

- `knife4j.enable: false` 关闭 Knife4j UI（仅开发/测试环境开放）
- 通过网关鉴权（Spring Cloud Gateway + JWT / OAuth2）限制 `/doc.html` 访问
