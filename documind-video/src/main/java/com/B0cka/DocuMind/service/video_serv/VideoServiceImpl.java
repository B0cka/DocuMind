package com.B0cka.DocuMind.service.video_serv;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.dto.SearchRequestDto;
import com.B0cka.DocuMind.service.search.SearchService;
import com.B0cka.DocuMind.service.vectorise.VectoriseService;
import com.B0cka.DocuMind.service.whisper.WhisperService;
import com.B0cka.DocuMind.service.cobalt.CobaltServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
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

    @Override
    public void transformVideo(RequestDto requestDto){
        log.info("Загрузка видео через ссылку {}", requestDto.getLink());
        File audio_file = cobaltService.downloadAudio(requestDto.getLink());

        log.info("Расшифровка видео");
        HashMap<Double, String> result = null;
        result = whisperService.transcribe(audio_file);

        log.info("Ответ нейросети: {}", result.toString());
        vectoriseService.processChunks(result, requestDto.getLink());

    }

    @Override
    public String searchVideo(SearchRequestDto dto){
        log.info("Поиск видео по ссылке {}", dto.getLink());

        List<String> keywords = searchService.analyzeQuestion(dto.getQuestion());
        //problems
        float[] questionVector = vectoriseService.callVectorizeServer(String.join(" ", keywords));
        List<String> relevantChunks = vectoriseService.findSimilarChunks(questionVector, 1, dto.getLink());

        return searchService.search(relevantChunks, dto.getQuestion());

        }


}
