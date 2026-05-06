package com.user.returnsassistant.utils;

import com.user.returnsassistant.pojo.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${app.auth.jwt-secret}")
    private String jwtSecret;
    @Value("${app.auth.jwt-issuer:returns-assistant}")
    private String jwtIssuer;

    public JwtToken createToken(UserAccount user, Duration ttl) {
        long now = System.currentTimeMillis();
        long expiresAt = now + ttl.toMillis();
        String jwtId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jwtId)
                .issuer(jwtIssuer)
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("displayName", user.getDisplayName())
                .claim("role", user.getRole())
                .issuedAt(new Date(now))
                .expiration(new Date(expiresAt))
                .signWith(signingKey())
                .compact();
        return new JwtToken(token, jwtId, expiresAt);
    }

    public JwtUser parseToken(String token) {
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(jwtIssuer)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = parsed.getPayload();
            if (claims.getSubject() == null || claims.getId() == null || claims.getExpiration() == null) {
                throw new IllegalArgumentException("JWT missing required claims");
            }
            return new JwtUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", String.class),
                    claims.getId(),
                    claims.getExpiration().getTime()
            );
        } catch (IllegalArgumentException | JwtException e) {
            throw new IllegalArgumentException("登录已过期，请重新登录", e);
        }
    }

    private SecretKey signingKey() {
        String secret = jwtSecret == null ? "" : jwtSecret.trim();
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.auth.jwt-secret must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public record JwtToken(String token, String jwtId, long expiresAt) {
    }

    public record JwtUser(Long userId, String username, String role, String jwtId, long expiresAt) {
    }
}
