package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayNotifyLog;
import com.weichat.finance.entity.enums.NotifyResult;
import com.weichat.finance.mapper.PayNotifyLogMapper;
import com.weichat.finance.service.PayNotifyLogService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 回调原始报文 Service 实现。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class PayNotifyLogServiceImpl extends ServiceImpl<PayNotifyLogMapper, PayNotifyLog> implements PayNotifyLogService {

    /**
     * 查询最近成功验签的 mchId 列表（去重）。
     *
     * <p>SQL 示例：</p>
     * <pre>{@code
     * SELECT DISTINCT parent_mch_id
     * FROM t_pay_notify_log
     * WHERE verify_result = 'PASS'
     *   AND parent_mch_id IS NOT NULL
     * ORDER BY id DESC
     * LIMIT ?
     * }</pre>
     */
    @Override
    public List<String> listRecentVerifiedMchIds(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        try {
            return lambdaQuery()
                .select(PayNotifyLog::getParentMchId)
                .eq(PayNotifyLog::getVerifyResult, NotifyResult.PASS)
                .isNotNull(PayNotifyLog::getParentMchId)
                .orderByDesc(PayNotifyLog::getId)
                .last("LIMIT " + Math.min(limit, 500))  // 安全上限
                .list()
                .stream()
                .map(PayNotifyLog::getParentMchId)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            // 兜底：表结构未升级 / 字段不存在时返回空列表
            return Collections.emptyList();
        }
    }
}
