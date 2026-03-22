package com.documind.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkService {
    private static final Logger log =
            LoggerFactory.getLogger(TextChunkService.class);
    public List<String> splitIntoChunks(String text, int chunkSize) {

        List<String> chunks = new ArrayList<>();
        log.info("Splitting text into chunks");

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));

            start = end;
        }
        log.info("Total chunks created: {}", chunks.size());

        return chunks;
    }
}
