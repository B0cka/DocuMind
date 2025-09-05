package com.B0cka.DocuMind.services.impl;

import com.B0cka.DocuMind.services.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebServiceImpl implements WebService {

    private final VectorStore vectorStore;

    @Override
    public void loadPDF(MultipartFile multipartFile) {
        log.info("Начало обработки PDF файла: {}", multipartFile.getOriginalFilename());

        File txtFile = null;
        try {
            validatePdfFile(multipartFile);

            txtFile = convertPdfToTxt(multipartFile);
            log.debug("PDF успешно конвертирован во временный файл: {}", txtFile.getAbsolutePath());

            List<String> textChunks = chunkTxtFileStream(txtFile, 200);
            log.info("Текст разбит на {} чанков", textChunks.size());

            if (textChunks.isEmpty()) {
                log.warn("PDF не содержит текста для обработки");
                return;
            }

            List<Document> documents = convertToDocuments(textChunks);

            saveToVectorStore(documents);

            log.info("Обработка PDF завершена успешно. Сохранено документов: {}", documents.size());

        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации файла: {}", e.getMessage());
            throw new RuntimeException("Неверный формат файла: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Ошибка чтения/записи файла: {}", e.getMessage());
            throw new RuntimeException("Ошибка обработки файла", e);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обработке PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Внутренняя ошибка сервера", e);
        } finally {

            cleanupTempFile(txtFile);
        }
    }

    private void validatePdfFile(MultipartFile multipartFile) {
        log.debug("Валидация PDF файла...");

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        if (!multipartFile.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("Файл должен быть в формате PDF");
        }

        if (multipartFile.getSize() > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("Размер файла превышает 50MB");
        }

        log.debug("Валидация PDF прошла успешно");
    }

    private File convertPdfToTxt(MultipartFile multipartFile) throws IOException {
        log.debug("Начало конвертации PDF в текст...");

        File txtFile = File.createTempFile("document_", ".txt");

        try (PDDocument document = PDDocument.load(multipartFile.getInputStream());
             FileWriter writer = new FileWriter(txtFile)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            if (fullText == null || fullText.trim().isEmpty()) {
                throw new IOException("PDF не содержит текста или защищен паролем");
            }

            writer.write(fullText);
            log.debug("Конвертация завершена. Размер текста: {} символов", fullText.length());
        } catch (IOException e) {
            log.error("Ошибка при чтении PDF: {}", e.getMessage());
            throw new IOException("Не удалось прочитать PDF файл", e);
        }

        return txtFile;
    }

    private List<String> chunkTxtFileStream(File txtFile, int wordsPerChunk) throws IOException {
        log.debug("Начало разбивки текста на чанки по {} слов...", wordsPerChunk);

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;
        int totalChunks = 0;

        try (Scanner scanner = new Scanner(txtFile)) {
            while (scanner.hasNext()) {
                String word = scanner.next();
                currentChunk.append(word).append(" ");
                wordCount++;

                if (wordCount >= wordsPerChunk) {
                    String chunkText = currentChunk.toString().trim();
                    if (!chunkText.isEmpty()) {
                        chunks.add(chunkText);
                        totalChunks++;
                        log.debug("Добавлен чанк #{}: {}...", totalChunks,
                                chunkText.substring(0, Math.min(50, chunkText.length())));
                    }
                    currentChunk.setLength(0);
                    wordCount = 0;
                }
            }

            if (wordCount > 0) {
                String lastChunk = currentChunk.toString().trim();
                if (!lastChunk.isEmpty()) {
                    chunks.add(lastChunk);
                    totalChunks++;
                    log.debug("Добавлен последний чанк #{}", totalChunks);
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при чтении текстового файла: {}", e.getMessage());
            throw new IOException("Не удалось прочитать текстовый файл", e);
        }

        log.info("Текст разбит на {} чанков", totalChunks);
        return chunks;
    }

    private List<Document> convertToDocuments(List<String> textChunks) {
        log.debug("Преобразование чанков в документы...");

        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {
            String chunk = textChunks.get(i);
            Document document = new Document(chunk);

            document.getMetadata().put("chunk_index", i);
            document.getMetadata().put("chunk_size", chunk.length());
            document.getMetadata().put("source", "pdf_upload");

            documents.add(document);

            if (i < 3) {
                log.debug("Документ {}: {}...", i,
                        chunk.substring(0, Math.min(100, chunk.length())));
            }
        }

        log.debug("Создано {} документов", documents.size());
        return documents;
    }

    private void saveToVectorStore(List<Document> documents) {
        log.debug("Сохранение {} документов в векторное хранилище...", documents.size());

        long startTime = System.currentTimeMillis();

        try {
            vectorStore.add(documents);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Успешно сохранено {} документов за {} мс",
                    documents.size(), duration);

        } catch (Exception e) {
            log.error("Ошибка при сохранении в векторное хранилище: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка сохранения в базу данных", e);
        }
    }

    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            try {
                if (tempFile.delete()) {
                    log.debug("Временный файл удален: {}", tempFile.getAbsolutePath());
                } else {
                    log.warn("Не удалось удалить временный файл: {}", tempFile.getAbsolutePath());
                }
            } catch (SecurityException e) {
                log.warn("Нет прав на удаление временного файла: {}", e.getMessage());
            }
        }
    }

    public long getDocumentCount() {
        try {
            log.debug("Получение количества документов в хранилище");
            return 0;
        } catch (Exception e) {
            log.error("Ошибка при получении количества документов: {}", e.getMessage());
            return -1;
        }
    }
}