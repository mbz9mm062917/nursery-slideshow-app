package com.nursery.slideshow.videojob.theme;

public interface ThemeRenderer {

    String themeCode();

    String frameColorHex();

    String transitionName();

    String titleFontColorHex();

    /**
     * フレーム画像・浮遊パーティクルなどの装飾定義。未対応テーマは装飾なし(null)のままでよい。
     */
    default ThemeDecoration decoration() {
        return null;
    }
}
