package com.documind.controller;

import com.documind.model.Document;
import com.documind.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin("*")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // =========================
    // ✅ Upload Document
    // =========================
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {

        System.out.println("🔥 Upload API called");

        // ❌ Check empty file
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ File is empty");
        }

        // ❌ File size check (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("❌ File too large (Max 5MB)");
        }

        // ❌ File type check
        String fileName = file.getOriginalFilename();

        if (fileName == null ||
                !(fileName.toLowerCase().endsWith(".txt") ||
                  fileName.toLowerCase().endsWith(".pdf") ||
                  fileName.toLowerCase().endsWith(".docx"))) {

            return ResponseEntity.badRequest()
                    .body("❌ Only TXT, PDF, DOCX files are allowed");
        }

        try {
            // 🔥 TEMP USER (until JWT is connected)
            String username = "testuser";

            documentService.saveDocumentMetadata(file, username);

            return ResponseEntity.ok("✅ Document uploaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Upload failed: " + e.getMessage());
        }
    }

    // =========================
    // ✅ Get Document History
    // =========================
    @GetMapping("/history")
    public ResponseEntity<?> getDocumentHistory() {

        try {
            // 🔥 TEMP USER
            String username = "testuser";

            List<Document> documents =
                    documentService.getDocumentsByUser(username);

            if (documents.isEmpty()) {
                return ResponseEntity.ok("📂 No documents uploaded yet");
            }

            return ResponseEntity.ok(documents);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to fetch document history");
        }
    }
}