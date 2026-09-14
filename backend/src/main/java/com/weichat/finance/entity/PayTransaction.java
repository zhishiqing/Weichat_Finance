package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信支付交易流水表实体。
 *
 * <p>对应表 t_pay_transaction。</p>
 *
 * <p>一笔业务订单对应一条交易流水。下单成功后写一条（pay_status=NOTPAY），回调成功后更新（pay_status=SUCCESS）。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_transaction")
public class PayTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String outTradeNo;
    private String transactionId;
    private String mchId;
    private String payStatus;
    private Long amountPayerTotal;
    private String bankType;
    private LocalDateTime successTime;
    private String rawResponse;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;
}
