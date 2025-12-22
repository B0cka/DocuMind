package com.B0cka.DocuMind.service.splitter;

import com.B0cka.DocuMind.dto.WhisperSegment;
import com.B0cka.DocuMind.model.VideoChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
@RequiredArgsConstructor
@Slf4j
public class SemanticSplitter {

    private final EmbeddingModel embeddingModel;


    private static final double SIMILARITY_THRESHOLD = 0.55;

    private static final int MIN_CHUNK_LENGTH = 300;

    public List<VideoChunk> split(List<WhisperSegment> rawSegments, String videoLink) {
        if (rawSegments.isEmpty()) return new ArrayList<>();

        List<SegmentGroup> sentences = mergeIntoSentences(rawSegments);
        log.info("Склеено {} сегментов Whisper в {} предложений", rawSegments.size(), sentences.size());

        List<VideoChunk> resultChunks = new ArrayList<>();

        List<SegmentGroup> buffer = new ArrayList<>();
        buffer.add(sentences.get(0));

        StringBuilder bufferTextLen = new StringBuilder(sentences.get(0).text);

        Embedding currentVec = embeddingModel.embed(sentences.get(0).text).content();

        for (int i = 1; i < sentences.size(); i++) {
            SegmentGroup nextSent = sentences.get(i);
            Embedding nextVec = embeddingModel.embed(nextSent.text).content();

            double similarity = CosineSimilarity.between(currentVec, nextVec);

            boolean isBufferTooSmall = bufferTextLen.length() < MIN_CHUNK_LENGTH;
            boolean isSimilar = similarity > SIMILARITY_THRESHOLD;

            if (isBufferTooSmall || isSimilar) {

                buffer.add(nextSent);
                bufferTextLen.append(" ").append(nextSent.text);

                currentVec = nextVec;
            } else {

                resultChunks.add(createChunk(buffer, videoLink));
                buffer.clear();
                buffer.add(nextSent);
                bufferTextLen.setLength(0);
                bufferTextLen.append(nextSent.text);
                currentVec = nextVec;
            }
        }

        // Хвост
        if (!buffer.isEmpty()) {
            resultChunks.add(createChunk(buffer, videoLink));
        }

        return resultChunks;
    }

    private record SegmentGroup(double start, double end, String text) {}

    private List<SegmentGroup> mergeIntoSentences(List<WhisperSegment> raw) {
        List<SegmentGroup> sentences = new ArrayList<>();
        if (raw.isEmpty()) return sentences;

        double currentStart = raw.get(0).getStart();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < raw.size(); i++) {
            WhisperSegment seg = raw.get(i);
            sb.append(seg.getText().trim()).append(" ");

            // Проверяем конец предложения (. ? !)
            String t = seg.getText().trim();
            boolean isEnd = t.endsWith(".") || t.endsWith("?") || t.endsWith("!");

            if (isEnd || i == raw.size() - 1) {
                sentences.add(new SegmentGroup(currentStart, seg.getEnd(), sb.toString().trim()));
                sb.setLength(0);
                if (i < raw.size() - 1) {
                    currentStart = raw.get(i + 1).getStart();
                }
            }
        }
        return sentences;
    }

    private VideoChunk createChunk(List<SegmentGroup> buffer, String link) {
        double start = buffer.get(0).start;
        double end = buffer.get(buffer.size() - 1).end;

        StringBuilder text = new StringBuilder();
        for (SegmentGroup s : buffer) text.append(s.text).append(" ");
        String finalText = text.toString().trim();

        float[] vector = embeddingModel.embed(finalText).content().vector();log.info(link);
        VideoChunk vid =VideoChunk.builder()
                .link(link)
                .startTime(start)
                .endTime(end)
                .text(finalText)
                .vector(vector)
                .build();
        log.info("chunk = {}", vid);
        return vid; }
}