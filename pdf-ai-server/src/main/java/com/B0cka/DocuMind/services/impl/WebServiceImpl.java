package com.B0cka.DocuMind.services.impl;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;
import com.B0cka.DocuMind.models.Document;
import com.B0cka.DocuMind.models.Vectors;
import com.B0cka.DocuMind.reposiroty.DocumentRepository;
import com.B0cka.DocuMind.reposiroty.WebRepository;
import com.B0cka.DocuMind.services.WebService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@PropertySource(value = "application.properties")
public class WebServiceImpl implements WebService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final WebRepository webRepository;
    private final DocumentRepository documentRepository;

    @Value("${api.key}")
    private String apiKey;
    @Value("${tesseract.datapath:#{null}}")
    private String tesseractDataPath;
    @Override
    public void loadPDF(FrontRequest request) {
        try {
            if (documentRepository.existsById(request.getDocId())) {
                log.info("Документ {} уже существует, пропускаем обработку", request.getDocId());
            }

            File txtFile = convertPdfToTxtWithOCR(request.getFile());
            List<String> chunks = chunkTxtFileByParagraphs(txtFile);

            Document document = Document.builder()
                    .id(request.getDocId())
                    .filename(request.getFile().getOriginalFilename())
                    .originalFilename(request.getFile().getOriginalFilename())
                    .fileSize(request.getFile().getSize())
                    .totalChunks(chunks.size())
                    .uploadedAt(Instant.now())
                    .build();

            documentRepository.save(document);

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                try {
                    Map<String, String> requestBody = new HashMap<>();
                    requestBody.put("text", chunk);

                    Map<String, Object> response = restTemplate.postForObject(
                            "http://localhost:8000/vectorize",
                            requestBody,
                            Map.class
                    );

                    if (response != null && response.containsKey("vector")) {
                        List<Double> vectorList = (List<Double>) response.get("vector");
                        float[] vectorArray = new float[vectorList.size()];
                        for (int j = 0; j < vectorList.size(); j++) {
                            vectorArray[j] = vectorList.get(j).floatValue();
                        }

                        Vectors vectorEntity = Vectors.builder()
                                .vector(vectorArray)
                                .docId(request.getDocId())
                                .text(chunk)
                                .createdAt(Instant.now())
                                .build();

                        webRepository.save(vectorEntity);
                        log.info("Чанк сохранен: {}", chunk);
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обработке чанка {}: {}", chunk, e.getMessage());
                }
            }

            if (txtFile.exists()) {
                txtFile.delete();
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка обработки PDF: " + e.getMessage(), e);
        }
    }
    @Override
    public String search(FrontSearchRequest frontSearchRequest) {
        try {

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", frontSearchRequest.getQuestion());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    "http://localhost:8000/vectorize",
                    requestBody,
                    Map.class
            );

            if (response == null || !response.containsKey("vector")) {
                throw new RuntimeException("Не удалось векторизовать вопрос");
            }

            List<Double> vectorList = (List<Double>) response.get("vector");
            float[] questionVector = new float[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                questionVector[i] = vectorList.get(i).floatValue();
            }

            log.info("Вектор вопроса получен, размер: {}", questionVector.length);

            List<String> relevantChunks = findSimilarChunks(questionVector, 5, frontSearchRequest.getDocId());

            if (relevantChunks.isEmpty()) {
                return "По вашему вопросу ничего не найдено";
            }

            String context = String.join("\n\n", relevantChunks);

            String prompt = """
            <|begin_of_text|><|start_header_id|>system<|end_header_id|>
            Ты - помощник, который отвечает на вопросы строго на основе предоставленного контекста.
            Старайся максимально конктретизировать ответ на вопрос, выжимая максимум информации из предоставленного тебе текста по данному вопросу.
            Если в контексте нет информации для ответа, скажи об этом.
            Отвечай только на русском языке.<|eot_id|>
            <|start_header_id|>user<|end_header_id|>
            Контекст:
            %s

            Вопрос: %s<|eot_id|>
            <|start_header_id|>assistant<|end_header_id|>
            """.formatted(context, frontSearchRequest.getQuestion());

            log.info("Отправляем запрос к AwanLLM API с контекстом из {} чанков", relevantChunks.size());
            log.info("Чанки: {}", relevantChunks);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", "Meta-Llama-3.1-70B-Instruct");
            requestMap.put("prompt", prompt);
            requestMap.put("max_tokens", 1000);
            requestMap.put("temperature", 0.1);
            requestMap.put("stop", Arrays.asList("<|eot_id|>", "<|end_of_text|>"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestMap, headers);

            ResponseEntity<Map> awanResponse = restTemplate.exchange(
                    "https://api.awanllm.com/v1/completions",
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
                    log.info("Ответ от AwanLLM: {}", answer);
                    return answer;
                }
            }

            throw new RuntimeException("Не удалось получить ответ от AwanLLM API");

        } catch (Exception e) {
            log.error("Ошибка при поиске: {}", e.getMessage());
            return "Произошла ошибка при поиске: " + e.getMessage();
        }
    }

    public List<String> findSimilarChunks(float[] questionVector, int limit, String docId) {
        try {

            String vectorString = Arrays.toString(questionVector)
                    .replace("[", "[")
                    .replace("]", "]");

            List<Object[]> results = webRepository.findSimilarVectors(vectorString, limit, docId);

            List<String> chunks = new ArrayList<>();
            for (Object[] result : results) {
                if (result.length > 0 && result[0] != null) {
                    chunks.add((String) result[0]);
                }
            }

            return chunks;

        } catch (Exception e) {
            log.error("Ошибка при поиске похожих чанков: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public File convertPdfToTxtWithOCR(MultipartFile multipartFile) throws IOException {
        File txtFile = File.createTempFile("document", ".txt");

        try (PDDocument document = PDDocument.load(multipartFile.getInputStream());
             FileWriter writer = new FileWriter(txtFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                stripper.setStartPage(page + 1);
                stripper.setEndPage(page + 1);

                String pageText = stripper.getText(document);

                if (isTextInsufficient(pageText)) {
                    log.info("Страница {} содержит недостаточно текста, применяем OCR через Tesseract Server", page + 1);

                    try {

                        BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
                        File imageFile = File.createTempFile("page_" + page, ".png");
                        ImageIO.write(image, "png", imageFile);

                        pageText = callTesseractServer(imageFile);
                        log.info("Tesseract Server успешно обработал страницу {}", page + 1);

                        imageFile.delete();

                    } catch (Exception e) {
                        log.error("Ошибка при обращении к Tesseract Server для страницы {}: {}", page + 1, e.getMessage());
                        pageText = " ";
                    }
                }

                writer.write(pageText);
                writer.write("\n\n");
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке PDF: {}", e.getMessage());
            throw new IOException("Ошибка при обработке PDF", e);
        }

        return txtFile;
    }

    public List<String> chunkTxtFileByParagraphs(File txtFile) throws IOException {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    // пустая строка = конец абзаца
                    if (currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                        currentChunk.setLength(0);
                    }
                } else {
                    currentChunk.append(line).append(" ");
                }
            }
            if (currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
            }
        }
        return chunks;
    }

    private String callTesseractServer(File imageFile) {
        try {
            // Создаем объект с настройками OCR
            Map<String, Object> options = new HashMap<>();
            options.put("languages", Arrays.asList("rus", "eng")); // Языки для распознавания
            options.put("dpi", 300); // DPI изображения
            options.put("pageSegmentationMethod", 6); // PSM (Page Segmentation Mode)
            options.put("ocrEngineMode", 1); // OEM (OCR Engine Mode)

            // Преобразуем настройки в JSON строку
            ObjectMapper objectMapper = new ObjectMapper();
            String optionsJson = objectMapper.writeValueAsString(options);

            // Формируем multipart/form-data запрос
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("options", optionsJson); // JSON с настройками
            body.add("file", new FileSystemResource(imageFile)); // Файл изображения

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "http://localhost:8123/tesseract",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Обрабатываем ответ
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);

                // Извлекаем данные из ответа
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                String extractedText = (String) data.get("stdout");

                log.info("Ответ tesseract сервера: {}", extractedText);
                return extractedText != null ? extractedText.trim() : "";
            } else {
                log.error("Tesseract Server вернул ошибку: {} - {}", response.getStatusCode(), response.getBody());
                return "";
            }
        } catch (Exception e) {
            log.error("Ошибка при обращении к Tesseract Server: {}", e.getMessage());
            return "";
        }
    }

    private boolean isTextInsufficient(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }

        String cleanText = text.replaceAll("\\s+", "");

        return cleanText.length() < 50;
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