package com.esun.seatsystem.service;

import com.esun.seatsystem.common.dto.AuthUser;
import com.esun.seatsystem.common.dto.LoginInfo;
import com.esun.seatsystem.common.exception.AuthException;
import com.esun.seatsystem.common.exception.RateLimitException;
import com.esun.seatsystem.common.security.JwtService;
import com.esun.seatsystem.common.security.RateLimiter;
import com.esun.seatsystem.dao.AuthDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 業務層：登入認證。
 * 安全要點：帳號不存在與密碼錯誤回相同訊息（防列舉）、登入失敗限流。
 */
@Service
public class AuthService {

    private static final String GENERIC_ERROR = "帳號或密碼錯誤";

    private final AuthDao authDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiter loginLimiter;

    public AuthService(AuthDao authDao, PasswordEncoder passwordEncoder,
                       JwtService jwtService, @Qualifier("loginRateLimiter") RateLimiter loginLimiter) {
        this.authDao = authDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginLimiter = loginLimiter;
    }

    /** 登入：成功回傳使用者與 JWT；失敗拋出 AuthException / RateLimitException */
    public LoginResult login(String rawEmpId, String rawPassword) {
        String empId = normalize(rawEmpId);

        if (!loginLimiter.tryAcquire(empId)) {
            throw new RateLimitException("登入嘗試過於頻繁，請稍後再試");
        }

        LoginInfo info = authDao.findLoginInfo(empId);
        // 帳號不存在或無密碼：與密碼錯誤一致處理，避免洩漏帳號是否存在
        if (info == null || info.passwordHash() == null
                || !passwordEncoder.matches(rawPassword, info.passwordHash())) {
            throw new AuthException(GENERIC_ERROR);
        }

        loginLimiter.reset(empId);
        String token = jwtService.generateToken(info.empId(), info.role());
        return new LoginResult(new AuthUser(info.empId(), info.name(), info.role()), token);
    }

    /** 取得目前登入者的公開資訊（供 /me 使用） */
    public AuthUser getUser(String empId) {
        LoginInfo info = authDao.findLoginInfo(normalize(empId));
        if (info == null) {
            throw new AuthException("使用者不存在");
        }
        return new AuthUser(info.empId(), info.name(), info.role());
    }

    private String normalize(String empId) {
        return empId == null ? "" : empId.trim().toUpperCase();
    }

    /** 登入結果：對外使用者資訊 + 簽發的 token */
    public record LoginResult(AuthUser user, String token) {
    }
}
