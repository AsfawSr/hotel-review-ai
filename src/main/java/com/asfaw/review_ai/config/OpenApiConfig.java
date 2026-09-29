package com.asfaw.review_ai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SESSION_SCHEME = "sessionCookie";

    @Bean
    public OpenAPI hotelReviewOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HotelReviewAI API")
                        .version("v1")
                        .description("""
                                JSON API for the HotelReviewAI frontend. Authenticate with POST /api/v1/auth/login; \
                                the session is kept in the JSESSIONID cookie. State-changing requests must send the \
                                XSRF-TOKEN cookie value in the X-XSRF-TOKEN header."""))
                .components(new Components().addSecuritySchemes(SESSION_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_SCHEME));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder().group("v1").pathsToMatch("/api/v1/**").build();
    }
}
