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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final int MAX_PHOTOS_PER_PAGE = 3;

    // グループの写真枚数ごとに選べるレイアウトパターン(いずれも写真同士が重ならない配置)
    private static final Map<Integer, Set<String>> ALLOWED_LAYOUT_PATTERNS = Map.of(
            1, Set.of("TILTED", "STRAIGHT"),
            2, Set.of("SIDE_BY_SIDE", "OFFSET"),
            3, Set.of("SIDE_BY_SIDE", "ZIGZAG"));

    // JPEG: FF D8 FF / PNG: 89 50 4E 47 0D 0A 1A 0A
    private static final byte[] JPEG_MAGIC_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC_BYTES =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

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
            if (!isValidImageFile(file)) {
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

    /**
     * pageBreakAfterPhotoIdsに含まれる写真の直後でページ(1カット)を区切る。
     * 含まれない写真は次の写真と同じページにまとまる。1ページの上限はMAX_PHOTOS_PER_PAGE枚。
     * layoutPatternsは、ページ最後の写真のIdをキーにそのページの並べ方コードを指定する(任意)。
     */
    public List<PhotoResponse> updatePageBreaks(String projectId, List<Long> pageBreakAfterPhotoIds,
                                                 Map<Long, String> layoutPatterns) {
        if (pageBreakAfterPhotoIds == null) {
            throw new ValidationException("ページ区切りの内容が正しくありません");
        }
        Map<Long, String> patterns = layoutPatterns != null ? layoutPatterns : Map.of();

        List<Photo> photos = photoRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        Set<Long> existingIds = photos.stream().map(Photo::getId).collect(Collectors.toSet());
        Set<Long> breakIds = new HashSet<>(pageBreakAfterPhotoIds);

        if (!existingIds.containsAll(breakIds) || !existingIds.containsAll(patterns.keySet())) {
            throw new ValidationException("ページ区切りの内容が正しくありません");
        }

        int groupSize = 0;
        for (Photo photo : photos) {
            groupSize++;
            if (groupSize > MAX_PHOTOS_PER_PAGE) {
                throw new ValidationException("1ページに配置できる写真は" + MAX_PHOTOS_PER_PAGE + "枚までです");
            }
            if (breakIds.contains(photo.getId())) {
                String pattern = patterns.get(photo.getId());
                if (pattern != null) {
                    Set<String> allowed = ALLOWED_LAYOUT_PATTERNS.get(groupSize);
                    if (allowed == null || !allowed.contains(pattern)) {
                        throw new ValidationException("レイアウトの指定が正しくありません");
                    }
                }
                groupSize = 0;
            }
        }

        for (Photo photo : photos) {
            boolean isPageEnd = breakIds.contains(photo.getId());
            photo.setPageBreakAfter(isPageEnd);
            photo.setLayoutPattern(isPageEnd ? patterns.get(photo.getId()) : null);
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

    private boolean isValidImageFile(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return false;
        }
        return hasMatchingMagicBytes(file, extension);
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private boolean hasMatchingMagicBytes(MultipartFile file, String extension) {
        byte[] expectedMagicBytes = switch (extension) {
            case "jpg", "jpeg" -> JPEG_MAGIC_BYTES;
            case "png" -> PNG_MAGIC_BYTES;
            default -> null;
        };
        if (expectedMagicBytes == null) {
            return false;
        }
        return Arrays.equals(readHeaderBytes(file, expectedMagicBytes.length), expectedMagicBytes);
    }

    private byte[] readHeaderBytes(MultipartFile file, int length) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PhotoResponse toResponse(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getOriginalFileName(),
                photo.getDisplayOrder(),
                "/api/photos/" + photo.getId() + "/file",
                photo.isPageBreakAfter(),
                photo.getLayoutPattern()
        );
    }
}
