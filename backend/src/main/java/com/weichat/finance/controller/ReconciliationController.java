package com.weichat.finance.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weichat.finance.common.R;
import com.weichat.finance.entity.Reconciliation;
import com.weichat.finance.entity.ReconciliationDiff;
import com.weichat.finance.reconciliation.ReconciliationExecutor;
import com.weichat.finance.service.ReconciliationDiffService;
import com.weichat.finance.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账管理 Controller（管理后台接口）。
 *
 * <p>提供 3 个接口：</p>
 * <ul>
 *   <li>手动触发对账</li>
 *   <li>查询对账汇总列表</li>
 *   <li>查询对账汇总详情</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-15
 */
@RestController
@RequestMapping("/v1/admin/reconciliation")
@Tag(name = "对账管理", description = "对账任务管理接口（手动触发 / 历史查询）")
public class ReconciliationController {

    @Autowired
    private ReconciliationExecutor reconciliationExecutor;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconciliationDiffService reconciliationDiffService;

    @Operation(summary = "手动触发对账",
               description = "对账指定日期的账单。billDate 必填（yyyy-MM-dd），billType 选填（ALL/SUCCESS/REFUND，默认 SUCCESS）")
    @PostMapping("/trigger")
    public R<Reconciliation> trigger(
            @Parameter(description = "账单日期（yyyy-MM-dd）") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billDate,
            @Parameter(description = "账单类型") @RequestParam(defaultValue = "SUCCESS") String billType) {
        Reconciliation result = reconciliationExecutor.reconcile(billDate, billType);
        return R.ok(result);
    }

    @Operation(summary = "查询对账汇总列表", description = "返回所有对账汇总，按账单日期降序")
    @GetMapping("/list")
    public R<List<Reconciliation>> list() {
        List<Reconciliation> list = reconciliationService.list(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<Reconciliation>lambdaQuery()
                .orderByDesc(Reconciliation::getBillDate));
        return R.ok(list);
    }

    @Operation(summary = "查询对账汇总详情")
    @GetMapping("/{id}")
    public R<Reconciliation> getById(@Parameter(description = "对账汇总 ID") @PathVariable Long id) {
        Reconciliation result = reconciliationService.getById(id);
        return result == null ? R.fail("对账记录不存在") : R.ok(result);
    }

    @Operation(summary = "查询对账差异明细",
               description = "按对账汇总 ID 查询所有差异记录")
    @GetMapping("/{id}/diffs")
    public R<List<ReconciliationDiff>> listDiffs(@Parameter(description = "对账汇总 ID") @PathVariable Long id) {
        List<ReconciliationDiff> diffs = reconciliationDiffService.list(
            new LambdaQueryWrapper<ReconciliationDiff>()
                .eq(ReconciliationDiff::getReconciliationId, id));
        return R.ok(diffs);
    }
}
