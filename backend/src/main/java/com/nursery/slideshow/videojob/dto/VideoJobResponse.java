package com.nursery.slideshow.videojob.dto;

public record VideoJobResponse(
        Long jobId,
        String status,
        int progress,
        String errorMessage,
        String downloadUrl
) {
}
