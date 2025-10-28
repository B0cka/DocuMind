package com.B0cka.DocuMind.service.impl;

import com.B0cka.DocuMind.dto.WhisperResponse;
import com.B0cka.DocuMind.dto.WhisperSegment;
import com.B0cka.DocuMind.service.WhisperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@PropertySource("application.properties")
public class WhisperServiceImpl implements WhisperService {

    private final RestTemplate restTemplate;
    @Value("${whisper.url}")
    private String whisperUrl;

    @Override
    public HashMap<Double, String> transcribe(File audioFile) {
        log.info("Обработка файла: {} через Whisper", audioFile.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio_file", new FileSystemResource(audioFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<WhisperResponse> response = restTemplate.exchange(
                whisperUrl,
                HttpMethod.POST,
                requestEntity,
                WhisperResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("Ошибка при обращении к Whisper: {}", response.getStatusCode());
            throw new RuntimeException("Ошибка при расшифровке аудио");
        }

        WhisperResponse whisperResponse = response.getBody();
        HashMap<Double, String> result = new HashMap<>();

        for (WhisperSegment segment : whisperResponse.getSegments()) {
            result.put(segment.getStart(), segment.getText());
        }

        log.info("Whisper вернул {} сегментов", whisperResponse.getSegments().size());
        return result;
    }

}
