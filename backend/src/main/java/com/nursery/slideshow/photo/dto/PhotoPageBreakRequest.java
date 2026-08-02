package com.nursery.slideshow.photo.dto;

import java.util.List;

/**
 * pageBreakAfterAfterIdsに含まれるPhotoIdの直後でページを区切る(=1カットが終わる)ことを表すリクエスト。
 * プロジェクト内の全写真のうち、ここに含まれないものはpageBreakAfter=falseとなり、次の写真と同じページにまとまる。
 */
public record PhotoPageBreakRequest(List<Long> pageBreakAfterPhotoIds) {
}
