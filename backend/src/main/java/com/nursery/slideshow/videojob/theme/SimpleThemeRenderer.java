package com.nursery.slideshow.videojob.theme;

import org.springframework.stereotype.Component;

@Component
public class SimpleThemeRenderer implements ThemeRenderer {

    @Override
    public String themeCode() {
        return "simple";
    }

    @Override
    public String frameColorHex() {
        return "0x333333";
    }

    @Override
    public String transitionName() {
        return "fade";
    }

    @Override
    public String titleFontColorHex() {
        return "0xFFFFFF";
    }
}
