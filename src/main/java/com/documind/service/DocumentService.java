package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;

import jakarta.transaction.Transactional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final HuggingFaceEmbeddingService embeddingService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentEmbeddingRepository embeddingRepository,
            HuggingFaceEmbeddingService embeddingService
    ) {
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            String fileName = file.getOriginalFilename();

            boolean exists = documentRepository
                    .existsByFileNameAndUserUsername(fileName, username);

            if (exists) return;

            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            Document savedDoc = documentRepository.save(doc);

            String content = extractText(file);
            content = cleanText(content);

            String[] chunks = content.split("\\. ");

            int MAX_CHUNKS = 20;

            List<DocumentEmbedding> list = new ArrayList<>();

            for (int i = 0; i < Math.min(chunks.length, MAX_CHUNKS); i++) {

                String chunk = cleanText(chunks[i]);
                if (chunk.isEmpty()) continue;

                float[] vector;
                try {
                    vector = embeddingService.embed(chunk).vector();
                } catch (Exception e) {
                    continue;
                }

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(convertToVectorString(vector));
                de.setDocumentId(savedDoc.getId());

                list.add(de);
            }

            embeddingRepository.saveAll(list);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed");
        }
    }

    private String extractText(MultipartFile file) throws Exception {

        String fileName = file.getOriginalFilename().toLowerCase();

        if (fileName.endsWith(".txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        else if (fileName.endsWith(".pdf")) {
            PDDocument pdf = PDDocument.load(file.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            pdf.close();
            return text;
        }

        else if (fileName.endsWith(".docx")) {
            XWPFDocument docx = new XWPFDocument(file.getInputStream());
            XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
            String text = extractor.getText();
            docx.close();
            return text;
        }

        else {
            throw new RuntimeException("Unsupported file");
        }
    }

    private String cleanText(String text) {
        return text.replaceAll("\\u0000", "")
                   .replaceAll("[^\\x00-\\x7F]", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private String convertToVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String debugData() {
    long docCount = documentRepository.count();
    long embedCount = embeddingRepository.count();

    return "📊 Documents: " + docCount +
           " | Embeddings: " + embedCount;
}
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }

    public void deleteDocument(Long docId) {

    // delete embeddings first
    embeddingRepository.deleteByDocumentId(docId);

    // delete document
    documentRepository.deleteById(docId);
}

    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }
}