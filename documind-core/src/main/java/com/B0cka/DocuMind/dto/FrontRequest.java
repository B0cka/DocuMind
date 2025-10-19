package com.B0cka.DocuMind.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FrontRequest {

    private final MultipartFile file;
    private final String docId;

}
