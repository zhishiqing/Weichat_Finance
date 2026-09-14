package com.weichat.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.PayRefund;

/**
 * 退款单 Service。
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface PayRefundService extends IService<PayRefund> {

    /**
     * 按商户退款单号查退款单。
     */
    PayRefund getByOutRefundNo(String outRefundNo);
}
