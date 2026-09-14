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
 * 回调原始报文表实体。
 *
 * <p>对应表 t_pay_notify_log。微信支付回调先落库，再处理；用于排查 + 重放。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_notify_log")
public class PayNotifyLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String notifyType;
    private String outTradeNo;
    private String outRefundNo;
    private String mchId;
    private String headers;
    private String rawBody;
    private String decryptedBody;
    private String verifyResult;
    private String processResult;
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableLogic
    private Integer isDeleted;
}
