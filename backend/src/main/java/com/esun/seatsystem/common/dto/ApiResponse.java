package com.esun.seatsystem.common.dto;

/**
 * 共用層：統一 API 回應格式。
 * code 0000 表示成功，其餘為錯誤代碼。
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("0000", "成功", data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
