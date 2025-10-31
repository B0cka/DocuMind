package com.B0cka.DocuMind.service.search;

import java.util.ArrayList;
import java.util.List;

public interface SearchService {

        ArrayList<String> analyzeQuestion(String question);

        ArrayList<String> analyzeQuestionForAbstract(String string);

        String search(List<String> relevantChunks, String question);
}
