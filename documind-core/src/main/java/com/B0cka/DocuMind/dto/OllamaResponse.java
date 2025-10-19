package com.B0cka.DocuMind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaResponse {
    private String model;
    private LocalDateTime created_at;
    private String response;
    private boolean done;
    private String done_reason;
    private List<Integer> context;
}
