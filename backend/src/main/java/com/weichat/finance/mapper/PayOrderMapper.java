package com.weichat.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weichat.finance.entity.PayOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务订单 Mapper。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrder> {
}
