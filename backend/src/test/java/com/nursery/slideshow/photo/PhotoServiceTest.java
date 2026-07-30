package com.nursery.slideshow.photo;

import com.nursery.slideshow.common.exception.ValidationException;
import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.photo.dto.PhotoResponse;
import com.nursery.slideshow.project.Project;
import com.nursery.slideshow.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    private static final String PROJECT_ID = "22222222-2222-2222-2222-222222222222";

    private static final byte[] JPEG_MAGIC_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10};
    private static final byte[] PNG_MAGIC_BYTES =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x10};

    @Mock
    private PhotoRepository photoRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private PhotoService photoService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(PROJECT_ID);
    }

    @Test
    void shouldUploadJpegFileSuccessfully() {
        // Arrange
        MultipartFile jpegFile =
                new MockMultipartFile("files", "photo.jpg", "image/jpeg", JPEG_MAGIC_BYTES);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(photoRepository.countByProjectId(PROJECT_ID)).thenReturn(0L);
        when(storageService.store(anyString(), anyString(), any())).thenReturn("projects/p/photos/a.jpg");
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> {
            Photo photo = invocation.getArgument(0);
            photo.setId(1L);
            return photo;
        });

        // Act
        List<PhotoResponse> result = photoService.upload(PROJECT_ID, List.of(jpegFile));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).originalFileName()).isEqualTo("photo.jpg");
        verify(storageService).store(anyString(), anyString(), any());
        verify(photoRepository).save(any(Photo.class));
    }

    @Test
    void shouldUploadPngFileSuccessfully() {
        // Arrange
        MultipartFile pngFile =
                new MockMultipartFile("files", "photo.png", "image/png", PNG_MAGIC_BYTES);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(photoRepository.countByProjectId(PROJECT_ID)).thenReturn(0L);
        when(storageService.store(anyString(), anyString(), any())).thenReturn("projects/p/photos/a.png");
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> {
            Photo photo = invocation.getArgument(0);
            photo.setId(2L);
            return photo;
        });

        // Act
        List<PhotoResponse> result = photoService.upload(PROJECT_ID, List.of(pngFile));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).originalFileName()).isEqualTo("photo.png");
        verify(storageService).store(anyString(), anyString(), any());
        verify(photoRepository).save(any(Photo.class));
    }

    @Test
    void shouldThrowValidationExceptionWhenMagicBytesDoNotMatchExtension() {
        // Arrange
        byte[] textDisguisedAsJpeg = "this is not a real jpeg file".getBytes();
        MultipartFile disguisedFile =
                new MockMultipartFile("files", "fake.jpg", "image/jpeg", textDisguisedAsJpeg);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        // Act & Assert
        assertThrows(ValidationException.class, () -> photoService.upload(PROJECT_ID, List.of(disguisedFile)));
        verify(storageService, never()).store(anyString(), anyString(), any());
        verify(photoRepository, never()).save(any(Photo.class));
    }

    @Test
    void shouldThrowValidationExceptionWhenFileIsEmpty() {
        // Arrange
        MultipartFile emptyFile =
                new MockMultipartFile("files", "empty.png", "image/png", new byte[0]);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        // Act & Assert
        assertThrows(ValidationException.class, () -> photoService.upload(PROJECT_ID, List.of(emptyFile)));
        verify(storageService, never()).store(anyString(), anyString(), any());
        verify(photoRepository, never()).save(any(Photo.class));
    }
}
