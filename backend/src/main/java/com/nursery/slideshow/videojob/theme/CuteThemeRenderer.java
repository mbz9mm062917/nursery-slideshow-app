package com.nursery.slideshow.videojob.theme;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CuteThemeRenderer implements ThemeRenderer {

    @Override
    public String themeCode() {
        return "cute";
    }

    @Override
    public String frameColorHex() {
        return "0xFFF0F5";
    }

    @Override
    public String transitionName() {
        return "circleopen";
    }

    @Override
    public String titleFontColorHex() {
        return "0xFF69B4";
    }

    @Override
    public ThemeDecoration decoration() {
        return new ThemeDecoration(
                "theme-assets/cute/background.png",
                "theme-assets/cute/frame.png",
                List.of());
    }
}
