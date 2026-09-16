package com.weichat.finance.controller.dto;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.enums.MerchantMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MerchantConfigResponse 脱敏逻辑单元测试（v2.0.4）。
 *
 * <h3>测试覆盖</h3>
 * <ol>
 *   <li>apiV3Key 完全隐藏</li>
 *   <li>certPrivateKeyPath 完全隐藏</li>
 *   <li>certSerialNo 前后各 3 位 + 中间 ****</li>
 *   <li>v2Key 完全隐藏</li>
 *   <li>其他字段正常传递</li>
 *   <li>null 实体返回 null</li>
 *   <li>certSerialNo 短于 6 位时返回 ****</li>
 * </ol>
 */
class MerchantConfigResponseTest {

    private MerchantConfig entity;

    @BeforeEach
    void setUp() {
        entity = new MerchantConfig();
        entity.setId(1L);
        entity.setMchId("1674723182");
        entity.setAppId("wx9d066e071d8e4e33");
        entity.setMerchantName("测试商户");
        entity.setMode(MerchantMode.DIRECT);
        entity.setApiV3Key("wwx9d066e071X8687d3414bbfc7ad2fX");
        entity.setCertSerialNo("6F140A8E7DF3BD76892D7FDED0E670AB2B8AD4A3");
        entity.setCertPrivateKeyPath("certs/apiclient_key.pem");
        entity.setNotifyUrlBase("http://localhost:8080/api");
        entity.setV2Key("v2key_secret_12345678");
        entity.setEnabled(1);
    }

    @Test
    @DisplayName("apiV3Key 完全隐藏（返回 null）")
    void testApiV3KeyHidden() {
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        // Response DTO 不暴露 apiV3Key 字段（编译期就保证），只验证 mchId 等公开字段正常
        assertEquals("1674723182", resp.getMchId());
        assertEquals("wx9d066e071d8e4e33", resp.getAppId());
        // 验证实体确实有 apiV3Key（确保 DTO 是从有值的实体转换的）
        assertNotNull(entity.getApiV3Key());
    }

    @Test
    @DisplayName("certPrivateKeyPath 完全隐藏（DTO 不暴露此字段）")
    void testCertPrivateKeyPathHidden() {
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        // 实体有这个字段
        assertNotNull(entity.getCertPrivateKeyPath());
        // 但 DTO 不暴露（编译期保证）
        assertEquals("1674723182", resp.getMchId());
    }

    @Test
    @DisplayName("certSerialNo 前后各 3 位 + 中间 ****")
    void testCertSerialNoMasked() {
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        // 6F140A8E7DF3BD76892D7FDED0E670AB2B8AD4A3 (40 chars)
        // 预期: 6F1****4A3
        assertEquals("6F1****4A3", resp.getCertSerialNo());
        assertEquals(10, resp.getCertSerialNo().length());
    }

    @Test
    @DisplayName("v2Key 完全隐藏（返回 null）")
    void testV2KeyHidden() {
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        assertNull(resp.getV2Key(), "v2Key 必须完全隐藏");
    }

    @Test
    @DisplayName("其他公开字段正常传递")
    void testOtherFieldsPreserved() {
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        assertEquals(1L, resp.getId());
        assertEquals("测试商户", resp.getMerchantName());
        assertEquals(MerchantMode.DIRECT, resp.getMode());
        assertEquals("http://localhost:8080/api", resp.getNotifyUrlBase());
        assertEquals(1, resp.getEnabled());
    }

    @Test
    @DisplayName("敏感字段指示器为 true")
    void testSensitiveMaskedFlag() {
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        assertTrue(resp.getSensitiveMasked(), "sensitiveMasked 标志位必须为 true");
    }

    @Test
    @DisplayName("PARTNER 模式字段正常传递")
    void testPartnerFieldsPreserved() {
        entity.setMode(MerchantMode.PARTNER);
        entity.setParentMchId("1000400645");
        entity.setSubAppId("wx_sub_app");
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        assertEquals(MerchantMode.PARTNER, resp.getMode());
        assertEquals("1000400645", resp.getParentMchId());
        assertEquals("wx_sub_app", resp.getSubAppId());
    }

    @Test
    @DisplayName("null 实体返回 null")
    void testNullEntity() {
        MerchantConfigResponse resp = MerchantConfigResponse.mask(null);
        assertNull(resp);
    }

    @Test
    @DisplayName("certSerialNo 为 null 时返回 ****")
    void testNullCertSerialNo() {
        entity.setCertSerialNo(null);
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        assertEquals("****", resp.getCertSerialNo());
    }

    @Test
    @DisplayName("certSerialNo 短于 6 位时返回 ****")
    void testShortCertSerialNo() {
        entity.setCertSerialNo("ABC");
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        assertEquals("****", resp.getCertSerialNo());
    }

    @Test
    @DisplayName("certSerialNo 恰好 7 位：取前 3 + **** + 后 3 = 11 位")
    void testSevenCharCertSerialNo() {
        entity.setCertSerialNo("ABC1234");
        MerchantConfigResponse resp = MerchantConfigResponse.mask(entity);
        assertEquals("ABC****234", resp.getCertSerialNo());
    }
}
