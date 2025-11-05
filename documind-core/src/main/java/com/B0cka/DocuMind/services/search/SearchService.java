package com.B0cka.DocuMind.services.search;

import com.B0cka.DocuMind.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

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

    private final LlmClient llmClient;

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


        String answer = llmClient.sendPrompt(prompt);

        log.info("Ответ от LLM: {}", answer);

        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

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



        String answer = llmClient.sendPrompt(prompt);

        log.info("Ответ от LLM: {}", answer);

        return Arrays.stream(answer.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

    }

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

            return llmClient.sendPrompt(prompt);

        } catch (Exception e) {
            log.error("Ошибка при поиске: {}", e.getMessage());
            return "Произошла ошибка при поиске: " + e.getMessage();
        }
    }

    public String searchForAbstract(List<String> relevantChunks, String question) {
        try {
            String context = String.join("\n\n", relevantChunks);
            log.info("Чанки, найденные по словам: {}", relevantChunks);
            String prompt = """ 
                    <|begin_of_text|><|start_header_id|>system<|end_header_id|> 
                    Ты — интеллектуальный помощник, который создает конспект исключительно на основе предоставленного контекста. 
                    Твоя задача — находить в тексте все относящиеся к вопросу сведения и формировать ясный, логичный, структурированный развернутый конспект. 
                    Действуй строго пошагово: 
                        1) Раздели контекст на смысловые части. 
                        2) Из каждой части выпиши все факты, относящиеся к вопросу. 
                        3) Объедини их, избегая потерь информации. 
                        4) Отдай приоритет конкретным данным, цифрам и примерам, а не общим формулировкам. 
                        5) Проверь каждое утверждение — есть ли в контексте прямое подтверждение? Если нет — исключи его или укажи, что данных недостаточно. 
                    
                    Не сокращай и не обобщай — включай даже мелкие детали. 
                    Не повторяй одну и ту же мысль разными словами. 
                    Пиши академически ясным стилем. 
                    Используй отступы и какие то выделения, если это необходимо. 
                    
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

            return llmClient.sendPrompt(prompt);

        } catch (Exception e) {
            log.error("Ошибка при поиске: {}", e.getMessage());

            return "Произошла ошибка при поиске: " + e.getMessage();
        }
    }
}
