package com.B0cka.DocuMind.services.chunk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChunkService {

    public List<String> chunkTxtFileByParagraphs(File txtFile) throws IOException {
        List<String> chunks = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                textBuilder.append(line).append("\n");
            }
        }

        String text = textBuilder.toString()
                .replaceAll("-\\s*\\n\\s*", "")
                .replaceAll("(?<![\\.\\?\\!])\\n", " ");

        String[] paragraphs = text.split("\\n\\s*\\n");


        int sentencesPerChunk = 7;
        int overlap = 1;
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            List<String> sentences = new ArrayList<>();
            BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.forLanguageTag("ru"));
            iterator.setText(paragraph);
            int start = iterator.first();
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                String sentence = paragraph.substring(start, end).trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
            }

            for (int i = 0; i < sentences.size(); i += sentencesPerChunk - overlap) {
                int end = Math.min(i + sentencesPerChunk, sentences.size());
                String chunk = String.join(" ", sentences.subList(i, end));
                chunks.add(chunk);
            }
        }

        return chunks;
    }
}
