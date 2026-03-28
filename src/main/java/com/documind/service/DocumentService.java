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

    // ====================================
    // ✅ MAIN UPLOAD METHOD (FINAL)
    // ====================================
    @Transactional
    public void saveDocumentMetadata(MultipartFile file, String username) {

        try {
            System.out.println("🔥 Processing document...");

            String fileName = file.getOriginalFilename();

            // ✅ Duplicate check
            boolean exists = documentRepository
                    .existsByFileNameAndUserUsername(fileName, username);

            if (exists) {
                System.out.println("⚠ File already exists");
                return;
            }

            // ✅ Save document
            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            Document savedDoc = documentRepository.save(doc);
            documentRepository.flush();

            System.out.println("📄 Doc ID: " + savedDoc.getId());

            // ====================================
            // ✅ EXTRACT CONTENT (SAFE)
            // ====================================
            String content = extractText(file);

            content = cleanText(content); // 🔥 IMPORTANT

            System.out.println("📄 Content length: " + content.length());

            // ====================================
            // ✅ SPLIT INTO CHUNKS
            // ====================================
            String[] chunks = content.split("\\. ");

            int MAX_CHUNKS = 20; // 🔥 Prevent Render crash

            List<DocumentEmbedding> embeddingList = new ArrayList<>();

            for (int i = 0; i < Math.min(chunks.length, MAX_CHUNKS); i++) {

                String chunk = cleanText(chunks[i]);

                if (chunk.isEmpty()) continue;

                float[] vector;

                try {
                    vector = embeddingService.embed(chunk).vector();
                } catch (Exception e) {
                    continue; // skip bad chunk
                }

                String vectorString = convertToVectorString(vector);

                DocumentEmbedding de = new DocumentEmbedding();
                de.setChunkText(chunk);
                de.setEmbedding(vectorString);
                de.setDocumentId(savedDoc.getId());

                embeddingList.add(de);
            }

            // ✅ Batch save (FAST + SAFE)
            embeddingRepository.saveAll(embeddingList);

            System.out.println("✅ Saved embeddings: " + embeddingList.size());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ File processing failed");
        }
    }

    // ====================================
    // ✅ TEXT EXTRACTION (PDF/DOCX/TXT)
    // ====================================
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
            throw new RuntimeException("Unsupported file type");
        }
    }

    // ====================================
    // ✅ CLEAN TEXT (FIX UTF-8 ERROR)
    // ====================================
    private String cleanText(String text) {
        return text
                .replaceAll("\\u0000", "") // remove null byte
                .replaceAll("[^\\x00-\\x7F]", " ") // remove weird chars
                .replaceAll("\\s+", " ") // normalize spaces
                .trim();
    }

    // ====================================
    // ✅ VECTOR → STRING
    // ====================================
    private String convertToVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // ====================================
    // ✅ OTHER METHODS
    // ====================================
    public boolean isFileAlreadyExists(String fileName, String username) {
        return documentRepository
                .existsByFileNameAndUserUsername(fileName, username);
    }

    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }

    public void clearAll() {
        embeddingRepository.deleteAll();
        documentRepository.deleteAll();
    }

    public String debugData() {
        return "Documents: " + documentRepository.count() +
               " | Embeddings: " + embeddingRepository.count();
    }
}