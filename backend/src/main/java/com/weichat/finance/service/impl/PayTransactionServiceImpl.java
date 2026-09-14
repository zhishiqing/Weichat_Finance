package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayTransaction;
import com.weichat.finance.mapper.PayTransactionMapper;
import com.weichat.finance.service.PayTransactionService;
import org.springframework.stereotype.Service;

/**
 * 微信支付交易流水 Service 实现。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class PayTransactionServiceImpl extends ServiceImpl<PayTransactionMapper, PayTransaction> implements PayTransactionService {

    @Override
    public PayTransaction getByOutTradeNo(String outTradeNo) {
        return getOne(new LambdaQueryWrapper<PayTransaction>()
            .eq(PayTransaction::getOutTradeNo, outTradeNo)
            .last("LIMIT 1"));
    }

    @Override
    public PayTransaction getByTransactionId(String transactionId) {
        return getOne(new LambdaQueryWrapper<PayTransaction>()
            .eq(PayTransaction::getTransactionId, transactionId)
            .last("LIMIT 1"));
    }
}
