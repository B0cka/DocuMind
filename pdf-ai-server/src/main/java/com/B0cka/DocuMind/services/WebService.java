package com.B0cka.DocuMind.services;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;

public interface WebService {

    void loadPDF(FrontRequest request);

    String search(FrontSearchRequest frontSearchRequest);


}
