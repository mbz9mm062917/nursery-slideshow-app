package com.nursery.slideshow.videojob;

import java.util.List;

public record VideoGenerationInput(
        List<String> photoStorageKeys,
        int slideDurationSec,
        String themeCode,
        String bgmStorageKey,
        String title) {
}
