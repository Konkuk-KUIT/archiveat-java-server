package com.archiveat.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // 사용할 보안 스키마의 이름을 정의합니다.
        String securitySchemeName = "bearerAuth";

        // 1. 모든 API 요청 시 해당 보안 스키마(JWT 토큰)를 사용하도록 지정합니다.
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        // 2. 실제 보안 스키마의 형식을 JWT Bearer 방식으로 정의합니다.
        SecurityScheme securityScheme = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)    // HTTP 인증 방식 사용
                .scheme("bearer")                 // Bearer 인증임을 명시
                .bearerFormat("JWT");              // 토큰의 형식이 JWT임을 명시

        return new OpenAPI()
                .info(new Info()
                        .title("Archiveat API Docs")
                        .description("Archiveat 프로젝트의 백엔드 API 명세서입니다.")
                        .version("v1.0.0"))
                // 3. 전체 API에 보안 요구사항을 적용합니다.
                .addSecurityItem(securityRequirement)
                // 4. 상세한 보안 스키마 정의를 API 구성 요소(Components)에 등록합니다.
                .components(new Components().addSecuritySchemes(securitySchemeName, securityScheme));
    }
}
