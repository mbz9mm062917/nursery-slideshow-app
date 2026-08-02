package com.nursery.slideshow.photo.dto;

import java.util.List;
import java.util.Map;

/**
 * pageBreakAfterAfterIdsに含まれるPhotoIdの直後でページを区切る(=1カットが終わる)ことを表すリクエスト。
 * プロジェクト内の全写真のうち、ここに含まれないものはpageBreakAfter=falseとなり、次の写真と同じページにまとまる。
 * layoutPatternsは、ページ最後の写真のPhotoIdをキーに、そのページ内の写真の並べ方コードを指定する(未指定ならデフォルト)。
 */
public record PhotoPageBreakRequest(List<Long> pageBreakAfterPhotoIds, Map<Long, String> layoutPatterns) {
}
