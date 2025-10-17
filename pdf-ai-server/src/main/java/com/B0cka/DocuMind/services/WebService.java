package com.B0cka.DocuMind.services;

import com.B0cka.DocuMind.dto.FrontRequest;
import com.B0cka.DocuMind.dto.FrontSearchRequest;

public interface WebService {

    void loadPDF(FrontRequest request, int group);

    String search(FrontSearchRequest frontSearchRequest, int limit);

    String searchForAbstract(FrontSearchRequest frontSearchRequest, int limit);
}
