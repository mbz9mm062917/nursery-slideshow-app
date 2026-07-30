package com.nursery.slideshow.videojob;

import com.nursery.slideshow.videojob.dto.VideoJobResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VideoJobController {

    private final VideoJobService videoJobService;

    public VideoJobController(VideoJobService videoJobService) {
        this.videoJobService = videoJobService;
    }

    @PostMapping("/api/projects/{projectId}/video-jobs")
    public ResponseEntity<VideoJobResponse> start(@PathVariable String projectId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(videoJobService.startGeneration(projectId));
    }

    @GetMapping("/api/video-jobs/{jobId}")
    public VideoJobResponse getStatus(@PathVariable String jobId) {
        return videoJobService.getStatus(jobId);
    }

    @GetMapping("/api/projects/{projectId}/video-jobs/latest")
    public VideoJobResponse getLatest(@PathVariable String projectId) {
        return videoJobService.getLatest(projectId);
    }

    @GetMapping("/api/video-jobs/{jobId}/download")
    public ResponseEntity<Resource> download(@PathVariable String jobId) {
        Resource resource = videoJobService.loadVideoFile(jobId);
        String fileName = "slideshow-" + jobId + ".mp4";
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(resource);
    }
}
