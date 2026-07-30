package com.nursery.slideshow.bgm;

import com.nursery.slideshow.bgm.dto.BgmResponse;
import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BgmService {

    private final BgmRepository bgmRepository;
    private final StorageService storageService;

    public BgmService(BgmRepository bgmRepository, StorageService storageService) {
        this.bgmRepository = bgmRepository;
        this.storageService = storageService;
    }

    public List<BgmResponse> listActive() {
        return bgmRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public Resource loadFile(Long bgmId) {
        Bgm bgm = bgmRepository.findById(bgmId)
                .orElseThrow(() -> new ResourceNotFoundException("指定されたBGMが見つかりません"));
        return storageService.load(bgm.getStorageKey());
    }

    private BgmResponse toResponse(Bgm bgm) {
        return new BgmResponse(
                bgm.getId(),
                bgm.getCode(),
                bgm.getName(),
                "/api/bgms/" + bgm.getId() + "/file",
                bgm.getDurationSec()
        );
    }
}
