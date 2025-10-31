package com.B0cka.DocuMind.service.vectorise;

import java.util.HashMap;
import java.util.List;

public interface VectoriseService {

    float[] callVectorizeServer(String str);

    List<String> findSimilarChunks(float[] questionVector, int limit, String docId);

    void processChunks(HashMap<Double, String> chunks, String docId);

}
