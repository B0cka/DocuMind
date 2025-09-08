package com.B0cka.DocuMind.services.impl;

import com.B0cka.DocuMind.VectorResponse;
import com.B0cka.DocuMind.dto.OllamaRequest;
import com.B0cka.DocuMind.dto.OllamaResponse;
import com.B0cka.DocuMind.models.Vectors;
import com.B0cka.DocuMind.reposiroty.WebRepository;
import com.B0cka.DocuMind.services.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebServiceImpl implements WebService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final WebRepository webRepository;

    @Override
    public void loadPDF(MultipartFile multipartFile) {
        try {
            File file = convertPdfToTxt(multipartFile);
            List<String> chunks = chunkTxtFileStream(file, 200);

            for (String chunk : chunks) {
                try {
                    Map<String, String> requestBody = new HashMap<>();
                    requestBody.put("text", chunk);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = restTemplate.postForObject(
                            "http://localhost:8000/vectorize",
                            requestBody,
                            Map.class
                    );

                    if (response != null && response.containsKey("vector")) {
                        List<Double> vectorList = (List<Double>) response.get("vector");
                        float[] vectorArray = new float[vectorList.size()];
                        for (int i = 0; i < vectorList.size(); i++) {
                            vectorArray[i] = vectorList.get(i).floatValue();
                        }

                        log.info("Вектор: {}", vectorArray);
                        log.info("Чанк: {}", chunk);

                        Vectors vectors = Vectors.builder()
                                .vector(vectorArray)
                                .text(chunk)
                                .build();

                        webRepository.save(vectors);
                        log.error("Чанк успешно сохранился");

                    }
                } catch (Exception e) {
                    log.error("Ошибка при отправке чанка на сервер: {}", e.getMessage());
                }
            }

            file.delete();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка обработки PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public String search(String question) {
        try {
            // 1. Векторизуем вопрос
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", question);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    "http://localhost:8000/vectorize",
                    requestBody,
                    Map.class
            );

            if (response == null || !response.containsKey("vector")) {
                throw new RuntimeException("Не удалось векторизовать вопрос");
            }

            // 2. Преобразуем вектор
            List<Double> vectorList = (List<Double>) response.get("vector");
            float[] questionVector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                questionVector[i] = vectorList.get(i).floatValue();
            }

            log.info("Вектор вопроса получен, размер: {}", questionVector.length);

            // 3. Ищем похожие чанки
            List<String> relevantChunks = findSimilarChunks(questionVector, 5);

            if (relevantChunks.isEmpty()) {
                return "По вашему вопросу ничего не найдено";
            }

            // 4. Формируем контекст для LLM
            String context = String.join("\n\n", relevantChunks);
            String prompt = """
            Ответь строго на основе предоставленного контекста. Если в контексте нет ответа, скажи об этом.

            Контекст:
            %s

            Вопрос: %s

            Ответ:
            """.formatted(context, question);

            log.info("Отправляем запрос к LLM с контекстом из {} чанков", relevantChunks);

            OllamaResponse ollamaResponse = restTemplate.postForObject(URI.create("http://localhost:11434/api/generate"), OllamaRequest.builder().model("llama3:8b").prompt(prompt).stream(false).build(), OllamaResponse.class);
            return ollamaResponse.getResponse();

        } catch (Exception e) {
            log.error("Ошибка при поиске: {}", e.getMessage());
            return "Произошла ошибка при поиске: " + e.getMessage();
        }
    }

    public List<String> findSimilarChunks(float[] questionVector, int limit) {
        List<Object[]> results = webRepository.findSimilarVectors(questionVector, limit);

        List<String> chunks = new ArrayList<>();
        for (Object[] result : results) {
            String chunkText = (String) result[1];
            chunks.add(chunkText);
        }

        return chunks;
    }

    public File convertPdfToTxt(MultipartFile multipartFile) throws IOException {
        File txtFile = File.createTempFile("document", ".txt");

        try (PDDocument document = PDDocument.load(multipartFile.getInputStream());
             FileWriter writer = new FileWriter(txtFile)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);
            writer.write(fullText);
        }

        return txtFile;
    }

    public List<String> chunkTxtFileStream(File txtFile, int wordsPerChunk) throws IOException {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;

        try (Scanner scanner = new Scanner(txtFile)) {
            while (scanner.hasNext()) {
                String word = scanner.next();
                currentChunk.append(word).append(" ");
                wordCount++;

                if (wordCount >= wordsPerChunk) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                    wordCount = 0;
                }
            }
            if (wordCount > 0) {
                chunks.add(currentChunk.toString().trim());
            }
        }
        return chunks;
    }
}