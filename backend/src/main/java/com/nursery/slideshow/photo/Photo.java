package com.nursery.slideshow.photo;

import com.nursery.slideshow.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, columnDefinition = "CHAR(36)")
    private Project project;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * このPhotoの直後でページ(1カット)を区切るかどうか。
     * trueの場合、このPhotoで1ページが終わり、次のPhotoから新しいページになる。
     * デフォルトtrue(=1枚1ページ)。falseにすると次の写真と同じページにまとめられる。
     */
    @Column(name = "page_break_after", nullable = false)
    private boolean pageBreakAfter = true;

    /**
     * このPhotoがページの最後(pageBreakAfter=true)のとき、そのページ内の写真の並べ方を表すコード。
     * 例: 1枚ページなら"TILTED"/"STRAIGHT"、2枚ページなら"SIDE_BY_SIDE"/"OFFSET"、
     * 3枚ページなら"SIDE_BY_SIDE"/"ZIGZAG"。nullの場合はグループ枚数ごとのデフォルトパターンを使う。
     */
    @Column(name = "layout_pattern")
    private String layoutPattern;

    /**
     * この写真のトリミング形状を表すコード。"RECTANGLE"(そのまま・デフォルト)、"ROUNDED"(角丸)、
     * "CIRCLE"(丸)、"OVAL"(楕円)。nullの場合はRECTANGLE(トリミングなし)として扱う。
     */
    @Column(name = "crop_shape")
    private String cropShape;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
