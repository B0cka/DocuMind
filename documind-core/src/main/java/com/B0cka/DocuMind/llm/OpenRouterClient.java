package com.B0cka.DocuMind.llm;

import com.B0cka.DocuMind.dto.OpenRouterResponse;
import com.B0cka.DocuMind.service.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@PropertySource("application.properties")
public class OpenRouterClient implements LlmClient {

    private final RestTemplate restTemplate;

    @Value("${api.key}")
    private String apiKey;

    @Value("${url}")
    private String url;

    @Value("${model.name}")
    private String model;

    @Override
    public String sendPrompt(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

            ResponseEntity<OpenRouterResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    OpenRouterResponse.class
            );

            OpenRouterResponse body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Пустой ответ от OpenRouter API");
            }

            String content = body.extractContent();
            if (content == null || content.isBlank()) {
                throw new RuntimeException("Ответ не содержит текста");
            }

            log.info("Ответ от OpenRouter: {}", content.trim());
            return content.trim();
        } catch (Exception e) {
            log.error("Ошибка при вызове OpenRouter API: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обращении к OpenRouter API: " + e.getMessage());
        }
    }
}