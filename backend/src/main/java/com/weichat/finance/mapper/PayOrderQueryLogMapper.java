package com.weichat.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weichat.finance.entity.PayOrderQueryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时查单日志 Mapper。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Mapper
public interface PayOrderQueryLogMapper extends BaseMapper<PayOrderQueryLog> {
}
