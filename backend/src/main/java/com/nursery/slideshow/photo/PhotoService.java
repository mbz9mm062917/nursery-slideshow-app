package com.nursery.slideshow.photo;

import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.exception.ValidationException;
import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.photo.dto.PhotoResponse;
import com.nursery.slideshow.project.Project;
import com.nursery.slideshow.project.ProjectRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PhotoService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final PhotoRepository photoRepository;
    private final ProjectRepository projectRepository;
    private final StorageService storageService;

    public PhotoService(PhotoRepository photoRepository, ProjectRepository projectRepository, StorageService storageService) {
        this.photoRepository = photoRepository;
        this.projectRepository = projectRepository;
        this.storageService = storageService;
    }

    public List<PhotoResponse> upload(String projectId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ValidationException("アップロードする写真を選択してください");
        }

        Project project = findProjectOrThrow(projectId);

        for (MultipartFile file : files) {
            if (!hasAllowedExtension(file.getOriginalFilename())) {
                throw new ValidationException(
                        "対応していない形式のファイルが含まれています（jpg, jpeg, pngのみ利用できます）: " + file.getOriginalFilename());
            }
        }

        int nextOrder = (int) photoRepository.countByProjectId(projectId);
        List<Photo> saved = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            Photo photo = new Photo();
            photo.setProject(project);
            photo.setStorageKey(storeFile(projectId, file));
            photo.setOriginalFileName(file.getOriginalFilename());
            photo.setDisplayOrder(nextOrder + i);
            saved.add(photoRepository.save(photo));
        }

        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> list(String projectId) {
        return photoRepository.findByProjectIdOrderByDisplayOrderAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PhotoResponse> reorder(String projectId, List<Long> photoIds) {
        if (photoIds == null) {
            throw new ValidationException("並び替えの内容が正しくありません");
        }

        List<Photo> photos = photoRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        Set<Long> existingIds = photos.stream().map(Photo::getId).collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(photoIds);

        if (requestedIds.size() != photoIds.size() || !requestedIds.equals(existingIds)) {
            throw new ValidationException("並び替えの内容が正しくありません");
        }

        Map<Long, Photo> photoById = photos.stream().collect(Collectors.toMap(Photo::getId, p -> p));
        for (int i = 0; i < photoIds.size(); i++) {
            photoById.get(photoIds.get(i)).setDisplayOrder(i);
        }

        return list(projectId);
    }

    public void delete(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("指定された写真が見つかりません"));
        storageService.delete(photo.getStorageKey());
        photoRepository.delete(photo);
    }

    @Transactional(readOnly = true)
    public Resource loadFile(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("指定された写真が見つかりません"));
        return storageService.load(photo.getStorageKey());
    }

    private Project findProjectOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("指定されたスライドショーが見つかりません"));
    }

    private String storeFile(String projectId, MultipartFile file) {
        try {
            return storageService.store("projects/" + projectId + "/photos", file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean hasAllowedExtension(String fileName) {
        if (fileName == null) {
            return false;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return false;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private PhotoResponse toResponse(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getOriginalFileName(),
                photo.getDisplayOrder(),
                "/api/photos/" + photo.getId() + "/file"
        );
    }
}
