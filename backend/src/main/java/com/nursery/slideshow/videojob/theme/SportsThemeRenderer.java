package com.nursery.slideshow.videojob.theme;

import org.springframework.stereotype.Component;

@Component
public class SportsThemeRenderer implements ThemeRenderer {

    @Override
    public String themeCode() {
        return "sports";
    }

    @Override
    public String frameColorHex() {
        return "0xFF5722";
    }

    @Override
    public String transitionName() {
        return "slideleft";
    }

    @Override
    public String titleFontColorHex() {
        return "0xFFFFFF";
    }
}
