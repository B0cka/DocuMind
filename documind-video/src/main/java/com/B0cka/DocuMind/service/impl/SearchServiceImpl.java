package com.B0cka.DocuMind.service.impl;

import com.B0cka.DocuMind.service.SearchService;
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
public class SearchServiceImpl implements SearchService {

    @Value("${api.key}")
    private String apiKey;

    @Value("${url}")
    private String url;

    @Value("${model.name}")
    private String model;

    private final RestTemplate restTemplate;

    @Override
    public ArrayList<String> analyzeQuestion(String string) {
        log.info("Детальный анализ вопроса: {}", string);

        String prompt = """
                    <|begin_of_text|><|start_header_id|>system<|end_header_id|>
                    Ты — аналитический помощник для системы поиска по текстовому конспекту из видео.
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


        HttpEntity<Map<String, Object>> entity = buildRequest(prompt, model);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response == null || response.getBody() == null) {
            log.error("Пустой ответ от API");
            throw new RuntimeException("API не вернуло тело ответа");
        }

        Map<String, Object> responseBody = response.getBody();
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

    @Override
    public ArrayList<String> analyzeQuestionForAbstract(String string) {
        log.info("Детальный анализ вопроса: {}", string);

        String prompt = """
                    <|begin_of_text|><|start_header_id|>system<|end_header_id|>
                    Ты — аналитический помощник для системы создания конспектов по тексту из видео.
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


        HttpEntity<Map<String, Object>> entity = buildRequest(prompt, model);
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response == null || response.getBody() == null) {
            log.error("Пустой ответ от API");
            throw new RuntimeException("API не вернуло тело ответа");
        }

        Map<String, Object> responseBody = response.getBody();
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
        throw new RuntimeException("Не удалось получить ответ от LLM API");

    }

    @Override
    public String search(List<String> relevantChunks, String question) {
        try {
            String context = String.join("\n\n", relevantChunks);
            log.info("Чанки, найденные по словам: {}", relevantChunks);
            String prompt = """ 
                    <|begin_of_text|><|start_header_id|>system<|end_header_id|> 
                    Ты — интеллектуальный помощник, который отвечает на вопросы исключительно на основе предоставленного контекста. 
                    Твоя задача — находить в тексте все относящиеся к вопросу сведения и формировать ясный, логичный и развернутый ответ. 
                    Действуй строго пошагово: 
                        1) Раздели контекст на смысловые части. 
                        2) Из каждой части выпиши все факты, относящиеся к вопросу. 
                        3) Объедини их, избегая потерь информации. 
                        4) Отдай приоритет конкретным данным, цифрам и примерам, а не общим формулировкам. 
                        5) Проверь каждое утверждение — есть ли в контексте прямое подтверждение? Если нет — исключи его или укажи, что данных недостаточно. 
                    
                    Не сокращай и не обобщай — включай даже мелкие детали. Не повторяй одну и ту же мысль разными словами. 
                    Пиши академически ясным стилем. 
                    Минимальный объём ответа — 200 слов (если информации достаточно). 
                    
                    Ответ всегда давай на русском языке. 
                    
                    <|eot_id|><|start_header_id|>user<|end_header_id|> 
                    
                    Контекст (фрагменты из документа): 
                    
                    %s 
                    
                    Вопрос пользователя: 
                    
                    %s 
                    
                    <|eot_id|><|start_header_id|>assistant<|end_header_id|> 
                    
                    """.formatted(context, question);

            log.info("Отправляем запрос к AwanLLM API с контекстом из {} чанков", relevantChunks.size());
            log.info("Чанки: {}", relevantChunks);

            HttpEntity<Map<String, Object>> entity = buildRequest(prompt, model);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response == null || response.getBody() == null) {
                log.error("Пустой ответ от API");
                throw new RuntimeException("API не вернуло тело ответа");
            }

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    String answer = (String) firstChoice.get("text");
                    log.info("Ответ от LLM: {}", answer);
                    return answer;
                }
            }
            throw new RuntimeException("Не удалось получить ответ от AwanLLM API");

        } catch (Exception e) {
            log.error("Ошибка при поиске: {}", e.getMessage());
            return "Произошла ошибка при поиске: " + e.getMessage();
        }
    }

    private HttpEntity<Map<String, Object>> buildRequest(String prompt, String model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", model);
        requestMap.put("prompt", prompt);
        requestMap.put("max_tokens", 1000);
        requestMap.put("temperature", 0.1);

        return new HttpEntity<>(requestMap, headers);
    }

}
