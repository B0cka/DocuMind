# vector_api.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import numpy as np

# --- 1. Инициализация ---
# Загружаем модель ОДИН РАЗ при старте сервера
print("Загрузка модели... это может занять несколько секунд")
model = SentenceTransformer('all-MiniLM-L6-v2')  # Самая популярная легкая модель
print("Модель загружена!")

# Создаем приложение FastAPI
app = FastAPI(title="Vectorization API", description="Микросервис для векторизации текста")

# Разрешаем запросы с любого origin (для удобства разработки)
# В продакшене укажите точный адрес вашего Java-приложения вместо "*"
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Разрешаем запросы откуда угодно
    allow_methods=["POST"],  # Разрешаем только POST-запросы
    allow_headers=["*"],
)

# --- 2. Модели данных для запроса и ответа ---
class VectorizeRequest(BaseModel):
    text: str  # Текст, который нужно векторизовать

class VectorizeResponse(BaseModel):
    vector: list[float]  # Вектор-эмбеддинг в виде списка чисел
    # Можете добавить другие поля, например, размерность или статус

# --- 3. Конечная точка (Endpoint) ---
@app.post("/vectorize", response_model=VectorizeResponse)
async def create_vector(request: VectorizeRequest):
    """
    Принимает текст, возвращает его векторное представление.
    """
    # Векторизируем текст
    embedding = model.encode(request.text)

    # Преобразуем numpy array в обычный список Python (так как JSON не понимает numpy)
    embedding_list = embedding.tolist()

    # Возвращаем ответ
    return VectorizeResponse(vector=embedding_list)

# --- Запуск сервера ---
# Этот блок сработает только если файл запущен напрямую: `python vector_api.py`
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)