package com.B0cka.DocuMind.service.video_serv;

import com.B0cka.DocuMind.dto.RequestDto;
import com.B0cka.DocuMind.dto.SearchRequestDto;


public interface VideoService {

    void transformVideo(RequestDto requestDto);

    String searchVideo(SearchRequestDto dto);
}
