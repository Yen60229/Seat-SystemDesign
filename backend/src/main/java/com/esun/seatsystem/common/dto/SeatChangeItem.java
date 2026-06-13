package com.esun.seatsystem.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 共用層：單筆座位異動。
 * ASSIGN：將 empId 指派到 floorSeatSeq；CLEAR：清空 floorSeatSeq 上的佔用者。
 */
public record SeatChangeItem(
        @NotNull(message = "action 不可為空") Action action,
        @Pattern(regexp = "^[A-Z0-9]{5}$", message = "員編須為 5 碼英數字") String empId,
        @NotNull(message = "floorSeatSeq 不可為空") Integer floorSeatSeq) {

    public enum Action { ASSIGN, CLEAR }
}
