INSERT INTO themes (code, name, thumbnail_storage_key, sort_order, is_active) VALUES
('simple', 'シンプル', 'themes/simple/thumbnail.jpg', 1, TRUE),
('cute', 'かわいい', 'themes/cute/thumbnail.jpg', 2, TRUE),
('graduation', '卒園式', 'themes/graduation/thumbnail.jpg', 3, TRUE),
('sports', '運動会', 'themes/sports/thumbnail.jpg', 4, TRUE);

INSERT INTO bgms (code, name, storage_key, duration_sec, sort_order, is_active) VALUES
('bright', '明るい', 'bgms/bright.mp3', 120, 1, TRUE),
('moving', '感動', 'bgms/moving.mp3', 120, 2, TRUE),
('energetic', '元気', 'bgms/energetic.mp3', 120, 3, TRUE);
