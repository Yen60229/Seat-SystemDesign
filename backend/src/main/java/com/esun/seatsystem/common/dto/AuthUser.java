package com.esun.seatsystem.common.dto;

/**
 * 共用層：對前端公開的使用者資訊（不含任何密碼資料）。
 */
public record AuthUser(String empId, String name, String role) {
}
