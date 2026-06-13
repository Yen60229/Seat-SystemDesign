package com.esun.seatsystem.common.dto;

import java.time.LocalDateTime;

/**
 * 共用層：公告。
 */
public record Announcement(long annSeq, String title, String content,
                           String createdBy, String createdByName,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
}
