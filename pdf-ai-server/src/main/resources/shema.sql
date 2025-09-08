DROP TABLE IF EXISTS vectors;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS vectors (
    id BIGSERIAL PRIMARY KEY,
    vector vector(384) NOT NULL,    -- Цифровой отпечаток для поиска
    text TEXT NOT NULL,        -- Исходный текст чанка
    metadata JSONB                   -- Доп. информация (опционально)
);