package com.hotelaria.security;

import com.hotelaria.models.enums.PermissionName;
import com.hotelaria.models.enums.RoleName;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[48]);
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    private final AuthenticatedUser user = new AuthenticatedUser(42L, "user@hotel.com", "Usuário",
            RoleName.ROLE_EMPLOYEE, null, EnumSet.of(PermissionName.CHECKIN));

    private JwtService service(String secret, Instant now) {
        return new JwtService(new JwtProperties(secret, 60, "hotelaria-test"), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Token gerado identifica o usuário")
    void roundTrip() {
        JwtService service = service(SECRET, NOW);

        assertThat(service.extractUserId(service.generateToken(user))).isEqualTo(42L);
    }

    @Test
    @DisplayName("Token expirado é rejeitado")
    void expiredTokenIsRejected() {
        String token = service(SECRET, NOW).generateToken(user);
        JwtService later = service(SECRET, NOW.plus(Duration.ofMinutes(61)));

        assertThatThrownBy(() -> later.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token assinado com outra chave é rejeitado")
    void tamperedTokenIsRejected() {
        byte[] otherKey = new byte[48];
        otherKey[0] = 1;
        String token = service(Base64.getEncoder().encodeToString(otherKey), NOW).generateToken(user);

        assertThatThrownBy(() -> service(SECRET, NOW).extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Segredo curto demais impede a inicialização")
    void shortSecretFailsFast() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> service(shortSecret, NOW)).isInstanceOf(IllegalStateException.class);
    }
}
