package com.B0cka.DocuMind.services;

import org.springframework.web.multipart.MultipartFile;

public interface WebService {

    void loadPDF(MultipartFile file);

    String search(String question);

}
