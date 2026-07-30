package com.nursery.slideshow.videojob.dto;

public record VideoJobResponse(
        String jobId,
        String status,
        int progress,
        String errorMessage,
        String downloadUrl
) {
}
