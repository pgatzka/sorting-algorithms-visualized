CREATE TABLE video_job (
    id              UUID PRIMARY KEY,
    algorithm       VARCHAR(50)  NOT NULL,
    visualization   VARCHAR(50)  NOT NULL,
    element_count   INT          NOT NULL DEFAULT 50,
    fps             INT          NOT NULL DEFAULT 60,
    width           INT          NOT NULL DEFAULT 1080,
    height          INT          NOT NULL DEFAULT 1920,
    frames_per_step INT          NOT NULL DEFAULT 3,
    status          VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    progress        INT          NOT NULL DEFAULT 0,
    output_path     VARCHAR(500),
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP
);
