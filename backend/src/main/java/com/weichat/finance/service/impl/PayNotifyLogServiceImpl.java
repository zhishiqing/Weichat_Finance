package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayNotifyLog;
import com.weichat.finance.mapper.PayNotifyLogMapper;
import com.weichat.finance.service.PayNotifyLogService;
import org.springframework.stereotype.Service;

/**
 * 回调原始报文 Service 实现。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class PayNotifyLogServiceImpl extends ServiceImpl<PayNotifyLogMapper, PayNotifyLog> implements PayNotifyLogService {
}
