package com.esun.seatsystem.common.dto;

/**
 * 共用層：座位資訊（empId 為 null 代表空位）。
 * posRow/posCol 供前端擬真格局定位；seatType 區分 NORMAL / MANAGER。
 */
public record SeatInfo(
        int floorSeatSeq,
        int floorNo,
        int seatNo,
        int posRow,
        int posCol,
        String seatType,
        String empId,
        String empName) {
}
