package com.esun.seatsystem.common.dto;

/**
 * 共用層：登入查詢結果（含密碼雜湊，僅供後端內部驗證使用，不回傳前端）。
 */
public record LoginInfo(String empId, String name, String email, String role, String passwordHash) {
}
