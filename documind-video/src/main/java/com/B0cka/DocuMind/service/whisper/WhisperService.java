package com.B0cka.DocuMind.service.whisper;

import com.B0cka.DocuMind.dto.WhisperSegment;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public interface WhisperService {
    List<WhisperSegment> transcribe(File audioFile);
}
