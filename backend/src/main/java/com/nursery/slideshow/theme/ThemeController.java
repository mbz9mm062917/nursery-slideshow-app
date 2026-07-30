package com.nursery.slideshow.theme;

import com.nursery.slideshow.theme.dto.ThemeResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/themes")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @GetMapping
    public List<ThemeResponse> list() {
        return themeService.listActive();
    }

    @GetMapping("/{themeId}/thumbnail")
    public ResponseEntity<Resource> thumbnail(@PathVariable Long themeId) {
        return ResponseEntity.ok(themeService.loadThumbnail(themeId));
    }
}
