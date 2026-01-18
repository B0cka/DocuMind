package com.B0cka.DocuMind.service.summary;

import com.B0cka.DocuMind.dto.SummaryItemDto;
import com.B0cka.DocuMind.dto.VideoSummaryResponseDto;
import com.B0cka.DocuMind.model.VideoChunk;
import com.B0cka.DocuMind.repository.VideoRepository;
import com.B0cka.DocuMind.service.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryServiceImpl implements SummaryService {

    private final VideoRepository videoRepository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper; // стандартный бин Spring Boot

    @Override
    public VideoSummaryResponseDto summarize(String link) {
        List<VideoChunk> chunks = videoRepository.findByLinkOrderByStartTimeAsc(link);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("Для этого видео нет сохранённых чанков: " + link);
        }

        String context = buildContext(chunks);
        log.info("Длина контекста для суммаризации: {} символов", context.length());

        String prompt = buildPrompt(context);

        String rawJson = llmClient.sendPrompt(prompt);
        log.debug("RAW summary JSON: {}", rawJson);

        try {
            VideoSummaryResponseDto dto =
                    objectMapper.readValue(rawJson, VideoSummaryResponseDto.class);
            dto.setLink(link);
            return dto;
        } catch (Exception e) {
            log.error("Ошибка парсинга JSON из LLM: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось распарсить ответ модели как JSON", e);
        }
    }

    private String buildContext(List<VideoChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        for (VideoChunk c : chunks) {
            sb.append(String.format(
                    "[start=%.1f; end=%.1f] %s%n%n",
                    c.getStartTime(),
                    c.getEndTime(),
                    c.getText()
            ));
        }
        return sb.toString();
    }

    private String buildPrompt(String context) {
        return """
            Ты ассистент, который составляет структурированный конспект видео по его расшифровке.

            Ниже дан текст, разбитый на фрагменты с таймкодами.
            Формат каждого фрагмента:
            [start=S; end=E] текст...
            где S и E — время в секундах с начала видео.

            ЗАДАНИЕ:
            1. Проанализируй весь текст и сгруппируй его в логические блоки (секции).
            2. Для каждой секции:
               - задай короткий, емкий заголовок (title),
               - определи startTime и endTime в СЕКУНДАХ (double), в пределах исходных S и E,
               - сделай 3–7 кратких bullet-пунктов по сути этой части.
            3. Сформулируй общий краткий обзор всего видео (overallSummary) на 3–7 предложений.
            4. Ничего не выдумывай сверх того, что есть в тексте.

            ОТВЕТ:
            Верни СТРОГО JSON следующей структуры (без пояснений, комментариев и любого другого текста):

            {
              "overallSummary": "строка",
              "items": [
                {
                  "startTime": 0.0,
                  "endTime": 120.5,
                  "title": "строка",
                  "bullets": ["строка", "строка"]
                }
              ]
            }

            Обрати внимание:
            - startTime и endTime должны быть только числами (double),
            - bullets — это массив строк,
            - JSON должен быть валидным, без лишних полей.

            ТРАНСКРИПТ:
            %s
            """.formatted(context);
    }
}