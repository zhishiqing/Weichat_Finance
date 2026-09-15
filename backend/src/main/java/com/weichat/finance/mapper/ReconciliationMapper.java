package com.weichat.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weichat.finance.entity.Reconciliation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账汇总 Mapper。
 *
 * @author panhw
 * @since 2026-09-15
 */
@Mapper
public interface ReconciliationMapper extends BaseMapper<Reconciliation> {
}
