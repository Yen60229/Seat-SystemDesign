package com.esun.seatsystem.controller;

import com.esun.seatsystem.common.dto.Announcement;
import com.esun.seatsystem.common.dto.ApiResponse;
import com.esun.seatsystem.service.AnnouncementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 展示層：公告 API。讀取需登入；新增/編輯/刪除需 ADMIN（SecurityConfig 把關）。
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ApiResponse<List<Announcement>> list() {
        return ApiResponse.ok(announcementService.list());
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody AnnouncementRequest request,
                                                  Authentication authentication) {
        String empId = (String) authentication.getPrincipal();
        long seq = announcementService.create(request.title(), request.content(), empId);
        return ApiResponse.ok(Map.of("annSeq", seq));
    }

    @PutMapping("/{annSeq}")
    public ApiResponse<Void> update(@PathVariable long annSeq,
                                    @Valid @RequestBody AnnouncementRequest request) {
        announcementService.update(annSeq, request.title(), request.content());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{annSeq}")
    public ApiResponse<Void> delete(@PathVariable long annSeq) {
        announcementService.delete(annSeq);
        return ApiResponse.ok(null);
    }

    public record AnnouncementRequest(
            @NotBlank(message = "標題不可為空") @Size(max = 100, message = "標題不可超過 100 字") String title,
            @NotBlank(message = "內容不可為空") @Size(max = 1000, message = "內容不可超過 1000 字") String content) {
    }
}
