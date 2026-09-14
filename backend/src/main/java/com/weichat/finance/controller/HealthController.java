package com.weichat.finance.controller;

import com.weichat.finance.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查 Controller。
 *
 * <p>访问 GET /api/health 看 Spring Boot + MySQL + Flyway 是否都正常。</p>
 *
 * @author panhw
 * @since 2026-09-14
 */
@RestController
@RequestMapping("/health")
@Tag(name = "健康检查", description = "应用探活 + 数据库连通性验证")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Operation(summary = "健康检查", description = "返回应用信息 + 数据库连通性验证（MySQL 版本）")
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
