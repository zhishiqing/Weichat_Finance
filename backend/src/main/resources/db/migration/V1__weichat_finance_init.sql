-- ============================================================
-- Weichat_Finance · 数据库初始化（唯一脚本）
-- 文件：docs/db/schema/V1__weichat_finance_init.sql
-- 版本：v1.0（2026-09-14）
-- 适用：MySQL 8.0+，字符集 utf8mb4 / utf8mb4_0900_ai_ci
-- 实例：本地 Docker `weichat-finance-mysql`（端口 3306，库 weichat_finance）
--
-- 使用规范（readme 6.6）：
--   1. docker cp 到容器内（不能用 Get-Content 管道，避免 PowerShell GBK 转码）
--   2. 容器内 mysql --default-character-set=utf8mb4 执行
--   3. 执行后用 LIKE '%?%' 或 HEX() 校验无乱码
-- ============================================================

USE weichat_finance;

-- -----------------------------------------------------------
-- 1. t_pay_order · 业务订单（商户侧订单）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pay_order` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `out_trade_no`    VARCHAR(64)  NOT NULL                              COMMENT '商户订单号（业务侧生成，UUID）',
  `mch_id`          VARCHAR(32)  NOT NULL                              COMMENT '商户号',
  `app_id`          VARCHAR(32)  NOT NULL                              COMMENT '公众号或小程序 AppID',
  `description`     VARCHAR(128) NOT NULL                              COMMENT '订单描述',
  `amount_total`    BIGINT       NOT NULL                              COMMENT '订单金额（分）',
  `currency`        VARCHAR(8)   NOT NULL DEFAULT 'CNY'                COMMENT '货币类型',
  `openid`          VARCHAR(64)           DEFAULT NULL                  COMMENT '用户标识（JSAPI 必填，Native 为空）',
  `product_type`    VARCHAR(16)  NOT NULL                              COMMENT '支付产品：JSAPI 或 NATIVE',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'CREATED'            COMMENT '订单状态：CREATED、SUBMITTING、SUCCESS、CLOSED、REFUNDING、REFUNDED',
  `time_expire`     DATETIME(3)           DEFAULT NULL                  COMMENT '订单失效时间',
  `success_time`    DATETIME(3)           DEFAULT NULL                  COMMENT '支付成功时间（微信回传）',
  `notify_url`      VARCHAR(512)          DEFAULT NULL                  COMMENT '回调地址',
  `attach`          VARCHAR(128)          DEFAULT NULL                  COMMENT '附加数据，原样回传',
  `ext`             JSON                  DEFAULT NULL                  COMMENT '扩展参数',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `gmt_modified`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0 未删 / 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_mch_status` (`mch_id`, `status`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务订单';

-- -----------------------------------------------------------
-- 2. t_pay_transaction · 微信支付交易流水
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pay_transaction` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `out_trade_no`        VARCHAR(64)  NOT NULL                              COMMENT '商户订单号',
  `transaction_id`      VARCHAR(64)           DEFAULT NULL                  COMMENT '微信支付订单号',
  `mch_id`              VARCHAR(32)  NOT NULL                              COMMENT '商户号',
  `pay_status`          VARCHAR(16)  NOT NULL DEFAULT 'NOTPAY'             COMMENT '支付状态：NOTPAY、SUCCESS、CLOSED、REVOKED、REFUNDED',
  `amount_payer_total`  BIGINT       NOT NULL                              COMMENT '用户实际支付金额（分，应收减去优惠）',
  `bank_type`           VARCHAR(32)           DEFAULT NULL                  COMMENT '付款银行',
  `success_time`        DATETIME(3)           DEFAULT NULL                  COMMENT '支付成功时间',
  `raw_response`        JSON                  DEFAULT NULL                  COMMENT '下单接口原始响应',
  `gmt_create`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `gmt_modified`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `is_deleted`          TINYINT      NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0 未删 / 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_id` (`transaction_id`),
  UNIQUE KEY `uk_out_trade_no` (`out_trade_no`),
  KEY `idx_mch_pay_status` (`mch_id`, `pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微信支付交易流水';

-- -----------------------------------------------------------
-- 3. t_pay_refund · 退款单
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pay_refund` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `out_refund_no`     VARCHAR(64)  NOT NULL                              COMMENT '商户退款单号',
  `out_trade_no`      VARCHAR(64)  NOT NULL                              COMMENT '原商户订单号',
  `transaction_id`    VARCHAR(64)           DEFAULT NULL                  COMMENT '微信支付订单号',
  `mch_id`            VARCHAR(32)  NOT NULL                              COMMENT '商户号',
  `refund_id`         VARCHAR(64)           DEFAULT NULL                  COMMENT '微信退款单号',
  `amount_refund`     BIGINT       NOT NULL                              COMMENT '退款金额（分）',
  `amount_total`      BIGINT       NOT NULL                              COMMENT '原订单金额（分）',
  `reason`            VARCHAR(255)          DEFAULT NULL                  COMMENT '退款原因',
  `refund_status`     VARCHAR(16)  NOT NULL DEFAULT 'PROCESSING'         COMMENT '退款状态：PROCESSING、SUCCESS、CLOSED、ABNORMAL',
  `notify_url`        VARCHAR(512)          DEFAULT NULL                  COMMENT '退款回调地址',
  `raw_response`      JSON                  DEFAULT NULL                  COMMENT '申请退款原始响应',
  `gmt_create`        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `gmt_modified`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `is_deleted`        TINYINT      NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0 未删 / 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_out_refund_no` (`out_refund_no`),
  KEY `idx_transaction_id` (`transaction_id`),
  KEY `idx_mch_refund_status` (`mch_id`, `refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款单';

-- -----------------------------------------------------------
-- 4. t_pay_notify_log · 回调原始报文（调试 + 重放）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pay_notify_log` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `notify_type`     VARCHAR(16)  NOT NULL                              COMMENT '回调类型：PAY 支付 / REFUND 退款',
  `out_trade_no`    VARCHAR(64)           DEFAULT NULL                  COMMENT '商户订单号',
  `out_refund_no`   VARCHAR(64)           DEFAULT NULL                  COMMENT '商户退款单号',
  `mch_id`          VARCHAR(32)  NOT NULL                              COMMENT '商户号',
  `headers`         JSON         NOT NULL                              COMMENT '请求头（含签名、时间戳、随机串）',
  `raw_body`        MEDIUMTEXT   NOT NULL                              COMMENT '请求体原文（验签前）',
  `decrypted_body`  JSON                  DEFAULT NULL                  COMMENT '解密后回调内容',
  `verify_result`   VARCHAR(16)           DEFAULT NULL                  COMMENT '签名校验：PASS 通过 / FAIL 失败',
  `process_result`  VARCHAR(16)           DEFAULT NULL                  COMMENT '处理结果：SUCCESS 成功 / FAILED 失败 / IGNORED 忽略',
  `error_message`   VARCHAR(1024)         DEFAULT NULL                  COMMENT '错误信息',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0 未删 / 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_out_trade_no` (`out_trade_no`),
  KEY `idx_out_refund_no` (`out_refund_no`),
  KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='回调原始报文';

-- -----------------------------------------------------------
-- 5. t_pay_idempotent · 幂等记录
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pay_idempotent` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `idempotent_key`  VARCHAR(128) NOT NULL                              COMMENT '幂等键，例如 out_trade_no:CREATE 或 out_refund_no:REFUND',
  `operation`       VARCHAR(32)  NOT NULL                              COMMENT '操作类型：CREATE_ORDER 创建订单 / REFUND 退款 / NOTIFY 回调',
  `result_code`     VARCHAR(16)  NOT NULL                              COMMENT '结果：SUCCESS 成功 / FAILED 失败',
  `result_body`     JSON                  DEFAULT NULL                  COMMENT '操作结果摘要（用于重放）',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `gmt_modified`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent_key` (`idempotent_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='幂等记录';

-- -----------------------------------------------------------
-- 6. t_merchant_config · 商户配置（多商户预留，v1.0 默认 1 条）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_merchant_config` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `mch_id`                VARCHAR(32)  NOT NULL                              COMMENT '商户号',
  `app_id`                VARCHAR(32)  NOT NULL                              COMMENT '公众号或小程序 AppID',
  `merchant_name`         VARCHAR(128)          DEFAULT NULL                  COMMENT '商户名称',
  `mode`                  VARCHAR(16)  NOT NULL DEFAULT 'DIRECT'            COMMENT '模式：DIRECT 直连商户 / PARTNER 服务商',
  `api_v3_key`            VARCHAR(64)  NOT NULL                              COMMENT 'V3 密钥（32 位，用于回调解密）',
  `cert_serial_no`        VARCHAR(64)  NOT NULL                              COMMENT '商户证书序列号',
  `cert_private_key_path` VARCHAR(512) NOT NULL                              COMMENT '商户私钥 PEM 路径',
  `notify_url_base`       VARCHAR(512)          DEFAULT NULL                  COMMENT '回调地址前缀',
  `v2_key`                VARCHAR(64)           DEFAULT NULL                  COMMENT 'V2 密钥（v2.1 付款码启用）',
  `enabled`               TINYINT      NOT NULL DEFAULT 1                    COMMENT '是否启用：0 禁用 / 1 启用',
  `ext`                   JSON                  DEFAULT NULL                  COMMENT '扩展配置',
  `gmt_create`            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `gmt_modified`          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `is_deleted`            TINYINT      NOT NULL DEFAULT 0                    COMMENT '逻辑删除：0 未删 / 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mch_id` (`mch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户配置';

-- -----------------------------------------------------------
-- 7. t_platform_cert · 微信平台证书缓存
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_platform_cert` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `mch_id`          VARCHAR(32)  NOT NULL                              COMMENT '商户号',
  `serial_no`       VARCHAR(64)  NOT NULL                              COMMENT '平台证书序列号',
  `effective_time`  DATETIME(3)           DEFAULT NULL                  COMMENT '生效时间',
  `expire_time`     DATETIME(3)           DEFAULT NULL                  COMMENT '过期时间',
  `public_key`      MEDIUMTEXT   NOT NULL                              COMMENT '平台公钥 PEM',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `gmt_modified`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mch_serial` (`mch_id`, `serial_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='微信平台证书缓存';

-- -----------------------------------------------------------
-- 8. t_pay_reconciliation · 对账记录（v1.1 启用）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `t_pay_reconciliation` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
  `mch_id`          VARCHAR(32)  NOT NULL                              COMMENT '商户号',
  `bill_date`       DATE         NOT NULL                              COMMENT '账单日期',
  `bill_type`       VARCHAR(16)  NOT NULL                              COMMENT '账单类型：ALL 全部 / SUCCESS 支付 / REFUND 退款',
  `total_count`     INT          NOT NULL DEFAULT 0                    COMMENT '总笔数',
  `total_amount`    BIGINT       NOT NULL DEFAULT 0                    COMMENT '总金额（分）',
  `diff_count`      INT          NOT NULL DEFAULT 0                    COMMENT '差异笔数',
  `diff_amount`     BIGINT       NOT NULL DEFAULT 0                    COMMENT '差异金额（分）',
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING'            COMMENT '处理状态：PENDING 待处理 / PROCESSING 处理中 / SUCCESS 成功 / FAILED 失败',
  `local_file_path` VARCHAR(512)          DEFAULT NULL                  COMMENT '本地账单文件路径',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `gmt_modified`    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mch_bill_date_type` (`mch_id`, `bill_date`, `bill_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对账记录';

-- -----------------------------------------------------------
-- 种子数据：默认商户配置占位（真实值由环境变量注入）
-- -----------------------------------------------------------
INSERT INTO `t_merchant_config`
  (`mch_id`, `app_id`, `merchant_name`, `mode`, `api_v3_key`, `cert_serial_no`, `cert_private_key_path`, `notify_url_base`, `enabled`)
VALUES
  ('PLACEHOLDER_MCH_ID', 'PLACEHOLDER_APP_ID', '默认直连商户', 'DIRECT',
   'PLACEHOLDER_32_CHARS_API_V3_KEY________', 'PLACEHOLDER_CERT_SERIAL',
   'certs/apiclient_key.pem', 'https://your-domain.com', 1)
ON DUPLICATE KEY UPDATE `gmt_modified` = CURRENT_TIMESTAMP(3);

-- -----------------------------------------------------------
-- 验证脚本：执行后必须返回 0 行（无乱码）
-- -----------------------------------------------------------
SELECT '乱码自检：返回 0 行即为 OK' AS check_message;
SELECT COLUMN_NAME, COLUMN_COMMENT, TABLE_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = 'weichat_finance'
   AND (COLUMN_COMMENT LIKE '%?%' OR COLUMN_COMMENT LIKE CONCAT('%', CHAR(0x3F), '%'));
