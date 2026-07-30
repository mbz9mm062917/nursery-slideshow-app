package com.nursery.slideshow.project.dto;

import java.time.LocalDateTime;

public record ProjectResponse(
        String id,
        String title,
        String themeCode,
        String bgmCode,
        Integer slideDurationSec,
        long photoCount,
        LocalDateTime createdAt
) {
}
