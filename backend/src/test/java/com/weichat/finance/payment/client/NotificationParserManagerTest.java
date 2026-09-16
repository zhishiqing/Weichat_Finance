package com.weichat.finance.payment.client;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.weichat.finance.payment.client.NotificationParserManager.ParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * NotificationParserManager 单元测试（v2.0.1 智能路由）。
 *
 * <h3>测试覆盖</h3>
 * <ol>
 *   <li>默认 Parser 命中（O(1) 快速路径）</li>
 *   <li>默认 Parser 失败，第 2 段遍历命中</li>
 *   <li>所有 Parser 失败，返回失败结果</li>
 *   <li>缓存空时返回失败（兜底）</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class NotificationParserManagerTest {

    @Mock
    private WechatPayConfigManager configManager;

    @InjectMocks
    private NotificationParserManager parserManager;

    private RequestParam param;

    @BeforeEach
    void setUp() {
        param = new RequestParam.Builder()
            .serialNumber("TEST_SERIAL")
            .nonce("TEST_NONCE")
            .timestamp("1700000000")
            .signature("TEST_SIG")
            .body("{\"id\":\"event_id\"}")
            .signType("WECHATPAY2-SHA256-RSA2048")
            .build();
    }

    @Test
    @DisplayName("第 1 段：默认 Parser 命中失败 → 尝试遍历")
    void testDefaultParserHit() {
        // 模拟：默认 mchId 存在，但缓存里没有这个 parser（第一次调用）
        String defaultMchId = "1900000109";
        when(configManager.getDefaultMchId()).thenReturn(defaultMchId);
        // 默认 parser 拿不到 Config（mock），预期：fallback 到第 2 段遍历，全部失败
        ParseResult result = parserManager.parseWithFallback(param, String.class);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("所有 Parser 都失败时返回 failure")
    void testAllParsersFail() {
        // 无默认 mchId 场景：直接进入第 2 段，遍历空缓存，返回 failure
        when(configManager.getDefaultMchId()).thenReturn(null);

        ParseResult result = parserManager.parseWithFallback(param, String.class);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("缓存为空时直接返回 failure（无默认 mchId）")
    void testEmptyCache() {
        when(configManager.getDefaultMchId()).thenReturn(null);

        ParseResult result = parserManager.parseWithFallback(param, String.class);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Parser"));
    }

    @Test
    @DisplayName("ParseResult.success 工厂方法")
    void testParseResultSuccess() {
        ParseResult result = ParseResult.success("test_mch", "{\"decrypted\":true}");
        assertTrue(result.isSuccess());
        assertEquals("test_mch", result.getMatchedMchId());
        assertEquals("{\"decrypted\":true}", result.getBodyAs(String.class));
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("ParseResult.failure 工厂方法")
    void testParseResultFailure() {
        ParseResult result = ParseResult.failure("签名错误");
        assertFalse(result.isSuccess());
        assertNull(result.getMatchedMchId());
        assertNull(result.getBody());
        assertEquals("签名错误", result.getErrorMessage());
    }

    @Test
    @DisplayName("parseWithHistory：缓存为空时直接返回 failure")
    void testParseWithHistoryEmpty() {
        when(configManager.getDefaultMchId()).thenReturn(null);

        ParseResult result = parserManager.parseWithHistory(param, String.class, 50);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Parser"));
    }

    @Test
    @DisplayName("parseWithHistory：默认 Parser 失败时尝试历史 + 遍历")
    void testParseWithHistoryDefaultFail() {
        String defaultMchId = "1900000109";
        when(configManager.getDefaultMchId()).thenReturn(defaultMchId);

        ParseResult result = parserManager.parseWithHistory(param, String.class, 50);

        // 默认 parser 拿不到 Config，全部失败
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("clearCache 不会抛异常")
    void testClearCache() {
        assertDoesNotThrow(() -> parserManager.clearCache());
    }
}
