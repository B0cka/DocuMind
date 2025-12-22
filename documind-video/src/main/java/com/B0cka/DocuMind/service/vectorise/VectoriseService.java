package com.B0cka.DocuMind.service.vectorise;

import com.B0cka.DocuMind.model.VideoChunk;

import java.util.HashMap;
import java.util.List;

public interface VectoriseService {

    float[] callVectorizeServer(String str);

    List<VideoChunk> findSimilarChunks(float[] questionVector, int limit, String docId);

    void saveChunks(List<VideoChunk> chunks);
}
