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
import java.util.ArrayList;
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
        Project project = projectRepository.findByIdForUpdate(projectId)
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
    public VideoJobResponse getStatus(String publicId) {
        return toResponse(findByPublicIdOrThrow(publicId));
    }

    @Transactional(readOnly = true)
    public VideoJobResponse getLatest(String projectId) {
        VideoJob job = videoJobRepository
                .findTopByProjectIdOrderByRequestedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("動画生成ジョブが見つかりません。"));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public Resource loadVideoFile(String publicId) {
        VideoJob job = findByPublicIdOrThrow(publicId);
        if (job.getStatus() != VideoJobStatus.COMPLETED || job.getOutputStorageKey() == null) {
            throw new ValidationException("動画がまだ完成していません");
        }
        return storageService.load(job.getOutputStorageKey());
    }

    @Transactional(readOnly = true)
    public VideoGenerationInput loadGenerationInput(Long jobId) {
        VideoJob job = findOrThrow(jobId);
        Project project = job.getProject();

        List<Photo> photos = photoRepository.findByProjectIdOrderByDisplayOrderAsc(project.getId());
        List<PhotoPageGroup> photoGroups = groupByPageBreak(photos);
        String bgmStorageKey = project.getBgm() != null ? project.getBgm().getStorageKey() : null;

        return new VideoGenerationInput(
                photoGroups,
                project.getSlideDurationSec(),
                project.getTheme().getCode(),
                bgmStorageKey,
                project.getTitle());
    }

    /**
     * 表示順に並んだ写真を、pageBreakAfterの位置で1ページ(1カット)ごとのグループに分割する。
     * 各グループのlayoutPatternは、そのページ最後の写真(pageBreakAfter=true)に保存された値を使う。
     */
    private List<PhotoPageGroup> groupByPageBreak(List<Photo> photos) {
        List<PhotoPageGroup> groups = new ArrayList<>();
        List<PhotoRef> currentRefs = new ArrayList<>();
        for (Photo photo : photos) {
            currentRefs.add(new PhotoRef(photo.getStorageKey(), photo.getCropShape()));
            if (photo.isPageBreakAfter()) {
                groups.add(new PhotoPageGroup(currentRefs, photo.getLayoutPattern()));
                currentRefs = new ArrayList<>();
            }
        }
        if (!currentRefs.isEmpty()) {
            groups.add(new PhotoPageGroup(currentRefs, null));
        }
        return groups;
    }

    public void markProcessing(Long jobId) {
        VideoJob job = findOrThrow(jobId);
        job.setStatus(VideoJobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
    }

    /**
     * ffmpegの実際のエンコード進捗(0〜99)を反映する。100%はmarkCompletedで
     * 動画の保存まで完了した時点で初めて設定する(エンコードが終わってもまだ
     * 保存処理が残っているため、ここでは99%までしか進めない)。
     */
    public void updateProgress(Long jobId, int progress) {
        VideoJob job = findOrThrow(jobId);
        job.setProgress(Math.min(progress, 99));
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

    private VideoJob findByPublicIdOrThrow(String publicId) {
        return videoJobRepository.findByPublicId(publicId)
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
                ? "/api/video-jobs/" + job.getPublicId() + "/download"
                : null;
        return new VideoJobResponse(
                job.getPublicId(),
                job.getStatus().name(),
                job.getProgress(),
                job.getErrorMessage(),
                downloadUrl
        );
    }
}
