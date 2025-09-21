
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS vectors (
    id BIGSERIAL PRIMARY KEY,
    vector vector(384) NOT NULL,    -- Цифровой отпечаток для поиска
    text TEXT NOT NULL,
    doc_id VARCHAR(255) REFERENCES documents(id),-- Исходный текст чанка
    metadata JSONB,
);

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(255) PRIMARY KEY,  -- тот самый docId с фронта
    filename VARCHAR(255),
    original_filename VARCHAR(255),
    file_size BIGINT,
    total_chunks INTEGER,
    uploaded_at TIMESTAMP
);