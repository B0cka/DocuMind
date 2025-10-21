package com.B0cka.DocuMind.services.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@PropertySource(value = "application.properties")
public class SearchService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${url}")
    private String url;

    @Value("${model.name}")
    private String model;

    private final RestTemplate restTemplate;

    public ArrayList<String> analyzeQuestion(String string) {
        log.info("Детальный анализ вопроса: {}", string);

        String prompt = """
                    <|begin_of_text|><|start_header_id|>system<|end_header_id|>
                    Ты — аналитический помощник для системы поиска.
                    Преобразуй вопрос в набор ключевых слов и тематических понятий,
                    которые помогут найти нужные фрагменты текста.
                    Не пиши объяснений — выведи только слова через запятую, без кавычек и нумерации.
                
                    Пример:
                    Вопрос: Почему Пётр I начал реформы?
                    Ответ: Пётр I, реформы, Россия, XVIII век, западные идеи, модернизация
                    <|eot_id|><|start_header_id|>user<|end_header_id|>
                
                    Вопрос:
                    %s
                """.formatted(string);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", model);
        requestMap.put("prompt", prompt);
        requestMap.put("max_tokens", 1000);
        requestMap.put("temperature", 0.1);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);
        ResponseEntity<Map> awanResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> responseBody = awanResponse.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                String answer = (String) firstChoice.get("text");

                log.info("Ответ от LLM: {}", answer);

                return Arrays.stream(answer.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));

            }
        }
        throw new RuntimeException("Не удалось получить ответ от AwanLLM API");

    }

    public ArrayList<String> analyzeQuestionForAbstract(String string) {
        log.info("Детальный анализ вопроса: {}", string);

        String prompt = """
                    <|begin_of_text|><|start_header_id|>system<|end_header_id|>
                    Ты — аналитический помощник для системы создания конспектов.
                    Преобразуй вопрос в набор ключевых слов и тематических понятий,
                    которые помогут найти нужные фрагменты текста, постарайся охватить множество понятий вопроса.
                    Не пиши объяснений — выведи только слова через запятую, без кавычек и нумерации.
                
                    Пример:
                    Вопрос: Почему Пётр I начал реформы?
                    Ответ: Пётр I, реформы, Россия, XVIII век, западные идеи, модернизация
                    <|eot_id|><|start_header_id|>user<|end_header_id|>
                
                    Вопрос:
                    %s
                """.formatted(string);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", model);
        requestMap.put("prompt", prompt);
        requestMap.put("max_tokens", 1000);
        requestMap.put("temperature", 0.1);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);
        ResponseEntity<Map> awanResponse = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> responseBody = awanResponse.getBody();
        if (responseBody != null && responseBody.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                String answer = (String) firstChoice.get("text");

                log.info("Ответ от LLM: {}", answer);

                return Arrays.stream(answer.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));

            }
        }
        throw new RuntimeException("Не удалось получить ответ от AwanLLM API");

    }


}
