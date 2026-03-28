package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final DocumentEmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;
    private final HuggingFaceEmbeddingService embeddingService;

    public RagService(
            DocumentEmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository,
            HuggingFaceEmbeddingService embeddingService
    ) {
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
    }

    public String ask(String question, String username) {

        float THRESHOLD = 0.6f;

        try {
            float[] queryVector = embeddingService.embed(question).vector();

            List<Document> docs = documentRepository.findByUserUsername(username);

            if (docs.isEmpty()) {
                return "⚠ Upload document first.";
            }

            List<Long> docIds = docs.stream().map(Document::getId).toList();

            List<DocumentEmbedding> embeddings =
                    embeddingRepository.findByDocumentIds(docIds);

            if (embeddings.isEmpty()) {
                return "⚠ No data found.";
            }

            List<DocumentEmbedding> top = embeddings.stream()
                    .sorted((a, b) -> Float.compare(
                            cosine(queryVector, stringToVector(b.getEmbedding())),
                            cosine(queryVector, stringToVector(a.getEmbedding()))
                    ))
                    .limit(3)
                    .toList();

            StringBuilder ans = new StringBuilder("📄 Answer:\n\n");

            int count = 0;

            for (DocumentEmbedding e : top) {

                float score = cosine(
                        queryVector,
                        stringToVector(e.getEmbedding())
                );

                if (score >= THRESHOLD) {
                    ans.append("- ").append(e.getChunkText()).append("\n\n");
                    count++;
                }
            }

            if (count == 0) {
                return "⚠ Not found in document.";
            }

            return ans.toString();

        } catch (Exception e) {
            return "❌ Error";
        }
    }

    private float cosine(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        float dot = 0, na = 0, nb = 0;

        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }

        if (na == 0 || nb == 0) return 0;

        return (float)(dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    private float[] stringToVector(String s) {
        s = s.replace("[", "").replace("]", "");
        String[] parts = s.split(",");

        float[] v = new float[parts.length];

        for (int i = 0; i < parts.length; i++) {
            v[i] = Float.parseFloat(parts[i]);
        }

        return v;
    }
}