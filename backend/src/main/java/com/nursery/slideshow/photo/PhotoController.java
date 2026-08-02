package com.nursery.slideshow.photo;

import com.nursery.slideshow.photo.dto.PhotoOrderRequest;
import com.nursery.slideshow.photo.dto.PhotoPageBreakRequest;
import com.nursery.slideshow.photo.dto.PhotoResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping("/api/projects/{projectId}/photos")
    public ResponseEntity<List<PhotoResponse>> upload(@PathVariable String projectId,
                                                        @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(photoService.upload(projectId, files));
    }

    @GetMapping("/api/projects/{projectId}/photos")
    public List<PhotoResponse> list(@PathVariable String projectId) {
        return photoService.list(projectId);
    }

    @PutMapping("/api/projects/{projectId}/photos/order")
    public List<PhotoResponse> reorder(@PathVariable String projectId, @RequestBody PhotoOrderRequest request) {
        return photoService.reorder(projectId, request.photoIds());
    }

    @PutMapping("/api/projects/{projectId}/photos/page-breaks")
    public List<PhotoResponse> updatePageBreaks(@PathVariable String projectId,
                                                 @RequestBody PhotoPageBreakRequest request) {
        return photoService.updatePageBreaks(projectId, request.pageBreakAfterPhotoIds());
    }

    @DeleteMapping("/api/photos/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable Long photoId) {
        photoService.delete(photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/photos/{photoId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long photoId) {
        return ResponseEntity.ok(photoService.loadFile(photoId));
    }
}
