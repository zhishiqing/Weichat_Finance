package com.weichat.finance.reconciliation;

import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.PayTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对账差异分析器。
 *
 * <h3>核心逻辑</h3>
 * 给定微信账单 + 本地订单，对比生成差异列表：
 * <ul>
 *   <li><strong>WECHAT_ONLY</strong>：微信有，本地无（疑似回调丢失，需补单）</li>
 *   <li><strong>LOCAL_ONLY</strong>：本地有，微信无（疑似本地误下单或微信未达账）</li>
 *   <li><strong>AMOUNT_DIFF</strong>：双方都有但金额不一致（极端情况，需核对）</li>
 *   <li><strong>STATUS_DIFF</strong>：双方都有但状态不一致（如微信 SUCCESS 本地 CREATED）</li>
 * </ul>
 *
 * <h3>对比维度</h3>
 * <ul>
 *   <li>主键：以 {@code outTradeNo}（商户订单号）为联合键</li>
 *   <li>金额：以 {@code amountTotal} / {@code amountPayerTotal}（注意是分）</li>
 *   <li>状态：微信 PayStatus vs 本地 OrderStatus</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-15
 */
@Component
public class ReconciliationDiffAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationDiffAnalyzer.class);

    /**
     * 一条微信账单记录（CSV 解析后）。
     */
    public static class WechatBillEntry {
        public String outTradeNo;      // 商户订单号
        public String transactionId;    // 微信订单号
        public Long amount;            // 金额（分）
        public String status;          // 交易状态（SUCCESS/REFUND）

        public WechatBillEntry(String outTradeNo, String transactionId, Long amount, String status) {
            this.outTradeNo = outTradeNo;
            this.transactionId = transactionId;
            this.amount = amount;
            this.status = status;
        }
    }

    /**
     * 对比并返回差异列表。
     *
     * @param wechatEntries 微信账单记录
     * @param localOrders   本地订单（按 outTradeNo 索引）
     * @return 差异列表
     */
    public List<DiffRecord> analyze(List<WechatBillEntry> wechatEntries, List<PayOrder> localOrders) {
        // 索引：本地订单 outTradeNo → PayOrder
        Map<String, PayOrder> localMap = new HashMap<>();
        for (PayOrder order : localOrders) {
            if (order.getOutTradeNo() != null) {
                localMap.put(order.getOutTradeNo(), order);
            }
        }

        // 索引：本地交易流水 transactionId → PayTransaction
        Map<String, PayTransaction> transactionMap = new HashMap<>();
        // 此处需要 payTransactionService.listByOutTradeNos（暂简化）

        // 索引：微信账单 outTradeNo → WechatBillEntry
        Map<String, WechatBillEntry> wechatMap = new HashMap<>();
        for (WechatBillEntry entry : wechatEntries) {
            wechatMap.put(entry.outTradeNo, entry);
        }

        List<DiffRecord> diffs = new ArrayList<>();
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(localMap.keySet());
        allKeys.addAll(wechatMap.keySet());

        for (String outTradeNo : allKeys) {
            PayOrder local = localMap.get(outTradeNo);
            WechatBillEntry wechat = wechatMap.get(outTradeNo);

            if (local == null) {
                // WECHAT_ONLY：微信有，本地无
                diffs.add(DiffRecord.wechatOnly(wechat));
            } else if (wechat == null) {
                // LOCAL_ONLY：本地有，微信无
                diffs.add(DiffRecord.localOnly(local));
            } else {
                // 双方都有，对比金额与状态
                compareBoth(local, wechat, diffs);
            }
        }

        log.info("[对账] 差异分析完成: 本地订单 {} 笔, 微信账单 {} 笔, 差异 {} 笔",
            localOrders.size(), wechatEntries.size(), diffs.size());
        return diffs;
    }

    /**
     * 对比双方都有的记录。
     */
    private void compareBoth(PayOrder local, WechatBillEntry wechat, List<DiffRecord> diffs) {
        // 1. 对比金额
        if (!local.getAmountTotal().equals(wechat.amount)) {
            diffs.add(DiffRecord.amountDiff(local, wechat));
        }

        // 2. 对比状态（微信 SUCCESS → 本地应为 SUCCESS / REFUNDING / REFUNDED）
        String localStatus = local.getStatus();
        String wechatStatus = wechat.status;
        if (!isStatusConsistent(localStatus, wechatStatus)) {
            diffs.add(DiffRecord.statusDiff(local, wechat));
        }
    }

    /**
     * 状态一致性判断。
     *
     * <p>微信 SUCCESS 可能对应本地：SUCCESS / REFUNDING / REFUNDED（退款中/已退款）</p>
     */
    private boolean isStatusConsistent(String localStatus, String wechatStatus) {
        if (wechatStatus == null) {
            return true;
        }
        return switch (wechatStatus) {
            case "SUCCESS" -> "SUCCESS".equals(localStatus)
                || "REFUNDING".equals(localStatus)
                || "REFUNDED".equals(localStatus);
            case "REFUND" -> "REFUNDING".equals(localStatus) || "REFUNDED".equals(localStatus);
            case "NOTPAY", "CLOSED", "REVOKED" -> true; // 不强制一致
            default -> true;
        };
    }

    /**
     * 差异记录（DTO，不入库）。
     */
    public static class DiffRecord {
        public String outTradeNo;
        public String transactionId;
        public String diffType;
        public Long localAmount;
        public Long wechatAmount;
        public String localStatus;
        public String wechatStatus;
        public Long diffAmount;

        public static DiffRecord localOnly(PayOrder local) {
            DiffRecord r = new DiffRecord();
            r.outTradeNo = local.getOutTradeNo();
            r.diffType = "LOCAL_ONLY";
            r.localAmount = local.getAmountTotal();
            r.localStatus = local.getStatus();
            r.wechatAmount = 0L;
            r.diffAmount = -r.localAmount; // 负数：本地多
            return r;
        }

        public static DiffRecord wechatOnly(WechatBillEntry wechat) {
            DiffRecord r = new DiffRecord();
            r.outTradeNo = wechat.outTradeNo;
            r.transactionId = wechat.transactionId;
            r.diffType = "WECHAT_ONLY";
            r.wechatAmount = wechat.amount;
            r.wechatStatus = wechat.status;
            r.localAmount = 0L;
            r.diffAmount = wechat.amount; // 正数：微信多
            return r;
        }

        public static DiffRecord amountDiff(PayOrder local, WechatBillEntry wechat) {
            DiffRecord r = new DiffRecord();
            r.outTradeNo = local.getOutTradeNo();
            r.transactionId = wechat.transactionId;
            r.diffType = "AMOUNT_DIFF";
            r.localAmount = local.getAmountTotal();
            r.wechatAmount = wechat.amount;
            r.localStatus = local.getStatus();
            r.wechatStatus = wechat.status;
            r.diffAmount = wechat.amount - local.getAmountTotal();
            return r;
        }

        public static DiffRecord statusDiff(PayOrder local, WechatBillEntry wechat) {
            DiffRecord r = new DiffRecord();
            r.outTradeNo = local.getOutTradeNo();
            r.transactionId = wechat.transactionId;
            r.diffType = "STATUS_DIFF";
            r.localAmount = local.getAmountTotal();
            r.wechatAmount = wechat.amount;
            r.localStatus = local.getStatus();
            r.wechatStatus = wechat.status;
            r.diffAmount = 0L;
            return r;
        }
    }
}
