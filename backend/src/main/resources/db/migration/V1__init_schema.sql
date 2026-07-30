CREATE TABLE themes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    thumbnail_storage_key VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_themes_code (code)
);

CREATE TABLE bgms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    duration_sec INT NOT NULL,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bgms_code (code)
);

CREATE TABLE projects (
    id CHAR(36) PRIMARY KEY,
    title VARCHAR(100) NULL,
    theme_id BIGINT NULL,
    bgm_id BIGINT NULL,
    slide_duration_sec TINYINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_theme FOREIGN KEY (theme_id) REFERENCES themes (id),
    CONSTRAINT fk_projects_bgm FOREIGN KEY (bgm_id) REFERENCES bgms (id),
    CONSTRAINT chk_projects_slide_duration CHECK (slide_duration_sec IS NULL OR slide_duration_sec IN (3, 5, 7))
);

CREATE TABLE photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_photos_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    KEY idx_photos_project_order (project_id, display_order)
);

CREATE TABLE video_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    progress TINYINT UNSIGNED NOT NULL DEFAULT 0,
    output_storage_key VARCHAR(255) NULL,
    error_message VARCHAR(500) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    CONSTRAINT fk_video_jobs_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT chk_video_jobs_progress CHECK (progress BETWEEN 0 AND 100),
    KEY idx_video_jobs_project_status_completed (project_id, status, completed_at)
);
