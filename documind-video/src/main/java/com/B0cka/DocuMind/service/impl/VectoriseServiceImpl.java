package com.B0cka.DocuMind.service.impl;

import com.B0cka.DocuMind.model.Video;
import com.B0cka.DocuMind.repository.VideoRepository;
import com.B0cka.DocuMind.service.VectoriseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@PropertySource("application.properties")
public class VectoriseServiceImpl implements VectoriseService {

    @Value("${vectorise.url}")
    private String vectoriseUrl;
    private final RestTemplate restTemplate;
    private final VideoRepository videoRepository;

    @Override
    public float[] callVectorizeServer(String str){
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", str);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                vectoriseUrl,
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

    @Override
    public List<String> findSimilarChunks(float[] questionVector, int limit, String docId) {
        try {

            String vectorString = Arrays.toString(questionVector)
                    .replace("[", "[")
                    .replace("]", "]");

            List<Object[]> results = videoRepository.findSimilarVectors(vectorString, limit, docId);

            return results.stream()
                    .filter(r -> r.length > 0 && r[0] != null)
                    .map(r -> (String) r[0])
                    .toList();

        } catch (Exception e) {
            log.error("Ошибка при поиске похожих чанков: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void processChunks(HashMap<Double, String> chunks, String docId) {
        for (Map.Entry<Double,String> chunk : chunks.entrySet()) {
            try {
                Map<String, String> requestBody = Map.of("text", chunk.getValue());

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

                    Video vectorEntity = Video.builder()
                            .vector(vectorArray)
                            .link(docId)
                            .text(chunk.getValue())
                            .startedAt(chunk.getKey())
                            .build();

                    videoRepository.save(vectorEntity);
                    log.info("Чанк сохранен: {}", chunk);
                }
            } catch (Exception e) {
                log.error("Ошибка при обработке чанка {}: {}", chunk, e.getMessage());
            }
        }
    }

    @Override
    public Optional<Video> searchByString(String link){
        return videoRepository.findByLink(link);
    }
}
