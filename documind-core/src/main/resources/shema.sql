CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vectors (
    id BIGSERIAL PRIMARY KEY,
    vector vector(768) NOT NULL,
    text TEXT NOT NULL,
    doc_id VARCHAR(255) REFERENCES documents(id),
    metadata JSONB,
);

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(255) PRIMARY KEY,
    filename VARCHAR(255),
    original_filename VARCHAR(255),
    file_size BIGINT,
    total_chunks INTEGER,
    uploaded_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS video (
    id BIGSERIAL PRIMARY KEY,
    vector vector(768) NOT NULL,
    text TEXT NOT NULL,
    link VARCHAR(255),
    started_at DOUBLE PRECISION NOT NULL
);