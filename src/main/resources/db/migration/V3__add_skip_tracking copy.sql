-- =============================================================
-- V3 — Skip tracking table
-- =============================================================
-- Stores every CSV row that was skipped due to a parsing error.
-- This gives operations teams full visibility into what was
-- rejected without having to grep application logs.
--
-- Populated by the SkipItemListener (Phase 2).
-- =============================================================

CREATE TABLE skipped_items (
    id               BIGSERIAL     PRIMARY KEY,
    job_execution_id BIGINT        NOT NULL,
    step_name        VARCHAR(100)  NOT NULL,
    line_number      INT,
    raw_line         TEXT,
    error_message    VARCHAR(1000) NOT NULL,
    skipped_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skipped_items_job_exec ON skipped_items (job_execution_id);