package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentRepository;
import com.documind.repository.DocumentEmbeddingRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final HuggingFaceEmbeddingService embeddingService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentEmbeddingRepository embeddingRepository,
                           HuggingFaceEmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingService = embeddingService;
    }

    // 🚀 FAST UPLOAD (no blocking)
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.save(doc);

            // 🔥 BACKGROUND PROCESS
            processDocumentAsync(file, doc.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🚀 BACKGROUND PROCESSING
    @Async
    public void processDocumentAsync(MultipartFile file, Long docId) {

        System.out.println("🚀 Processing started in background");

        try {
            PDDocument pdf = PDDocument.load(file.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            pdf.close();

            List<String> chunks = splitText(text);

            // 🔥 PARALLEL PROCESSING
            chunks.parallelStream().forEach(chunk -> {
                try {
                    var embedding = embeddingService.embed(chunk);

                    DocumentEmbedding de = new DocumentEmbedding();
                    de.setChunkText(chunk);
                    de.setDocumentId(docId);
                    de.setEmbedding(convertToVectorString(embedding.vector()));

                    embeddingRepository.save(de);

                } catch (Exception e) {
                    System.out.println("❌ chunk failed");
                }
            });

            System.out.println("✅ Background processing completed");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔥 OPTIMIZED CHUNKING FOR LONG FILES
    private List<String> splitText(String text) {

        List<String> chunks = new ArrayList<>();

        int size = 1500; // bigger chunks → faster

        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(text.length(), i + size)));
        }

        return chunks;
    }

    // 🔥 VECTOR FORMAT
    private String convertToVectorString(float[] vector) {

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");

        return sb.toString();
    }

    // 📄 Document history
    public List<String> getDocumentHistory(String username) {

        List<Document> docs = documentRepository.findByUserUsername(username);

        List<String> fileNames = new ArrayList<>();

        for (Document doc : docs) {
            fileNames.add(doc.getFileName());
        }

        return fileNames;
    }
}