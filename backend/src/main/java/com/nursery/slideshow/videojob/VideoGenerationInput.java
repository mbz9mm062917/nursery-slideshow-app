package com.nursery.slideshow.videojob;

import java.util.List;

public record VideoGenerationInput(
        List<List<String>> photoGroups,
        int slideDurationSec,
        String themeCode,
        String bgmStorageKey,
        String title) {
}
