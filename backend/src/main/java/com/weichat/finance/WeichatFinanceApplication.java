package com.weichat.finance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 微信支付财务对接系统 · 主程序入口
 *
 * <p>v1.0 阶段：完成 Spring Boot 骨架 + MyBatis-Plus + Flyway 自动迁移；
 * 业务层（统一下单、退款、回调）在后续 Phase 填充。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@SpringBootApplication
@MapperScan("com.weichat.finance.mapper")
@EnableScheduling
public class WeichatFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeichatFinanceApplication.class, args);
    }
}
