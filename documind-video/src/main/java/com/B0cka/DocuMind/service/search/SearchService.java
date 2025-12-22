package com.B0cka.DocuMind.service.search;

import com.B0cka.DocuMind.model.VideoChunk;

import java.util.ArrayList;
import java.util.List;

public interface SearchService {

        String search(List<VideoChunk> relevantChunks, String question);
}
