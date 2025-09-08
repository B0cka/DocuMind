package com.B0cka.DocuMind.controllers;

import com.B0cka.DocuMind.services.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
public class WebController {

    private final WebService webService;

    @PostMapping(value = "/load-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        log.info("получен файл: {}", file.getOriginalFilename());
        webService.loadPDF(file);
        return ResponseEntity.ok().body("Файл '" + file.getOriginalFilename() + "' загружен!");
    }

    @PostMapping(value = "/search")
    public ResponseEntity<String> searchAnswer(@RequestBody String question){
        log.info("получен вопрос: {}", question);
        return ResponseEntity.ok().body(webService.search(question));
    }

}
