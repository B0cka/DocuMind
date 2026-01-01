CREATE EXTENSION IF NOT EXISTS vector;

DROP TABLE IF EXISTS video_chunks CASCADE;
DROP TABLE IF EXISTS vectors CASCADE;

CREATE TABLE IF NOT EXISTS vectors (
    id BIGSERIAL PRIMARY KEY,
    vector vector(384) NOT NULL,
    text TEXT NOT NULL,
    doc_id VARCHAR(255)
);

CREATE TABLE video_chunks (
    id BIGSERIAL PRIMARY KEY,
    link VARCHAR(255),
    text TEXT,
    vector vector(384),
    start_time DOUBLE PRECISION,
    end_time DOUBLE PRECISION
);