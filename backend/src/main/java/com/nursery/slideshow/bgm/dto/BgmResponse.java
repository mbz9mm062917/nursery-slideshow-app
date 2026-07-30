package com.nursery.slideshow.bgm.dto;

public record BgmResponse(
        Long id,
        String code,
        String name,
        String fileUrl,
        Integer durationSec
) {
}
