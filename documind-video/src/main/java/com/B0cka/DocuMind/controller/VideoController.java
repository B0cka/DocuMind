package com.B0cka.DocuMind.controller;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/video")
@Slf4j
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping()
    public ResponseEntity<String> transformVideo(@RequestBody RequestDto dto) {
        log.info("Получен запрос на трансформацию видео: {}", dto.getLink());
        videoService.transformVideo(dto);

        return ResponseEntity.ok().body("Видео '" + dto.getLink() + "' загружено!");
    }
}
