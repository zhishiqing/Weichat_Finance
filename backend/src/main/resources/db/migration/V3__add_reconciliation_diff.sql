-- ============================================================
-- V3 · 对账差异明细表（t_pay_reconciliation_diff）
-- 时间：2026-09-15
-- 背景：
--   t_pay_reconciliation 已在 V1 中创建，只存汇总（笔数/金额）
--   v1.1 启用对账后，需要明细表记录每笔差异，便于人工核对
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_pay_reconciliation_diff` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '主键',
  `reconciliation_id` BIGINT     NOT NULL                                COMMENT '对账汇总 ID（关联 t_pay_reconciliation.id）',
  `mch_id`          VARCHAR(32)  NOT NULL                                COMMENT '商户号',
  `bill_date`       DATE         NOT NULL                                COMMENT '账单日期',
  `out_trade_no`    VARCHAR(64)           DEFAULT NULL                    COMMENT '商户订单号',
  `transaction_id`  VARCHAR(64)           DEFAULT NULL                    COMMENT '微信支付订单号',
  `diff_type`       VARCHAR(16)  NOT NULL                                COMMENT '差异类型：LOCAL_ONLY 本地有微信无 / WECHAT_ONLY 微信有本地无 / AMOUNT_DIFF 金额不一致 / STATUS_DIFF 状态不一致',
  `local_amount`    BIGINT                DEFAULT NULL                    COMMENT '本地订单金额（分）',
  `wechat_amount`   BIGINT                DEFAULT NULL                    COMMENT '微信侧订单金额（分）',
  `local_status`    VARCHAR(16)           DEFAULT NULL                    COMMENT '本地订单状态',
  `wechat_status`   VARCHAR(16)           DEFAULT NULL                    COMMENT '微信侧订单状态',
  `diff_amount`     BIGINT       NOT NULL DEFAULT 0                      COMMENT '差异金额（分，微信 - 本地）',
  `handle_status`   VARCHAR(16)  NOT NULL DEFAULT 'PENDING'              COMMENT '处理状态：PENDING 待处理 / IGNORED 忽略 / FIXED 已修复',
  `handle_remark`   VARCHAR(512)          DEFAULT NULL                    COMMENT '处理备注',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)   COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_reconciliation_id` (`reconciliation_id`),
  KEY `idx_mch_bill_date` (`mch_id`, `bill_date`),
  KEY `idx_out_trade_no` (`out_trade_no`),
  KEY `idx_handle_status` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对账差异明细';
