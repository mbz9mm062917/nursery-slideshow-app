package com.nursery.slideshow.project.dto;

public record ProjectPatchRequest(
        String title,
        String themeCode,
        String bgmCode,
        Integer slideDurationSec
) {
}
