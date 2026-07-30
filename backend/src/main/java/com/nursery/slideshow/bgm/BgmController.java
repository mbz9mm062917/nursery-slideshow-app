package com.nursery.slideshow.bgm;

import com.nursery.slideshow.bgm.dto.BgmResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bgms")
public class BgmController {

    private final BgmService bgmService;

    public BgmController(BgmService bgmService) {
        this.bgmService = bgmService;
    }

    @GetMapping
    public List<BgmResponse> list() {
        return bgmService.listActive();
    }

    @GetMapping("/{bgmId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long bgmId) {
        return ResponseEntity.ok(bgmService.loadFile(bgmId));
    }
}
