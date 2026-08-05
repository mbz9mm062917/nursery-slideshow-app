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

    // 1カットあたり1〜3枚までの写真を枠なしで配置する(枚数はページ編集画面で作成者が指定)
    private static final int MAX_PHOTOS_PER_SLIDE = 3;

    // 1〜2枚のカットで使う配置枠の一辺サイズ(縦横比を保ったままこの枠に収まるよう縮小し、枠の中央に配置する)
    private static final int PHOTO_TILE_SIZE = 460;

    // 3枚のカットは1枚あたりを小さくして横に並べる
    private static final int SMALL_PHOTO_TILE_SIZE = 340;

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
     * 1枚の写真のファイルパスと、そのトリミング形状コード
     * ("RECTANGLE"/"ROUNDED"/"OVAL"、nullはRECTANGLE(トリミングなし)扱い)。
     */
    public record PhotoTile(Path path, String cropShape) {
    }

    /**
     * 1ページ(1カット)分の写真タイルと、その並べ方パターン(null可、デフォルト配置を使う)。
     */
    public record SlideGroup(List<PhotoTile> photos, String layoutPattern) {
    }

    /**
     * photoGroupsは1カット(1スライド)ごとに作成者が指定した写真のグループ(1〜{@value MAX_PHOTOS_PER_SLIDE}枚)。
     * グルーピングと並べ方パターンはVideoJobService側(pageBreakAfter/layoutPatternの並び)で確定済みのものをそのまま利用する。
     */
    public void generateSlideshowVideo(List<SlideGroup> photoGroups, Path outputPath, int slideDurationSec,
                                        Path bgmPath, ThemeRenderer theme, String titleText) {
        if (photoGroups.isEmpty() || photoGroups.stream().allMatch(g -> g.photos().isEmpty())) {
            throw new VideoGenerationException("動画生成には1枚以上の写真が必要です");
        }

        List<PhotoTile> flatTiles = photoGroups.stream().flatMap(g -> g.photos().stream()).toList();

        List<String> args = new ArrayList<>();
        args.add("-y");
        for (int g = 0; g < photoGroups.size(); g++) {
            // 最後以外のスライドは、次のスライドへのクロスフェードで末尾のTRANSITION_DURATION_SEC秒分を
            // 追加で使うため、その分だけ長く入力を用意しておく(でないとスライド自体の表示時間が
            // transitionの分だけ短くなり、合計尺が「枚数×秒数」からずれてしまう)。
            boolean isLastGroup = g == photoGroups.size() - 1;
            double tileDurationSec = isLastGroup ? slideDurationSec : slideDurationSec + TRANSITION_DURATION_SEC;
            for (PhotoTile tile : photoGroups.get(g).photos()) {
                args.add("-loop");
                args.add("1");
                args.add("-t");
                args.add(String.valueOf(tileDurationSec));
                args.add("-i");
                args.add(tile.path().toString());
            }
        }

        int nextInputIndex = flatTiles.size();
        Integer bgmInputIndex = null;
        if (bgmPath != null) {
            bgmInputIndex = nextInputIndex;
            args.add("-stream_loop");
            args.add("-1");
            args.add("-i");
            args.add(bgmPath.toString());
            nextInputIndex++;
        }

        double totalDurationSec = (double) photoGroups.size() * slideDurationSec;

        ResolvedDecoration decoration = resolveDecoration(theme, nextInputIndex);
        addDecorationInputs(args, decoration, totalDurationSec);
        nextInputIndex += decoration != null ? decoration.inputPaths().size() : 0;

        CropMaskInputs cropMasks = resolveCropMaskInputs(flatTiles, nextInputIndex);
        addCropMaskInputs(args, cropMasks, totalDurationSec);

        // -shortestは出力オプションのため、全ての-i入力(装飾素材・トリミングマスクを含む)を追加し終えた後に指定する
        if (bgmPath != null) {
            args.add("-shortest");
        }

        args.add("-filter_complex");
        args.add(buildFullFilterComplex(photoGroups, slideDurationSec, theme, titleText, decoration, cropMasks));
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

    private String buildFullFilterComplex(List<SlideGroup> slideGroups, int slideDurationSec, ThemeRenderer theme,
                                           String titleText, ResolvedDecoration decoration, CropMaskInputs cropMasks) {
        StringBuilder filter = new StringBuilder();

        String[] backgroundLabels = appendBackgroundSource(filter, slideGroups.size(), decoration);
        String[] ovalMaskLabels = appendCropMaskSource(filter, cropMasks.ovalInputIndex(),
                cropMasks.ovalCount(), "omask");

        int[] inputIndexCursor = {0};
        int[] ovalMaskCursor = {0};
        for (int g = 0; g < slideGroups.size(); g++) {
            // 写真タイル側の入力尺(-t)と同じルールで、最後以外のスライドはクロスフェード分だけ
            // 単色背景の尺も延ばしておく(でないとoverlayのshortest=1で写真側の延長分が切り詰められてしまう)。
            boolean isLastGroup = g == slideGroups.size() - 1;
            double groupDurationSec = isLastGroup ? slideDurationSec : slideDurationSec + TRANSITION_DURATION_SEC;
            appendSlideGroupComposite(filter, slideGroups.get(g), inputIndexCursor, g, groupDurationSec,
                    theme.frameColorHex(), backgroundLabels[g], "v" + g, ovalMaskLabels, ovalMaskCursor);
        }

        String finalSlideLabel;
        if (slideGroups.size() == 1) {
            finalSlideLabel = "v0";
        } else {
            String previousLabel = "v0";
            // 各スライドは丸ごとslideDurationSec秒表示され、その末尾のTRANSITION_DURATION_SEC秒を使って
            // 次のスライドへクロスフェードする(表示時間を削って重ねるのではなく、表示時間はそのままに
            // 末尾で滲むように重ねるため、合計尺は常に「枚数×秒数」に一致する)。
            double offset = slideDurationSec;
            for (int g = 1; g < slideGroups.size(); g++) {
                String currentLabel = "v" + g;
                String outputLabel = (g == slideGroups.size() - 1) ? "vbase" : "x" + g;
                filter.append('[').append(previousLabel).append("][").append(currentLabel).append(']')
                        .append("xfade=transition=").append(theme.transitionName())
                        .append(":duration=").append(TRANSITION_DURATION_SEC)
                        .append(":offset=").append(offset)
                        .append('[').append(outputLabel).append("];");
                previousLabel = outputLabel;
                offset += slideDurationSec;
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
     * 1〜{@value MAX_PHOTOS_PER_SLIDE}枚の写真を枠なし・トリミングなしで(トリミング形状指定時を除く)、
     * 背景装飾(または単色背景)の上に重ねて1スライド分の映像を作る。
     */
    private void appendSlideGroupComposite(StringBuilder filter, SlideGroup group, int[] inputIndexCursor,
                                            int slideIndex, double slideDurationSec, String frameColorHex,
                                            String backgroundLabel, String outputLabel,
                                            String[] ovalMaskLabels, int[] ovalMaskCursor) {
        String bgLabel;
        if (backgroundLabel != null) {
            bgLabel = backgroundLabel;
        } else {
            bgLabel = "bg" + slideIndex;
            filter.append("color=c=").append(frameColorHex).append(":size=").append(VIDEO_WIDTH).append('x')
                    .append(VIDEO_HEIGHT).append(":d=").append(slideDurationSec).append('[').append(bgLabel)
                    .append("];");
        }

        int groupSize = group.photos().size();
        int tileSize = tileSizeFor(groupSize);
        String currentLabel = bgLabel;
        for (int i = 0; i < groupSize; i++) {
            int inputIndex = inputIndexCursor[0]++;
            PhotoTile tile = group.photos().get(i);
            CardLayout layout = pickCardLayout(slideIndex, i, groupSize, tileSize, group.layoutPattern());
            String cardLabel = "card" + slideIndex + "_" + i;

            appendPhotoTileFilter(filter, inputIndex, tileSize, layout.angleDeg(), tile.cropShape(),
                    ovalMaskLabels, ovalMaskCursor, cardLabel);

            // 写真ごとにトリミングなしで大きさが変わるため、配置枠の中心にoverlay_w/hを使って中央寄せする
            int slotCenterX = layout.x() + tileSize / 2;
            int slotCenterY = layout.y() + tileSize / 2;
            String nextLabel = "slide" + slideIndex + "_ov" + i;
            filter.append('[').append(currentLabel).append("][").append(cardLabel).append(']')
                    .append("overlay=x=").append(slotCenterX).append("-overlay_w/2:y=").append(slotCenterY)
                    .append("-overlay_h/2:shortest=1")
                    .append('[').append(nextLabel).append("];");
            currentLabel = nextLabel;
        }

        filter.append('[').append(currentLabel).append(']').append("format=yuv420p,setsar=1[")
                .append(outputLabel).append("];");
    }

    // 角丸の丸みの大きさ(短辺に対する割合)。写真の縦横比に関わらず常に真円のカーブになる(引き伸ばされない)。
    private static final double ROUNDED_CORNER_RADIUS_FRACTION = 0.08;

    /**
     * geqフィルタのalpha式。写真の実際の幅W・高さHをもとに、四隅それぞれについて
     * 「角丸みの正方形の内側かつ、その角の内接円の外側」であれば透明(0)、それ以外は不透明(255)にする。
     * min(W,H)を基準にすることで、写真の縦横比に関わらずカーブが真円のまま一定の大きさになる。
     */
    private static final String ROUNDED_CORNER_ALPHA_EXPR =
            "if(lt(X\\,(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))*lt(Y\\,(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))"
                    + "*gt(pow(X-(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)+pow(Y-(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)\\,pow(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "\\,2))"
                    + "+gt(X\\,(W-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))*lt(Y\\,(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))"
                    + "*gt(pow(X-(W-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)+pow(Y-(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)\\,pow(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "\\,2))"
                    + "+lt(X\\,(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))*gt(Y\\,(H-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))"
                    + "*gt(pow(X-(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)+pow(Y-(H-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)\\,pow(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "\\,2))"
                    + "+gt(X\\,(W-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))*gt(Y\\,(H-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "))"
                    + "*gt(pow(X-(W-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)+pow(Y-(H-min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + ")\\,2)\\,pow(min(W\\,H)*" + ROUNDED_CORNER_RADIUS_FRACTION + "\\,2))"
                    + "\\,0\\,255)";

    // OVALで、正方形に近い写真(この比率の範囲内)を丸(CIRCLE)と見分けがつかなくならないよう、
    // 少しだけ縦長にクランプする際の判定しきい値と、クランプ後の目標比率(幅/高さ)。
    private static final double OVAL_NEAR_SQUARE_RATIO_MIN = 0.87;
    private static final double OVAL_NEAR_SQUARE_RATIO_MAX = 1.15;
    private static final double OVAL_CLAMPED_RATIO = 0.85;

    /**
     * OVALの幅クロップ式。写真が正方形に近い(縦横比がOVAL_NEAR_SQUARE_RATIO_MIN〜MAXの範囲内)場合のみ、
     * 幅をih*OVAL_CLAMPED_RATIOまで削って縦長の楕円になるようにする。それ以外の写真はトリミングしない
     * (iwのまま)ため、横長・縦長の写真は従来どおり削られない。
     */
    private static final String OVAL_WIDTH_CROP_EXPR =
            "if(gt(iw/ih\\," + OVAL_NEAR_SQUARE_RATIO_MIN + ")*lt(iw/ih\\," + OVAL_NEAR_SQUARE_RATIO_MAX + ")"
                    + "\\,ih*" + OVAL_CLAMPED_RATIO + "\\,iw)";

    /**
     * 1枚の写真を、切り取らずtileSize四方に収まるよう縮小し
     * (RECTANGLEはそのまま、ROUNDEDは真円の角丸マスクを、CIRCLE/OVALは丸マスク画像でその形にトリミングし)、
     * 指定角度で回転させてcardLabelという名前のラベルに出力する。
     */
    private void appendPhotoTileFilter(StringBuilder filter, int inputIndex, int tileSize, double angleDeg,
                                        String cropShape, String[] ovalMaskLabels, int[] ovalMaskCursor,
                                        String cardLabel) {
        String preRotateLabel;
        if ("ROUNDED".equals(cropShape)) {
            // 縦横比を保ったまま縮小するだけ(トリミングなし)、角だけ真円でくり抜く
            String roundedLabel = cardLabel + "_rounded";
            filter.append('[').append(inputIndex).append(":v]scale=").append(tileSize).append(':').append(tileSize)
                    .append(":force_original_aspect_ratio=decrease,format=rgba,")
                    .append("geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='").append(ROUNDED_CORNER_ALPHA_EXPR).append("'[")
                    .append(roundedLabel).append("];");
            preRotateLabel = roundedLabel;
        } else if ("CIRCLE".equals(cropShape)) {
            // 写真の短辺に合わせて中央を正方形にトリミングしたうえで、真円にくり抜く
            String squaredLabel = cardLabel + "_squared";
            filter.append('[').append(inputIndex).append(":v]scale=").append(tileSize).append(':').append(tileSize)
                    .append(":force_original_aspect_ratio=increase,")
                    .append("crop=w='min(iw,ih)':h='min(iw,ih)'[").append(squaredLabel).append("];");
            preRotateLabel = appendOvalMask(filter, squaredLabel, ovalMaskLabels[ovalMaskCursor[0]++], cardLabel);
        } else if ("OVAL".equals(cropShape)) {
            // 縦横比を保ったまま縮小するだけ(トリミングなし)、写真の外形に沿った楕円でくり抜く。
            // ただし正方形に近い写真はCIRCLEと見分けがつかなくなるため、少しだけ縦長にクランプする。
            String scaledLabel = cardLabel + "_scaled";
            String ovalCropLabel = cardLabel + "_ovalcrop";
            filter.append('[').append(inputIndex).append(":v]scale=").append(tileSize).append(':').append(tileSize)
                    .append(":force_original_aspect_ratio=decrease[").append(scaledLabel).append("];");
            filter.append('[').append(scaledLabel).append(']')
                    .append("crop=w='").append(OVAL_WIDTH_CROP_EXPR).append("':h='ih'[")
                    .append(ovalCropLabel).append("];");
            preRotateLabel = appendOvalMask(filter, ovalCropLabel, ovalMaskLabels[ovalMaskCursor[0]++], cardLabel);
        } else {
            // RECTANGLE(デフォルト): 縦横比を保ったまま縮小するだけ、トリミングなし
            String rgbaLabel = cardLabel + "_rgba";
            filter.append('[').append(inputIndex).append(":v]scale=").append(tileSize).append(':').append(tileSize)
                    .append(":force_original_aspect_ratio=decrease,format=rgba[").append(rgbaLabel).append("];");
            preRotateLabel = rgbaLabel;
        }

        filter.append('[').append(preRotateLabel).append(']')
                .append("rotate=").append(angleDeg).append("*PI/180:c=black@0.0:ow=rotw(")
                .append(angleDeg).append("*PI/180):oh=roth(").append(angleDeg)
                .append("*PI/180)[").append(cardLabel).append("];");
    }

    /**
     * グレースケールの丸マスク画像(白=不透明/黒=透明)をscale2refで写真と同じ大きさに拡大縮小し、
     * alphamergeで写真のアルファチャンネルとして合成する。正方形の写真に適用すればCIRCLE(真円)、
     * 元の縦横比のままの写真に適用すればOVAL(楕円)になる。
     */
    private String appendOvalMask(StringBuilder filter, String photoLabel, String maskLabel, String cardLabel) {
        String maskScaledLabel = cardLabel + "_mscaled";
        String photoPassthroughLabel = cardLabel + "_pass";
        String rgbaLabel = cardLabel + "_rgba";
        String maskedLabel = cardLabel + "_masked";
        filter.append('[').append(maskLabel).append("][").append(photoLabel).append(']')
                .append("scale2ref=w=rw:h=rh[").append(maskScaledLabel).append("][")
                .append(photoPassthroughLabel).append("];");
        filter.append('[').append(photoPassthroughLabel).append(']').append("format=rgba[")
                .append(rgbaLabel).append("];");
        filter.append('[').append(rgbaLabel).append("][").append(maskScaledLabel).append(']')
                .append("alphamerge=shortest=1[").append(maskedLabel).append("];");
        return maskedLabel;
    }

    /**
     * トリミングマスク画像(あれば)を、動画全体で使う枚数ぶんsplitして、各写真タイルで使えるラベル配列を返す。
     * 該当形状の写真が1枚もない場合はinputIndexが-1となり、空配列を返す(マスク入力自体を追加していないため)。
     */
    private String[] appendCropMaskSource(StringBuilder filter, int inputIndex, int count, String labelPrefix) {
        if (count == 0) {
            return new String[0];
        }
        String[] labels = new String[count];
        if (count == 1) {
            String label = labelPrefix + "0";
            filter.append('[').append(inputIndex).append(":v]format=gray[").append(label).append("];");
            labels[0] = label;
            return labels;
        }
        filter.append('[').append(inputIndex).append(":v]format=gray,split=").append(count);
        for (int k = 0; k < count; k++) {
            filter.append('[').append(labelPrefix).append(k).append(']');
        }
        filter.append(';');
        for (int k = 0; k < count; k++) {
            labels[k] = labelPrefix + k;
        }
        return labels;
    }

    private record CardLayout(double angleDeg, int x, int y) {
    }

    private int tileSizeFor(int groupSize) {
        return groupSize >= 3 ? SMALL_PHOTO_TILE_SIZE : PHOTO_TILE_SIZE;
    }

    /**
     * スライド内の写真タイルの配置(角度・位置)を決める。奇数番目のスライドでは配置を反転し、
     * 動画全体で同じ配置が単調に繰り返されないようにしている。
     * layoutPatternは作成者がページ構成画面で選んだ並べ方(未指定ならグループ枚数ごとのデフォルト)。
     * いずれのパターンも写真同士が重ならないシンプルな配置とする。
     */
    private CardLayout pickCardLayout(int slideIndex, int photoIndexInGroup, int groupSize, int tileSize,
                                       String layoutPattern) {
        boolean flip = slideIndex % 2 == 1;

        if (groupSize == 1) {
            // TILTED(デフォルト): 少し傾ける / STRAIGHT: 傾けない
            double angle = "STRAIGHT".equals(layoutPattern) ? 0 : (flip ? 4 : -4);
            return new CardLayout(angle, (VIDEO_WIDTH - tileSize) / 2, (VIDEO_HEIGHT - tileSize) / 2);
        }

        if (groupSize == 2) {
            int gap = 40;
            int totalWidth = tileSize * 2 + gap;
            int startX = (VIDEO_WIDTH - totalWidth) / 2;
            int[] xPositions = {startX, startX + tileSize + gap};
            int baseY = (VIDEO_HEIGHT - tileSize) / 2;

            if ("OFFSET".equals(layoutPattern)) {
                // 上下に少しずらす: 横位置はそのまま、片方を少し上に、もう片方を少し下にずらす
                int shift = 50;
                int[] yPositions = flip ? new int[] {baseY + shift, baseY - shift} : new int[] {baseY - shift, baseY + shift};
                return new CardLayout(0, xPositions[photoIndexInGroup], yPositions[photoIndexInGroup]);
            }
            // SIDE_BY_SIDE(デフォルト): ただ横に並べる
            return new CardLayout(0, xPositions[photoIndexInGroup], baseY);
        }

        // groupSize == 3
        int gap = 20;
        int totalWidth = tileSize * 3 + gap * 2;
        int startX = (VIDEO_WIDTH - totalWidth) / 2;
        int[] xPositions = {startX, startX + tileSize + gap, startX + (tileSize + gap) * 2};
        int baseY = (VIDEO_HEIGHT - tileSize) / 2;

        if ("ZIGZAG".equals(layoutPattern)) {
            // 山谷に並べる: 横位置はそのまま、両端と中央を上下互い違いにずらす
            int shift = 50;
            int[] yOffsets = flip ? new int[] {shift, -shift, shift} : new int[] {-shift, shift, -shift};
            return new CardLayout(0, xPositions[photoIndexInGroup], baseY + yOffsets[photoIndexInGroup]);
        }
        // SIDE_BY_SIDE(デフォルト): ただ横に並べる
        return new CardLayout(0, xPositions[photoIndexInGroup], baseY);
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

    /**
     * ROUNDED/OVALトリミング用マスク画像の入力インデックスと、動画全体でその形状を使う写真の枚数。
     * 該当形状の写真が1枚もなければinputIndexは-1(そのマスク入力自体を-iに追加しない)。
     */
    /**
     * CIRCLE/OVALで使う丸マスク画像(masks/oval.png)の入力インデックスと、動画全体でその画像を使う写真の
     * 合計枚数(CIRCLE+OVAL)。該当形状の写真が1枚もなければinputIndexは-1(マスク入力自体を-iに追加しない)。
     * ROUNDEDはgeqフィルタで写真ごとに動的計算するため、マスク画像は使わない。
     */
    private record CropMaskInputs(int ovalInputIndex, int ovalCount, Path ovalPath) {
    }

    private CropMaskInputs resolveCropMaskInputs(List<PhotoTile> flatTiles, int startInputIndex) {
        int ovalCount = (int) flatTiles.stream()
                .filter(t -> "OVAL".equals(t.cropShape()) || "CIRCLE".equals(t.cropShape()))
                .count();

        int ovalIndex = -1;
        Path ovalPath = null;
        if (ovalCount > 0) {
            ovalIndex = startInputIndex;
            ovalPath = storageService.resolveLocalPath("masks/oval.png");
        }
        return new CropMaskInputs(ovalIndex, ovalCount, ovalPath);
    }

    private void addCropMaskInputs(List<String> args, CropMaskInputs cropMasks, double totalDurationSec) {
        if (cropMasks.ovalPath() == null) {
            return;
        }
        // -loop 1の静止画入力は-tで尺を明示しないと無制限ストリームとなり、
        // overlay/alphamergeが終端を検出できずffmpegがハングするため、動画全体の尺に合わせて明示的に区切る。
        args.add("-loop");
        args.add("1");
        args.add("-t");
        args.add(String.valueOf(totalDurationSec));
        args.add("-i");
        args.add(cropMasks.ovalPath().toString());
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
