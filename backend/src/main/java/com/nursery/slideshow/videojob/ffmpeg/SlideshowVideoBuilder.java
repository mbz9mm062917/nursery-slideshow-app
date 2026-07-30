package com.nursery.slideshow.videojob.ffmpeg;

import com.nursery.slideshow.videojob.VideoGenerationException;
import com.nursery.slideshow.videojob.theme.ThemeRenderer;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlideshowVideoBuilder {

    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;
    private static final double TRANSITION_DURATION_SEC = 1.0;
    private static final double TITLE_DISPLAY_SEC = 3.0;

    private final FfmpegExecutor ffmpegExecutor;
    private final FfmpegProperties ffmpegProperties;

    public SlideshowVideoBuilder(FfmpegExecutor ffmpegExecutor, FfmpegProperties ffmpegProperties) {
        this.ffmpegExecutor = ffmpegExecutor;
        this.ffmpegProperties = ffmpegProperties;
    }

    public void generateSingleImageVideo(Path imagePath, Path outputPath, int durationSec, Path bgmPath,
                                          ThemeRenderer theme, String titleText) {
        List<String> args = new ArrayList<>();
        args.add("-y");
        args.add("-loop");
        args.add("1");
        args.add("-i");
        args.add(imagePath.toString());
        args.add("-t");
        args.add(String.valueOf(durationSec));

        if (bgmPath != null) {
            args.add("-stream_loop");
            args.add("-1");
            args.add("-i");
            args.add(bgmPath.toString());
            args.add("-shortest");
        }

        String videoFilter = scaleAndPadFilter(theme.frameColorHex());
        String drawtextFilter = buildDrawtextFilter(titleText, theme.titleFontColorHex());
        if (drawtextFilter != null) {
            videoFilter += "," + drawtextFilter;
        }

        args.add("-vf");
        args.add(videoFilter);
        args.add("-map");
        args.add("0:v");
        if (bgmPath != null) {
            args.add("-map");
            args.add("1:a");
        }
        args.add("-c:v");
        args.add("libx264");
        args.add("-r");
        args.add("30");
        if (bgmPath != null) {
            args.add("-c:a");
            args.add("aac");
        }
        args.add(outputPath.toString());

        ffmpegExecutor.run(args);
    }

    public void generateSlideshowVideo(List<Path> imagePaths, Path outputPath, int slideDurationSec, Path bgmPath,
                                        ThemeRenderer theme, String titleText) {
        if (imagePaths.isEmpty()) {
            throw new VideoGenerationException("動画生成には1枚以上の写真が必要です");
        }
        if (imagePaths.size() == 1) {
            generateSingleImageVideo(imagePaths.get(0), outputPath, slideDurationSec, bgmPath, theme, titleText);
            return;
        }

        List<String> args = new ArrayList<>();
        args.add("-y");
        for (Path imagePath : imagePaths) {
            args.add("-loop");
            args.add("1");
            args.add("-t");
            args.add(String.valueOf(slideDurationSec));
            args.add("-i");
            args.add(imagePath.toString());
        }

        int bgmInputIndex = imagePaths.size();
        if (bgmPath != null) {
            args.add("-stream_loop");
            args.add("-1");
            args.add("-i");
            args.add(bgmPath.toString());
            args.add("-shortest");
        }

        args.add("-filter_complex");
        args.add(buildSlideshowFilterComplex(imagePaths.size(), slideDurationSec, theme, titleText));
        args.add("-map");
        args.add("[vout]");
        if (bgmPath != null) {
            args.add("-map");
            args.add(bgmInputIndex + ":a");
        }
        args.add("-c:v");
        args.add("libx264");
        args.add("-r");
        args.add("30");
        if (bgmPath != null) {
            args.add("-c:a");
            args.add("aac");
        }
        args.add(outputPath.toString());

        ffmpegExecutor.run(args);
    }

    private String buildSlideshowFilterComplex(int imageCount, int slideDurationSec, ThemeRenderer theme, String titleText) {
        StringBuilder filter = new StringBuilder();

        for (int i = 0; i < imageCount; i++) {
            filter.append('[').append(i).append(":v]").append(scaleAndPadFilter(theme.frameColorHex()))
                    .append(",setsar=1[v").append(i).append("];");
        }

        String drawtextFilter = buildDrawtextFilter(titleText, theme.titleFontColorHex());

        String previousLabel = "v0";
        double offset = slideDurationSec - TRANSITION_DURATION_SEC;
        for (int i = 1; i < imageCount; i++) {
            String currentLabel = "v" + i;
            boolean isLast = i == imageCount - 1;
            String outputLabel = isLast ? (drawtextFilter != null ? "vraw" : "vout") : "x" + i;
            filter.append('[').append(previousLabel).append("][").append(currentLabel).append(']')
                    .append("xfade=transition=").append(theme.transitionName())
                    .append(":duration=").append(TRANSITION_DURATION_SEC)
                    .append(":offset=").append(offset)
                    .append('[').append(outputLabel).append("];");
            previousLabel = outputLabel;
            offset += slideDurationSec - TRANSITION_DURATION_SEC;
        }

        if (drawtextFilter != null) {
            filter.append("[vraw]").append(drawtextFilter).append("[vout];");
        }

        filter.setLength(filter.length() - 1);
        return filter.toString();
    }

    private String scaleAndPadFilter(String frameColorHex) {
        String size = VIDEO_WIDTH + ":" + VIDEO_HEIGHT;
        return "scale=" + size + ":force_original_aspect_ratio=decrease,"
                + "pad=" + size + ":(ow-iw)/2:(oh-ih)/2:color=" + frameColorHex + ",format=yuv420p";
    }

    private String buildDrawtextFilter(String titleText, String fontColorHex) {
        if (titleText == null || titleText.isBlank()) {
            return null;
        }
        String escapedFontPath = escapeDrawtextValue(ffmpegProperties.getTitleFontPath());
        String escapedText = escapeDrawtextValue(titleText);
        return "drawtext=fontfile='" + escapedFontPath + "':text='" + escapedText + "'"
                + ":fontcolor=" + fontColorHex + ":fontsize=64:borderw=3:bordercolor=0x000000"
                + ":x=(w-text_w)/2:y=80:enable='lt(t," + TITLE_DISPLAY_SEC + ")'";
    }

    private String escapeDrawtextValue(String value) {
        return value.replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\'");
    }
}
