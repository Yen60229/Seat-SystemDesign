package com.esun.seatsystem.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtService 單元測試：聚焦簽發/驗證的 edge case。
 */
class JwtServiceTest {

    private static final String SECRET = "esun-seat-system-very-secret-key-change-me-32bytes!!";
    private final JwtService jwtService = new JwtService(SECRET, 120);

    @Test
    @DisplayName("簽發後可驗回相同的員編與角色")
    void generateAndParse_roundTrip() {
        String token = jwtService.generateToken("A0001", "ADMIN");

        JwtService.TokenInfo info = jwtService.parse(token);

        assertThat(info.empId()).isEqualTo("A0001");
        assertThat(info.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("過期的 token 會被拒絕")
    void parse_expiredToken_rejected() {
        JwtService shortLived = new JwtService(SECRET, -1); // 立即過期
        String token = shortLived.generateToken("A0001", "EMPLOYEE");

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("用不同密鑰簽的 token（等同簽章被竄改）會被拒絕")
    void parse_tamperedSignature_rejected() {
        JwtService attacker = new JwtService("another-totally-different-secret-key-32bytes-long!!", 120);
        String forged = attacker.generateToken("A0001", "ADMIN");

        assertThatThrownBy(() -> jwtService.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("格式錯誤的 token 會被拒絕")
    void parse_malformedToken_rejected() {
        assertThatThrownBy(() -> jwtService.parse("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("竄改內容後簽章不符會被拒絕")
    void parse_modifiedPayload_rejected() {
        String token = jwtService.generateToken("A0001", "EMPLOYEE");
        // 竄改 payload 區段
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x." + parts[2];

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }
}
