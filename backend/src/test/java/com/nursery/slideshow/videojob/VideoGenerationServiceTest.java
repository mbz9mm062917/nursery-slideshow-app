package com.nursery.slideshow.videojob;

import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.videojob.ffmpeg.SlideshowVideoBuilder;
import com.nursery.slideshow.videojob.theme.ThemeRenderer;
import com.nursery.slideshow.videojob.theme.ThemeRendererResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoGenerationServiceTest {

    private static final long JOB_ID = 1L;

    @Mock
    private VideoJobService videoJobService;
    @Mock
    private SlideshowVideoBuilder slideshowVideoBuilder;
    @Mock
    private ThemeRendererResolver themeRendererResolver;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private VideoGenerationService videoGenerationService;

    @Test
    void shouldMarkJobCompletedWhenGenerationSucceeds() {
        // Arrange
        VideoGenerationInput input = new VideoGenerationInput(
                List.of(new PhotoPageGroup(List.of(new PhotoRef("projects/p/photos/a.png", null)), null)),
                3, "simple", null, "たのしい思い出");
        when(videoJobService.loadGenerationInput(JOB_ID)).thenReturn(input);
        when(storageService.resolveLocalPath(anyString())).thenReturn(Path.of("dummy-photo.png"));
        when(themeRendererResolver.resolve("simple")).thenReturn(mockThemeRenderer());
        doAnswer(invocation -> {
            Path outputPath = invocation.getArgument(1);
            Files.createFile(outputPath);
            return null;
        }).when(slideshowVideoBuilder).generateSlideshowVideo(
                anyList(), any(Path.class), anyInt(), nullable(Path.class), any(ThemeRenderer.class), anyString());
        when(storageService.store(eq("videos"), anyString(), any(InputStream.class)))
                .thenReturn("videos/output.mp4");

        // Act
        videoGenerationService.onVideoGenerationRequested(new VideoGenerationRequestedEvent(JOB_ID));

        // Assert
        verify(videoJobService).markProcessing(JOB_ID);
        verify(videoJobService).markCompleted(JOB_ID, "videos/output.mp4");
        verify(videoJobService, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void shouldMarkJobFailedWhenGenerationThrowsException() {
        // Arrange
        VideoGenerationInput input = new VideoGenerationInput(
                List.of(new PhotoPageGroup(List.of(new PhotoRef("projects/p/photos/a.png", null)), null)),
                3, "simple", null, "たのしい思い出");
        when(videoJobService.loadGenerationInput(JOB_ID)).thenReturn(input);
        when(storageService.resolveLocalPath(anyString())).thenReturn(Path.of("dummy-photo.png"));
        when(themeRendererResolver.resolve("simple")).thenReturn(mockThemeRenderer());
        doThrow(new VideoGenerationException("FFmpegの実行に失敗しました(exit=1)"))
                .when(slideshowVideoBuilder).generateSlideshowVideo(
                        anyList(), any(Path.class), anyInt(), nullable(Path.class), any(ThemeRenderer.class), anyString());

        // Act
        videoGenerationService.onVideoGenerationRequested(new VideoGenerationRequestedEvent(JOB_ID));

        // Assert
        verify(videoJobService).markProcessing(JOB_ID);
        verify(videoJobService).markFailed(eq(JOB_ID), anyString());
        verify(videoJobService, never()).markCompleted(anyLong(), anyString());
    }

    private ThemeRenderer mockThemeRenderer() {
        return org.mockito.Mockito.mock(ThemeRenderer.class);
    }
}
