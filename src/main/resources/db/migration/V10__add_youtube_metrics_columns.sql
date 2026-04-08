ALTER TABLE video_job ADD COLUMN youtube_views BIGINT;
ALTER TABLE video_job ADD COLUMN youtube_likes BIGINT;
ALTER TABLE video_job ADD COLUMN youtube_comments BIGINT;
ALTER TABLE video_job ADD COLUMN youtube_title VARCHAR(200);
ALTER TABLE video_job ADD COLUMN metrics_updated_at TIMESTAMP;
