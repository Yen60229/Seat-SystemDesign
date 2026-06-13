package com.esun.seatsystem.service;

import com.esun.seatsystem.common.dto.LoginInfo;
import com.esun.seatsystem.common.dto.ResetCode;
import com.esun.seatsystem.common.exception.BusinessException;
import com.esun.seatsystem.common.exception.RateLimitException;
import com.esun.seatsystem.common.security.RateLimiter;
import com.esun.seatsystem.dao.AuthDao;
import com.esun.seatsystem.dao.ResetCodeDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PasswordResetService 測試：聚焦忘記密碼 / 重設的安全 edge case。
 */
class PasswordResetServiceTest {

    private ResetCodeDao resetCodeDao;
    private AuthDao authDao;
    private PasswordResetService service;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        resetCodeDao = mock(ResetCodeDao.class);
        authDao = mock(AuthDao.class);
        RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(10));
        service = new PasswordResetService(resetCodeDao, authDao, encoder, limiter, 10, 5, true);
    }

    private LoginInfo existing() {
        return new LoginInfo("A0001", "王小明", "a@example.com", "ADMIN", encoder.encode("oldpass123"));
    }

    private ResetCode activeCode(String plainCode, LocalDateTime expiresAt, int attempts) {
        return new ResetCode(1L, "A0001", encoder.encode(plainCode), expiresAt, attempts, false);
    }

    // ---------- 索取驗證碼 ----------

    @Test
    @DisplayName("帳號存在 → 產生驗證碼（模擬模式回傳 6 碼）")
    void requestCode_existingAccount() {
        when(authDao.findLoginInfo("A0001")).thenReturn(existing());

        PasswordResetService.RequestResult result = service.requestCode("A0001");

        assertThat(result.devCode()).matches("\\d{6}");
        verify(resetCodeDao).createCode(eq("A0001"), anyString(), any());
    }

    @Test
    @DisplayName("帳號不存在 → 不產碼，但回相同訊息且無 devCode（防列舉）")
    void requestCode_unknownAccount_noEnumeration() {
        when(authDao.findLoginInfo("Z9999")).thenReturn(null);
        when(authDao.findLoginInfo("A0001")).thenReturn(existing());

        PasswordResetService.RequestResult unknown = service.requestCode("Z9999");
        PasswordResetService.RequestResult known = service.requestCode("A0001");

        assertThat(unknown.devCode()).isNull();
        assertThat(unknown.message()).isEqualTo(known.message());
        verify(resetCodeDao, never()).createCode(eq("Z9999"), anyString(), any());
    }

    @Test
    @DisplayName("索取過於頻繁 → RateLimitException")
    void requestCode_rateLimited() {
        when(authDao.findLoginInfo("A0001")).thenReturn(existing());
        for (int i = 0; i < 5; i++) {
            service.requestCode("A0001");
        }
        assertThatThrownBy(() -> service.requestCode("A0001")).isInstanceOf(RateLimitException.class);
    }

    // ---------- 重設密碼 ----------

    @Test
    @DisplayName("驗證碼正確 + 新密碼合規 → 標記已用 + 更新密碼")
    void reset_success() {
        when(resetCodeDao.findActiveCode("A0001"))
                .thenReturn(activeCode("123456", LocalDateTime.now().plusMinutes(5), 0));

        service.resetPassword("A0001", "123456", "newPass123");

        verify(resetCodeDao).markUsed(1L);
        verify(authDao).updatePassword(eq("A0001"), anyString());
    }

    @Test
    @DisplayName("無有效驗證碼 → 拒絕")
    void reset_noActiveCode() {
        when(resetCodeDao.findActiveCode("A0001")).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword("A0001", "123456", "newPass123"))
                .isInstanceOf(BusinessException.class);
        verify(authDao, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("驗證碼已過期 → 拒絕")
    void reset_expiredCode() {
        when(resetCodeDao.findActiveCode("A0001"))
                .thenReturn(activeCode("123456", LocalDateTime.now().minusMinutes(1), 0));

        assertThatThrownBy(() -> service.resetPassword("A0001", "123456", "newPass123"))
                .isInstanceOf(BusinessException.class);
        verify(authDao, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("驗證碼錯誤 → 累加嘗試次數並拒絕")
    void reset_wrongCode_incrementsAttempts() {
        when(resetCodeDao.findActiveCode("A0001"))
                .thenReturn(activeCode("123456", LocalDateTime.now().plusMinutes(5), 0));

        assertThatThrownBy(() -> service.resetPassword("A0001", "000000", "newPass123"))
                .isInstanceOf(BusinessException.class);
        verify(resetCodeDao).incrementAttempts(1L);
        verify(authDao, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("嘗試次數已達上限 → 鎖定，不再比對")
    void reset_attemptsExceeded_locked() {
        when(resetCodeDao.findActiveCode("A0001"))
                .thenReturn(activeCode("123456", LocalDateTime.now().plusMinutes(5), 5));

        assertThatThrownBy(() -> service.resetPassword("A0001", "123456", "newPass123"))
                .isInstanceOf(BusinessException.class);
        verify(authDao, never()).updatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("新密碼太弱（無數字 / 太短）→ 拒絕")
    void reset_weakPassword() {
        assertThatThrownBy(() -> service.resetPassword("A0001", "123456", "abcdefgh"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.resetPassword("A0001", "123456", "ab1"))
                .isInstanceOf(BusinessException.class);
        verify(authDao, never()).updatePassword(anyString(), anyString());
    }
}
