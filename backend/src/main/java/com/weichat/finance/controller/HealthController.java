package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查 Controller
 *
 * <p>Phase 1 验收入口：访问 GET /api/health 看 Spring Boot + MySQL + Flyway 是否都正常。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    public R<Map<String, Object>> health() throws Exception {
        Map<String, Object> info = new HashMap<>();
        info.put("app", "weichat-finance");
        info.put("version", "1.0.0");
        info.put("timestamp", LocalDateTime.now().toString());

        try (Connection conn = dataSource.getConnection()) {
            info.put("db", "UP");
            info.put("dbCatalog", conn.getCatalog());
            info.put("dbCharset", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            info.put("db", "DOWN");
            info.put("dbError", e.getMessage());
        }
        return R.ok(info);
    }
}
