package com.esun.seatsystem.common.dto;

/**
 * 共用層：員工資訊（floorSeatSeq 為 null 代表尚未配位）。
 */
public record EmployeeInfo(String empId, String name, String email,
                           Integer floorSeatSeq, Integer floorNo, Integer seatNo) {
}
