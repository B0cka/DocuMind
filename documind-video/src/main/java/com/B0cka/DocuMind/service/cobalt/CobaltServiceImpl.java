package com.B0cka.DocuMind.service.cobalt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@PropertySource("application.properties")
public class CobaltServiceImpl implements CobaltService {

    private final RestTemplate restTemplate;

    @Value("${cobalt.url}")
    private String COBALT_URL;

    @Override
    public File downloadAudio(String link) {
        try {
            log.info("Отправка запроса в Cobalt: {}", link);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("url", link);
            requestMap.put("audioBitrate", "128");
            requestMap.put("audioFormat", "mp3");
            requestMap.put("downloadMode", "audio");
            requestMap.put("filenameStyle", "basic");
            requestMap.put("youtubeVideoCodec", "h264");
            requestMap.put("youtubeVideoContainer", "auto");
            requestMap.put("videoQuality", "1080");
            requestMap.put("localProcessing", "disabled");
            requestMap.put("disableMetadata", false);
            requestMap.put("allowH265", false);
            requestMap.put("convertGif", true);
            requestMap.put("tiktokFullAudio", false);
            requestMap.put("alwaysProxy", false);
            requestMap.put("youtubeHLS", false);
            requestMap.put("youtubeBetterAudio", false);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    COBALT_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() == null || !response.getBody().containsKey("url")) {
                throw new RuntimeException("Cobalt не вернул ссылку на файл");
            }

            String fileUrl = (String) response.getBody().get("url");
            log.info("Получена ссылка: {}", fileUrl);

            File tempFile = File.createTempFile("audio_", ".mp3");
            try (InputStream in = new URL(fileUrl).openStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                in.transferTo(out);
            }

            log.info("Файл сохранён: {}", tempFile.getAbsolutePath());
            return tempFile;

        } catch (Exception e) {
            log.error("Ошибка при загрузке из Cobalt", e);
            throw new RuntimeException(e);
        }
    }
}
