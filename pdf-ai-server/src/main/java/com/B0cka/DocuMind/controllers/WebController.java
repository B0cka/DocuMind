package com.B0cka.DocuMind.controllers;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;
import com.B0cka.DocuMind.services.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://127.0.0.1:5500"})
public class WebController {

    private final WebService webService;


    @PostMapping(value = "/load-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("docId") String docId) {
        log.info("получен файл: {}", file.getOriginalFilename());
        log.info("получен ключ: {}", docId);

        FrontRequest request = new FrontRequest();
        request.setFile(file);
        request.setDocId(docId);
        webService.loadPDF(request);

        return ResponseEntity.ok().body("Файл '" + file.getOriginalFilename() + "' загружен!");
    }

    @PostMapping(value = "/search")
    public ResponseEntity<String> searchAnswer(@RequestBody FrontSearchRequest frontSearchRequest){
        log.info("получен вопрос: {}", frontSearchRequest.getQuestion());
        log.info("получен ключ: {}", frontSearchRequest.getDocId());
        return ResponseEntity.ok().body(webService.search(frontSearchRequest));
    }

}
