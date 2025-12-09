package com.B0cka.DocuMind.service.video_serv;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.dto.SearchRequestDto;
import com.B0cka.DocuMind.service.llm.LlmService;
import com.B0cka.DocuMind.service.search.SearchService;
import com.B0cka.DocuMind.service.vectorise.VectoriseService;
import com.B0cka.DocuMind.service.whisper.WhisperService;
import com.B0cka.DocuMind.service.cobalt.CobaltServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final CobaltServiceImpl cobaltService;
    private final WhisperService whisperService;
    private final VectoriseService vectoriseService;
    private final SearchService searchService;
    private final LlmService llmService;

    @Override
    public void transformVideo(RequestDto requestDto) {
        log.info("Загрузка видео через ссылку {}", requestDto.getLink());
        File audioFile = cobaltService.downloadAudio(requestDto.getLink());

        log.info("Расшифровка аудио через Whisper");
        String rawText = whisperService.transcribe(audioFile);

        log.info("Отправка транскрипта в LLM для смыслового разбиения");
        List<String> chunks = llmService.chunkText(rawText);

        log.info("Получено {} чанков, передаём их в VectoriseService", chunks.size());
        vectoriseService.processChunks(chunks, requestDto.getLink());
    }

    @Override
    public String searchVideo(SearchRequestDto dto) {
        log.info("Поиск видео по ссылке {}", dto.getLink());

        List<String> keywords = searchService.analyzeQuestion(dto.getQuestion());
        List<String> relevantChunks = new ArrayList<>();

        for (String s : keywords) {
            float[] questionVector = vectoriseService.callVectorizeServer(s);
            List<String> v = vectoriseService.findSimilarChunks(questionVector, 1, dto.getLink());
            relevantChunks.addAll(v);
        }
        return searchService.search(relevantChunks, dto.getQuestion());

    }


}
