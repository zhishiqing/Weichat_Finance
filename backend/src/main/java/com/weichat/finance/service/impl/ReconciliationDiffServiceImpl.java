package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.ReconciliationDiff;
import com.weichat.finance.mapper.ReconciliationDiffMapper;
import com.weichat.finance.service.ReconciliationDiffService;
import org.springframework.stereotype.Service;

/**
 * 对账差异明细 Service 实现。
 *
 * @author panhw
 * @since 2026-09-15
 */
@Service
public class ReconciliationDiffServiceImpl extends ServiceImpl<ReconciliationDiffMapper, ReconciliationDiff>
    implements ReconciliationDiffService {
}
