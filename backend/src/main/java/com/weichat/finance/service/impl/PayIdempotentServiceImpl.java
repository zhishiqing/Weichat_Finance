package com.weichat.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichat.finance.entity.PayIdempotent;
import com.weichat.finance.mapper.PayIdempotentMapper;
import com.weichat.finance.service.PayIdempotentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 幂等记录 Service 实现。
 *
 * @author panhw
 * @since 2026-09-14
 */
@Service
public class PayIdempotentServiceImpl extends ServiceImpl<PayIdempotentMapper, PayIdempotent> implements PayIdempotentService {

    private static final Logger log = LoggerFactory.getLogger(PayIdempotentServiceImpl.class);

    @Override
    public PayIdempotent getByKey(String idempotentKey) {
        return getOne(new LambdaQueryWrapper<PayIdempotent>()
            .eq(PayIdempotent::getIdempotentKey, idempotentKey)
            .last("LIMIT 1"));
    }

    @Override
    public boolean tryAcquire(String idempotentKey, String operation) {
        PayIdempotent record = new PayIdempotent();
        record.setIdempotentKey(idempotentKey);
        record.setOperation(operation);
        record.setResultCode("PROCESSING");
        try {
            boolean ok = save(record);
            if (ok) {
                log.info("占用幂等键成功: key={}, operation={}", idempotentKey, operation);
            }
            return ok;
        } catch (DuplicateKeyException e) {
            log.warn("幂等键已被占用: key={}, operation={}", idempotentKey, operation);
            return false;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 兼容部分 JDBC 驱动包装的 duplicate key 异常
            log.warn("幂等键已被占用（兼容捕获）: key={}, operation={}", idempotentKey, operation);
            return false;
        }
    }

    @Override
    public boolean updateResult(String idempotentKey, String resultCode, String resultBody) {
        PayIdempotent update = new PayIdempotent();
        update.setResultCode(resultCode);
        update.setResultBody(resultBody);
        boolean ok = update(update, new LambdaUpdateWrapper<PayIdempotent>()
            .eq(PayIdempotent::getIdempotentKey, idempotentKey));
        if (ok) {
            log.info("更新幂等结果: key={}, result={}", idempotentKey, resultCode);
        }
        return ok;
    }
}
