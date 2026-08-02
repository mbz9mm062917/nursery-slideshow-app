package com.nursery.slideshow.videojob.theme;

import java.util.List;

/**
 * テーマに付与する装飾(背景画像・フレーム画像・浮遊/落下パーティクル)の定義。
 * 未対応テーマは {@link ThemeRenderer#decoration()} が null を返し、装飾なし(従来どおり)となる。
 * backgroundAssetKeyがnullの場合は、従来どおりframeColorHexの単色背景になる。
 */
public record ThemeDecoration(String backgroundAssetKey, String frameAssetKey, List<ParticleLayer> particles) {

    public record ParticleLayer(String assetKey, int count, ParticleMotion motion) {
    }

    public enum ParticleMotion {
        /** ゆっくり上昇しながら左右に揺れる(例: かわいいテーマのハート・星) */
        FLOATING_UP,
        /** ゆっくり落下しながら左右に揺れる(例: 卒業式テーマの桜の花びら) */
        FALLING
    }
}
