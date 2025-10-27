package com.B0cka.DocuMind.service;

import java.io.File;
import java.util.HashMap;

public interface WhisperService {
    HashMap<Double, String> transcribe(File audioFile);
}
