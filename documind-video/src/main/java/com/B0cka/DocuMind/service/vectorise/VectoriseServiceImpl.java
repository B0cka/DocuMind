package com.B0cka.DocuMind.service.vectorise;

import com.B0cka.DocuMind.model.VideoChunk;
import com.B0cka.DocuMind.repository.VideoRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectoriseServiceImpl implements VectoriseService {

    private final VideoRepository videoRepository;
    private final EmbeddingModel embeddingModel;

    @Override
    public float[] callVectorizeServer(String text) {
        return embeddingModel.embed(text).content().vector();
    }

    @Override
    public List<VideoChunk> findSimilarChunks(float[] questionVector, int limit, String docId) {

        String vectorStr = formatVectorToString(questionVector);

        List<Object[]> rawResults = videoRepository.findSimilarChunksNative(vectorStr, limit, docId);

        return rawResults.stream()
                .map(row -> VideoChunk.builder()
                        .text((String) row[0])
                        .startTime(((Number) row[1]).doubleValue())
                        .endTime(((Number) row[2]).doubleValue())
                        .link(docId)
                        .build())
                .toList();
    }


    private String formatVectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(String.format(java.util.Locale.US, "%.8f", vector[i]));
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void saveChunks(List<VideoChunk> chunks) {
        for (VideoChunk c : chunks) {
            log.info("сохранение чанка с id={}", c.getText());
            videoRepository.save(c);
        }
    }
}