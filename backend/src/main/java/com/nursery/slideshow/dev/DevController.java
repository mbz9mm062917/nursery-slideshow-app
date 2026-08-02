package com.nursery.slideshow.dev;

import com.nursery.slideshow.bgm.Bgm;
import com.nursery.slideshow.bgm.BgmRepository;
import com.nursery.slideshow.common.TempDirectoryCleanup;
import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.photo.Photo;
import com.nursery.slideshow.photo.PhotoRepository;
import com.nursery.slideshow.project.Project;
import com.nursery.slideshow.project.ProjectRepository;
import com.nursery.slideshow.videojob.ffmpeg.FfmpegExecutor;
import com.nursery.slideshow.videojob.ffmpeg.SlideshowVideoBuilder;
import com.nursery.slideshow.videojob.theme.ThemeRenderer;
import com.nursery.slideshow.videojob.theme.ThemeRendererResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Profile("dev")
@RestController
public class DevController {

    private final FfmpegExecutor ffmpegExecutor;
    private final SlideshowVideoBuilder slideshowVideoBuilder;
    private final ThemeRendererResolver themeRendererResolver;
    private final ProjectRepository projectRepository;
    private final PhotoRepository photoRepository;
    private final BgmRepository bgmRepository;
    private final StorageService storageService;

    public DevController(FfmpegExecutor ffmpegExecutor,
                          SlideshowVideoBuilder slideshowVideoBuilder,
                          ThemeRendererResolver themeRendererResolver,
                          ProjectRepository projectRepository,
                          PhotoRepository photoRepository,
                          BgmRepository bgmRepository,
                          StorageService storageService) {
        this.ffmpegExecutor = ffmpegExecutor;
        this.slideshowVideoBuilder = slideshowVideoBuilder;
        this.themeRendererResolver = themeRendererResolver;
        this.projectRepository = projectRepository;
        this.photoRepository = photoRepository;
        this.bgmRepository = bgmRepository;
        this.storageService = storageService;
    }

    @GetMapping("/api/dev/ffmpeg-version")
    public String ffmpegVersion() {
        return ffmpegExecutor.run(List.of("-version"));
    }

    @GetMapping(value = "/api/dev/ffmpeg-test-video", produces = "video/mp4")
    public ResponseEntity<Resource> ffmpegTestVideo() throws IOException {
        Path workDir = Files.createTempDirectory("ffmpeg-dev-test");
        try {
            Path imagePath = workDir.resolve("test-image.png");
            Path outputPath = workDir.resolve("test-video.mp4");

            ffmpegExecutor.run(List.of(
                    "-y", "-f", "lavfi", "-i", "testsrc=size=640x480:rate=1",
                    "-frames:v", "1", imagePath.toString()));

            slideshowVideoBuilder.generateSingleImageVideo(
                    imagePath, outputPath, 5, null, themeRendererResolver.resolve("simple"), null);

            byte[] videoBytes = Files.readAllBytes(outputPath);
            return ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4")).body(new ByteArrayResource(videoBytes));
        } finally {
            TempDirectoryCleanup.deleteQuietly(workDir);
        }
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/api/dev/ffmpeg-test-slideshow/{projectId}", produces = "video/mp4")
    public ResponseEntity<Resource> ffmpegTestSlideshow(@PathVariable String projectId) throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("指定されたスライドショーが見つかりません"));
        List<Photo> photos = photoRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        if (photos.isEmpty()) {
            throw new ResourceNotFoundException("このプロジェクトには写真がありません");
        }

        List<List<Path>> photoGroups = groupByPageBreak(photos);

        int slideDurationSec = project.getSlideDurationSec() != null ? project.getSlideDurationSec() : 3;

        Path bgmPath = null;
        if (project.getBgm() != null) {
            Bgm bgm = bgmRepository.findById(project.getBgm().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("指定されたBGMが見つかりません"));
            bgmPath = storageService.resolveLocalPath(bgm.getStorageKey());
        }

        String themeCode = project.getTheme() != null ? project.getTheme().getCode() : "simple";
        ThemeRenderer theme = themeRendererResolver.resolve(themeCode);

        Path workDir = Files.createTempDirectory("ffmpeg-dev-slideshow");
        try {
            Path outputPath = workDir.resolve("slideshow.mp4");
            slideshowVideoBuilder.generateSlideshowVideo(
                    photoGroups, outputPath, slideDurationSec, bgmPath, theme, project.getTitle());

            byte[] videoBytes = Files.readAllBytes(outputPath);
            return ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4")).body(new ByteArrayResource(videoBytes));
        } finally {
            TempDirectoryCleanup.deleteQuietly(workDir);
        }
    }

    /**
     * 表示順に並んだ写真を、pageBreakAfterの位置で1ページ(1カット)ごとのグループに分割する。
     * VideoJobService#groupByPageBreakと同等の処理(dev診断用エンドポイントのため個別に保持)。
     */
    private List<List<Path>> groupByPageBreak(List<Photo> photos) {
        List<List<Path>> groups = new ArrayList<>();
        List<Path> currentGroup = new ArrayList<>();
        for (Photo photo : photos) {
            currentGroup.add(storageService.resolveLocalPath(photo.getStorageKey()));
            if (photo.isPageBreakAfter()) {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
        }
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        return groups;
    }
}
