package com.weichat.finance.reconciliation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.MerchantConfig;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.Reconciliation;
import com.weichat.finance.entity.ReconciliationDiff;
import com.weichat.finance.service.MerchantConfigService;
import com.weichat.finance.service.PayOrderService;
import com.weichat.finance.service.ReconciliationDiffService;
import com.weichat.finance.service.ReconciliationService;
import com.weichat.finance.trace.ScheduledTaskMdcHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * 对账执行服务（核心业务逻辑）。
 *
 * <p>命名说明：避免与 {@link ReconciliationService}（CRUD 基类）同名混淆，
 * 本类聚焦"对账流程编排"，命名为 {@code ReconciliationExecutor}。</p>
 *
 * <h3>完整对账流程</h3>
 * <ol>
 *   <li>下载微信账单到本地（gzip）</li>
 *   <li>解析 gzip → CSV → List&lt;WechatBillEntry&gt;</li>
 *   <li>查本地订单（按日期范围）</li>
 *   <li>调用 {@link ReconciliationDiffAnalyzer} 对比</li>
 *   <li>写入汇总表 t_pay_reconciliation</li>
 *   <li>写入明细表 t_pay_reconciliation_diff</li>
 * </ol>
 *
 * @author panhw
 * @since 2026-09-15
 */
