package com.nursery.slideshow.videojob.theme;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimpleThemeRenderer implements ThemeRenderer {

    @Override
    public String themeCode() {
        return "simple";
    }

    @Override
    public String frameColorHex() {
        return "0xE8D6B3";
    }

    @Override
    public String transitionName() {
        return "fade";
    }

    @Override
    public String titleFontColorHex() {
        return "0x5C4033";
    }

    @Override
    public ThemeDecoration decoration() {
        return new ThemeDecoration(
                "theme-assets/simple/background.png",
                "theme-assets/simple/frame.png",
                List.of());
    }
}
