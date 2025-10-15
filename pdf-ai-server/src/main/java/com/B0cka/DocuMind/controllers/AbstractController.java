package com.B0cka.DocuMind.controllers;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.services.AbstractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://127.0.0.1:5500"})
public class AbstractController {

    private final AbstractService abstractService;

    @PostMapping(value = "/abstract-load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("docId") String docId) {
        log.info("Получение файла для конспекта: {} полное имя: {}", file.getName(), file.getOriginalFilename());

        FrontRequest frontRequest = new FrontRequest(file, docId);
        abstractService.loadAbstract(frontRequest);

        return ResponseEntity.ok().body("Файл '" + file.getOriginalFilename() + "' загружен!");
    }

}
