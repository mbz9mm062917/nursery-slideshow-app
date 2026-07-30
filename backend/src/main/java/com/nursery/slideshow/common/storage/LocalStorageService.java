package com.nursery.slideshow.common.storage;

import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.exception.StorageException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootDir;

    public LocalStorageService(StorageProperties properties) {
        this.rootDir = Paths.get(properties.getRootDir());
    }

    @Override
    public String store(String directory, String originalFileName, InputStream content) {
        String extension = extractExtension(originalFileName);
        String storageKey = directory + "/" + UUID.randomUUID() + extension;
        Path targetPath = rootDir.resolve(storageKey);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(content, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("ファイルの保存に失敗しました", e);
        }
        return storageKey;
    }

    @Override
    public Resource load(String storageKey) {
        return new FileSystemResource(resolveLocalPath(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(rootDir.resolve(storageKey));
        } catch (IOException e) {
            throw new StorageException("ファイルの削除に失敗しました", e);
        }
    }

    @Override
    public Path resolveLocalPath(String storageKey) {
        Path path = rootDir.resolve(storageKey);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("ファイルが見つかりません");
        }
        return path;
    }

    private String extractExtension(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');
        return dotIndex >= 0 ? originalFileName.substring(dotIndex) : "";
    }
}
