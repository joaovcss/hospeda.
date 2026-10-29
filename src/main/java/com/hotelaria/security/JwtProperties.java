package com.hotelaria.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do JWT. O segredo deve vir da variável de ambiente JWT_SECRET
 * (Base64, mínimo de 32 bytes).
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Min(1) long expirationMinutes,
        @NotBlank String issuer
) {
}
