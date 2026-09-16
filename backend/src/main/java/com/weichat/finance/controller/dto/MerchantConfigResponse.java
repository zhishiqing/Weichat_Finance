package com.weichat.finance.controller.dto;

import com.weichat.finance.entity.MerchantConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户配置响应 DTO（v2.0.4 脱敏版）。
 *
 * <h3>设计动机</h3>
 * <p>v2.0.3 直接返回 {@link MerchantConfig} 实体，导致以下安全风险：</p>
 * <ul>
 *   <li>{@code apiV3Key} 暴露给前端 → 可解密回调 resource.ciphertext</li>
 *   <li>{@code certPrivateKeyPath} 暴露 → 攻击者可推测私钥存储路径</li>
 *   <li>{@code certSerialNo} 完整暴露 → 可用于伪造签名攻击（需配合私钥）</li>
 * </ul>
 *
 * <h3>脱敏策略</h3>
 * <table>
 *   <tr><th>字段</th><th>原值</th><th>脱敏后</th></tr>
 *   <tr><td>apiV3Key</td><td>32 位明文</td><td>完全隐藏（返回 null）</td></tr>
 *   <tr><td>certPrivateKeyPath</td><td>完整路径</td><td>完全隐藏（返回 null）</td></tr>
 *   <tr><td>certSerialNo</td><td>40 位 hex</td><td>前后各 3 位，中间 ****</td></tr>
 * </table>
 *
 * <h3>使用建议</h3>
 * <p>管理后台查看时，可临时显示完整值（带审计日志），通过单独的"明文查询接口"
 * 走强鉴权 + 审计日志，普通查询接口一律返回脱敏值。</p>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Data
@Schema(description = "商户配置响应（脱敏版）")
public class MerchantConfigResponse {

    @Schema(description = "主键 ID（自增）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "商户号", example = "1674723182")
    private String mchId;

    @Schema(description = "公众号或小程序 AppID", example = "wx8888888888888888")
    private String appId;

    @Schema(description = "商户名称", example = "示例商户")
    private String merchantName;

    @Schema(description = "商户模式",
        example = "DIRECT",
        allowableValues = {"DIRECT", "PARTNER"})
    private String mode;

    @Schema(description = "服务商号（PARTNER 模式必填）",
        example = "1000400645", nullable = true)
    private String parentMchId;

    @Schema(description = "特约商户 AppID（PARTNER 模式必填）",
        example = "wx_yyy_yyy_yyy", nullable = true)
    private String subAppId;

    @Schema(description = "API 证书序列号（脱敏：前后各 3 位 + ****）",
        example = "6F1****4A3", nullable = true)
    private String certSerialNo;

    @Schema(description = "回调地址前缀", example = "https://api.example.com/api")
    private String notifyUrlBase;

    @Schema(description = "V2 密钥（脱敏：完全隐藏）", nullable = true)
    private String v2Key;

    @Schema(description = "是否启用", example = "1", allowableValues = {"0", "1"})
    private Integer enabled;

    @Schema(description = "扩展配置（JSON 字符串）", example = "{\"k\":\"v\"}", nullable = true)
    private String ext;

    /**
     * 敏感字段指示器：true 表示敏感字段已脱敏（用于前端 UI 提示"已脱敏，点击查看需申请"）。
     */
    @Schema(description = "敏感字段是否已脱敏", example = "true")
    private Boolean sensitiveMasked;

    @Schema(description = "创建时间", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime gmtCreate;

    @Schema(description = "更新时间", example = "2026-09-14T10:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime gmtModified;

    // ====================================================================
    // 转换方法（Entity → Response）
    // ====================================================================

    /**
     * Entity → Response 脱敏转换。
     *
     * <p>敏感字段处理：</p>
     * <ul>
     *   <li>{@code apiV3Key} → null（永不出参）</li>
     *   <li>{@code certPrivateKeyPath} → null（永不出参）</li>
     *   <li>{@code certSerialNo} → 前后 3 位 + 中间 ****</li>
     *   <li>{@code v2Key} → null（永不出参）</li>
     * </ul>
     */
    public static MerchantConfigResponse mask(MerchantConfig entity) {
        if (entity == null) {
            return null;
        }
        MerchantConfigResponse resp = new MerchantConfigResponse();
        resp.setId(entity.getId());
        resp.setMchId(entity.getMchId());
        resp.setAppId(entity.getAppId());
        resp.setMerchantName(entity.getMerchantName());
        resp.setMode(entity.getMode());
        resp.setParentMchId(entity.getParentMchId());
        resp.setSubAppId(entity.getSubAppId());
        resp.setCertSerialNo(maskCertSerialNo(entity.getCertSerialNo()));
        resp.setNotifyUrlBase(entity.getNotifyUrlBase());
        resp.setV2Key(null); // 完全隐藏
        resp.setEnabled(entity.getEnabled());
        resp.setExt(entity.getExt());
        resp.setSensitiveMasked(Boolean.TRUE);
        resp.setGmtCreate(entity.getGmtCreate());
        resp.setGmtModified(entity.getGmtModified());
        return resp;
    }

    /**
     * 证书序列号脱敏：6F140A8E...4A3 → 6F1****4A3
     *
     * @param certSerialNo 原始证书序列号（40 位 hex）
     * @return 脱敏后的字符串
     */
    private static String maskCertSerialNo(String certSerialNo) {
        if (certSerialNo == null || certSerialNo.length() <= 6) {
            return "****";
        }
        return certSerialNo.substring(0, 3) + "****" + certSerialNo.substring(certSerialNo.length() - 3);
    }
}
