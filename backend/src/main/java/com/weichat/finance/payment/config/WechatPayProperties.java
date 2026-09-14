package com.weichat.finance.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置（绑定 application.yml 的 wechatpay.* 配置段）。
 *
 * <p>完整配置示例：</p>
 * <pre>
 * wechatpay:
 *   mode: MOCK                      # MOCK（默认，本地开发，零风险）/ REAL 真实 APIv3 调用
 *   merchant:
 *     mch-id: ${WX_MCH_ID}
 *     app-id: ${WX_APP_ID}
 *     api-v3-key: ${WX_API_V3_KEY}
 *     cert:
 *       serial-no: ${WX_CERT_SERIAL_NO}
 *       private-key-path: backend/certs/apiclient_key.pem
 *   notify-url-base: https://your-domain.com
 * </pre>
 *
 * <p>真实值通过环境变量注入，绝对不能写入代码或提交到 git。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Component
@ConfigurationProperties(prefix = "wechatpay")
public class WechatPayProperties {

    private String mode = "MOCK";
    private Merchant merchant = new Merchant();
    private String notifyUrlBase;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }
    public String getNotifyUrlBase() { return notifyUrlBase; }
    public void setNotifyUrlBase(String notifyUrlBase) { this.notifyUrlBase = notifyUrlBase; }

    /**
     * 商户内部配置。
     */
    public static class Merchant {
        private String mchId;
        private String appId;
        private String apiV3Key;
        private Cert cert = new Cert();

        public String getMchId() { return mchId; }
        public void setMchId(String mchId) { this.mchId = mchId; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getApiV3Key() { return apiV3Key; }
        public void setApiV3Key(String apiV3Key) { this.apiV3Key = apiV3Key; }
        public Cert getCert() { return cert; }
        public void setCert(Cert cert) { this.cert = cert; }

        public static class Cert {
            private String serialNo;
            private String privateKeyPath;
            public String getSerialNo() { return serialNo; }
            public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
            public String getPrivateKeyPath() { return privateKeyPath; }
            public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        }
    }
}
