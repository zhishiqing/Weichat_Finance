package com.weichat.finance.entity;

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
 * 商户配置表实体
 *
 * <p>对应表 t_merchant_config，建表脚本见 docs/db/schema/V1__weichat_finance_init.sql。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_merchant_config")
public class MerchantConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String mchId;

    /** 公众号或小程序 AppID */
    private String appId;

    /** 商户名称 */
    private String merchantName;

    /** 模式：DIRECT 直连商户 / PARTNER 服务商 */
    private String mode;

    /** V3 密钥（32 位，用于回调解密） */
    private String apiV3Key;

    /** 商户证书序列号 */
    private String certSerialNo;

    /** 商户私钥 PEM 路径 */
    private String certPrivateKeyPath;

    /** 回调地址前缀 */
    private String notifyUrlBase;

    /** V2 密钥（v2.1 付款码启用） */
    private String v2Key;

    /** 是否启用：0 禁用 / 1 启用 */
    private Integer enabled;

    /** 扩展配置 */
    private String ext;

    /** 创建时间（MetaObjectHandler 自动填充） */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    /** 更新时间（MetaObjectHandler 自动填充） */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    /** 逻辑删除：0 未删 / 1 已删 */
    @TableLogic
    private Integer isDeleted;
}
