package com.weichat.finance.payment.v3.refund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退款申请响应 DTO。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@Schema(description = "退款申请响应")
public class RefundCreateResponse {

    @Schema(description = "商户退款单号", example = "REFUND_20260914_001")
    private String outRefundNo;

    @Schema(description = "微信退款单号（退款成功后才有）",
        example = "50300308202609140001234567890")
    private String refundId;

    @Schema(description = "退款状态",
        example = "PROCESSING",
        allowableValues = {"PROCESSING", "SUCCESS", "CLOSED", "ABNORMAL"})
    private String refundStatus;

    @Schema(description = "退款受理时间")
    private LocalDateTime createdAt;

    @Schema(description = "数据来源（MOCK / REAL）", example = "MOCK", allowableValues = {"MOCK", "REAL"})
    private String source;
}
