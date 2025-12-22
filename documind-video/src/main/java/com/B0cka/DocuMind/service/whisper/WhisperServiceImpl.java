package com.B0cka.DocuMind.service.whisper;

import com.B0cka.DocuMind.dto.WhisperResponse;
import com.B0cka.DocuMind.dto.WhisperSegment;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhisperServiceImpl implements WhisperService {

    private final RestTemplate restTemplate;
    @Value("${whisper.url}")
    private String whisperUrl;

    @Override
    public List<WhisperSegment> transcribe(File audioFile) {
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
            throw new RuntimeException("Ошибка Whisper");
        }

        return response.getBody().getSegments();
    }

}
