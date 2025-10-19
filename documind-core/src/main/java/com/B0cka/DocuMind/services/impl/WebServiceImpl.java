package com.B0cka.DocuMind.services.impl;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;
import com.B0cka.DocuMind.services.WebService;
import com.B0cka.DocuMind.services.chunk.ChunkService;
import com.B0cka.DocuMind.services.chunk.VectorizationService;
import com.B0cka.DocuMind.services.document.PdfProcessingService;
import com.B0cka.DocuMind.services.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebServiceImpl implements WebService {

    private final PdfProcessingService pdfProcessingService;
    private final ChunkService chunkService;
    private final VectorizationService vectorizationService;
    private final SearchService searchService;

    @Override
    public void loadPDF(FrontRequest request, int group) {
        try {
            File txtFile = pdfProcessingService.convertPdfToTxtWithOCR(request.getFile());
            List<String> chunks = chunkService.chunkTxtFileByParagraphs(txtFile);
            vectorizationService.processChunks(chunks, request.getDocId());

            log.info("Документ {} успешно загружен и обработан", request.getDocId());
        } catch (Exception e) {
            log.error("Ошибка при загрузке PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось загрузить PDF", e);
        }
    }

    @Override
    public String search(FrontSearchRequest request, int limit) {
        List<String> keywords = searchService.analyzeQuestion(request.getQuestion());
        float[] questionVector = vectorizationService.callVectorizeServer(String.join(" ", keywords));
        List<String> relevantChunks = vectorizationService.findSimilarChunks(questionVector, limit, request.getDocId());

        return String.join("\n\n", relevantChunks);
    }

    @Override
    public String searchForAbstract(FrontSearchRequest request, int limit) {
        List<String> keywords = searchService.analyzeQuestionForAbstract(request.getQuestion());
        float[] questionVector = vectorizationService.callVectorizeServer(String.join(" ", keywords));
        List<String> relevantChunks = vectorizationService.findSimilarChunks(questionVector, limit, request.getDocId());

        return String.join("\n\n", relevantChunks);
    }
}
