package com.esun.seatsystem.common.exception;

/**
 * 共用層：認證失敗（帳號或密碼錯誤）。訊息刻意一致，避免帳號列舉。
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }
}
