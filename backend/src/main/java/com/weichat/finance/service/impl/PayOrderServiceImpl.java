package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayOrder;
import com.weichat.finance.entity.enums.OrderStatus;
import com.weichat.finance.mapper.PayOrderMapper;
import com.weichat.finance.service.PayOrderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public List<PayOrder> listHangingOrders(LocalDateTime batchStartTime, int limit) {
        // last_query_time 为空（从未被查过）
        // 或 last_query_time 早于本次批次开始时间（已在上一批次查过，但状态未变化）
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
            .in(PayOrder::getStatus, OrderStatus.SUBMITTING, OrderStatus.CREATED)
            .and(w -> w
                .isNull(PayOrder::getLastQueryTime)
                .or()
                .lt(PayOrder::getLastQueryTime, batchStartTime)
            )
            .orderByAsc(PayOrder::getGmtCreate)
            .last("LIMIT " + limit);

        return this.list(wrapper);
    }
}
