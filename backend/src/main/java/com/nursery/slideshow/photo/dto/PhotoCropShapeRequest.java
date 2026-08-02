package com.nursery.slideshow.photo.dto;

import java.util.Map;

/**
 * 写真ごとのトリミング形状を更新するリクエスト。キーはPhotoId、値は"RECTANGLE"/"ROUNDED"/"OVAL"。
 */
public record PhotoCropShapeRequest(Map<Long, String> cropShapes) {
}