@Service
public class ReconciliationExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationExecutor.class);

    @Autowired private WechatBillDownloader billDownloader;
    @Autowired private ReconciliationDiffAnalyzer diffAnalyzer;
    @Autowired private PayOrderService payOrderService;
    @Autowired private MerchantConfigService merchantConfigService;
    @Autowired private ReconciliationService reconciliationCrudService;
    @Autowired private ReconciliationDiffService reconciliationDiffCrudService;

    /**
     * 执行对账（指定日期 + 类型）。
     *
     * @return 对账汇总记录（含 ID）
     */
    @Transactional(rollbackFor = Exception.class)
    public Reconciliation reconcile(LocalDate billDate, String billType) {
        ScheduledTaskMdcHelper.startScheduledTask("reconciliation");
        long start = System.currentTimeMillis();
        try {
            // 1. 查询默认商户（v1.0 直连商户固定 1 条）
            MerchantConfig merchant = merchantConfigService.getOne(
                new LambdaQueryWrapper<MerchantConfig>().last("LIMIT 1"));
            if (merchant == null) {
                throw new IllegalStateException("未找到商户配置，请先初始化 t_merchant_config");
            }

            // 2. 幂等：检查是否已存在（同一商户+日期+类型）
            // 如果已存在且上一次成功，直接返回；如果是失败状态则覆盖重试
            Reconciliation existing = reconciliationCrudService.getOne(
                new LambdaQueryWrapper<Reconciliation>()
                    .eq(Reconciliation::getMchId, merchant.getMchId())
                    .eq(Reconciliation::getBillDate, billDate)
                    .eq(Reconciliation::getBillType, billType)
                    .last("LIMIT 1"));
            Reconciliation summary;
            if (existing != null && "SUCCESS".equals(existing.getStatus())) {
                log.info("[对账] 已存在成功的对账记录，跳过: mchId={}, billDate={}, billType={}",
                    merchant.getMchId(), billDate, billType);
                return existing;
            }
            if (existing != null) {
                // 失败状态 → 覆盖重试（状态重置为 PROCESSING）
                summary = existing;
                summary.setStatus("PROCESSING");
                reconciliationCrudService.updateById(summary);
                log.info("[对账] 重新对账（上次失败）: reconciliationId={}", summary.getId());
            } else {
                summary = new Reconciliation();
                summary.setMchId(merchant.getMchId());
                summary.setBillDate(billDate);
                summary.setBillType(billType);
                summary.setStatus("PROCESSING");
                reconciliationCrudService.save(summary);
            }

            try {
                // 3. 下载账单
                String localPath = billDownloader.download(merchant, billDate, billType);
                summary.setLocalFilePath(localPath);

                // 4. 解析账单
                List<ReconciliationDiffAnalyzer.WechatBillEntry> wechatEntries = parseBillFile(localPath);
                log.info("[对账] 解析微信账单完成: 共 {} 笔", wechatEntries.size());

                // 5. 查本地订单（按日期）
                List<PayOrder> localOrders = payOrderService.list(
                    new LambdaQueryWrapper<PayOrder>()
                        .ge(PayOrder::getGmtCreate, billDate.atStartOfDay())
                        .lt(PayOrder::getGmtCreate, billDate.plusDays(1).atStartOfDay()));
                log.info("[对账] 查询本地订单完成: 共 {} 笔", localOrders.size());

                // 6. 对比分析
                List<ReconciliationDiffAnalyzer.DiffRecord> diffs =
                    diffAnalyzer.analyze(wechatEntries, localOrders);

                // 7. 写汇总
                long totalAmount = wechatEntries.stream().mapToLong(e -> e.amount).sum();
                long diffAmount = diffs.stream()
                    .mapToLong(d -> d.diffAmount != null ? d.diffAmount : 0L).sum();
                summary.setTotalCount(wechatEntries.size());
                summary.setTotalAmount(totalAmount);
                summary.setDiffCount(diffs.size());
                summary.setDiffAmount(diffAmount);
                summary.setStatus("SUCCESS");
                reconciliationCrudService.updateById(summary);

                // 8. 写明细
                for (ReconciliationDiffAnalyzer.DiffRecord diff : diffs) {
                    ReconciliationDiff diffEntity = new ReconciliationDiff();
                    diffEntity.setReconciliationId(summary.getId());
                    diffEntity.setMchId(merchant.getMchId());
                    diffEntity.setBillDate(billDate);
                    diffEntity.setOutTradeNo(diff.outTradeNo);
                    diffEntity.setTransactionId(diff.transactionId);
                    diffEntity.setDiffType(diff.diffType);
                    diffEntity.setLocalAmount(diff.localAmount);
                    diffEntity.setWechatAmount(diff.wechatAmount);
                    diffEntity.setLocalStatus(diff.localStatus);
                    diffEntity.setWechatStatus(diff.wechatStatus);
                    diffEntity.setDiffAmount(diff.diffAmount);
                    diffEntity.setHandleStatus("PENDING");
                    reconciliationDiffCrudService.save(diffEntity);
                }

                long cost = System.currentTimeMillis() - start;
                log.info("[对账] ✅ 完成: billDate={}, billType={}, 微信{}笔/总额{}分, 本地{}笔, 差异{}笔/差{}分, 耗时{}ms",
                    billDate, billType, wechatEntries.size(), totalAmount,
                    localOrders.size(), diffs.size(), diffAmount, cost);
                return summary;

            } catch (Exception e) {
                summary.setStatus("FAILED");
                reconciliationCrudService.updateById(summary);
                log.error("[对账] ❌ 失败: billDate={}, billType={}, error={}", billDate, billType, e.getMessage(), e);
                throw new RuntimeException("对账失败", e);
            }
        } finally {
            ScheduledTaskMdcHelper.endScheduledTask();
        }
    }

    /**
     * 解析 gzip CSV 账单文件（支持微信标准 24 字段格式）。
     *
     * <p>通过解析 CSV 表头动态获取字段索引，兼容字段顺序可能的微小变化。
     * 本解析器用到的核心字段：商户订单号 / 微信订单号 / 交易状态 / 应结订单金额。</p>
     */
    private List<ReconciliationDiffAnalyzer.WechatBillEntry> parseBillFile(String localPath) {
        List<ReconciliationDiffAnalyzer.WechatBillEntry> entries = new ArrayList<>();
        try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(Paths.get(localPath)));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis, java.nio.charset.StandardCharsets.UTF_8))) {

            int idxOutTradeNo = -1;
            int idxTransactionId = -1;
            int idxStatus = -1;
            int idxAmount = -1;

            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = parseCsvLine(line);
                if (isHeader) {
                    // 动态解析表头，获取字段索引
                    for (int i = 0; i < fields.length; i++) {
                        String col = fields[i].trim();
                        if ("商户订单号".equals(col)) idxOutTradeNo = i;
                        else if ("微信订单号".equals(col)) idxTransactionId = i;
                        else if ("交易状态".equals(col)) idxStatus = i;
                        else if ("应结订单金额".equals(col)) idxAmount = i;
                    }
                    if (idxOutTradeNo < 0 || idxTransactionId < 0 || idxStatus < 0 || idxAmount < 0) {
                        throw new RuntimeException("微信账单表头缺少必要字段，请检查格式。"
                            + " 期望: 商户订单号/微信订单号/交易状态/应结订单金额，"
                            + " 实际: " + String.join(",", fields));
                    }
                    log.debug("[对账] CSV 字段映射: outTradeNo={}, transactionId={}, status={}, amount={}",
                        idxOutTradeNo, idxTransactionId, idxStatus, idxAmount);
                    isHeader = false;
                    continue;
                }

                // 数据行
                if (idxOutTradeNo >= fields.length || idxTransactionId >= fields.length
                    || idxStatus >= fields.length || idxAmount >= fields.length) {
                    log.warn("[对账] 跳过字段不足的行: {}", line);
                    continue;
                }
                try {
                    String outTradeNo = fields[idxOutTradeNo].trim();
                    String transactionId = fields[idxTransactionId].trim();
                    long amount = parseFenAmount(fields[idxAmount].trim());
                    String status = fields[idxStatus].trim();

                    if (outTradeNo.isEmpty()) {
                        continue;  // 跳过合计行等非交易行
                    }
                    entries.add(new ReconciliationDiffAnalyzer.WechatBillEntry(
                        outTradeNo, transactionId, amount, status));
                } catch (Exception ex) {
                    log.warn("[对账] 解析行失败: {}, error={}", line, ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析账单文件失败: " + e.getMessage(), e);
        }
        return entries;
    }

    /**
     * 解析 CSV 行（支持带双引号包裹的字段，含内部逗号）。
     */
    private String[] parseCsvLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    /**
     * 解析金额（支持带逗号的数字如 "1,000"）。
     */
    private long parseFenAmount(String s) {
        if (s == null || s.trim().isEmpty()) {
            return 0L;
        }
        return Long.parseLong(s.trim().replace(",", ""));
    }
}
