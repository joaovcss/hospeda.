package com.hotelaria.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Emissão e validação de tokens JWT assinados com HMAC-SHA.
 * O subject do token é o ID do usuário; papel e permissões seguem como claims
 * informativas para o frontend (a autorização usa sempre os dados atuais do banco).
 */
@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = buildKey(properties.secret());
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = clock.instant();
        Instant expiration = now.plus(Duration.ofMinutes(properties.expirationMinutes()));
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("email", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("permissions", user.getPermissions().stream().map(Enum::name).toList())
                .signWith(key)
                .compact();
    }

    /**
     * Valida assinatura, emissor e expiração e devolve o ID do usuário.
     *
     * @throws JwtException se o token for inválido ou estiver expirado
     */
    public Long extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new JwtException("Subject inválido no token");
        }
    }

    public long getExpirationSeconds() {
        return Duration.ofMinutes(properties.expirationMinutes()).toSeconds();
    }

    private static SecretKey buildKey(String secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException ex) {
            throw new IllegalStateException("JWT_SECRET deve estar codificado em Base64", ex);
        }
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET deve ter no mínimo " + MIN_KEY_BYTES
                    + " bytes (256 bits). Gere com: openssl rand -base64 48");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
