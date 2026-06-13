package com.esun.seatsystem.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共用層：以 key（例如員編）為單位的滑動視窗計數限流。
 * 記憶體實作，適用單機；正式環境多台分流時應改用 Redis 等共享儲存。
 */
public class RateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    /** 記錄一次嘗試並回傳是否仍在允許範圍內（true=允許，false=已超限） */
    public synchronized boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Window w = windows.get(key);
        if (w == null || now.isAfter(w.resetAt)) {
            windows.put(key, new Window(1, now.plus(window)));
            return true;
        }
        if (w.count >= maxAttempts) {
            return false;
        }
        w.count++;
        return true;
    }

    /** 清除某 key 的計數（例如登入成功後） */
    public synchronized void reset(String key) {
        windows.remove(key);
    }

    private static final class Window {
        int count;
        final Instant resetAt;

        Window(int count, Instant resetAt) {
            this.count = count;
            this.resetAt = resetAt;
        }
    }
}
