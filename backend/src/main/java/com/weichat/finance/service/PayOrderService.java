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

    /**
     * 分页查询悬挂中的未支付订单（用于定时查单兜底）。
     *
     * <p>条件：status IN (CREATED, SUBMITTING) 且 last_query_time 为空或早于 batchStartTime。</p>
     *
     * @param batchStartTime 本次调度开始时间（用于判断 last_query_time 是否过期）
     * @param limit         每页最大条数
     * @return 悬挂订单列表
     */
    java.util.List<PayOrder> listHangingOrders(java.time.LocalDateTime batchStartTime, int limit);
}
