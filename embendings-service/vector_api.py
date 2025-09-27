# vector_api.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
import numpy as np

print("Загрузка модели... это может занять несколько секунд")
model = SentenceTransformer('all-MiniLM-L6-v2')  # Самая популярная легкая модель
print("Модель загружена!")

app = FastAPI(title="Vectorization API", description="Микросервис для векторизации текста")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["POST"],
    allow_headers=["*"],
)


class VectorizeRequest(BaseModel):
    text: str

class VectorizeResponse(BaseModel):
    vector: list[float]

@app.post("/vectorize", response_model=VectorizeResponse)
async def create_vector(request: VectorizeRequest):
    """
    Принимает текст, возвращает его векторное представление.
    """

    embedding = model.encode(request.text)

    embedding_list = embedding.tolist()

    return VectorizeResponse(vector=embedding_list)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

#python vector_api.py