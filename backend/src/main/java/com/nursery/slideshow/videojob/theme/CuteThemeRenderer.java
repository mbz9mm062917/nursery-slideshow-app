package com.nursery.slideshow.videojob.theme;

import org.springframework.stereotype.Component;

@Component
public class CuteThemeRenderer implements ThemeRenderer {

    @Override
    public String themeCode() {
        return "cute";
    }

    @Override
    public String frameColorHex() {
        return "0xFFB6C1";
    }

    @Override
    public String transitionName() {
        return "circleopen";
    }

    @Override
    public String titleFontColorHex() {
        return "0xFF69B4";
    }
}
