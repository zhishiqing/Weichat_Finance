-- ============================================================
-- V5 · 增加 next_query_at 字段（拉起支付后主动轮询查单）
-- 时间：2026-09-16
-- 背景：
--   回调地址无法配置，改为"拉起支付后主动调用查单接口轮询"
--   需要 next_query_at 字段表示"下次应被轮询的时间"
-- ============================================================

ALTER TABLE `t_pay_order`
  ADD COLUMN `next_query_at` DATETIME(3) NULL COMMENT '轮询查单时间（<= NOW() 的订单将被扫描）' AFTER `last_query_time`;

-- 加索引：调度器按 next_query_at 扫描高频
CREATE INDEX `idx_pay_order_next_query_at` ON `t_pay_order` (`next_query_at`);
