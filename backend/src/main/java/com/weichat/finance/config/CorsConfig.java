package com.weichat.finance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局 CORS 跨域配置。
 *
 * <p>允许前端页面（默认 {@code http-server} 起在 {@code http://localhost:8091}）
 * 跨域调用本服务 {@code http://localhost:8080}。</p>
 *
 * <p>生产环境应通过网关统一处理 CORS，不要把 {@code allowedOriginPatterns("*" )}
 * 暴露到公网。</p>
 *
 * @author panhw
 * @since 2026-09-16
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        // 允许的来源（开发期通配，生产应改为具体域名）
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");
        config.addAllowedOriginPattern("http://0.0.0.0:*");
        // 允许的请求方法
        config.addAllowedMethod("*");
        // 允许的请求头（前端自定义 header 如 X-Trace-Id / Authorization）
        config.addAllowedHeader("*");
        // 允许发送 Cookie（如未来用 Session）
        config.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        // 暴露给浏览器的响应头（前端可读）
        config.addExposedHeader("X-Trace-Id");

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
