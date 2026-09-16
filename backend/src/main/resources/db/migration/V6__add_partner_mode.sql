-- ============================================================
-- V6 · 服务商模式（PARTNER）架构扩展
-- 时间：2026-09-16
-- 背景：
--   v2.0 引入服务商模式。当前仅 v1.6 直连商户（DIRECT）。
--   服务商模式关键差异：
--     - Config 用服务商号 + 服务商私钥 + 服务商证书序列号（不是子商户）
--     - 业务请求体传 sub_mch_id + sub_appid（特约商户）
--     - 回调验签用服务商的 Config（因为是服务商签名）
-- ============================================================

-- -----------------------------------------------------------
-- 1. t_merchant_config：服务商字段
-- -----------------------------------------------------------

-- parent_mch_id：服务商号（PARTNER 模式必填，DIRECT 模式为空）
-- 表示当前商户配置是某个服务商下的特约商户
ALTER TABLE `t_merchant_config`
  ADD COLUMN `parent_mch_id` VARCHAR(32) NULL COMMENT '服务商号（PARTNER 模式必填，DIRECT 模式为空）' AFTER `mode`;

-- sub_app_id：特约商户 AppID（PARTNER 模式必填）
-- 与顶层 app_id 字段的区别：
--   - app_id：服务商 AppID（用于服务商自身的 Config）
--   - sub_app_id：特约商户 AppID（用于业务请求体）
ALTER TABLE `t_merchant_config`
  ADD COLUMN `sub_app_id` VARCHAR(32) NULL COMMENT '特约商户 AppID（PARTNER 模式必填）' AFTER `parent_mch_id`;

-- 索引：服务商查询子商户列表
CREATE INDEX `idx_parent_mch_id` ON `t_merchant_config` (`parent_mch_id`);

-- -----------------------------------------------------------
-- 2. t_pay_order：业务订单加服务商信息
-- -----------------------------------------------------------
ALTER TABLE `t_pay_order`
  ADD COLUMN `parent_mch_id` VARCHAR(32) NULL COMMENT '服务商号（PARTNER 模式必填）' AFTER `mch_id`,
  ADD COLUMN `sub_mch_id`    VARCHAR(32) NULL COMMENT '特约商户号（PARTNER 模式必填，DIRECT 模式等于 mch_id）' AFTER `parent_mch_id`,
  ADD COLUMN `sub_app_id`     VARCHAR(32) NULL COMMENT '特约商户 AppID（PARTNER 模式必填）' AFTER `sub_mch_id`;

CREATE INDEX `idx_sub_mch_status` ON `t_pay_order` (`sub_mch_id`, `status`);
CREATE INDEX `idx_parent_sub_mch` ON `t_pay_order` (`parent_mch_id`, `sub_mch_id`);

-- -----------------------------------------------------------
-- 3. t_pay_transaction：交易流水加服务商信息
-- -----------------------------------------------------------
ALTER TABLE `t_pay_transaction`
  ADD COLUMN `parent_mch_id` VARCHAR(32) NULL COMMENT '服务商号（PARTNER 模式必填）' AFTER `mch_id`,
  ADD COLUMN `sub_mch_id`    VARCHAR(32) NULL COMMENT '特约商户号（PARTNER 模式必填）' AFTER `parent_mch_id`,
  ADD COLUMN `sub_app_id`     VARCHAR(32) NULL COMMENT '特约商户 AppID（PARTNER 模式必填）' AFTER `sub_mch_id`;

-- -----------------------------------------------------------
-- 4. t_pay_refund：退款单加服务商信息
-- -----------------------------------------------------------
ALTER TABLE `t_pay_refund`
  ADD COLUMN `parent_mch_id` VARCHAR(32) NULL COMMENT '服务商号（PARTNER 模式必填）' AFTER `mch_id`,
  ADD COLUMN `sub_mch_id`    VARCHAR(32) NULL COMMENT '特约商户号（PARTNER 模式必填）' AFTER `parent_mch_id`,
  ADD COLUMN `sub_app_id`     VARCHAR(32) NULL COMMENT '特约商户 AppID（PARTNER 模式必填）' AFTER `sub_mch_id`;

-- -----------------------------------------------------------
-- 5. t_pay_notify_log：回调日志加服务商信息（用于路由验签 Config）
-- -----------------------------------------------------------
ALTER TABLE `t_pay_notify_log`
  ADD COLUMN `parent_mch_id` VARCHAR(32) NULL COMMENT '服务商号（PARTNER 模式必填）' AFTER `mch_id`,
  ADD COLUMN `sub_mch_id`    VARCHAR(32) NULL COMMENT '特约商户号（PARTNER 模式必填）' AFTER `parent_mch_id`;
