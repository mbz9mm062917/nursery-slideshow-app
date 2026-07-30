package com.nursery.slideshow.photo.dto;

public record PhotoResponse(
        Long id,
        String originalFileName,
        Integer displayOrder,
        String fileUrl
) {
}
