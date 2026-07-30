package com.nursery.slideshow.project;

import com.nursery.slideshow.bgm.Bgm;
import com.nursery.slideshow.bgm.BgmRepository;
import com.nursery.slideshow.common.exception.ResourceNotFoundException;
import com.nursery.slideshow.common.exception.ValidationException;
import com.nursery.slideshow.photo.PhotoRepository;
import com.nursery.slideshow.project.dto.ProjectPatchRequest;
import com.nursery.slideshow.project.dto.ProjectResponse;
import com.nursery.slideshow.theme.Theme;
import com.nursery.slideshow.theme.ThemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ProjectService {

    private static final Set<Integer> ALLOWED_SLIDE_DURATIONS = Set.of(3, 5, 7);
    private static final int TITLE_MAX_LENGTH = 50;

    private final ProjectRepository projectRepository;
    private final ThemeRepository themeRepository;
    private final BgmRepository bgmRepository;
    private final PhotoRepository photoRepository;

    public ProjectService(ProjectRepository projectRepository,
                           ThemeRepository themeRepository,
                           BgmRepository bgmRepository,
                           PhotoRepository photoRepository) {
        this.projectRepository = projectRepository;
        this.themeRepository = themeRepository;
        this.bgmRepository = bgmRepository;
        this.photoRepository = photoRepository;
    }

    public ProjectResponse create() {
        Project project = projectRepository.save(Project.createNew());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(String projectId) {
        return toResponse(findOrThrow(projectId));
    }

    public void delete(String projectId) {
        projectRepository.delete(findOrThrow(projectId));
    }

    public ProjectResponse patch(String projectId, ProjectPatchRequest request) {
        Project project = findOrThrow(projectId);

        if (request.title() != null) {
            project.setTitle(validatedTitle(request.title()));
        }
        if (request.themeCode() != null) {
            project.setTheme(findThemeByCode(request.themeCode()));
        }
        if (request.bgmCode() != null) {
            project.setBgm(findBgmByCode(request.bgmCode()));
        }
        if (request.slideDurationSec() != null) {
            project.setSlideDurationSec(validatedSlideDuration(request.slideDurationSec()));
        }

        return toResponse(project);
    }

    private Project findOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("指定されたスライドショーが見つかりません"));
    }

    private String validatedTitle(String title) {
        if (title.isBlank()) {
            throw new ValidationException("タイトルを入力してください");
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException("タイトルは" + TITLE_MAX_LENGTH + "文字以内で入力してください");
        }
        return title;
    }

    private int validatedSlideDuration(int slideDurationSec) {
        if (!ALLOWED_SLIDE_DURATIONS.contains(slideDurationSec)) {
            throw new ValidationException("スライドの表示時間は3秒・5秒・7秒のいずれかを選択してください");
        }
        return slideDurationSec;
    }

    private Theme findThemeByCode(String code) {
        return themeRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new ValidationException("指定されたテーマが見つかりません"));
    }

    private Bgm findBgmByCode(String code) {
        return bgmRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new ValidationException("指定されたBGMが見つかりません"));
    }

    private ProjectResponse toResponse(Project project) {
        long photoCount = photoRepository.countByProjectId(project.getId());
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getTheme() != null ? project.getTheme().getCode() : null,
                project.getBgm() != null ? project.getBgm().getCode() : null,
                project.getSlideDurationSec(),
                photoCount,
                project.getCreatedAt()
        );
    }
}
