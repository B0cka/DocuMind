package com.B0cka.DocuMind.services.impl;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;
import com.B0cka.DocuMind.models.Document;
import com.B0cka.DocuMind.models.Vectors;
import com.B0cka.DocuMind.reposiroty.DocumentRepository;
import com.B0cka.DocuMind.reposiroty.WebRepository;
import com.B0cka.DocuMind.services.WebService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
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
import java.text.BreakIterator;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    public void loadPDF(FrontRequest request, int group) {
        try {
            if (documentRepository.existsById(request.getDocId())) {
                log.info("Документ {} уже существует, пропускаем обработку", request.getDocId());
            }

            File txtFile = convertPdfToTxtWithOCR(request.getFile());
            List<String> chunks = chunkTxtFileByParagraphs(txtFile);
            List<String> bigChunks = new ArrayList<>();

            int groupSize = group;
            StringBuilder sb = new StringBuilder();
            int count = 0;

            for (String p : chunks) {
                sb.append(p).append("\n\n");
                count++;

                if (count >= groupSize) {
                    bigChunks.add(sb.toString().trim());
                    sb.setLength(0);
                    count = 0;
                }
            }

            if (sb.length() > 0) {
                bigChunks.add(sb.toString().trim());
            }

            Document document = Document.builder()
                    .id(request.getDocId())
                    .filename(request.getFile().getOriginalFilename())
                    .originalFilename(request.getFile().getOriginalFilename())
                    .fileSize(request.getFile().getSize())
                    .totalChunks(bigChunks.size())
                    .uploadedAt(Instant.now())
                    .build();

            documentRepository.save(document);

            List<String> evenChunks = new ArrayList<>();
            List<String> oddChunks = new ArrayList<>();

            for (int i = 0; i < bigChunks.size(); i++) {
                if (i % 2 == 0) {
                    evenChunks.add(bigChunks.get(i));
                } else {
                    oddChunks.add(bigChunks.get(i));
                }
            }

            var executor = Executors.newVirtualThreadPerTaskExecutor();
                executor.submit(() -> processChunks(evenChunks, request.getDocId()));
                executor.submit(() -> processChunks(oddChunks, request.getDocId()));
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.MINUTES);

            if (txtFile.exists()) {
                txtFile.delete();
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка обработки PDF: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String search(FrontSearchRequest frontSearchRequest, int limit) {
        try {

            ArrayList<String> verbs = analyzeQuestion(frontSearchRequest.getQuestion());
            List<String> relevantChunks = verbs.parallelStream()
                    .map(this::callVectorizeServer)
                    .flatMap(v -> findSimilarChunks(v, limit, frontSearchRequest.getDocId()).stream())
                    .distinct()
                    .toList();

            if (relevantChunks.isEmpty()) {
                return "По вашему вопросу ничего не найдено";
            }

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
                    5) Проверь каждое утверждение — есть ли в контексте прямое подтверждение? 
                       Если нет — исключи его или укажи, что данных недостаточно.
                    
                    Не сокращай и не обобщай — включай даже мелкие детали.
                    Не повторяй одну и ту же мысль разными словами.
                    Пиши академически ясным стилем.
                    Минимальный объём ответа — 200 слов (если информации достаточно).
                    Ответ всегда давай на русском языке.
                    
                    <|eot_id|><|start_header_id|>user<|end_header_id|>
                    Контекст (фрагменты из документа):
                    %s
                    
                    Вопрос пользователя:
                    %s
                    
                    <|eot_id|><|start_header_id|>assistant<|end_header_id|>
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

    @Override
    public String searchForAbstract(FrontSearchRequest frontSearchRequest, int limit) {
        try {

            ArrayList<String> verbs = analyzeQuestionForAbstract(frontSearchRequest.getQuestion());
            List<String> relevantChunks = verbs.parallelStream()
                    .map(this::callVectorizeServer)
                    .flatMap(v -> findSimilarChunks(v, limit, frontSearchRequest.getDocId()).stream())
                    .distinct()
                    .toList();

            if (relevantChunks.isEmpty()) {
                return "По вашему вопросу ничего не найдено";
            }

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
                    5) Проверь каждое утверждение — есть ли в контексте прямое подтверждение? 
                       Если нет — исключи его или укажи, что данных недостаточно.
                    
                    Не сокращай и не обобщай — включай даже мелкие детали.
                    Не повторяй одну и ту же мысль разными словами.
                    Пиши академически ясным стилем.
                    Используй отступы и какие то выделения, если это необходимо
                    Минимальный объём ответа — 200 слов (если информации достаточно).
                    Ответ всегда давай на русском языке.
                    
                    <|eot_id|><|start_header_id|>user<|end_header_id|>
                    Контекст (фрагменты из документа):
                    %s
                    
                    Вопрос пользователя:
                    %s
                    
                    <|eot_id|><|start_header_id|>assistant<|end_header_id|>
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

    //---------------Tesseract-Server---------------//
    private boolean isTextInsufficient(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        String cleanText = text.replaceAll("\\s+", "");
        return cleanText.length() < 50;
    }

    public File convertPdfToTxtWithOCR(MultipartFile multipartFile) throws IOException {
        File txtFile = File.createTempFile("document", ".txt");

        try (PDDocument document = PDDocument.load(multipartFile.getInputStream());
             FileWriter writer = new FileWriter(txtFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

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
    }//method with tesseract implementation :)

    private String callTesseractServer(File imageFile) {
        try {

            Map<String, Object> options = new HashMap<>();
            options.put("languages", Arrays.asList("rus", "eng"));
            options.put("dpi", 300);
            options.put("pageSegmentationMethod", 6);
            options.put("ocrEngineMode", 1);

            ObjectMapper objectMapper = new ObjectMapper();
            String optionsJson = objectMapper.writeValueAsString(options);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("options", optionsJson);
            body.add("file", new FileSystemResource(imageFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "http://localhost:8123/tesseract",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);

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

    //-------------Working with document------------//
    public List<String> findSimilarChunks(float[] questionVector, int limit, String docId) {
        try {

            String vectorString = Arrays.toString(questionVector)
                    .replace("[", "[")
                    .replace("]", "]");

            List<Object[]> results = webRepository.findSimilarVectors(vectorString, limit, docId);

            return results.stream()
                    .filter(r -> r.length > 0 && r[0] != null)
                    .map(r -> (String) r[0])
                    .toList();

        } catch (Exception e) {
            log.error("Ошибка при поиске похожих чанков: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<String> chunkTxtFileByParagraphs(File txtFile) throws IOException {
        List<String> chunks = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                textBuilder.append(line).append("\n");
            }
        }

        String text = textBuilder.toString()
                .replaceAll("-\\s*\\n\\s*", "")
                .replaceAll("(?<![\\.\\?\\!])\\n", " ");

        String[] paragraphs = text.split("\\n\\s*\\n");


        int sentencesPerChunk = 7;
        int overlap = 1;
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            List<String> sentences = new ArrayList<>();
            BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.forLanguageTag("ru"));
            iterator.setText(paragraph);
            int start = iterator.first();
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                String sentence = paragraph.substring(start, end).trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
            }

            for (int i = 0; i < sentences.size(); i += sentencesPerChunk - overlap) {
                int end = Math.min(i + sentencesPerChunk, sentences.size());
                String chunk = String.join(" ", sentences.subList(i, end));
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    private void processChunks(List<String> chunks, String docId) {
        for (String chunk : chunks) {
            try {
                Map<String, String> requestBody = Map.of("text", chunk);

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
                            .docId(docId)
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
    }

    private float[] callVectorizeServer(String str){
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", str);

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

        return questionVector;
    }

    //------------------Questions------------------//
    private ArrayList<String> analyzeQuestion(String string) {
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
        requestMap.put("model", "Meta-Llama-3.1-70B-Instruct");
        requestMap.put("prompt", prompt);
        requestMap.put("max_tokens", 1000);
        requestMap.put("temperature", 0.1);

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

                log.info("Ответ от LLM: {}", answer);

                return Arrays.stream(answer.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));

            }
        }
        throw new RuntimeException("Не удалось получить ответ от AwanLLM API");

    }

    private ArrayList<String> analyzeQuestionForAbstract(String string) {
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
        requestMap.put("model", "Meta-Llama-3.1-70B-Instruct");
        requestMap.put("prompt", prompt);
        requestMap.put("max_tokens", 1000);
        requestMap.put("temperature", 0.1);

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