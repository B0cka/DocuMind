package com.B0cka.DocuMind.controllers;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;
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
@CrossOrigin(origins = {"http://127.0.0.1:5500"})
public class AbstractController {

    private final WebService webService;

    @PostMapping(value = "/abstract-load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("docId") String docId) {
        log.info("Получение файла для конспекта: {} полное имя: {}", file.getName(), file.getOriginalFilename());

        FrontRequest frontRequest = new FrontRequest(file, docId);
        webService.loadPDF(frontRequest, 1);

        return ResponseEntity.ok().body("Файл '" + file.getOriginalFilename() + "' загружен!");
    }

    @PostMapping(value = "/abstract-search")
    public ResponseEntity<String> searchAnswer(@RequestBody FrontSearchRequest frontSearchRequest){
        log.info("получен вопрос: {}", frontSearchRequest.getQuestion());
        log.info("получен ключ: {}", frontSearchRequest.getDocId());
        return ResponseEntity.ok().body(webService.search(frontSearchRequest, 2));
    }


}
