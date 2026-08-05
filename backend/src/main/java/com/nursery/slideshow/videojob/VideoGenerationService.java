package com.nursery.slideshow.videojob;

import com.nursery.slideshow.common.TempDirectoryCleanup;
import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.videojob.ffmpeg.SlideshowVideoBuilder;
import com.nursery.slideshow.videojob.theme.ThemeRenderer;
import com.nursery.slideshow.videojob.theme.ThemeRendererResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class VideoGenerationService {

    private final VideoJobService videoJobService;
    private final SlideshowVideoBuilder slideshowVideoBuilder;
    private final ThemeRendererResolver themeRendererResolver;
    private final StorageService storageService;

    public VideoGenerationService(VideoJobService videoJobService,
                                   SlideshowVideoBuilder slideshowVideoBuilder,
                                   ThemeRendererResolver themeRendererResolver,
                                   StorageService storageService) {
        this.videoJobService = videoJobService;
        this.slideshowVideoBuilder = slideshowVideoBuilder;
        this.themeRendererResolver = themeRendererResolver;
        this.storageService = storageService;
    }

    @Async("videoGenerationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVideoGenerationRequested(VideoGenerationRequestedEvent event) {
        generate(event.jobId());
    }

    private void generate(Long jobId) {
        Path workDir = null;
        try {
            videoJobService.markProcessing(jobId);

            VideoGenerationInput input = videoJobService.loadGenerationInput(jobId);
            List<SlideshowVideoBuilder.SlideGroup> photoGroups = input.photoGroups().stream()
                    .map(group -> new SlideshowVideoBuilder.SlideGroup(
                            group.photos().stream()
                                    .map(ref -> new SlideshowVideoBuilder.PhotoTile(
                                            storageService.resolveLocalPath(ref.storageKey()), ref.cropShape()))
                                    .toList(),
                            group.layoutPattern()))
                    .toList();
            Path bgmPath = input.bgmStorageKey() != null
                    ? storageService.resolveLocalPath(input.bgmStorageKey())
                    : null;
            ThemeRenderer theme = themeRendererResolver.resolve(input.themeCode());

            workDir = Files.createTempDirectory("video-job-" + jobId);
            Path outputPath = workDir.resolve("output.mp4");
            slideshowVideoBuilder.generateSlideshowVideo(
                    photoGroups, outputPath, input.slideDurationSec(), bgmPath, theme, input.title(),
                    percent -> videoJobService.updateProgress(jobId, percent));

            String outputStorageKey;
            try (InputStream in = Files.newInputStream(outputPath)) {
                outputStorageKey = storageService.store("videos", "job-" + jobId + ".mp4", in);
            }

            videoJobService.markCompleted(jobId, outputStorageKey);
            log.info("Video generation completed for job {}", jobId);
        } catch (IOException e) {
            log.error("Video generation failed for job {}", jobId, e);
            videoJobService.markFailed(jobId, "動画の生成に失敗しました。もう一度お試しください。");
        } catch (RuntimeException e) {
            log.error("Video generation failed for job {}", jobId, e);
            videoJobService.markFailed(jobId, "動画の生成に失敗しました。もう一度お試しください。");
        } finally {
            TempDirectoryCleanup.deleteQuietly(workDir);
        }
    }
}
