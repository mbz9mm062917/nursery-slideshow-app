package com.nursery.slideshow.videojob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoJobRepository extends JpaRepository<VideoJob, Long> {
    List<VideoJob> findByProjectIdAndStatusIn(String projectId, List<VideoJobStatus> statuses);
    Optional<VideoJob> findTopByProjectIdOrderByRequestedAtDesc(String projectId);
    Optional<VideoJob> findByPublicId(String publicId);
}
