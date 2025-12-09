package com.B0cka.DocuMind.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String normalizeText(String rawText) {
        String prompt = """
                Ты — корректурная нейросеть. 
                Задача: добавить корректную пунктуацию и большие буквы, 
                разделить текст на логические абзацы. 
                Не меняй смысл и порядок слов. 
                Верни только отредактированный текст.

                Текст:
                """ + rawText;

        log.info("Отправка текста в LLM на нормализацию");
        return openRouterClient.sendPrompt(prompt);
    }

    @Override
    public List<String> chunkText(String rawText) {
        String prompt = """
        Ты — нейросеть, которая умеет разбивать длинный текст на смысловые части для последующего анализа.
        Разбей текст на смысловые блоки (примерно по 5–7 предложений).
        После каждого блока вставь разделитель /nn
        Не сокращай и не перефразируй оригинал.
        Верни только текст с разделителями /nn.
        
        Текст:
        """ + rawText;

        log.info("Отправка текста в LLM для чанкирования");
        String llmResponse = openRouterClient.sendPrompt(prompt);

        String[] parts = llmResponse.split("/nn");
        List<String> chunks = new ArrayList<>();

        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                chunks.add(cleaned);
            }
        }

        log.info("LLM вернула {} чанков", chunks.size());
        return chunks;
    }
}