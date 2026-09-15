package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账差异明细表实体。
 *
 * <p>对应表 t_pay_reconciliation_diff。记录每笔差异订单的明细，
 * 便于对账人员快速定位并人工处理。</p>
 *
 * <p>差异类型：</p>
 * <ul>
 *   <li>{@code LOCAL_ONLY}：本地有，微信没有（疑似本地误下单）</li>
 *   <li>{@code WECHAT_ONLY}：微信有，本地没有（疑似回调丢失，需补单）</li>
 *   <li>{@code AMOUNT_DIFF}：双方都有但金额不一致（极端情况，需核对）</li>
 *   <li>{@code STATUS_DIFF}：双方都有但状态不一致（如微信 SUCCESS 本地 CREATED）</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-15
 */
@Data
@TableName("t_pay_reconciliation_diff")
@Schema(description = "对账差异明细表（t_pay_reconciliation_diff）")
public class ReconciliationDiff implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "对账汇总 ID（关联 t_pay_reconciliation.id）", example = "1")
    private Long reconciliationId;

    @Schema(description = "商户号", example = "1900000109")
    private String mchId;

    @Schema(description = "账单日期", example = "2026-09-14")
    private LocalDate billDate;

    @Schema(description = "商户订单号", example = "ORDER_20260914_001")
    private String outTradeNo;

    @Schema(description = "微信支付订单号", example = "4200001234202309156060123456789")
    private String transactionId;

    @Schema(description = "差异类型", example = "WECHAT_ONLY",
        allowableValues = {"LOCAL_ONLY", "WECHAT_ONLY", "AMOUNT_DIFF", "STATUS_DIFF"})
    private String diffType;

    @Schema(description = "本地订单金额（分）", example = "100")
    private Long localAmount;

    @Schema(description = "微信侧订单金额（分）", example = "100")
    private Long wechatAmount;

    @Schema(description = "本地订单状态", example = "SUCCESS")
    private String localStatus;

    @Schema(description = "微信侧订单状态", example = "SUCCESS")
    private String wechatStatus;

    @Schema(description = "差异金额（分，微信 - 本地）", example = "0")
    private Long diffAmount;

    @Schema(description = "处理状态",
        example = "PENDING",
        allowableValues = {"PENDING", "IGNORED", "FIXED"})
    private String handleStatus;

    @Schema(description = "处理备注", example = "已查微信后台确认")
    private String handleRemark;

    @Schema(description = "创建时间（MyBatis-Plus 自动填充）", example = "2026-09-15T03:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
}
