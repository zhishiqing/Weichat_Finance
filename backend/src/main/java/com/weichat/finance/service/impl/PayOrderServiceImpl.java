package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.mapper.PayOrderMapper;
import com.weichat.finance.service.PayOrderService;
import org.springframework.stereotype.Service;

/**
 * 业务订单 Service 实现。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements PayOrderService {

    @Override
    public PayOrder getByOutTradeNo(String outTradeNo) {
        return this.getOne(new LambdaQueryWrapper<PayOrder>()
            .eq(PayOrder::getOutTradeNo, outTradeNo)
            .last("LIMIT 1"));
    }
}
