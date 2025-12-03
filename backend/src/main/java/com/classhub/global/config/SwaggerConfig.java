package com.classhub.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI classHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClassHub API")
                        .description("ClassHub 서비스 백엔드 OpenAPI 문서")
                        .version("v1.0.0")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("Auth")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi invitationApi() {
        return GroupedOpenApi.builder()
                .group("Invitation")
                .pathsToMatch("/api/v1/invitations/**")
                .build();
    }

    @Bean
    public GroupedOpenApi studentProfileApi() {
        return GroupedOpenApi.builder()
                .group("StudentProfile")
                .pathsToMatch(
                        "/api/v1/student-profiles/**",
                        "/api/v1/courses/*/students"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi personalLessonApi() {
        return GroupedOpenApi.builder()
                .group("PersonalLesson")
                .pathsToMatch("/api/v1/personal-lessons/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("All")
                .pathsToMatch("/**")
                .pathsToExclude("/error")
                .build();
    }
}
