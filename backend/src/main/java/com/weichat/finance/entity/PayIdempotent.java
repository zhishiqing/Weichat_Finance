package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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
 *   <li>{@code out_trade_no:CREATE} · 下单幂等</li>
 *   <li>{@code out_refund_no:REFUND} · 退款幂等</li>
 *   <li>{@code out_trade_no:NOTIFY:nonce} · 回调去重</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@TableName("t_pay_idempotent")
public class PayIdempotent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String idempotentKey;
    private String operation;
    private String resultCode;
    private String resultBody;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdempotentKey() { return idempotentKey; }
    public void setIdempotentKey(String idempotentKey) { this.idempotentKey = idempotentKey; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getResultBody() { return resultBody; }
    public void setResultBody(String resultBody) { this.resultBody = resultBody; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
}
