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

    /**
     * 查询到期的轮询订单（v1.4 拉起支付后主动轮询用）。
     *
     * <p>条件：status IN (CREATED, SUBMITTING) 且 next_query_at &lt;= now。</p>
     *
     * @param now 当前时间（数据库时间）
     * @param limit 每批最大条数
     * @return 到期应查询的订单列表
     */
    java.util.List<PayOrder> listDueForQuery(java.time.LocalDateTime now, int limit);

    /**
     * 入队：立即置 next_query_at = NOW()（下单成功后由 Controller 调用）。
     *
     * @param orderId 订单主键 ID
     */
    void enqueueQuery(Long orderId);

    /**
     * 计算下次应查询时间（基于已查询次数的指数退避）。
     *
     * @param baseSeconds 基础间隔（秒）
     * @param queriedTimes 已查询次数
     * @return 下次查询时间
     */
    java.time.LocalDateTime computeNextQueryTime(int baseSeconds, int queriedTimes);

    /**
     * 设置订单的下次查询时间。
     */
    void setNextQueryTime(Long orderId, java.time.LocalDateTime nextQueryAt);
}
