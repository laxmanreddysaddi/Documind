package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentRepository;
import com.documind.repository.DocumentEmbeddingRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final OllamaEmbeddingService embeddingService; // ✅ FIXED

    public DocumentService(DocumentRepository documentRepository,
                           DocumentEmbeddingRepository embeddingRepository,
                           OllamaEmbeddingService embeddingService) {

        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingService = embeddingService;
    }

    public void saveDocumentMetadata(MultipartFile file, String username) {

        System.out.println("🔥 Upload started");

        try {

            String fileName = file.getOriginalFilename();

            if (!fileName.endsWith(".pdf") && !fileName.endsWith(".docx")) {
                throw new RuntimeException("Only PDF and DOCX supported");
            }

            // ✅ Save metadata
            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.save(doc);

            // ✅ Extract text
            String text = extractText(file);

            if (text == null || text.isEmpty()) {
                System.out.println("❌ No text found");
                return;
            }

            // ✅ Split text
            List<String> chunks = splitText(text);

            // ✅ Generate embeddings
            for (String chunk : chunks) {

                try {
                    var embedding = embeddingService.embed(chunk);

                    float[] vector = embedding.vector();

                    String vectorString = convertToVectorString(vector);

                    DocumentEmbedding de = new DocumentEmbedding();
                    de.setChunkText(chunk);
                    de.setDocumentId(doc.getId());
                    de.setEmbedding(vectorString);

                    embeddingRepository.save(de);

                    System.out.println("✅ Saved embedding");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractText(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();

            if (fileName.endsWith(".pdf")) {
                PDDocument pdf = PDDocument.load(file.getInputStream());
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(pdf);
                pdf.close();
                return text;
            }

            else if (fileName.endsWith(".docx")) {
                XWPFDocument doc = new XWPFDocument(file.getInputStream());
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                String text = extractor.getText();
                doc.close();
                return text;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int size = 500;

        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(text.length(), i + size)));
        }

        return chunks;
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

    public List<String> getDocumentHistory(String username) {

        List<Document> docs = documentRepository.findByUserUsername(username);

        List<String> names = new ArrayList<>();

        for (Document doc : docs) {
            names.add(doc.getFileName());
        }

        return names;
    }
}