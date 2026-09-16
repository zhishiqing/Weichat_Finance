package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.PayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "微信支付交易流水表（t_pay_transaction）")
public class PayTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商户订单号（与 t_pay_order.out_trade_no 一致）", example = "ORDER_20260914_001")
    private String outTradeNo;

    @Schema(description = "微信支付订单号（SUCCESS 后才有，唯一）", example = "4200001234202309156060123456789")
    private String transactionId;

    @Schema(description = "商户号", example = "1900000109")
    private String mchId;

    @Schema(description = "服务商号（PARTNER 模式必填，DIRECT 模式为空）",
        example = "1000400645", nullable = true)
    private String parentMchId;

    @Schema(description = "特约商户号（PARTNER 模式必填，DIRECT 模式等于 mch_id）",
        example = "1900000109", nullable = true)
    private String subMchId;

    @Schema(description = "特约商户 AppID（PARTNER 模式必填，DIRECT 模式等于 app_id）",
        example = "wx_sub_appid", nullable = true)
    private String subAppId;

    @Schema(description = "支付状态（来自微信侧）",
        example = PayStatus.SUCCESS,
        allowableValues = {
            PayStatus.NOTPAY, PayStatus.SUCCESS,
            PayStatus.CLOSED, PayStatus.REVOKED, PayStatus.REFUNDED})
    private String payStatus;

    @Schema(description = "用户实际支付金额（单位：分，应收减去优惠）", example = "100")
    private Long amountPayerTotal;

    @Schema(description = "付款银行类型（如 CMC、ICBC 等，SUCCESS 后由微信回传）", example = "CMC")
    private String bankType;

    @Schema(description = "支付成功时间（微信回传）", example = "2026-09-14T12:00:00")
    private LocalDateTime successTime;

    @Schema(description = "下单接口原始响应（JSON 格式，含 prepay_id 或 code_url，便于排障）",
        example = "{\"prepay_id\":\"wx2014102720093954e6e7d1a01234567\"}")
    private String rawResponse;

    @Schema(description = "创建时间（MyBatis-Plus 自动填充）", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @Schema(description = "更新时间（MyBatis-Plus 自动填充）", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @Schema(description = "逻辑删除标记",
        example = "0",
        allowableValues = {"0", "1"},
        defaultValue = "0",
        accessMode = Schema.AccessMode.READ_ONLY)
    @TableLogic
    private Integer isDeleted;
}
