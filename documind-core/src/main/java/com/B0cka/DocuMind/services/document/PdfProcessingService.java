package com.B0cka.DocuMind.services.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
import java.util.*;

@Slf4j
@Service
@PropertySource(value = "application.properties")
public class PdfProcessingService {
    private final RestTemplate restTemplate;

    @Value("${tesseract.datapath:#{null}}")
    private String tesseractDataPath;

    public PdfProcessingService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
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
    } // method with tesseract implementation :)

    private boolean isTextInsufficient(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        String cleanText = text.replaceAll("\\s+", "");
        return cleanText.length() < 50;
    }

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

}
