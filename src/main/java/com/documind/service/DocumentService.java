package com.documind.service;

import com.documind.model.Document;
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
    private final OllamaEmbeddingService embeddingService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentEmbeddingRepository embeddingRepository,
                           OllamaEmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingService = embeddingService;
    }

    // 🔥 MAIN METHOD
    public void saveDocumentMetadata(MultipartFile file, String username) {

        System.out.println("🔥 Upload started");

        try {

            String fileName = file.getOriginalFilename();

            // ✅ Validate file type
            if (!fileName.endsWith(".pdf") && !fileName.endsWith(".docx")) {
                throw new RuntimeException("Only PDF and DOCX supported");
            }

            // 1️⃣ Save metadata
            Document doc = new Document();
            doc.setFileName(fileName);
            doc.setFileSize(file.getSize());
            doc.setUserUsername(username);
            doc.setUploadedAt(LocalDateTime.now());

            documentRepository.save(doc);

            System.out.println("✅ Document saved ID: " + doc.getId());

            // 2️⃣ Extract text (PDF + DOCX)
            String text = extractText(file);

            if (text == null || text.isEmpty()) {
                System.out.println("❌ No text found in document");
                return;
            }

            System.out.println("📄 Text length: " + text.length());

            // 3️⃣ Split text
            List<String> chunks = splitText(text);

            System.out.println("🔹 Total chunks: " + chunks.size());

            // 4️⃣ Generate embeddings + SAVE
            for (String chunk : chunks) {

                try {
                    var embedding = embeddingService.embed(chunk);

                    if (embedding == null || embedding.vector() == null) {
                        System.out.println("❌ Embedding null");
                        continue;
                    }

                    float[] vector = embedding.vector();

                    System.out.println("VECTOR SIZE: " + vector.length);

                    String vectorString = convertToVectorString(vector);

                    System.out.println("Saving embedding...");

                    // 🔥 FIXED INSERT
                    embeddingRepository.insertEmbedding(
                            chunk,
                            doc.getId(),
                            vectorString
                    );

                } catch (Exception e) {
                    System.out.println("❌ Embedding failed");
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.out.println("❌ FULL ERROR:");
            e.printStackTrace();
        }
    }

    // 🔥 TEXT EXTRACTION (PDF + DOCX)
    private String extractText(MultipartFile file) {

        try {
            String fileName = file.getOriginalFilename();

            // 📄 PDF
            if (fileName.endsWith(".pdf")) {
                PDDocument pdf = PDDocument.load(file.getInputStream());
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(pdf);
                pdf.close();
                return text;
            }

            // 📝 DOCX
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

    // 🔹 TEXT SPLIT
    private List<String> splitText(String text) {

        List<String> chunks = new ArrayList<>();
        int size = 500;

        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(text.length(), i + size)));
        }

        return chunks;
    }

    // 🔥 VECTOR FORMAT
    private String convertToVectorString(float[] vector) {

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);

            if (i < vector.length - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    // 📂 DOCUMENT HISTORY
    public List<String> getDocumentHistory(String username) {

        List<Document> docs = documentRepository.findByUserUsername(username);

        List<String> fileNames = new ArrayList<>();

        for (Document doc : docs) {
            fileNames.add(doc.getFileName());
        }

        return fileNames;
    }
}