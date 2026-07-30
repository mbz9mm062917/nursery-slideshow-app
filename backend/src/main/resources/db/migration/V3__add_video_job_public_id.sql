ALTER TABLE video_jobs ADD COLUMN public_id CHAR(36) NULL;

UPDATE video_jobs SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE video_jobs MODIFY COLUMN public_id CHAR(36) NOT NULL;

ALTER TABLE video_jobs ADD CONSTRAINT uk_video_jobs_public_id UNIQUE (public_id);
