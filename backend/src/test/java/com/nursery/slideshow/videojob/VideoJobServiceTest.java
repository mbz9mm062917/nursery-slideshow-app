package com.nursery.slideshow.videojob;

import com.nursery.slideshow.bgm.Bgm;
import com.nursery.slideshow.common.exception.ConflictException;
import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.photo.PhotoRepository;
import com.nursery.slideshow.project.Project;
import com.nursery.slideshow.project.ProjectRepository;
import com.nursery.slideshow.theme.Theme;
import com.nursery.slideshow.videojob.dto.VideoJobResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoJobServiceTest {

    private static final String PROJECT_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private VideoJobRepository videoJobRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VideoJobService videoJobService;

    private Project readyProject;

    @BeforeEach
    void setUp() {
        readyProject = new Project();
        readyProject.setId(PROJECT_ID);
        readyProject.setTitle("卒園式のおもいで");

        Theme theme = new Theme();
        theme.setCode("simple");
        readyProject.setTheme(theme);

        Bgm bgm = new Bgm();
        bgm.setStorageKey("bgms/bright.mp3");
        readyProject.setBgm(bgm);

        readyProject.setSlideDurationSec(3);
    }

    @Test
    void shouldStartGenerationWhenProjectIsReadyAndNoJobInProgress() {
        // Arrange
        when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(readyProject));
        when(photoRepository.countByProjectId(PROJECT_ID)).thenReturn(1L);
        when(videoJobRepository.findByProjectIdAndStatusIn(eq(PROJECT_ID), any())).thenReturn(List.of());
        when(videoJobRepository.save(any(VideoJob.class))).thenAnswer(invocation -> {
            VideoJob job = invocation.getArgument(0);
            job.setId(100L);
            job.setPublicId("job-public-id");
            return job;
        });

        // Act
        VideoJobResponse response = videoJobService.startGeneration(PROJECT_ID);

        // Assert
        assertThat(response.jobId()).isEqualTo("job-public-id");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.progress()).isZero();
        verify(videoJobRepository).save(any(VideoJob.class));
        verify(eventPublisher).publishEvent(new VideoGenerationRequestedEvent(100L));
    }

    @Test
    void shouldThrowConflictExceptionWhenJobAlreadyInProgress() {
        // Arrange
        when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(readyProject));
        when(photoRepository.countByProjectId(PROJECT_ID)).thenReturn(1L);
        VideoJob inProgressJob = new VideoJob();
        when(videoJobRepository.findByProjectIdAndStatusIn(eq(PROJECT_ID), any()))
                .thenReturn(List.of(inProgressJob));

        // Act & Assert
        assertThrows(ConflictException.class, () -> videoJobService.startGeneration(PROJECT_ID));
        verify(videoJobRepository, never()).save(any(VideoJob.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenProjectDoesNotExist() {
        // Arrange
        when(projectRepository.findByIdForUpdate(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> videoJobService.startGeneration(PROJECT_ID));
        verify(videoJobRepository, never()).save(any(VideoJob.class));
    }
}
