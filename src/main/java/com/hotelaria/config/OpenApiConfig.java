package com.hotelaria.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI hotelariaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Gestão Hoteleira - API")
                        .version("1.0.0")
                        .description("""
                                API REST para administradores, funcionários e hóspedes.

                                **Autenticação:** faça login em `POST /api/auth/login`, copie o `accessToken` \
                                e clique em **Authorize** informando apenas o token. \
                                O frontend deve enviar o cabeçalho `Authorization: Bearer <token>` em todas \
                                as requisições protegidas."""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
