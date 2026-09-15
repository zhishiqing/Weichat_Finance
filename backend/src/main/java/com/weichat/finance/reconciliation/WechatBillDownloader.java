package com.weichat.finance.reconciliation;

import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.payment.config.WechatPayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 微信账单下载器（Mock + Real 双模式）。
 *
 * <h3>账单类型</h3>
 * <ul>
 *   <li>ALL：当日所有交易</li>
 *   <li>SUCCESS：当日成功支付的交易（推荐）</li>
 *   <li>REFUND：当日成功退款的交易</li>
 * </ul>
 *
 * <h3>账单下载 API（Real 模式）</h3>
 * <pre>
 * GET /v3/billdownload/file?bill_date=2026-09-14&amp;bill_type=SUCCESS
 * </pre>
 * 响应：gzip 压缩的 CSV 文件流，字段参考微信官方文档。
 *
 * <h3>本地存储</h3>
 * 文件路径：{@code {bill.local-dir}/{yyyy-MM-dd}_{billType}.gz}
 *
 * <h3>Mock 模式</h3>
 * 生成示例 CSV，便于本地验证对账流程。
 *
 * @author panhw
 * @since 2026-09-15
 */
@Service
public class WechatBillDownloader {

    private static final Logger log = LoggerFactory.getLogger(WechatBillDownloader.class);

    @Autowired
    private WechatPayProperties wechatPayProperties;

    /**
     * 下载账单到本地文件。
     *
     * @param mchId   商户号
     * @param billDate 账单日期
     * @param billType 账单类型 ALL / SUCCESS / REFUND
     * @return 本地文件路径
     */
    public String download(MerchantConfig merchant, LocalDate billDate, String billType) {
        String localPath = resolveLocalPath(billDate, billType);
        log.info("[对账] 开始下载账单: mchId={}, billDate={}, billType={}, localPath={}",
            merchant.getMchId(), billDate, billType, localPath);

        if ("MOCK".equalsIgnoreCase(wechatPayProperties.getMode())) {
            return downloadMock(merchant, billDate, billType, localPath);
        } else {
            return downloadReal(merchant, billDate, billType, localPath);
        }
    }

    /**
     * 生成 Mock 账单（gzip CSV）。
     *
     * <p>Mock 场景（3 笔，详见 ReconciliationDiffAnalyzer 注释）：</p>
     * <ul>
     *   <li>ORDER202609140001/ORDER202609140002：与本地匹配，无差异</li>
     *   <li>ORDER202609140003：本地无此订单 → WECHAT_ONLY 差异</li>
     *   <li>ORDER202609140004（本地有，微信无）：在本地查时体现</li>
     * </ul>
     *
     * <p>微信账单 CSV 字段顺序（共 24 字段，本解析器用到的）：</p>
     * 0交易时间 1公众账号ID 2商户号 3商户订单号 4微信订单号
     * 5用户标识 6交易类型 7交易状态 8付款银行 9货币类型
     * 10应结订单金额 11实付订单金额
     */
    private String downloadMock(MerchantConfig merchant, LocalDate billDate, String billType, String localPath) {
        try {
            Path target = Paths.get(localPath);
            Files.createDirectories(target.getParent());

            String appId = merchant.getAppId();
            String mchId = merchant.getMchId();

            // CSV 内容（24 字段，用双引号包裹含逗号字段）
            String csv = "交易时间,公众账号ID,商户号,商户订单号,微信订单号,用户标识,交易类型,交易状态,付款银行,货币类型,应结订单金额,实付订单金额,代金券金额,退款单号,商户退款单号,退款金额,退款原因,商品名称,商户数据包,手续费,费率,订单金额,申请退款金额,费率备注\n"
                + "2026-09-14 10:30:00," + appId + "," + mchId + ",ORDER202609140001,420000123420230914100001A,oUpF8uMuA,JSAPI,SUCCESS,CMC,CNY,100,100,0,,,,0,,test item,,0,0.6%,100,0,\n"
                + "2026-09-14 11:45:00," + appId + "," + mchId + ",ORDER202609140002,420000123420230914100002B,oUpF8uMuAJLq5EQxS6,JSAPI,SUCCESS,ICBC,CNY,200,200,0,,,,0,,商品A,,0,0.6%,200,0,\n"
                + "2026-09-14 14:20:00," + appId + "," + mchId + ",ORDER202609140003,420000123420230914100003C,oUpF8uMu3gB,NATIVE,SUCCESS,ABC,CNY,300,300,0,,,,0,,商品B,,0,0.6%,300,0,\n";

            byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try (java.io.OutputStream fos = Files.newOutputStream(target);
                 java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(fos)) {
                gzos.write(csvBytes);
            }

            log.info("[MOCK] 已生成 Mock 账单: {} ({}字节 gzip)", localPath, Files.size(target));
            return localPath;
        } catch (IOException e) {
            throw new RuntimeException("生成 Mock 账单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 真实模式下载（占位，需真实 SDK 后激活）。
     */
    private String downloadReal(MerchantConfig merchant, LocalDate billDate, String billType, String localPath) {
        log.warn("[Real] 微信账单下载尚未实现，需要真实 SDK 激活。merchant={}, billDate={}, billType={}",
            merchant.getMchId(), billDate, billType);
        // TODO v1.3：使用 wechatpay-java SDK 调用 GET /v3/billdownload/file
        throw new UnsupportedOperationException("真实账单下载尚未实现，需要真实 SDK");
    }

    /**
     * 拼接本地文件路径。
     */
    private String resolveLocalPath(LocalDate billDate, String billType) {
        String baseDir = System.getProperty("wechatpay.bill.local-dir",
            System.getProperty("user.home") + "/weichat-finance/bill");
        String fileName = billDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "_" + billType + ".gz";
        return Paths.get(baseDir, fileName).toString();
    }
}
