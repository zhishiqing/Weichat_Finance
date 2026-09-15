package com.weichat.finance.job;

import com.weichat.finance.reconciliation.ReconciliationExecutor;
import com.weichat.finance.trace.ScheduledTaskMdcHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 对账调度任务（每日 03:00 执行）。
 *
 * <h3>执行内容</h3>
 * 下载并对账 <strong>昨天的</strong>账单（避开当天凌晨的"次日账单未生成"问题）：
 * <ul>
 *   <li>每天 03:00 拉取昨天日期的账单</li>
 *   <li>账单类型：SUCCESS（支付成功）</li>
 *   <li>解析 → 与本地 DB 比对 → 写入差异</li>
 * </ul>
 *
 * <h3>为什么是 03:00</h3>
 * 微信账单通常在次日 04:00 后生成完整，03:00 是个折中值。
 * 失败的任务会在下一次调度时重试（基于 bill_date 幂等）。
 *
 * @author panhw
 * @since 2026-09-15
 */
@Component
public class ReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

    @Autowired
    private ReconciliationExecutor reconciliationExecutor;

    /**
     * 每日 03:00 执行对账（cron：秒 分 时 日 月 周）。
     * 也支持通过 {@code /api/admin/reconciliation/trigger} 手动触发（见 ReconciliationController）。
     */
    @Scheduled(cron = "${scheduler.reconciliation.cron:0 0 3 * * ?}")
    public void dailyReconciliation() {
        String traceId = ScheduledTaskMdcHelper.startScheduledTask("dailyReconciliation");
        try {
            // 对账"昨天"（避开次日 04:00 前的账单未生成问题）
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("[对账调度] 开始对账: billDate={}, billType=SUCCESS", yesterday);

            reconciliationExecutor.reconcile(yesterday, "SUCCESS");
        } catch (Exception e) {
            // 调度任务只记录日志，不影响下次执行
            log.error("[对账调度] 失败: traceId={}, error={}", traceId, e.getMessage(), e);
        } finally {
            ScheduledTaskMdcHelper.endScheduledTask();
        }
    }
}
