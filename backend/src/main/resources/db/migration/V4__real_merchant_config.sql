-- ============================================================
-- V4 · 占位商户配置 → 真实直连商户 1674723182
-- 时间：2026-09-16
-- 背景：
--   v1.0~v1.3 都是占位（PLACEHOLDER_*）
--   现在有真实商户凭证，更新为 1674723182 / wx9d066e071d8e4e33 / 等
-- ============================================================

UPDATE `t_merchant_config`
   SET `mch_id`              = '1674723182',
       `app_id`              = 'wx9d066e071d8e4e33',
       `merchant_name`       = '直连商户 1674723182',
       `mode`                = 'DIRECT',
       `api_v3_key`          = 'wwx9d066e071X8687d3414bbfc7ad2fX',
       `cert_serial_no`      = '6F140A8E7DF3BD76892D7FDED0E670AB2B8AD4A3',
       `cert_private_key_path` = 'certs/apiclient_key.pem',
       `notify_url_base`     = 'http://localhost:8080/api',
       `enabled`             = 1,
       `gmt_modified`        = CURRENT_TIMESTAMP(3)
 WHERE `mch_id` = 'PLACEHOLDER_MCH_ID';

-- 如果上一步未命中（占位已被删/改），兜底插入真实商户
INSERT INTO `t_merchant_config`
  (`mch_id`, `app_id`, `merchant_name`, `mode`, `api_v3_key`, `cert_serial_no`, `cert_private_key_path`, `notify_url_base`, `enabled`)
VALUES
  ('1674723182', 'wx9d066e071d8e4e33', '直连商户 1674723182', 'DIRECT',
   'wwx9d066e071X8687d3414bbfc7ad2fX', '6F140A8E7DF3BD76892D7FDED0E670AB2B8AD4A3',
   'certs/apiclient_key.pem', 'http://localhost:8080/api', 1)
ON DUPLICATE KEY UPDATE `gmt_modified` = CURRENT_TIMESTAMP(3);
