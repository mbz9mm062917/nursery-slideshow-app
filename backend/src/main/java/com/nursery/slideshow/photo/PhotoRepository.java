package com.nursery.slideshow.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByProjectIdOrderByDisplayOrderAsc(String projectId);
    long countByProjectId(String projectId);
}
