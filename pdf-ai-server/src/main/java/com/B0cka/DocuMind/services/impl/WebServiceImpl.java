package com.B0cka.DocuMind.services.impl;

import com.B0cka.DocuMind.services.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebServiceImpl implements WebService {

    @Override
    public void loadPDF(MultipartFile multipartFile) {
        try (PDDocument document = PDDocument.load(multipartFile.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();

            String extractedText = stripper.getText(document);

            System.out.println(extractedText);

        } catch (IOException e) {

            throw new RuntimeException("Failed to parse PDF file: " + e.getMessage(), e);
        }
    }
}
