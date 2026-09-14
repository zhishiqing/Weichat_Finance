package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款单表实体。
 *
 * <p>对应表 t_pay_refund。记录所有退款请求与结果。</p>
 *
 * <p>资金安全：累计退款金额不能超过原订单金额（由 {@code RefundController#createRefund} 强制校验）。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_refund")
@Schema(description = "退款单表（t_pay_refund）")
public class PayRefund implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商户退款单号（业务侧生成，唯一）", example = "REFUND_20260914_001", maxLength = 64)
    private String outRefundNo;

    @Schema(description = "原商户订单号（关联 t_pay_order.out_trade_no）", example = "ORDER_20260914_001", maxLength = 64)
    private String outTradeNo;

    @Schema(description = "微信支付订单号（关联 t_pay_transaction.transaction_id，可选）",
        example = "4200001234202309156060123456789")
    private String transactionId;

    @Schema(description = "商户号", example = "1900000109")
    private String mchId;

    @Schema(description = "微信退款单号（SUCCESS 后才有）",
        example = "50300308202609140001234567890")
    private String refundId;

    @Schema(description = "退款金额（单位：分）", example = "30")
    private Long amountRefund;

    @Schema(description = "原订单金额（单位：分，便于审计追溯）", example = "100")
    private Long amountTotal;

    @Schema(description = "退款原因（最长 255 字符）", example = "用户主动申请退款", maxLength = 255)
    private String reason;

    @Schema(description = "退款状态",
        example = RefundStatus.PROCESSING,
        allowableValues = {
            RefundStatus.PROCESSING, RefundStatus.SUCCESS,
            RefundStatus.CLOSED, RefundStatus.ABNORMAL})
    private String refundStatus;

    @Schema(description = "退款回调地址", example = "https://api.example.com/api/notify/v3/refund/success")
    private String notifyUrl;

    @Schema(description = "申请退款接口原始响应（JSON 格式，含 refund_id 等，便于排障）",
        example = "{\"refund_id\":\"50300308202609140001234567890\"}")
    private String rawResponse;

    @Schema(description = "创建时间（MyBatis-Plus 自动填充）", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @Schema(description = "更新时间（MyBatis-Plus 自动填充）", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @Schema(description = "逻辑删除标记",
        example = "0",
        allowableValues = {"0", "1"},
        defaultValue = "0",
        accessMode = Schema.AccessMode.READ_ONLY)
    @TableLogic
    private Integer isDeleted;
}
