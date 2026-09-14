package com.weichat.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.PayTransaction;

/**
 * 微信支付交易流水 Service。
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface PayTransactionService extends IService<PayTransaction> {

    /**
     * 按商户订单号查流水（唯一索引保证唯一）。
     */
    PayTransaction getByOutTradeNo(String outTradeNo);

    /**
     * 按微信支付订单号查流水。
     */
    PayTransaction getByTransactionId(String transactionId);
}
