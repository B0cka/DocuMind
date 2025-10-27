package com.B0cka.DocuMind.dto;

import lombok.Data;

import java.util.List;

@Data
public class WhisperSegment {
    private int id;
    private int seek;
    private double start;
    private double end;
    private String text;
    private List<Integer> tokens;
    private double temperature;
    private double avg_logprob;
    private double compression_ratio;
    private double no_speech_prob;
}