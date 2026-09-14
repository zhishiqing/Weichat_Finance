package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.PayStatus;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信支付交易流水表实体。
 *
 * <p>对应表 t_pay_transaction。一笔业务订单对应一条交易流水。</p>
 *
 * <p>下单成功后写一条（pay_status=NOTPAY），回调成功后更新（pay_status=SUCCESS）。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_transaction")
public class PayTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商户订单号（与 t_pay_order.out_trade_no 一致） */
    private String outTradeNo;

    /** 微信支付订单号（SUCCESS 后才有，唯一） */
    private String transactionId;

    /** 商户号 */
    private String mchId;

    /**
     * 支付状态（来自微信侧）。
     *
     * @see PayStatus#NOTPAY 未支付
     * @see PayStatus#SUCCESS 支付成功
     * @see PayStatus#CLOSED 已关闭
     * @see PayStatus#REVOKED 已撤销（付款码专用）
     * @see PayStatus#REFUNDED 已全额退款
     */
    private String payStatus;

    /** 用户实际支付金额（单位：分，应收减去优惠） */
    private Long amountPayerTotal;

    /** 付款银行类型（如 CMC、ICBC 等，SUCCESS 后由微信回传） */
    private String bankType;

    /** 支付成功时间（微信回传） */
    private LocalDateTime successTime;

    /** 下单接口原始响应（JSON 格式，含 prepay_id 或 code_url，便于排障） */
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
