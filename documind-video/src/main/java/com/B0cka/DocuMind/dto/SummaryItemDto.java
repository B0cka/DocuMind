package com.B0cka.DocuMind.dto;

import lombok.Data;

import java.util.List;

@Data
public class SummaryItemDto {
    private double startTime;
    private double endTime;
    private String title;
    private List<String> bullets;
}