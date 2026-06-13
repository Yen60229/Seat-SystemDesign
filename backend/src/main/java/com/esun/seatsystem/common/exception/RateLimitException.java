package com.esun.seatsystem.common.exception;

/**
 * 共用層：操作過於頻繁（超過速率限制）。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
