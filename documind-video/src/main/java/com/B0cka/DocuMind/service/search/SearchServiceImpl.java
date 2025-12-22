package com.B0cka.DocuMind.service.search;

import com.B0cka.DocuMind.model.VideoChunk;
import com.B0cka.DocuMind.service.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LlmClient llmClient;

    @Override
    public String search(List<VideoChunk> relevantChunks, String question) {

        StringBuilder contextBuilder = new StringBuilder();
        for (VideoChunk chunk : relevantChunks) {
            contextBuilder.append(String.format("[%s - %s]: %s\n\n",
                    formatTime(chunk.getStartTime()),
                    formatTime(chunk.getEndTime()),
                    chunk.getText()));
        }
        String context = contextBuilder.toString();

        String prompt = """
            <|begin_of_text|><|start_header_id|>system<|end_header_id|>
            Ты полезный помощник. Используй ТОЛЬКО следующий контекст из видео, чтобы ответить на вопрос пользователя.
            Если в контексте нет ответа, скажи "Я не нашел информации в этом видео".
            Всегда указывай таймкоды, когда ссылаешься на факты (например: "Как сказано на 05:20...").
            
            Контекст:
            %s
            <|eot_id|><|start_header_id|>user<|end_header_id|>
            Вопрос: %s
            """.formatted(context, question);

        return llmClient.sendPrompt(prompt);
    }

    private String formatTime(double seconds) {
        int m = (int) (seconds / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d", m, s);
    }
}