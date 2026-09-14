package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.NotifyResult;
import com.weichat.finance.entity.enums.NotifyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信支付回调原始报文表实体。
 *
 * <p>对应表 t_pay_notify_log。微信支付回调先落库，再处理；用于排障 + 重放。</p>
 *
 * <p>每条记录保存：原始 headers（用于验签）、原始 body（用于解签 / 排障）、处理结果、错误信息。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_notify_log")
@Schema(description = "微信支付回调原始报文表（t_pay_notify_log）")
public class PayNotifyLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "回调类型",
        example = NotifyType.PAY,
        allowableValues = {NotifyType.PAY, NotifyType.REFUND})
    private String notifyType;

    @Schema(description = "商户订单号（PAY 回调）", example = "ORDER_20260914_001")
    private String outTradeNo;

    @Schema(description = "商户退款单号（REFUND 回调）", example = "REFUND_20260914_001")
    private String outRefundNo;

    @Schema(description = "商户号", example = "1900000109")
    private String mchId;

    @Schema(description = "请求头（含 Wechatpay-Signature / Timestamp / Nonce / Serial，JSON 格式存储）",
        example = "{\"Wechatpay-Signature\":\"abc...\"}")
    private String headers;

    @Schema(description = "请求体原文（验签前，用于排障 + 重新验签）",
        example = "{\"id\":\"event_id\",\"resource\":{\"ciphertext\":\"...\"}}")
    private String rawBody;

    @Schema(description = "解密后回调内容（resource.ciphertext 解密后的 JSON）",
        example = "{\"out_trade_no\":\"ORDER_...\",\"transaction_id\":\"420...\"}")
    private String decryptedBody;

    @Schema(description = "签名校验结果",
        example = NotifyResult.PASS,
        allowableValues = {
            NotifyResult.PASS, NotifyResult.FAIL, NotifyResult.SKIPPED_PHASE3})
    private String verifyResult;

    @Schema(description = "业务处理结果",
        example = NotifyResult.SUCCESS,
        allowableValues = {
            NotifyResult.SUCCESS, NotifyResult.FAILED,
            NotifyResult.IGNORED, NotifyResult.IGNORED_PHASE3})
    private String processResult;

    @Schema(description = "错误信息（验签失败 / 业务处理异常等，最长 1024 字符）",
        example = "签名校验失败",
        maxLength = 1024)
    private String errorMessage;

    @Schema(description = "创建时间（MyBatis-Plus 自动填充）", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @Schema(description = "逻辑删除标记",
        example = "0",
        allowableValues = {"0", "1"},
        defaultValue = "0",
        accessMode = Schema.AccessMode.READ_ONLY)
    @TableLogic
    private Integer isDeleted;
}
