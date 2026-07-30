package com.nursery.slideshow.bgm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BgmRepository extends JpaRepository<Bgm, Long> {
    List<Bgm> findByActiveTrueOrderBySortOrderAsc();
    Optional<Bgm> findByCodeAndActiveTrue(String code);
}
