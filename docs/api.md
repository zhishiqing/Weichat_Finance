# Weichat_Finance · 接口文档

> 本文档描述本服务**对外暴露**的 HTTP 接口和**微信支付回调**的接口。
> 内部调用的微信支付 API（统一下单、退款、对账等）由 `wechatpay-java` SDK 直接处理，无需在本服务封装。

---

## 一、接口分类

| 类型 | 说明 |
|---|---|
| **对外业务接口** | 业务系统 → 本服务（创建订单、查询、退款） |
| **微信回调接口** | 微信支付 → 本服务（支付成功通知、退款结果通知） |
| **内部调用** | 本服务 → 微信支付（无需在本服务封装） |

---

## 二、对外业务接口

### 2.1 健康检查

| 项 | 值 |
|---|---|
| 方法 | `GET` |
| 路径 | `/api/health` |
| 鉴权 | 无 |
| 用途 | 探活 + DB 连通性验证 |

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

| 项 | 值 |
|---|---|
| 方法 | `GET` |
| 路径 | `/api/v1/merchant/{mchId}` |
| 鉴权 | 内部调用，无 token |
| 用途 | 按商户号读取配置（v1.0 仅有 1 条默认数据） |

**路径参数**：

| 名称 | 类型 | 说明 |
|---|---|---|
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
    "enabled": 1,
    "gmtCreate": "2026-09-14T10:00:00",
    "gmtModified": "2026-09-14T10:00:00"
  }
}
```

**当前状态**：✅ 已实现（Phase 1）

---

### 2.3 待实现 · v1.0 完整接口清单

> 以下接口在 Phase 2/3 实现，本节作为**契约占位**，先定义清楚便于前后端并行。

| 接口 | 方法 | 路径 | 用途 |
|---|---|---|---|
| 创建 JSAPI 订单 | POST | `/api/v1/payment/jsapi/create` | 返回 prepay_id，前端调起支付 |
| 创建 Native 订单 | POST | `/api/v1/payment/native/create` | 返回 code_url，前端生成二维码 |
| 查询订单 | GET | `/api/v1/payment/order/{out_trade_no}` | 主动查单（兜底轮询） |
| 申请退款 | POST | `/api/v1/payment/refund/create` | 同步发起退款 |
| 查询退款 | GET | `/api/v1/payment/refund/{out_refund_no}` | 查退款进度 |

---

## 三、微信回调接口

### 3.1 支付成功通知

| 项 | 值 |
|---|---|
| 路径 | `/notify/v3/pay/success` |
| 触发时机 | 用户支付成功后 |
| 鉴权 | 微信签名（验签在请求头 `Wechatpay-Signature`） |

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

| 项 | 值 |
|---|---|
| 路径 | `/notify/v3/refund/success` |
| 触发时机 | 退款状态变化时 |

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

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 404 | 资源不存在 |
| 500 | 内部错误 |
| 502 | 微信支付服务端错误 |

---

## 五、数据模型

### 5.1 数据库表清单（v1.0）

| # | 表名 | 用途 | 关键索引 |
|---|---|---|---|
| 1 | `t_pay_order` | 支付订单（业务订单） | UK(`out_trade_no`)、IDX(`mch_id`, `status`)、IDX(`gmt_create`) |
| 2 | `t_pay_transaction` | 微信支付交易流水 | UK(`transaction_id`)、UK(`out_trade_no`)、IDX(`mch_id`, `pay_status`) |
| 3 | `t_pay_refund` | 退款单 | UK(`out_refund_no`)、IDX(`transaction_id`)、IDX(`mch_id`, `refund_status`) |
| 4 | `t_pay_notify_log` | 回调原始报文 | IDX(`out_trade_no`)、IDX(`gmt_create`) |
| 5 | `t_pay_idempotent` | 幂等记录 | UK(`idempotent_key`) |
| 6 | `t_merchant_config` | 商户配置 | UK(`mch_id`) |
| 7 | `t_platform_cert` | 微信平台证书缓存 | UK(`serial_no`) |
| 8 | `t_pay_reconciliation` | 对账记录（v1.1 启用） | IDX(`bill_date`, `mch_id`) |

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
  `enabled`        TINYINT      NOT NULL DEFAULT 1       COMMENT '启用：1=启用，0=禁用',
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
