package com.nursery.slideshow.videojob.ffmpeg;

import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.videojob.VideoGenerationException;
import com.nursery.slideshow.videojob.theme.ThemeDecoration;
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

    // 1カットあたり1〜3枚までの写真をポラロイド風カードとして重ねて配置する(枚数はページ編集画面で作成者が指定)
    private static final int MAX_PHOTOS_PER_SLIDE = 3;

    // 1〜2枚のカットで使う標準サイズのカード
    private static final int CARD_CONTENT_SIZE = 460;
    private static final int CARD_BORDER_SIDE = 20;
    private static final int CARD_BORDER_TOP = 20;
    private static final int CARD_BORDER_BOTTOM = 70;
    private static final int CARD_WIDTH = CARD_CONTENT_SIZE + CARD_BORDER_SIDE * 2;
    private static final int CARD_HEIGHT = CARD_CONTENT_SIZE + CARD_BORDER_TOP + CARD_BORDER_BOTTOM;

    // 3枚のカットは1枚あたりを小さくして横に並べる
    private static final int SMALL_CARD_CONTENT_SIZE = 340;
    private static final int SMALL_CARD_BORDER_SIDE = 16;
    private static final int SMALL_CARD_BORDER_TOP = 16;
    private static final int SMALL_CARD_BORDER_BOTTOM = 56;
    private static final int SMALL_CARD_WIDTH = SMALL_CARD_CONTENT_SIZE + SMALL_CARD_BORDER_SIDE * 2;
    private static final int SMALL_CARD_HEIGHT = SMALL_CARD_CONTENT_SIZE + SMALL_CARD_BORDER_TOP + SMALL_CARD_BORDER_BOTTOM;

    private final FfmpegExecutor ffmpegExecutor;
    private final FfmpegProperties ffmpegProperties;
    private final StorageService storageService;

    public SlideshowVideoBuilder(FfmpegExecutor ffmpegExecutor, FfmpegProperties ffmpegProperties,
                                  StorageService storageService) {
        this.ffmpegExecutor = ffmpegExecutor;
        this.ffmpegProperties = ffmpegProperties;
        this.storageService = storageService;
    }

    public void generateSingleImageVideo(Path imagePath, Path outputPath, int durationSec, Path bgmPath,
                                          ThemeRenderer theme, String titleText) {
        List<String> args = new ArrayList<>();
        args.add("-y");
        args.add("-loop");
        args.add("1");
        args.add("-t");
        args.add(String.valueOf(durationSec));
        args.add("-i");
        args.add(imagePath.toString());

        int decorationStartIndex = 1;
        if (bgmPath != null) {
            args.add("-stream_loop");
            args.add("-1");
            args.add("-i");
            args.add(bgmPath.toString());
            decorationStartIndex = 2;
        }

        ResolvedDecoration decoration = resolveDecoration(theme, decorationStartIndex);
        addDecorationInputs(args, decoration, durationSec);

        // -shortestは出力オプションのため、全ての-i入力(装飾素材を含む)を追加し終えた後に指定する
        if (bgmPath != null) {
            args.add("-shortest");
        }

        args.add("-filter_complex");
        args.add(buildSingleImageFilterComplex(theme, titleText, decoration));
        args.add("-map");
        args.add("[vout]");
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

    /**
     * photoGroupsは1カット(1スライド)ごとに作成者が指定した写真のグループ(1〜{@value MAX_PHOTOS_PER_SLIDE}枚)。
     * グルーピング自体はVideoJobService側(pageBreakAfterの並び)で確定済みのものをそのまま利用する。
     */
    public void generateSlideshowVideo(List<List<Path>> photoGroups, Path outputPath, int slideDurationSec,
                                        Path bgmPath, ThemeRenderer theme, String titleText) {
        if (photoGroups.isEmpty() || photoGroups.stream().allMatch(List::isEmpty)) {
            throw new VideoGenerationException("動画生成には1枚以上の写真が必要です");
        }

        List<Path> flatImagePaths = photoGroups.stream().flatMap(List::stream).toList();

        List<String> args = new ArrayList<>();
        args.add("-y");
        for (Path imagePath : flatImagePaths) {
            args.add("-loop");
            args.add("1");
            args.add("-t");
            args.add(String.valueOf(slideDurationSec));
            args.add("-i");
            args.add(imagePath.toString());
        }

        int bgmInputIndex = flatImagePaths.size();
        int decorationStartIndex = flatImagePaths.size();
        if (bgmPath != null) {
            args.add("-stream_loop");
            args.add("-1");
            args.add("-i");
            args.add(bgmPath.toString());
            decorationStartIndex = flatImagePaths.size() + 1;
        }

        double totalDurationSec = photoGroups.size() * slideDurationSec
                - (photoGroups.size() - 1) * TRANSITION_DURATION_SEC;
        ResolvedDecoration decoration = resolveDecoration(theme, decorationStartIndex);
        addDecorationInputs(args, decoration, totalDurationSec);

        // -shortestは出力オプションのため、全ての-i入力(装飾素材を含む)を追加し終えた後に指定する
        if (bgmPath != null) {
            args.add("-shortest");
        }

        args.add("-filter_complex");
        args.add(buildFullFilterComplex(photoGroups, slideDurationSec, theme, titleText, decoration));
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

    private String buildSingleImageFilterComplex(ThemeRenderer theme, String titleText, ResolvedDecoration decoration) {
        StringBuilder filter = new StringBuilder();
        filter.append("[0:v]").append(scaleAndPadFilter(theme.frameColorHex())).append(",setsar=1[vbase];");

        String currentLabel = appendDecorationOverlays(filter, "vbase", decoration);
        appendFinalStage(filter, currentLabel, theme, titleText);

        filter.setLength(filter.length() - 1);
        return filter.toString();
    }

    private String buildFullFilterComplex(List<List<Path>> slideGroups, int slideDurationSec, ThemeRenderer theme,
                                           String titleText, ResolvedDecoration decoration) {
        StringBuilder filter = new StringBuilder();

        String[] backgroundLabels = appendBackgroundSource(filter, slideGroups.size(), decoration);

        int[] inputIndexCursor = {0};
        for (int g = 0; g < slideGroups.size(); g++) {
            appendSlideGroupComposite(filter, slideGroups.get(g), inputIndexCursor, g, slideDurationSec,
                    theme.frameColorHex(), backgroundLabels[g], "v" + g);
        }

        String finalSlideLabel;
        if (slideGroups.size() == 1) {
            finalSlideLabel = "v0";
        } else {
            String previousLabel = "v0";
            double offset = slideDurationSec - TRANSITION_DURATION_SEC;
            for (int g = 1; g < slideGroups.size(); g++) {
                String currentLabel = "v" + g;
                String outputLabel = (g == slideGroups.size() - 1) ? "vbase" : "x" + g;
                filter.append('[').append(previousLabel).append("][").append(currentLabel).append(']')
                        .append("xfade=transition=").append(theme.transitionName())
                        .append(":duration=").append(TRANSITION_DURATION_SEC)
                        .append(":offset=").append(offset)
                        .append('[').append(outputLabel).append("];");
                previousLabel = outputLabel;
                offset += slideDurationSec - TRANSITION_DURATION_SEC;
            }
            finalSlideLabel = previousLabel;
        }

        String decoratedLabel = appendDecorationOverlays(filter, finalSlideLabel, decoration);
        appendFinalStage(filter, decoratedLabel, theme, titleText);

        filter.setLength(filter.length() - 1);
        return filter.toString();
    }

    /**
     * 背景装飾画像(あれば)をスライド数ぶんsplitして、各スライドで使えるラベル配列を返す。
     * 装飾に背景画像が設定されていないテーマではnull配列を返し、呼び出し側で単色背景にフォールバックする。
     */
    private String[] appendBackgroundSource(StringBuilder filter, int slideCount, ResolvedDecoration decoration) {
        String[] labels = new String[slideCount];
        if (decoration == null || decoration.decoration().backgroundAssetKey() == null) {
            return labels;
        }
        int backgroundInputIndex = decoration.startInputIndex();

        if (slideCount == 1) {
            filter.append('[').append(backgroundInputIndex).append(":v]scale=").append(VIDEO_WIDTH).append(':')
                    .append(VIDEO_HEIGHT).append("[bgsrc0];");
            labels[0] = "bgsrc0";
            return labels;
        }

        filter.append('[').append(backgroundInputIndex).append(":v]scale=").append(VIDEO_WIDTH).append(':')
                .append(VIDEO_HEIGHT).append(",split=").append(slideCount);
        for (int g = 0; g < slideCount; g++) {
            filter.append("[bgsrc").append(g).append(']');
        }
        filter.append(';');
        for (int g = 0; g < slideCount; g++) {
            labels[g] = "bgsrc" + g;
        }
        return labels;
    }

    /**
     * 1〜{@value MAX_PHOTOS_PER_SLIDE}枚の写真をポラロイド風カードとして、
     * 背景装飾(または単色背景)の上に重ねて1スライド分の映像を作る。
     */
    private void appendSlideGroupComposite(StringBuilder filter, List<Path> group, int[] inputIndexCursor,
                                            int slideIndex, int slideDurationSec, String frameColorHex,
                                            String backgroundLabel, String outputLabel) {
        String bgLabel;
        if (backgroundLabel != null) {
            bgLabel = backgroundLabel;
        } else {
            bgLabel = "bg" + slideIndex;
            filter.append("color=c=").append(frameColorHex).append(":size=").append(VIDEO_WIDTH).append('x')
                    .append(VIDEO_HEIGHT).append(":d=").append(slideDurationSec).append('[').append(bgLabel)
                    .append("];");
        }

        CardSize size = cardSizeFor(group.size());
        String currentLabel = bgLabel;
        for (int i = 0; i < group.size(); i++) {
            int inputIndex = inputIndexCursor[0]++;
            CardLayout layout = pickCardLayout(slideIndex, i, group.size(), size);
            String cardLabel = "card" + slideIndex + "_" + i;

            filter.append('[').append(inputIndex).append(":v]")
                    .append("scale=").append(size.contentSize()).append(':').append(size.contentSize())
                    .append(":force_original_aspect_ratio=decrease,")
                    .append("pad=").append(size.contentSize()).append(':').append(size.contentSize())
                    .append(":(ow-iw)/2:(oh-ih)/2:color=white,")
                    .append("pad=").append(size.width()).append(':').append(size.height()).append(':')
                    .append(size.borderSide()).append(':').append(size.borderTop()).append(":color=white,")
                    .append("format=rgba,")
                    .append("rotate=").append(layout.angleDeg()).append("*PI/180:c=black@0.0:ow=rotw(")
                    .append(layout.angleDeg()).append("*PI/180):oh=roth(").append(layout.angleDeg())
                    .append("*PI/180)[").append(cardLabel).append("];");

            String nextLabel = "slide" + slideIndex + "_ov" + i;
            filter.append('[').append(currentLabel).append("][").append(cardLabel).append(']')
                    .append("overlay=x=").append(layout.x()).append(":y=").append(layout.y())
                    .append('[').append(nextLabel).append("];");
            currentLabel = nextLabel;
        }

        filter.append('[').append(currentLabel).append(']').append("format=yuv420p,setsar=1[")
                .append(outputLabel).append("];");
    }

    private record CardSize(int contentSize, int borderSide, int borderTop, int borderBottom, int width, int height) {
    }

    private record CardLayout(double angleDeg, int x, int y) {
    }

    private CardSize cardSizeFor(int groupSize) {
        if (groupSize >= 3) {
            return new CardSize(SMALL_CARD_CONTENT_SIZE, SMALL_CARD_BORDER_SIDE, SMALL_CARD_BORDER_TOP,
                    SMALL_CARD_BORDER_BOTTOM, SMALL_CARD_WIDTH, SMALL_CARD_HEIGHT);
        }
        return new CardSize(CARD_CONTENT_SIZE, CARD_BORDER_SIDE, CARD_BORDER_TOP, CARD_BORDER_BOTTOM,
                CARD_WIDTH, CARD_HEIGHT);
    }

    /**
     * スライド内のカード配置(角度・位置)を決める。奇数番目のスライドでは左右を反転し、
     * 動画全体で同じ配置が単調に繰り返されないようにしている。
     */
    private CardLayout pickCardLayout(int slideIndex, int photoIndexInGroup, int groupSize, CardSize size) {
        boolean flip = slideIndex % 2 == 1;

        if (groupSize == 1) {
            double angle = flip ? 4 : -4;
            return new CardLayout(angle, (VIDEO_WIDTH - size.width()) / 2, (VIDEO_HEIGHT - size.height()) / 2);
        }

        if (groupSize == 2) {
            if (photoIndexInGroup == 0) {
                return flip ? new CardLayout(7, 560, 60) : new CardLayout(-7, 110, 60);
            }
            return flip ? new CardLayout(-7, 110, 130) : new CardLayout(7, 560, 130);
        }

        // groupSize == 3: 扇状に少し重ねながら横に並べる(中央のカードを少し上に、両端を少し下げる)
        int baseY = (VIDEO_HEIGHT - size.height()) / 2 - 25;
        int[] xPositions = {90, (VIDEO_WIDTH - size.width()) / 2, VIDEO_WIDTH - size.width() - 90};
        double[] angles = {-9, 2, 9};
        int[] yOffsets = {50, 0, 50};
        int index = flip ? (2 - photoIndexInGroup) : photoIndexInGroup;
        return new CardLayout(angles[index], xPositions[index], baseY + yOffsets[index]);
    }

    private void appendFinalStage(StringBuilder filter, String inputLabel, ThemeRenderer theme, String titleText) {
        String drawtextFilter = buildDrawtextFilter(titleText, theme.titleFontColorHex());
        if (drawtextFilter != null) {
            filter.append('[').append(inputLabel).append(']').append(drawtextFilter)
                    .append(",format=yuv420p[vout];");
        } else {
            filter.append('[').append(inputLabel).append(']').append("format=yuv420p[vout];");
        }
    }

    /**
     * テーマ装飾(フレーム画像・浮遊/落下パーティクル)をfilter_complexへ追加し、最終ラベル名を返す。
     * decorationがnullの場合は何もせずinputLabelをそのまま返す。
     */
    private String appendDecorationOverlays(StringBuilder filter, String inputLabel, ResolvedDecoration decoration) {
        if (decoration == null) {
            return inputLabel;
        }

        String currentLabel = inputLabel;
        int assetCursor = decoration.decoration().backgroundAssetKey() != null ? 1 : 0;
        int globalParticleIndex = 0;

        for (ThemeDecoration.ParticleLayer particle : decoration.decoration().particles()) {
            int particleInputIndex = decoration.startInputIndex() + assetCursor;
            assetCursor++;

            String splitPrefix = "particle" + particleInputIndex + "_";
            if (particle.count() == 1) {
                String particleLabel = splitPrefix + "0";
                filter.append('[').append(particleInputIndex).append(":v]format=rgba[")
                        .append(particleLabel).append("];");
                currentLabel = appendSingleParticleOverlay(filter, currentLabel, particleLabel,
                        particle.motion(), globalParticleIndex);
                globalParticleIndex++;
            } else {
                filter.append('[').append(particleInputIndex).append(":v]format=rgba,split=")
                        .append(particle.count());
                for (int k = 0; k < particle.count(); k++) {
                    filter.append('[').append(splitPrefix).append(k).append(']');
                }
                filter.append(';');
                for (int k = 0; k < particle.count(); k++) {
                    String particleLabel = splitPrefix + k;
                    currentLabel = appendSingleParticleOverlay(filter, currentLabel, particleLabel,
                            particle.motion(), globalParticleIndex);
                    globalParticleIndex++;
                }
            }
        }

        if (decoration.decoration().frameAssetKey() != null) {
            int frameInputIndex = decoration.startInputIndex() + assetCursor;
            String nextLabel = "vdecor";
            filter.append('[').append(currentLabel).append("][").append(frameInputIndex).append(":v]")
                    .append("overlay=x=0:y=0[").append(nextLabel).append("];");
            currentLabel = nextLabel;
        }

        return currentLabel;
    }

    private String appendSingleParticleOverlay(StringBuilder filter, String baseLabel, String particleLabel,
                                                ThemeDecoration.ParticleMotion motion, int particleIndex) {
        String outputLabel = "ov_" + particleLabel;
        String xExpr = particleXExpression(particleIndex);
        String yExpr = particleYExpression(motion, particleIndex);
        filter.append('[').append(baseLabel).append("][").append(particleLabel).append(']')
                .append("overlay=x='").append(xExpr).append("':y='").append(yExpr).append("':format=auto[")
                .append(outputLabel).append("];");
        return outputLabel;
    }

    private String particleXExpression(int particleIndex) {
        int baseX = 140 + (particleIndex * 733) % 1000;
        double freq = 0.7 + (particleIndex % 4) * 0.2;
        double phase = particleIndex * 1.7;
        int amplitude = 30 + (particleIndex % 3) * 10;
        return baseX + "+" + amplitude + "*sin(t*" + freq + "+" + phase + ")";
    }

    private String particleYExpression(ThemeDecoration.ParticleMotion motion, int particleIndex) {
        double speed = 45 + (particleIndex % 5) * 8;
        int yOffset = (particleIndex * 190) % 820;
        return switch (motion) {
            case FLOATING_UP -> "H-mod(t*" + speed + "+" + yOffset + ",H+100)";
            case FALLING -> "mod(t*" + speed + "+" + yOffset + ",H+100)-100";
        };
    }

    private ResolvedDecoration resolveDecoration(ThemeRenderer theme, int startInputIndex) {
        ThemeDecoration decoration = theme.decoration();
        if (decoration == null) {
            return null;
        }
        List<Path> inputPaths = new ArrayList<>();
        if (decoration.backgroundAssetKey() != null) {
            inputPaths.add(storageService.resolveLocalPath(decoration.backgroundAssetKey()));
        }
        for (ThemeDecoration.ParticleLayer particle : decoration.particles()) {
            inputPaths.add(storageService.resolveLocalPath(particle.assetKey()));
        }
        if (decoration.frameAssetKey() != null) {
            inputPaths.add(storageService.resolveLocalPath(decoration.frameAssetKey()));
        }
        return new ResolvedDecoration(decoration, startInputIndex, inputPaths);
    }

    private void addDecorationInputs(List<String> args, ResolvedDecoration decoration, double totalDurationSec) {
        if (decoration == null) {
            return;
        }
        // -loop 1の静止画入力は-tで尺を明示しないと無制限ストリームとなり、
        // overlayフィルタが終端を検出できずffmpegがハングするため、動画全体の尺に合わせて明示的に区切る。
        for (Path assetPath : decoration.inputPaths()) {
            args.add("-loop");
            args.add("1");
            args.add("-t");
            args.add(String.valueOf(totalDurationSec));
            args.add("-i");
            args.add(assetPath.toString());
        }
    }

    private record ResolvedDecoration(ThemeDecoration decoration, int startInputIndex, List<Path> inputPaths) {
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
