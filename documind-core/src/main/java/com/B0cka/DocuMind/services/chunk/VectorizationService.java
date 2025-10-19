package com.B0cka.DocuMind.services.chunk;

import com.B0cka.DocuMind.models.Vectors;
import com.B0cka.DocuMind.reposiroty.WebRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class VectorizationService {
    private final RestTemplate restTemplate;
    private final WebRepository webRepository;

    public void processChunks(List<String> chunks, String docId) {
        for (String chunk : chunks) {
            try {
                Map<String, String> requestBody = Map.of("text", chunk);

                Map<String, Object> response = restTemplate.postForObject(
                        "http://localhost:8000/vectorize",
                        requestBody,
                        Map.class
                );

                if (response != null && response.containsKey("vector")) {
                    List<Double> vectorList = (List<Double>) response.get("vector");
                    float[] vectorArray = new float[vectorList.size()];
                    for (int j = 0; j < vectorList.size(); j++) {
                        vectorArray[j] = vectorList.get(j).floatValue();
                    }

                    Vectors vectorEntity = Vectors.builder()
                            .vector(vectorArray)
                            .docId(docId)
                            .text(chunk)
                            .createdAt(Instant.now())
                            .build();

                    webRepository.save(vectorEntity);
                    log.info("Чанк сохранен: {}", chunk);
                }
            } catch (Exception e) {
                log.error("Ошибка при обработке чанка {}: {}", chunk, e.getMessage());
            }
        }
    }

    public List<String> findSimilarChunks(float[] questionVector, int limit, String docId) {
        try {

            String vectorString = Arrays.toString(questionVector)
                    .replace("[", "[")
                    .replace("]", "]");

            List<Object[]> results = webRepository.findSimilarVectors(vectorString, limit, docId);

            return results.stream()
                    .filter(r -> r.length > 0 && r[0] != null)
                    .map(r -> (String) r[0])
                    .toList();

        } catch (Exception e) {
            log.error("Ошибка при поиске похожих чанков: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public float[] callVectorizeServer(String str){
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", str);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                "http://localhost:8000/vectorize",
                requestBody,
                Map.class
        );

        if (response == null || !response.containsKey("vector")) {
            throw new RuntimeException("Не удалось векторизовать вопрос");
        }

        List<Double> vectorList = (List<Double>) response.get("vector");
        float[] questionVector = new float[vectorList.size()];
        for (int i = 0; i < vectorList.size(); i++) {
            questionVector[i] = vectorList.get(i).floatValue();
        }

        return questionVector;
    }

}
