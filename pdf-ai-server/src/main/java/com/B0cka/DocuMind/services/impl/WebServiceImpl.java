package com.B0cka.DocuMind.services.impl;

import com.B0cka.DocuMind.VectorResponse;
import com.B0cka.DocuMind.models.Vectors;
import com.B0cka.DocuMind.repository.WebRepository;
import com.B0cka.DocuMind.services.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.*;

@Service
@Slf4j
public class WebServiceImpl implements WebService {
    RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private final WebRepository webRepository;

    public WebServiceImpl(WebRepository webRepository) {
        this.webRepository = webRepository;
    }

    @Override
    public void loadPDF(MultipartFile multipartFile) {
        try {
            File file = convertPdfToTxt(multipartFile);
            List<String> chunks = chunkTxtFileStream(file, 200);

            String pythonServerUrl = "http://localhost:8000/vectorize";

            for (String chunk : chunks) {
                try {

                    Map<String, String> requestBody = new HashMap<>();
                    requestBody.put("text", chunk);

                    VectorResponse vectorResponse = restTemplate.postForObject(
                            "http://localhost:8000/vectorize",
                            requestBody,
                            VectorResponse.class
                    );

                    Vectors vectors = Vectors.builder()
                            .vectors(vectorResponse.getVector())
                            .build();
                    log.info("сохраненние в репозиторий: {}", vectorResponse.getVector());
                    webRepository.save(vectors);

                } catch (Exception e) {
                    log.error("Ошибка при отправке чанка на сервер: {}", e.getMessage());
                }
            }

            file.delete();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка обработки PDF: " + e.getMessage(), e);
        }
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
                    log.info("Добавление чанка: {}", currentChunk.toString().trim());
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
