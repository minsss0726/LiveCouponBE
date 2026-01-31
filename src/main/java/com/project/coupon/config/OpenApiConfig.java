package com.project.coupon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI(Swagger) 문서 설정.
 * API 문서 제목, 설명, 서버 URL 등을 정의합니다.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Live Coupon API")
                        .description("선착순 쿠폰 발급 서비스 API. 이벤트·쿠폰 조회, 쿠폰 발급, 마이페이지 등 프론트엔드 연동용 문서입니다.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Live Coupon Project")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server")));
    }
}
