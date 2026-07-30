package com.nursery.slideshow.common.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {

    String store(String directory, String originalFileName, InputStream content);

    Resource load(String storageKey);

    void delete(String storageKey);

    /**
     * FFmpeg等、ファイルシステム上のパスを必要とするツール向けに実体パスを解決する。
     * S3等の将来実装では、ここで一時ファイルへのダウンロードを行うことになる。
     */
    Path resolveLocalPath(String storageKey);
}
