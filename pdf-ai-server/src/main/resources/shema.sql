-- Создание таблицы для векторов
CREATE TABLE IF NOT EXISTS vectors (
    id BIGSERIAL PRIMARY KEY,
    vector vector(384) NOT NULL
);

-- Создание индекса для быстрого поиска похожих векторов
CREATE INDEX IF NOT EXISTS vectors_vector_idx
ON vectors USING ivfflat (vector vector_cosine_ops)
WITH (lists = 100);