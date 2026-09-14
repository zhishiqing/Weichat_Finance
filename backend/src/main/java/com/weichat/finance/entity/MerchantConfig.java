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
 * 商户配置表实体。
 *
 * <p>对应表 t_merchant_config。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Data
@TableName("t_merchant_config")
public class MerchantConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String mchId;
    private String appId;
    private String merchantName;
    private String mode;
    private String apiV3Key;
    private String certSerialNo;
    private String certPrivateKeyPath;
    private String notifyUrlBase;
    private String v2Key;
    private Integer enabled;
    private String ext;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;
}
