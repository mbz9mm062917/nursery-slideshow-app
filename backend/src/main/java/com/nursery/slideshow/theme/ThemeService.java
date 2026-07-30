package com.nursery.slideshow.theme;

import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.theme.dto.ThemeResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final StorageService storageService;

    public ThemeService(ThemeRepository themeRepository, StorageService storageService) {
        this.themeRepository = themeRepository;
        this.storageService = storageService;
    }

    public List<ThemeResponse> listActive() {
        return themeRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public Resource loadThumbnail(Long themeId) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("指定されたテーマが見つかりません"));
        return storageService.load(theme.getThumbnailStorageKey());
    }

    private ThemeResponse toResponse(Theme theme) {
        return new ThemeResponse(
                theme.getId(),
                theme.getCode(),
                theme.getName(),
                "/api/themes/" + theme.getId() + "/thumbnail"
        );
    }
}
