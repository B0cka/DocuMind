package com.B0cka.DocuMind.dto;

import lombok.Data;

import java.util.List;

@Data
public class VideoSummaryResponseDto {
    private String link;
    private String overallSummary;
    private List<SummaryItemDto> items;
}