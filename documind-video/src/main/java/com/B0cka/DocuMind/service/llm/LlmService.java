package com.B0cka.DocuMind.service.llm;

import java.util.List;

public interface LlmService {

    String normalizeText(String rawText);

    List<String> chunkText(String rawText);
}
