package com.esun.seatsystem.common.exception;

import com.esun.seatsystem.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 共用層：全域例外處理，統一轉成 ApiResponse 格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 業務規則錯誤（例如座位已被佔用），整批交易已回滾 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.error("B001", ex.getMessage());
    }

    /** 認證失敗（帳號或密碼錯誤），訊息一致避免帳號列舉 */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuth(AuthException ex) {
        return ApiResponse.error("A401", ex.getMessage());
    }

    /** 操作過於頻繁 */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiResponse<Void> handleRateLimit(RateLimitException ex) {
        return ApiResponse.error("A429", ex.getMessage());
    }

    /** 輸入驗證錯誤（員編格式、必填欄位） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("輸入格式錯誤");
        return ApiResponse.error("V001", message);
    }

    /** 其他未預期錯誤：不回傳內部細節，避免洩漏資訊 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception ex) {
        log.error("未預期錯誤", ex);
        return ApiResponse.error("E999", "系統發生錯誤，請稍後再試");
    }
}
