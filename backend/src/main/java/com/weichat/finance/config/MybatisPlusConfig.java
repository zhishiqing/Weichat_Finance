package com.weichat.finance.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：
 * 1. 分页插件（v1.0 暂未使用，预留）
 * 2. gmt_create / gmt_modified 自动填充
 *
 * @author panhw
 * @since 2026-09-14
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件（v1.0 业务暂未用到，预留给后续 Phase）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 字段自动填充：insert 时填 gmt_create / gmt_modified，update 时填 gmt_modified
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                this.strictInsertFill(metaObject, "gmtCreate", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "gmtModified", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "gmtModified", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
