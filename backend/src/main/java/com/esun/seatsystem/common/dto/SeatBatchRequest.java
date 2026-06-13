package com.esun.seatsystem.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 共用層：批次座位異動請求（按「送出」時一次提交）。
 */
public record SeatBatchRequest(
        @NotEmpty(message = "異動清單不可為空") @Valid List<SeatChangeItem> changes) {
}
