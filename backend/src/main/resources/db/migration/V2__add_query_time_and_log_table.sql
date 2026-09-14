-- ============================================================
-- V2 · 定时查单兜底字段 + 调度日志表
-- 时间：2026-09-14
-- 功能：
--   1. t_pay_order.last_query_time：记录最近一次定时查单时间，
--      避免在同一个调度周期内重复查同一笔单（防止并发重复查）
--   2. t_pay_order_query_log：查单执行日志（批次/笔数/耗时/异常统计）
-- ============================================================

-- 1. last_query_time 字段
ALTER TABLE `t_pay_order`
  ADD COLUMN `last_query_time` DATETIME(3) DEFAULT NULL COMMENT '最近一次定时查单时间（兜底用）'
  AFTER `ext`;

-- 2. 查单执行日志表（记录每个调度批次的统计信息）
CREATE TABLE IF NOT EXISTS `t_pay_order_query_log` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no`         VARCHAR(64)  NOT NULL                COMMENT '本批次唯一标识（时间戳+随机）',
  `mch_id`          VARCHAR(32)           DEFAULT NULL    COMMENT '商户号（空=全部商户）',
  `scan_start_time`  DATETIME(3)  NOT NULL                COMMENT '本批次扫描开始时间',
  `scan_end_time`    DATETIME(3)           DEFAULT NULL    COMMENT '本批次扫描结束时间',
  `scan_order_count` INT          NOT NULL DEFAULT 0      COMMENT '本次扫描出的悬挂单数量',
  `query_order_count` INT         NOT NULL DEFAULT 0      COMMENT '本次实际调用查单接口次数',
  `success_count`    INT          NOT NULL DEFAULT 0      COMMENT '查单成功次数',
  `fail_count`       INT          NOT NULL DEFAULT 0      COMMENT '查单失败次数',
  `notpay_count`     INT          NOT NULL DEFAULT 0      COMMENT '仍为 NOTPAY（未变化）',
  `closed_count`     INT          NOT NULL DEFAULT 0      COMMENT '状态变为 CLOSED',
  `updated_count`    INT          NOT NULL DEFAULT 0      COMMENT '状态发生变化（兜底生效）',
  `cost_ms`          BIGINT       NOT NULL DEFAULT 0      COMMENT '本批次总耗时（毫秒）',
  `error_message`    VARCHAR(1024)          DEFAULT NULL    COMMENT '本批次异常摘要（最严重的一条）',
  `gmt_create`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_mch_id` (`mch_id`),
  KEY `idx_scan_start_time` (`scan_start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时查单执行日志';
