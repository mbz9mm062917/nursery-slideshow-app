package com.nursery.slideshow.common;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * FFmpeg処理などで作成される一時ディレクトリを、正常終了・例外終了を問わず確実に削除するためのユーティリティ。
 * 呼び出し側は try-finally の finally 節から呼び出すことを想定している。
 */
@Slf4j
public final class TempDirectoryCleanup {

    private TempDirectoryCleanup() {
    }

    public static void deleteQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("一時ディレクトリの削除に失敗しました: {}", directory, e);
        }
    }
}
