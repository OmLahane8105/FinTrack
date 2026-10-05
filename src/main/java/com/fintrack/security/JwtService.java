package com.fintrack.security;

import com.fintrack.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration}") long accessExpiration,
            @Value("${app.jwt.refresh-expiration}") long refreshExpiration
    ) {
        if (secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 characters"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(User user) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + accessExpiration
        );

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + refreshExpiration
        );

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object value = extractAllClaims(token).get("userId");

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(value.toString());
    }

    public String extractRole(String token) {
        return extractAllClaims(token)
                .get("role", String.class);
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token)
                .get("tokenType", String.class);
    }

    public boolean isTokenValid(String token, String username) {

        try {

            Claims claims = extractAllClaims(token);

            return claims.getSubject().equals(username)
                    && claims.getExpiration().after(new Date());

        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {

        try {
            return "ACCESS".equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {

        try {
            return "REFRESH".equals(extractTokenType(token));
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessExpiration() {
        return accessExpiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}