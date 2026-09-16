package com.weichat.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.PayNotifyLog;

import java.util.List;

/**
 * 回调原始报文 Service。
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface PayNotifyLogService extends IService<PayNotifyLog> {

    /**
     * 查询最近成功验签的 mchId 列表（v2.0.2 智能路由用）。
     *
     * <p>实现原理：从 {@code t_pay_notify_log} 查最近 N 条 {@code verify_result=PASS}
     * 且 {@code parent_mch_id IS NOT NULL} 的记录，作为回调路由的"已知 mchId 候选列表"。</p>
     *
     * <p>为什么不直接从 t_merchant_config 查？
     * 因为 PARTNER 模式下回调用服务商号签名，但服务端预先可能并不知道哪些服务商需要预加载。
     * 历史回调用过的 mchId 才是最可靠的候选。</p>
     *
     * @param limit 查询条数上限（建议 50~100）
     * @return 最近成功验签的 mchId 列表（去重）
     */
    List<String> listRecentVerifiedMchIds(int limit);
}
