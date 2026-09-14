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

    /**
     * 按商户订单号更新交易流水。
     *
     * @param outTradeNo   商户订单号
     * @param payStatus    支付状态
     * @param transactionId 微信支付单号（可选，SUCCESS 时填充）
     * @param amountPayer  用户实际支付金额（可选，SUCCESS 时填充）
     * @param bankType     付款银行类型（可选）
     * @param successTime  支付成功时间（可选）
     * @return 是否更新成功
     */
    boolean updateByOutTradeNo(String outTradeNo, String payStatus,
                                String transactionId, Long amountPayer,
                                String bankType, java.time.LocalDateTime successTime);
}
