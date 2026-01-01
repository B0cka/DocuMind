package com.B0cka.DocuMind.services.chunk;

import com.B0cka.DocuMind.reposiroty.WebRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class VectorizationService {
    private final EmbeddingModel embeddingModel;
    private final WebRepository webRepository;

    @Async
    public void processChunks(List<String> chunks, String docId) {
        log.info("Начинаем векторизацию {} чанков для дока {}", chunks.size(), docId);

        for (String chunkText : chunks) {
            try {
                log.info("Чанки для векторизации: {}", chunks);
                float[] vector = embeddingModel.embed(chunkText).content().vector();
                String vectorStr = formatVectorToString(vector);

                webRepository.saveNative(docId, chunkText, vectorStr);
            } catch (Exception e) {
                log.error("Ошибка сохранения чанка: {}", e.getMessage());
            }
        }
        log.info("Документ {} обработан", docId);
    }

    public List<String> findSimilarChunks(float[] questionVector, int limit, String docId) {
        String vectorStr = formatVectorToString(questionVector);
        return webRepository.findSimilar(vectorStr, limit, docId);
    }

    public float[] callVectorizeServer(String text) {
        return embeddingModel.embed(text).content().vector();
    }

    private String formatVectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(String.format(java.util.Locale.US, "%.8f", vector[i]));
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}