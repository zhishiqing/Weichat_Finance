package com.weichat.finance.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 服务商进件请求（v2.0.5）。
 *
 * <h3>业务流程</h3>
 * <pre>
 * 1. 服务商准备好营业执照、银行账户、联系人
 * 2. 调用本接口创建服务商记录（mode=PARTNER）
 * 3. 后台审核 → 启用（enabled=1）
 * 4. 服务商在「特约商户管理」菜单发起子商户进件
 * </pre>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code mchId}：服务商号（微信支付分配，唯一）</li>
 *   <li>{@code appId}：服务商 AppID（用于服务商自身的 Config）</li>
 *   <li>{@code apiV3Key}：V3 密钥（32 位，用于回调解密）</li>
 *   <li>{@code certSerialNo}：服务商 API 证书序列号</li>
 *   <li>{@code certPrivateKeyPath}：服务商 API 证书私钥文件路径</li>
 *   <li>{@code contactName/contactPhone/contactEmail}：联系人信息（用于业务对接）</li>
 *   <li>{@code businessLicenseNo}：营业执照号（用于审核）</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Data
@Schema(description = "服务商进件请求")
public class PartnerOnboardRequest {

    @Schema(description = "服务商号（微信支付分配，全局唯一）",
        example = "1000400645", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务商号不能为空")
    @Pattern(regexp = "\\d{10,32}", message = "服务商号格式错误（10-32 位数字）")
    private String mchId;

    @Schema(description = "服务商 AppID（公众号或小程序 AppID）",
        example = "wx_partner_appid", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务商 AppID 不能为空")
    private String appId;

    @Schema(description = "服务商名称（用于展示）",
        example = "示例服务商有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务商名称不能为空")
    @Size(max = 128, message = "服务商名称不能超过 128 字符")
    private String merchantName;

    @Schema(description = "V3 密钥（32 位，用于回调解密）",
        example = "abcdefghijklmnopqrstuvwxyz123456",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "V3 密钥不能为空")
    @Size(min = 32, max = 32, message = "V3 密钥必须为 32 位")
    private String apiV3Key;

    @Schema(description = "服务商 API 证书序列号",
        example = "SERIAL_NO_PARTNER_001",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证书序列号不能为空")
    private String certSerialNo;

    @Schema(description = "服务商 API 证书私钥文件路径（PEM 格式）",
        example = "certs/partner/apiclient_partner_key.pem",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证书私钥文件路径不能为空")
    private String certPrivateKeyPath;

    @Schema(description = "回调地址前缀",
        example = "https://api.example.com/api", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "回调地址前缀不能为空")
    private String notifyUrlBase;

    @Schema(description = "联系人姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @Schema(description = "联系人电话", example = "13800138000",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系人电话不能为空")
    @Pattern(regexp = "1[3-9]\\d{9}", message = "联系人电话格式错误")
    private String contactPhone;

    @Schema(description = "联系人邮箱", example = "[email protected]",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "联系人邮箱不能为空")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "邮箱格式错误")
    private String contactEmail;

    @Schema(description = "营业执照号（用于审核）", example = "91110000123456789X", nullable = true)
    private String businessLicenseNo;

    @Schema(description = "银行账户号（用于资金结算）", example = "6222021234567890123", nullable = true)
    private String bankAccountNo;

    @Schema(description = "银行账户名", example = "示例服务商有限公司", nullable = true)
    private String bankAccountName;

    @Schema(description = "开户行", example = "工商银行北京分行", nullable = true)
    private String bankName;
}
