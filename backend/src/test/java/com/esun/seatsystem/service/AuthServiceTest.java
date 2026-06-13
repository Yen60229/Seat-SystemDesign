package com.esun.seatsystem.service;

import com.esun.seatsystem.common.dto.LoginInfo;
import com.esun.seatsystem.common.exception.AuthException;
import com.esun.seatsystem.common.exception.RateLimitException;
import com.esun.seatsystem.common.security.JwtService;
import com.esun.seatsystem.common.security.RateLimiter;
import com.esun.seatsystem.dao.AuthDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AuthService 登入邏輯測試，聚焦安全相關 edge case：
 * 防帳號列舉、密碼錯誤、限流、員編正規化。
 */
class AuthServiceTest {

    private static final String SECRET = "esun-seat-system-very-secret-key-change-me-32bytes!!";
    private static final String RAW_PASSWORD = "198503126789";

    private AuthDao authDao;
    private AuthService authService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        authDao = mock(AuthDao.class);
        JwtService jwtService = new JwtService(SECRET, 120);
        RateLimiter loginLimiter = new RateLimiter(5, Duration.ofMinutes(10));
        authService = new AuthService(authDao, encoder, jwtService, loginLimiter);
    }

    private LoginInfo validUser() {
        return new LoginInfo("A0001", "王小明", "a@example.com", "ADMIN", encoder.encode(RAW_PASSWORD));
    }

    @Test
    @DisplayName("帳密正確 → 回傳使用者資訊與 token")
    void login_success() {
        when(authDao.findLoginInfo("A0001")).thenReturn(validUser());

        AuthService.LoginResult result = authService.login("A0001", RAW_PASSWORD);

        assertThat(result.user().empId()).isEqualTo("A0001");
        assertThat(result.user().role()).isEqualTo("ADMIN");
        assertThat(result.token()).isNotBlank();
    }

    @Test
    @DisplayName("密碼錯誤 → AuthException")
    void login_wrongPassword() {
        when(authDao.findLoginInfo("A0001")).thenReturn(validUser());

        assertThatThrownBy(() -> authService.login("A0001", "wrong-password"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("帳號不存在 → 與密碼錯誤回相同訊息（防列舉）")
    void login_unknownUser_sameMessageAsWrongPassword() {
        when(authDao.findLoginInfo("A0001")).thenReturn(validUser());
        when(authDao.findLoginInfo("Z9999")).thenReturn(null);

        String wrongPwdMsg = catchMessage(() -> authService.login("A0001", "wrong-password"));
        String unknownMsg = catchMessage(() -> authService.login("Z9999", "whatever12345"));

        assertThat(unknownMsg).isEqualTo(wrongPwdMsg);
    }

    @Test
    @DisplayName("密碼雜湊為 null（未設定密碼）→ AuthException，不會 NPE")
    void login_nullHash() {
        when(authDao.findLoginInfo("A0001"))
                .thenReturn(new LoginInfo("A0001", "王小明", "a@x.com", "ADMIN", null));

        assertThatThrownBy(() -> authService.login("A0001", RAW_PASSWORD))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("員編大小寫/空白正規化：a0001 仍可登入")
    void login_normalizesEmpId() {
        when(authDao.findLoginInfo("A0001")).thenReturn(validUser());

        AuthService.LoginResult result = authService.login("  a0001 ", RAW_PASSWORD);

        assertThat(result.user().empId()).isEqualTo("A0001");
    }

    @Test
    @DisplayName("連續失敗超過上限 → RateLimitException")
    void login_rateLimited() {
        when(authDao.findLoginInfo("A0001")).thenReturn(validUser());

        for (int i = 0; i < 5; i++) {
            try {
                authService.login("A0001", "wrong-password");
            } catch (AuthException ignored) {
                // 預期的密碼錯誤
            }
        }
        // 第 6 次應被限流擋下
        assertThatThrownBy(() -> authService.login("A0001", "wrong-password"))
                .isInstanceOf(RateLimitException.class);
    }

    private String catchMessage(Runnable r) {
        try {
            r.run();
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}
