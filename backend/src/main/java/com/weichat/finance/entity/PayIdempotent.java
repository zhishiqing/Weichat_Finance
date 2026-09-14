package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.IdempotentOperation;
import com.weichat.finance.entity.enums.IdempotentResult;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "幂等记录表（t_pay_idempotent）")
public class PayIdempotent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "幂等键（业务侧拼装：例如 out_trade_no:CREATE_ORDER）",
        example = "ORDER_20260914_001:CREATE_ORDER")
    private String idempotentKey;

    @Schema(description = "操作类型",
        example = IdempotentOperation.CREATE_ORDER,
        allowableValues = {
            IdempotentOperation.CREATE_ORDER,
            IdempotentOperation.REFUND,
            IdempotentOperation.NOTIFY})
    private String operation;

    @Schema(description = "结果码",
        example = IdempotentResult.SUCCESS,
        allowableValues = {
            IdempotentResult.PROCESSING,
            IdempotentResult.SUCCESS,
            IdempotentResult.FAILED})
    private String resultCode;

    @Schema(description = "操作结果摘要（JSON 格式，用于重放，避免重新调用下游）",
        example = "{\"prepayId\":\"wx...\"}")
    private String resultBody;

    @Schema(description = "创建时间（MyBatis-Plus 自动填充）", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @Schema(description = "更新时间（MyBatis-Plus 自动填充）", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
