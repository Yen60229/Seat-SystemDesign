package com.esun.seatsystem.common.exception;

/**
 * 共用層：業務規則違反時拋出（例如座位已被佔用）。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
