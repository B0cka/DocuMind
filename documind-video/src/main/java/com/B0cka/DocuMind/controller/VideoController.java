package com.B0cka.DocuMind.controller;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.dto.SearchRequestDto;
import com.B0cka.DocuMind.dto.VideoSummaryResponseDto;
import com.B0cka.DocuMind.service.summary.SummaryService;
import com.B0cka.DocuMind.service.video_serv.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/video")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5050",
        "http://127.0.0.1:5050"
}, allowCredentials = "true")
public class VideoController {

    private final VideoService videoService;
    private final SummaryService summaryService;

    @PostMapping("/load-link")
    public ResponseEntity<String> transformVideo(@RequestBody RequestDto dto) {
        log.info("Получен запрос на трансформацию видео: {}", dto.getLink());
        videoService.transformVideo(dto);

        return ResponseEntity.ok().body("Видео '" + dto.getLink() + "' загружено!");
    }

    @PostMapping("/search-video")
    public ResponseEntity<String> searchFromVideo(@RequestBody SearchRequestDto dto){
        log.info("Получе�� запрос на поиск по видео: {}", dto.getLink());

        return ResponseEntity.ok().body(videoService.searchVideo(dto));
    }

    @PostMapping("/summarize")
    public ResponseEntity<VideoSummaryResponseDto> summarize(@RequestBody RequestDto dto) {
        log.info("Получен запрос на конспект видео: {}", dto.getLink());
        VideoSummaryResponseDto summary = summaryService.summarize(dto.getLink());
        return ResponseEntity.ok(summary);
    }
}