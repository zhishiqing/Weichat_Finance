package com.weichat.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.PayOrder;

/**
 * 业务订单 Service。
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface PayOrderService extends IService<PayOrder> {

    /**
     * 按商户订单号查询。
     */
    PayOrder getByOutTradeNo(String outTradeNo);
}
