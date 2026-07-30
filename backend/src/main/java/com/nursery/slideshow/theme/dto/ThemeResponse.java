package com.nursery.slideshow.theme.dto;

public record ThemeResponse(
        Long id,
        String code,
        String name,
        String thumbnailUrl
) {
}
