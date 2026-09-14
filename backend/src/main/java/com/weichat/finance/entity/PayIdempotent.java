package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.IdempotentOperation;
import com.weichat.finance.entity.enums.IdempotentResult;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 幂等记录表实体。
 *
 * <p>对应表 t_pay_idempotent。用于防止重复创建订单 / 重复退款 / 重复回调。</p>
 *
 * <p>幂等键示例：</p>
 * <ul>
 *   <li>{@code out_trade_no:CREATE_ORDER} · 下单幂等</li>
 *   <li>{@code out_refund_no:REFUND} · 退款幂等</li>
 *   <li>{@code out_trade_no:NOTIFY:nonce} · 回调去重</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_idempotent")
public class PayIdempotent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 幂等键（业务侧拼装：例如 {@code out_trade_no:CREATE_ORDER}） */
    private String idempotentKey;

    /**
     * 操作类型。
     *
     * @see IdempotentOperation#CREATE_ORDER 创建订单
     * @see IdempotentOperation#REFUND 退款
     * @see IdempotentOperation#NOTIFY 回调
     */
    private String operation;

    /**
     * 结果码。
     *
     * @see IdempotentResult#PROCESSING 处理中
     * @see IdempotentResult#SUCCESS 成功
     * @see IdempotentResult#FAILED 失败
     */
    private String resultCode;

    /** 操作结果摘要（JSON 格式，用于重放，避免重新调用下游） */
    private String resultBody;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
