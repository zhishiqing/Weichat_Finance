package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.OrderStatus;
import com.weichat.finance.entity.enums.ProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务订单表实体。
 *
 * <p>对应表 t_pay_order。一笔业务订单对应一条记录，是系统的核心单据。</p>
 *
 * <p>v1.0：业务订单 = 支付订单（金额从前端入参）。v2.0 计划拆出独立的 {@code t_business_order} 表。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_pay_order")
@Schema(description = "业务订单表（t_pay_order）")
public class PayOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商户订单号（业务侧生成，唯一）", example = "ORDER_20260914_001", maxLength = 32)
    private String outTradeNo;

    @Schema(description = "商户号", example = "1900000109", maxLength = 32)
    private String mchId;

    @Schema(description = "公众号或小程序 AppID", example = "wx8888888888888888", maxLength = 32)
    private String appId;

    @Schema(description = "订单描述（最长 127 字符）", example = "商品名称", maxLength = 127)
    private String description;

    @Schema(description = "订单金额（单位：分，必须为正整数，避免浮点精度问题）", example = "100")
    private Long amountTotal;

    @Schema(description = "货币类型，默认 CNY", example = "CNY", maxLength = 16, defaultValue = "CNY")
    private String currency;

    @Schema(description = "用户标识（JSAPI 必填，Native 为空）", example = "oUpF8uMuAJxxyfBWk2e3tR3R6_T4", maxLength = 128)
    private String openid;

    @Schema(description = "支付产品类型",
        example = ProductType.JSAPI,
        allowableValues = {ProductType.JSAPI, ProductType.NATIVE})
    private String productType;

    @Schema(description = "订单状态",
        example = OrderStatus.SUCCESS,
        allowableValues = {
            OrderStatus.SUBMITTING, OrderStatus.CREATED, OrderStatus.SUCCESS,
            OrderStatus.CLOSED, OrderStatus.REFUNDING, OrderStatus.REFUNDED})
    private String status;

    @Schema(description = "订单失效时间（未支付则到期自动关闭）", example = "2026-12-31T23:59:59")
    private LocalDateTime timeExpire;

    @Schema(description = "支付成功时间（微信回传，状态变为 SUCCESS 时填充）", example = "2026-09-14T12:00:00")
    private LocalDateTime successTime;

    @Schema(description = "回调地址（商户配置 notifyUrlBase + 业务路径拼接）", example = "https://api.example.com/api/notify/v3/pay/success")
    private String notifyUrl;

    @Schema(description = "附加数据（最长 128 字符，原样回传）", example = "{\"k\":\"v\"}", maxLength = 128)
    private String attach;

    @Schema(description = "扩展参数（JSON 格式，预留字段）", example = "{\"k\":\"v\"}")
    private String ext;

    @Schema(description = "最近一次定时查单时间（兜底用，避免同一批次重复查）", example = "2026-09-14T12:00:00")
    private LocalDateTime lastQueryTime;

    @Schema(description = "下次轮询查单时间（v1.4 拉起支付后主动轮询，<= NOW() 的订单将被扫描）",
        example = "2026-09-14T12:00:00")
    private LocalDateTime nextQueryAt;

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
