package com.B0cka.DocuMind.dto;

import lombok.Data;
import java.util.List;

@Data
public class WhisperResponse {
    private String text;
    private List<WhisperSegment> segments;
}
