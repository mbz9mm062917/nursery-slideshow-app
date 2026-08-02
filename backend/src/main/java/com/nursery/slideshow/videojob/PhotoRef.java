package com.nursery.slideshow.videojob;

/**
 * 1枚の写真のストレージキーと、その写真のトリミング形状コード
 * ("RECTANGLE"/"ROUNDED"/"OVAL"、nullならRECTANGLE扱い)。
 */
public record PhotoRef(String storageKey, String cropShape) {
}
