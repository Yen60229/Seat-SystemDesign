package com.esun.seatsystem.service;

import com.esun.seatsystem.common.dto.LoginInfo;
import com.esun.seatsystem.common.dto.ResetCode;
import com.esun.seatsystem.common.exception.BusinessException;
import com.esun.seatsystem.common.exception.RateLimitException;
import com.esun.seatsystem.common.security.RateLimiter;
import com.esun.seatsystem.dao.AuthDao;
import com.esun.seatsystem.dao.ResetCodeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 業務層：忘記密碼與密碼重設。
 * 安全要點：防帳號列舉、驗證碼雜湊儲存 + 到期 + 單次使用 + 嘗試上限、新密碼強度檢查。
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String GENERIC_MESSAGE = "若該員編存在，系統已將 6 碼驗證碼寄送至綁定信箱";
    private static final Pattern STRONG_PWD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ResetCodeDao resetCodeDao;
    private final AuthDao authDao;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter resetLimiter;
    private final int expirationMinutes;
    private final int maxAttempts;
    private final boolean mockEmail;

    public PasswordResetService(ResetCodeDao resetCodeDao, AuthDao authDao, PasswordEncoder passwordEncoder,
                                @Qualifier("resetRateLimiter") RateLimiter resetLimiter,
                                @Value("${app.reset-code.expiration-minutes}") int expirationMinutes,
                                @Value("${app.reset-code.max-attempts}") int maxAttempts,
                                @Value("${app.mock-email}") boolean mockEmail) {
        this.resetCodeDao = resetCodeDao;
        this.authDao = authDao;
        this.passwordEncoder = passwordEncoder;
        this.resetLimiter = resetLimiter;
        this.expirationMinutes = expirationMinutes;
        this.maxAttempts = maxAttempts;
        this.mockEmail = mockEmail;
    }

    /**
     * 索取驗證碼。無論帳號是否存在都回傳相同訊息（防列舉）。
     * 帳號存在時產生 6 碼、雜湊儲存並「寄送」（模擬模式於回應/log 顯示）。
     */
    public RequestResult requestCode(String rawEmpId) {
        String empId = normalize(rawEmpId);
        if (!resetLimiter.tryAcquire(empId)) {
            throw new RateLimitException("索取驗證碼過於頻繁，請稍後再試");
        }

        LoginInfo info = authDao.findLoginInfo(empId);
        if (info == null) {
            // 帳號不存在：不洩漏，回相同訊息且不產碼
            return new RequestResult(GENERIC_MESSAGE, null);
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(code);
        resetCodeDao.createCode(empId, codeHash, LocalDateTime.now().plusMinutes(expirationMinutes));

        if (mockEmail) {
            log.info("[模擬寄送] 員編 {} 的密碼重設驗證碼：{}（{} 分鐘內有效）", empId, code, expirationMinutes);
            return new RequestResult(GENERIC_MESSAGE, code);
        }
        // 正式環境：實際寄送 email（此處略），不回傳驗證碼
        return new RequestResult(GENERIC_MESSAGE, null);
    }

    /**
     * 重設密碼。整批（標記驗證碼已用 + 更新密碼）以單一交易處理。
     */
    @Transactional
    public void resetPassword(String rawEmpId, String code, String newPassword) {
        String empId = normalize(rawEmpId);

        if (newPassword == null || !STRONG_PWD.matcher(newPassword).matches()) {
            throw new BusinessException("新密碼須至少 8 碼，且同時包含英文字母與數字");
        }

        ResetCode active = resetCodeDao.findActiveCode(empId);
        if (active == null) {
            throw new BusinessException("驗證碼無效或已過期，請重新索取");
        }
        if (active.expiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("驗證碼已過期，請重新索取");
        }
        if (active.attempts() >= maxAttempts) {
            throw new BusinessException("驗證碼錯誤次數過多，請重新索取");
        }
        if (!passwordEncoder.matches(code, active.codeHash())) {
            resetCodeDao.incrementAttempts(active.codeSeq());
            throw new BusinessException("驗證碼錯誤");
        }

        resetCodeDao.markUsed(active.codeSeq());
        authDao.updatePassword(empId, passwordEncoder.encode(newPassword));
    }

    private String normalize(String empId) {
        return empId == null ? "" : empId.trim().toUpperCase();
    }

    /** 索取結果：對外訊息一致；devCode 僅在模擬模式且帳號存在時帶值 */
    public record RequestResult(String message, String devCode) {
    }
}
