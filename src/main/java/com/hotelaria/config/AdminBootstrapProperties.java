package com.hotelaria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dados do primeiro administrador, lidos de variáveis de ambiente
 * (APP_BOOTSTRAP_ADMIN_*). Nenhuma senha fica fixa no código.
 */
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record AdminBootstrapProperties(
        boolean enabled,
        String name,
        String email,
        String password
) {
}
