package com.esun.seatsystem.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 共用層：JWT 簽發與驗證（HS256）。
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /** 簽發 token，subject=員編，並帶入角色 */
    public String generateToken(String empId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
                .subject(empId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /**
     * 驗證並解析 token。簽章不符、過期、格式錯誤皆拋出 {@link io.jsonwebtoken.JwtException}。
     */
    public TokenInfo parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new TokenInfo(claims.getSubject(), claims.get("role", String.class));
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    /** 解析後的 token 內容 */
    public record TokenInfo(String empId, String role) {
    }
}
