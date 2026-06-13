package com.esun.seatsystem.controller;

import com.esun.seatsystem.common.dto.ApiResponse;
import com.esun.seatsystem.common.dto.AuthUser;
import com.esun.seatsystem.common.security.JwtCookieFilter;
import com.esun.seatsystem.common.security.JwtService;
import com.esun.seatsystem.service.AuthService;
import com.esun.seatsystem.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 展示層：認證 API（登入 / 登出 / 取得目前使用者）。
 * 成功登入後將 JWT 種入 httpOnly + SameSite=Strict cookie。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtService jwtService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          JwtService jwtService, @Value("${app.cookie.secure}") boolean cookieSecure) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthUser>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.empId(), request.password());
        ResponseCookie cookie = buildCookie(result.token(), Duration.ofMinutes(jwtService.getExpirationMinutes()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(result.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie cookie = buildCookie("", Duration.ZERO);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUser> me(Authentication authentication) {
        String empId = (String) authentication.getPrincipal();
        return ApiResponse.ok(authService.getUser(empId));
    }

    /** 忘記密碼：索取驗證碼（模擬寄送；devCode 僅開發模式回傳） */
    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        PasswordResetService.RequestResult result = passwordResetService.requestCode(request.empId());
        Map<String, String> data = new java.util.HashMap<>();
        data.put("message", result.message());
        if (result.devCode() != null) {
            data.put("devCode", result.devCode());
        }
        return ApiResponse.ok(data);
    }

    /** 重設密碼：驗證碼 + 新密碼 */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.empId(), request.code(), request.newPassword());
        return ApiResponse.ok(null);
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(JwtCookieFilter.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record LoginRequest(
            @NotBlank(message = "員編不可為空") String empId,
            @NotBlank(message = "密碼不可為空") String password) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "員編不可為空") String empId) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "員編不可為空") String empId,
            @NotBlank(message = "驗證碼不可為空") String code,
            @NotBlank(message = "新密碼不可為空") String newPassword) {
    }
}
