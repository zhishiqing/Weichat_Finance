package com.weichat.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weichat.finance.entity.PayTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 微信支付交易流水 Mapper。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Mapper
public interface PayTransactionMapper extends BaseMapper<PayTransaction> {
}
