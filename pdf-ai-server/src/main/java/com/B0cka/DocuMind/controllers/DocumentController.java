package com.B0cka.DocuMind.controllers;

import com.B0cka.DocuMind.models.Document;
import com.B0cka.DocuMind.reposiroty.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentRepository documentRepository;

    @GetMapping("/{docId}/exists")
    public ResponseEntity<Map<String, Boolean>> checkDocumentExists(@PathVariable String docId) {
        boolean exists = documentRepository.existsById(docId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/{docId}")
    public ResponseEntity<?> getDocumentInfo(@PathVariable String docId) {
        Optional<Document> document = documentRepository.findById(docId);

        if (document.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "id", document.get().getId(),
                "filename", document.get().getFilename(),
                "originalFilename", document.get().getOriginalFilename(),
                "fileSize", document.get().getFileSize(),
                "totalChunks", document.get().getTotalChunks(),
                "uploadedAt", document.get().getUploadedAt()
        ));
    }
}