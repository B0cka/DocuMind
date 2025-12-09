package com.B0cka.DocuMind.service.whisper;

import java.io.File;
import java.util.HashMap;

public interface WhisperService {
    String transcribe(File audioFile);
}
