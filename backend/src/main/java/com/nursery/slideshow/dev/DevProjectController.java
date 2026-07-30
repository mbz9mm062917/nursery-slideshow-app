package com.nursery.slideshow.dev;

import com.nursery.slideshow.project.ProjectService;
import com.nursery.slideshow.project.dto.ProjectResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * プロジェクト一覧・削除は本番ウィザードUIでは使用しない開発・デバッグ専用機能のため、
 * 本番の {@link com.nursery.slideshow.project.ProjectController} とは分離している。
 */
@Profile("dev")
@RestController
@RequestMapping("/api/dev/projects")
public class DevProjectController {

    private final ProjectService projectService;

    public DevProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.list();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable String projectId) {
        projectService.delete(projectId);
        return ResponseEntity.noContent().build();
    }
}
