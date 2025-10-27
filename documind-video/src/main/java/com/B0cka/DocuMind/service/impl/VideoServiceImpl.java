package com.B0cka.DocuMind.service.impl;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.service.VideoService;
import com.B0cka.DocuMind.service.WhisperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor

public class VideoServiceImpl implements VideoService {

    private final CobaltServiceImpl cobaltService;
    private final WhisperService whisperService;
    private final VectoriseService vectoriseService;

    @Override
    public void transformVideo(RequestDto requestDto){
        log.info("Загрузка видео через ссылку {}", requestDto.getLink());
        File audio_file = cobaltService.downloadAudio(requestDto.getLink());

        log.info("Расшифровка видео");
        HashMap<Double, String> result = whisperService.transcribe(audio_file);
        log.info("Ответ нейросети: {}", result.toString());
        vectoriseService.processChunks(result, requestDto.getLink());

    }


}
