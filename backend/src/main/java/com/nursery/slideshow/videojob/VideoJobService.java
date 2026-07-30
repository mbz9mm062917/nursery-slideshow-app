package com.nursery.slideshow.videojob;

import com.nursery.slideshow.common.exception.ConflictException;
import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.exception.ValidationException;
import com.nursery.slideshow.common.storage.StorageService;
import com.nursery.slideshow.photo.Photo;
import com.nursery.slideshow.photo.PhotoRepository;
import com.nursery.slideshow.project.Project;
import com.nursery.slideshow.project.ProjectRepository;
import com.nursery.slideshow.videojob.dto.VideoJobResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VideoJobService {

    private static final List<VideoJobStatus> IN_PROGRESS_STATUSES =
            List.of(VideoJobStatus.PENDING, VideoJobStatus.PROCESSING);

    private final VideoJobRepository videoJobRepository;
    private final ProjectRepository projectRepository;
    private final PhotoRepository photoRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public VideoJobService(VideoJobRepository videoJobRepository,
                            ProjectRepository projectRepository,
                            PhotoRepository photoRepository,
                            StorageService storageService,
                            ApplicationEventPublisher eventPublisher) {
        this.videoJobRepository = videoJobRepository;
        this.projectRepository = projectRepository;
        this.photoRepository = photoRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    public VideoJobResponse startGeneration(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("指定されたスライドショーが見つかりません"));

        validateReadyForGeneration(project);

        if (!videoJobRepository.findByProjectIdAndStatusIn(projectId, IN_PROGRESS_STATUSES).isEmpty()) {
            throw new ConflictException("既に動画を生成中です");
        }

        VideoJob job = new VideoJob();
        job.setProject(project);
        job.setStatus(VideoJobStatus.PENDING);
        job.setProgress(0);
        job = videoJobRepository.save(job);

        eventPublisher.publishEvent(new VideoGenerationRequestedEvent(job.getId()));

        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public VideoJobResponse getStatus(Long jobId) {
        return toResponse(findOrThrow(jobId));
    }

    @Transactional(readOnly = true)
    public VideoJobResponse getLatestCompleted(String projectId) {
        VideoJob job = videoJobRepository
                .findTopByProjectIdAndStatusOrderByCompletedAtDesc(projectId, VideoJobStatus.COMPLETED)
                .orElseThrow(() -> new ResourceNotFoundException("まだ動画が生成されていません"));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public Resource loadVideoFile(Long jobId) {
        VideoJob job = findOrThrow(jobId);
        if (job.getStatus() != VideoJobStatus.COMPLETED || job.getOutputStorageKey() == null) {
            throw new ValidationException("動画がまだ完成していません");
        }
        return storageService.load(job.getOutputStorageKey());
    }

    @Transactional(readOnly = true)
    public VideoGenerationInput loadGenerationInput(Long jobId) {
        VideoJob job = findOrThrow(jobId);
        Project project = job.getProject();

        List<String> photoStorageKeys = photoRepository.findByProjectIdOrderByDisplayOrderAsc(project.getId())
                .stream()
                .map(Photo::getStorageKey)
                .toList();
        String bgmStorageKey = project.getBgm() != null ? project.getBgm().getStorageKey() : null;

        return new VideoGenerationInput(
                photoStorageKeys,
                project.getSlideDurationSec(),
                project.getTheme().getCode(),
                bgmStorageKey,
                project.getTitle());
    }

    public void markProcessing(Long jobId) {
        VideoJob job = findOrThrow(jobId);
        job.setStatus(VideoJobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
    }

    public void markCompleted(Long jobId, String outputStorageKey) {
        VideoJob job = findOrThrow(jobId);
        job.setStatus(VideoJobStatus.COMPLETED);
        job.setProgress(100);
        job.setOutputStorageKey(outputStorageKey);
        job.setCompletedAt(LocalDateTime.now());
    }

    public void markFailed(Long jobId, String errorMessage) {
        VideoJob job = findOrThrow(jobId);
        job.setStatus(VideoJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(LocalDateTime.now());
    }

    private VideoJob findOrThrow(Long jobId) {
        return videoJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("指定された動画が見つかりません"));
    }

    private void validateReadyForGeneration(Project project) {
        long photoCount = photoRepository.countByProjectId(project.getId());
        boolean ready = project.getTitle() != null
                && project.getTheme() != null
                && project.getBgm() != null
                && project.getSlideDurationSec() != null
                && photoCount > 0;
        if (!ready) {
            throw new ValidationException("動画を作成するには、タイトル・テーマ・BGM・スライド時間の設定と、1枚以上の写真が必要です");
        }
    }

    private VideoJobResponse toResponse(VideoJob job) {
        String downloadUrl = job.getStatus() == VideoJobStatus.COMPLETED
                ? "/api/video-jobs/" + job.getId() + "/download"
                : null;
        return new VideoJobResponse(
                job.getId(),
                job.getStatus().name(),
                job.getProgress(),
                job.getErrorMessage(),
                downloadUrl
        );
    }
}
