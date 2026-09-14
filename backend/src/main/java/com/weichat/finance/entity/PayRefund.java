package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.RefundStatus;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款单表实体。
 *
 * <p>对应表 t_pay_refund。记录所有退款请求与结果。</p>
 *
 * <p>🔴 资金安全：累计退款金额不能超过原订单金额（由 {@code RefundController#createRefund} 强制校验）。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_refund")
public class PayRefund implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商户退款单号（业务侧生成，唯一） */
    private String outRefundNo;

    /** 原商户订单号（关联 t_pay_order.out_trade_no） */
    private String outTradeNo;

    /** 微信支付订单号（关联 t_pay_transaction.transaction_id，可选） */
    private String transactionId;

    /** 商户号 */
    private String mchId;

    /** 微信退款单号（SUCCESS 后才有） */
    private String refundId;

    /** 退款金额（单位：分） */
    private Long amountRefund;

    /** 原订单金额（单位：分，便于审计追溯） */
    private Long amountTotal;

    /** 退款原因（最长 255 字符） */
    private String reason;

    /**
     * 退款状态。
     *
     * @see RefundStatus#PROCESSING 退款中
     * @see RefundStatus#SUCCESS 退款成功
     * @see RefundStatus#CLOSED 退款关闭
     * @see RefundStatus#ABNORMAL 退款异常
     */
    private String refundStatus;

    /** 退款回调地址 */
    private String notifyUrl;

    /** 申请退款接口原始响应（JSON 格式，含 refund_id 等，便于排障） */
    private String rawResponse;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /**
     * 逻辑删除标记。
     *
     * @see CommonFlag#NOT_DELETED 未删
     * @see CommonFlag#DELETED 已删
     */
    @TableLogic
    private Integer isDeleted;
}
