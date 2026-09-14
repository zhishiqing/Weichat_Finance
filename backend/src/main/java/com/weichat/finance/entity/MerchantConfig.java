package com.weichat.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.weichat.finance.entity.enums.CommonFlag;
import com.weichat.finance.entity.enums.MerchantMode;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "商户配置表（t_merchant_config）")
public class MerchantConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商户号（微信支付分配，唯一）", example = "1900000109")
    private String mchId;

    @Schema(description = "公众号或小程序 AppID", example = "wx8888888888888888")
    private String appId;

    @Schema(description = "商户名称（仅作展示，不参与业务逻辑）", example = "示例商户")
    private String merchantName;

    @Schema(description = "商户模式",
        example = MerchantMode.DIRECT,
        allowableValues = {MerchantMode.DIRECT, MerchantMode.PARTNER})
    private String mode;

    @Schema(description = "V3 密钥（32 位，用于回调解密 resource.ciphertext）", example = "abcdefghijklmnopqrstuvwxyz123456")
    private String apiV3Key;

    @Schema(description = "商户 API 证书序列号（用于请求加签 + 回调验签）", example = "SERIAL_NO_123")
    private String certSerialNo;

    @Schema(description = "商户 API 证书私钥文件路径（PEM 格式）", example = "D:/certs/apiclient_key.pem")
    private String certPrivateKeyPath;

    @Schema(description = "回调地址前缀（拼上具体路径即回调 URL）", example = "https://api.example.com/api/notify/v3")
    private String notifyUrlBase;

    @Schema(description = "V2 密钥（v2.1 付款码启用）", example = "v2keyxxxxxxxxxxxxxxxxxxxxxx")
    private String v2Key;

    @Schema(description = "是否启用",
        example = "1",
        allowableValues = {"0", "1"},
        defaultValue = "1")
    private Integer enabled;

    @Schema(description = "扩展配置（JSON 格式，预留字段）", example = "{\"k\":\"v\"}")
    private String ext;

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
