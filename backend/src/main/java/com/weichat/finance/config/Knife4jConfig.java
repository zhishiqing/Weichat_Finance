package com.weichat.finance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 配置。
 *
 * <p>Knife4j 是基于 OpenAPI 3（Swagger 3）的增强 UI，
 * 比原生 Swagger UI 提供更友好的中文界面、调试体验和文档组织。</p>
 *
 * <p>访问路径：</p>
 * <ul>
 *   <li>Knife4j UI：{@code /api/doc.html}</li>
 *   <li>OpenAPI JSON：{@code /api/v3/api-docs}</li>
 *   <li>原生 Swagger UI：{@code /api/swagger-ui.html}</li>
 * </ul>
 *
 * @author panhw
 * @since 2026-09-14
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI weichatFinanceOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Weichat_Finance API")
                .description("微信支付（APIv3）财务对接系统 接口文档\n\n"
                    + "本服务对接微信支付 V3 接口，支持 JSAPI / Native 支付下单、退款、查询等核心能力。\n\n"
                    + "**环境**：MOCK 模式（默认，本地零风险） / REAL 模式（生产，需配置真实商户凭证）\n\n"
                    + "**安全提示**：所有接口仅限内部调用，生产环境请通过网关鉴权。")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("panhw")
                    .email("zhishiqing@github.com")
                    .url("https://github.com/zhishiqing/Weichat_Finance"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")));
    }
}
