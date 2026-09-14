package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.MerchantMode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商户配置表实体。
 *
 * <p>对应表 t_merchant_config。多商户预留，v1.0 默认 1 条。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_merchant_config")
public class MerchantConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（自增） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商户号（微信支付分配，唯一） */
    private String mchId;

    /** 公众号或小程序 AppID */
    private String appId;

    /** 商户名称（仅作展示，不参与业务逻辑） */
    private String merchantName;

    /**
     * 商户模式。
     *
     * @see MerchantMode#DIRECT 直连商户
     * @see MerchantMode#PARTNER 服务商（v2.0）
     */
    private String mode;

    /** V3 密钥（32 位，用于回调解密 resource.ciphertext） */
    private String apiV3Key;

    /** 商户 API 证书序列号（用于请求加签 + 回调验签） */
    private String certSerialNo;

    /** 商户 API 证书私钥文件路径（PEM 格式） */
    private String certPrivateKeyPath;

    /** 回调地址前缀（拼上具体路径即回调 URL） */
    private String notifyUrlBase;

    /** V2 密钥（v2.1 付款码启用） */
    private String v2Key;

    /**
     * 是否启用。
     *
     * @see CommonFlag#ENABLED 启用
     * @see CommonFlag#DISABLED 禁用
     */
    private Integer enabled;

    /** 扩展配置（JSON 格式，预留字段） */
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
