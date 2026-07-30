package com.nursery.slideshow.videojob.theme;

import com.nursery.slideshow.videojob.VideoGenerationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ThemeRendererResolver {

    private final Map<String, ThemeRenderer> renderersByCode;

    public ThemeRendererResolver(List<ThemeRenderer> renderers) {
        this.renderersByCode = renderers.stream()
                .collect(Collectors.toMap(ThemeRenderer::themeCode, Function.identity()));
    }

    public ThemeRenderer resolve(String themeCode) {
        ThemeRenderer renderer = renderersByCode.get(themeCode);
        if (renderer == null) {
            throw new VideoGenerationException("未対応のテーマです: " + themeCode);
        }
        return renderer;
    }
}
