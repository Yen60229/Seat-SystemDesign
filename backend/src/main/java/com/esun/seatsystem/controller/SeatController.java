package com.esun.seatsystem.controller;

import com.esun.seatsystem.common.dto.ApiResponse;
import com.esun.seatsystem.common.dto.SeatBatchRequest;
import com.esun.seatsystem.common.dto.SeatInfo;
import com.esun.seatsystem.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 展示層：座位相關 REST API。
 */
@RestController
@RequestMapping("/api")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    /** 取得所有樓層 */
    @GetMapping("/floors")
    public ApiResponse<List<Integer>> getFloors() {
        return ApiResponse.ok(seatService.getFloors());
    }

    /** 取得指定樓層全部座位（含佔用狀態） */
    @GetMapping("/floors/{floorNo}/seats")
    public ApiResponse<List<SeatInfo>> getSeats(@PathVariable int floorNo) {
        return ApiResponse.ok(seatService.getSeatsByFloor(floorNo));
    }

    /** 批次送出座位異動（整批單一交易） */
    @PostMapping("/seats/batch")
    public ApiResponse<Void> applyChanges(@Valid @RequestBody SeatBatchRequest request) {
        seatService.applyChanges(request.changes());
        return ApiResponse.ok(null);
    }
}
