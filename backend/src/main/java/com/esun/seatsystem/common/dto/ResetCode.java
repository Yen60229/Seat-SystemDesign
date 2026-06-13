package com.esun.seatsystem.common.dto;

import java.time.LocalDateTime;

/**
 * 共用層：密碼重設驗證碼（內部使用；CODE_HASH 不回傳前端）。
 */
public record ResetCode(long codeSeq, String empId, String codeHash,
                        LocalDateTime expiresAt, int attempts, boolean used) {
}
