package com.B0cka.DocuMind.service;

import com.B0cka.DocuMind.model.Video;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface VectoriseService {

    float[] callVectorizeServer(String str);

    List<String> findSimilarChunks(float[] questionVector, int limit, String docId);

    void processChunks(HashMap<Double, String> chunks, String docId);

    Optional<Video> searchByString(String link);
}
