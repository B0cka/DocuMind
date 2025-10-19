package com.B0cka.DocuMind.service;

import com.B0cka.DocuMind.dto.RequestDto;

import java.util.HashMap;

public interface VideoService {

    HashMap<Double, String> transformVideo(RequestDto requestDto);

}
