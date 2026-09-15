package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.Reconciliation;
import com.weichat.finance.mapper.ReconciliationMapper;
import com.weichat.finance.service.ReconciliationService;
import org.springframework.stereotype.Service;

/**
 * 对账汇总 Service 实现。
 *
 * @author panhw
 * @since 2026-09-15
 */
@Service
public class ReconciliationServiceImpl extends ServiceImpl<ReconciliationMapper, Reconciliation>
    implements ReconciliationService {
}
