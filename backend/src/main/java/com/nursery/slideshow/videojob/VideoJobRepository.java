package com.nursery.slideshow.videojob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoJobRepository extends JpaRepository<VideoJob, Long> {
    List<VideoJob> findByProjectIdAndStatusIn(String projectId, List<VideoJobStatus> statuses);
    Optional<VideoJob> findTopByProjectIdAndStatusOrderByCompletedAtDesc(String projectId, VideoJobStatus status);
    Optional<VideoJob> findByPublicId(String publicId);
}
