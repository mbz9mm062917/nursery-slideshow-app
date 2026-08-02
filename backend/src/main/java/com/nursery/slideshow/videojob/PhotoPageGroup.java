package com.nursery.slideshow.videojob;

import java.util.List;

/**
 * 1ページ(1カット)分の写真グループ。storageKeysの並び順がそのままカード表示順になる。
 * layoutPatternはグループ内の写真の並べ方コード(例: "SIDE_BY_SIDE", "ZIGZAG")。nullならデフォルト配置。
 */
public record PhotoPageGroup(List<String> storageKeys, String layoutPattern) {
}
