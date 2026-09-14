package com.weichat.finance.payment.v3.refund;

import java.time.LocalDateTime;

/**
 * 退款申请响应 DTO。
 *
 * @author panhw
 * @since 2026-09-14
 */
public class RefundCreateResponse {

    private String outRefundNo;
    private String refundId;
    private String refundStatus;
    private LocalDateTime createdAt;
    private String source;

    public String getOutRefundNo() { return outRefundNo; }
    public void setOutRefundNo(String outRefundNo) { this.outRefundNo = outRefundNo; }
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
