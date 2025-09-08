package com.B0cka.DocuMind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {

    private String model;
    private String prompt;
    private Boolean stream;

}
