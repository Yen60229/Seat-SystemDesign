package com.esun.seatsystem.controller;

import com.esun.seatsystem.common.dto.ApiResponse;
import com.esun.seatsystem.common.dto.EmployeeInfo;
import com.esun.seatsystem.service.SeatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 展示層：員工相關 REST API（下拉選單資料來源）。
 */
@RestController
@RequestMapping("/api")
public class EmployeeController {

    private final SeatService seatService;

    public EmployeeController(SeatService seatService) {
        this.seatService = seatService;
    }

    /** 取得全部員工（含目前座位） */
    @GetMapping("/employees")
    public ApiResponse<List<EmployeeInfo>> getEmployees() {
        return ApiResponse.ok(seatService.getAllEmployees());
    }
}
