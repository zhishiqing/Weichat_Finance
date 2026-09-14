package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayRefund;
import com.weichat.finance.mapper.PayRefundMapper;
import com.weichat.finance.service.PayRefundService;
import org.springframework.stereotype.Service;

/**
 * 退款单 Service 实现。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class PayRefundServiceImpl extends ServiceImpl<PayRefundMapper, PayRefund> implements PayRefundService {

    @Override
    public PayRefund getByOutRefundNo(String outRefundNo) {
        return getOne(new LambdaQueryWrapper<PayRefund>()
            .eq(PayRefund::getOutRefundNo, outRefundNo)
            .last("LIMIT 1"));
    }
}
