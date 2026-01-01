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

            log.info("Документ {} успешно загружен", request.getDocId());
        } catch (Exception e) {
            log.error("Ошибка...", e);
            throw new RuntimeException("Не удалось загрузить PDF", e);
        }
    }
    @Override
    public String search(FrontSearchRequest request, int limit) {
        float[] questionVector = vectorizationService.callVectorizeServer(request.getQuestion());

        List<String> relevantChunks = vectorizationService.findSimilarChunks(questionVector, limit, request.getDocId());

        if (relevantChunks.isEmpty()) {
            return "Я не нашел информации в этом документе.";
        }

        return searchService.search(relevantChunks, request.getQuestion());
    }

    @Override
    public String searchForAbstract(FrontSearchRequest request, int limit) {
        float[] questionVector = vectorizationService.callVectorizeServer(request.getQuestion());
        List<String> relevantChunks = vectorizationService.findSimilarChunks(questionVector, limit, request.getDocId());

        if (relevantChunks.isEmpty()) {
            return "Я не нашел информации в этом документе.";
        }

        return searchService.searchForAbstract(relevantChunks, request.getQuestion());
    }
}
