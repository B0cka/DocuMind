package com.B0cka.DocuMind.service.video_serv;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.dto.SearchRequestDto;
import com.B0cka.DocuMind.dto.WhisperSegment;
import com.B0cka.DocuMind.model.VideoChunk;
import com.B0cka.DocuMind.service.cobalt.CobaltServiceImpl;
import com.B0cka.DocuMind.service.search.SearchService;
import com.B0cka.DocuMind.service.splitter.SemanticSplitter;
import com.B0cka.DocuMind.service.vectorise.VectoriseService;
import com.B0cka.DocuMind.service.whisper.WhisperService;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final CobaltServiceImpl cobaltService;
    private final WhisperService whisperService;
    private final VectoriseService vectoriseService;
    private final SearchService searchService;
    private final SemanticSplitter semanticSplitter;

    @Override
    public void transformVideo(RequestDto requestDto) {
        log.info("1. Скачивание аудио...");
        File audioFile = cobaltService.downloadAudio(requestDto.getLink());

        log.info("2. Whisper: получение сегментов...");
        List<WhisperSegment> rawSegments = whisperService.transcribe(audioFile);

        log.info("3. Semantic Chunking...");

        List<VideoChunk> chunks = semanticSplitter.split(rawSegments, requestDto.getLink());

        log.info("4. Сохранение {} чанков...", chunks.size());
        vectoriseService.saveChunks(chunks);
    }

    @Override
    public String searchVideo(SearchRequestDto dto) {
        log.info("Поиск: {}", dto.getQuestion());

        float[] questionVector = vectoriseService.callVectorizeServer(dto.getQuestion());

        List<VideoChunk> relevantChunks = vectoriseService.findSimilarChunks(questionVector, 5, dto.getLink());
        log.info("Chunks for question: {}", relevantChunks);

        return searchService.search(relevantChunks, dto.getQuestion());
    }
}