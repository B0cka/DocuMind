package com.B0cka.DocuMind.controller;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.dto.SearchRequestDto;
import com.B0cka.DocuMind.service.video_serv.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/video")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://127.0.0.1:5500"})
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/load-link")
    public ResponseEntity<String> transformVideo(@RequestBody RequestDto dto) {
        log.info("Получен запрос на трансформацию видео: {}", dto.getLink());
        videoService.transformVideo(dto);

        return ResponseEntity.ok().body("Видео '" + dto.getLink() + "' загружено!");
    }

    @PostMapping("/search-video")
    public ResponseEntity<String> searchFromVideo(@RequestBody SearchRequestDto dto){
        log.info("Получен запрос на трансформацию видео: {}", dto.getLink());

        return ResponseEntity.ok().body(videoService.searchVideo(dto));

    }
}
