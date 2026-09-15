package com.weichat.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weichat.finance.entity.ReconciliationDiff;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对账差异明细 Mapper。
 *
 * @author panhw
 * @since 2026-09-15
 */
@Mapper
public interface ReconciliationDiffMapper extends BaseMapper<ReconciliationDiff> {
}
