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
public class PayOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商户订单号（业务侧生成，UUID 等，唯一） */
    private String outTradeNo;

    /** 商户号 */
    private String mchId;

    /** 公众号或小程序 AppID */
    private String appId;

    /** 订单描述（最长 127 字符） */
    private String description;

    /** 订单金额（单位：分，必须为正整数，避免浮点精度问题） */
    private Long amountTotal;

    /** 货币类型，默认 CNY */
    private String currency;

    /** 用户标识（JSAPI 必填，Native 为空） */
    private String openid;

    /**
     * 支付产品类型。
     *
     * @see ProductType#JSAPI JSAPI 支付（公众号 / 小程序内）
     * @see ProductType#NATIVE Native 支付（扫码）
     */
    private String productType;

    /**
     * 订单状态。
     *
     * @see OrderStatus 详细状态机说明
     */
    private String status;

    /** 订单失效时间（未支付则到期自动关闭） */
    private LocalDateTime timeExpire;

    /** 支付成功时间（微信回传，状态变为 SUCCESS 时填充） */
    private LocalDateTime successTime;

    /** 回调地址（商户配置 notifyUrlBase + 业务路径拼接） */
    private String notifyUrl;

    /** 附加数据（最长 128 字符，原样回传） */
    private String attach;

    /** 扩展参数（JSON 格式，预留字段） */
    private String ext;

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
