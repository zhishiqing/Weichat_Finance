package com.weichat.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weichat.finance.entity.PayNotifyLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 回调原始报文 Mapper。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Mapper
public interface PayNotifyLogMapper extends BaseMapper<PayNotifyLog> {
}
