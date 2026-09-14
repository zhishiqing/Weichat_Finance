package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayOrderQueryLog;
import com.weichat.finance.mapper.PayOrderQueryLogMapper;
import com.weichat.finance.service.PayOrderQueryLogService;
import org.springframework.stereotype.Service;

/**
 * 定时查单日志 Service 实现。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class PayOrderQueryLogServiceImpl extends ServiceImpl<PayOrderQueryLogMapper, PayOrderQueryLog>
    implements PayOrderQueryLogService {
}
