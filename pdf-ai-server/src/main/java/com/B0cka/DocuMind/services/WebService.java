package com.B0cka.DocuMind.services;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;
import org.springframework.web.multipart.MultipartFile;

public interface WebService {

    void loadPDF(FrontRequest request);

    String search(FrontSearchRequest frontSearchRequest);

}
