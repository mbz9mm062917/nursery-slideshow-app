package com.nursery.slideshow.videojob.ffmpeg;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ffmpeg")
public class FfmpegProperties {
    private String binaryPath = "ffmpeg";
    private String titleFontPath = "C:/Windows/Fonts/meiryo.ttc";
}
