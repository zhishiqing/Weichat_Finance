package com.weichat.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weichat.finance.entity.PayIdempotent;

/**
 * 幂等记录 Service。
 *
 * @author panhw
 * @since 2026-09-14
 */
public interface PayIdempotentService extends IService<PayIdempotent> {

    /**
     * 按幂等键查询（唯一索引）。
     */
    PayIdempotent getByKey(String idempotentKey);

    /**
     * 尝试占用幂等键：插入成功返回 true；已存在返回 false。
     *
     * <p>插入初始状态为 PROCESSING，业务完成后更新为 SUCCESS/FAILED。</p>
     *
     * @param idempotentKey 幂等键
     * @param operation     操作类型
     * @return true=首次请求（已占用），false=重复请求
     */
    boolean tryAcquire(String idempotentKey, String operation);

    /**
     * 更新幂等结果。
     */
    boolean updateResult(String idempotentKey, String resultCode, String resultBody);
}
