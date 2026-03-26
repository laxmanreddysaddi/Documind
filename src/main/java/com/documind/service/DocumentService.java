package com.documind.service;

import com.documind.model.Document;
import com.documind.model.DocumentEmbedding;
import com.documind.repository.DocumentEmbeddingRepository;
import com.documind.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentEmbeddingRepository embeddingRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentEmbeddingRepository embeddingRepository
    ) {
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
    }

    // 🚀 MAIN METHOD
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            // 1️⃣ Save document metadata
            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.save(doc);

            // 2️⃣ Extract text (PDF / DOCX / TXT)
            String content = extractText(file);

            System.out.println("📄 Content preview: " +
                    content.substring(0, Math.min(200, content.length())));

            // 3️⃣ Split into chunks
            String[] chunks = content.split("\\. ");

            // 4️⃣ Generate embeddings
            for (String chunk : chunks) {

                if (chunk.trim().isEmpty()) continue;

                String vector = generateSimpleEmbedding(chunk);

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(vector);
                de.setDocumentId(doc.getId());

                embeddingRepository.save(de);
            }

            System.out.println("✅ Embeddings saved successfully");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed");
        }
    }

    // 🔥 TEXT EXTRACTION METHOD
    private String extractText(MultipartFile file) {

        try {
            String fileName = file.getOriginalFilename().toLowerCase();

            // ✅ TXT
            if (fileName.endsWith(".txt")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            // ✅ PDF
            else if (fileName.endsWith(".pdf")) {

                org.apache.pdfbox.pdmodel.PDDocument document =
                        org.apache.pdfbox.pdmodel.PDDocument.load(file.getInputStream());

                org.apache.pdfbox.text.PDFTextStripper stripper =
                        new org.apache.pdfbox.text.PDFTextStripper();

                String text = stripper.getText(document);
                document.close();

                return text;
            }

            // ✅ DOCX
            else if (fileName.endsWith(".docx")) {

                org.apache.poi.xwpf.usermodel.XWPFDocument doc =
                        new org.apache.poi.xwpf.usermodel.XWPFDocument(file.getInputStream());

                StringBuilder text = new StringBuilder();

                for (org.apache.poi.xwpf.usermodel.XWPFParagraph para : doc.getParagraphs()) {
                    text.append(para.getText()).append("\n");
                }

                doc.close();
                return text.toString();
            }

            else {
                throw new RuntimeException("❌ Unsupported file type");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Failed to extract text");
        }
    }

    // 🔥 SIMPLE EMBEDDING (WORKING, NO API)
    private String generateSimpleEmbedding(String text) {

        float[] vector = new float[10];

        for (int i = 0; i < text.length(); i++) {
            vector[i % 10] += text.charAt(i);
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");

        return sb.toString();
    }

    // ✅ GET USER DOCUMENTS
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }
}