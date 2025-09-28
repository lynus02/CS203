package com.lynus.cs203.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("CS203 Project API")
                        .version("1.0.0")
                        .description("""
                                API documentation for CS203 Project - User Management  and Tariff System.
                                
                                This API uses JWT Bearer token for authentication.
                                
                                **Authentication:**
                                - Obtain token from `/auth/login` endpoint
                                - Include in header: `Authorization: Bearer <token>`
                            """)
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                JWT Bearer token authentication.
                                                Obtain token from `/auth/login` endpoint.
                                                Include in header: `Authorization: Bearer <token>`
                                                """)));
    }
}
