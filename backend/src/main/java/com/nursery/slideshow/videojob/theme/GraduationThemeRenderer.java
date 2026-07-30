package com.nursery.slideshow.videojob.theme;

import org.springframework.stereotype.Component;

@Component
public class GraduationThemeRenderer implements ThemeRenderer {

    @Override
    public String themeCode() {
        return "graduation";
    }

    @Override
    public String frameColorHex() {
        return "0x1A237E";
    }

    @Override
    public String transitionName() {
        return "fadeblack";
    }

    @Override
    public String titleFontColorHex() {
        return "0xFFD700";
    }
}
