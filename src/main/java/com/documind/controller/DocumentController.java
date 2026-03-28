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
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username
    ) {

        System.out.println("🔥 Upload API called");
        System.out.println("👤 Username: " + username);

        // ❌ Validate username
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Username is required");
        }

        // ❌ Empty file check
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ File is empty");
        }

        // ❌ File size check (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("❌ File too large (Max 5MB)");
        }

        // ❌ File type validation
        String fileName = file.getOriginalFilename();

        if (fileName == null ||
                !(fileName.toLowerCase().endsWith(".txt") ||
                  fileName.toLowerCase().endsWith(".pdf") ||
                  fileName.toLowerCase().endsWith(".docx"))) {

            return ResponseEntity.badRequest()
                    .body("❌ Only TXT, PDF, DOCX files are allowed");
        }

        try {

            // ✅ Check duplicate file
            boolean exists = documentService.isFileAlreadyExists(fileName, username);

            if (exists) {
                return ResponseEntity.ok("⚠ File already uploaded");
            }

            // ✅ Save document + embeddings
            documentService.saveDocumentMetadata(file, username);

            return ResponseEntity.ok("✅ Document uploaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Upload failed: " + e.getMessage());
        }
    }
    @DeleteMapping("/clear")
public String clearAll() {
    documentService.clearAll();
    return "All data cleared";
}
   @GetMapping("/debug")
public ResponseEntity<?> debug() {
    return ResponseEntity.ok(documentService.debugData());
}

    // =========================
    // ✅ Get Document History
    // =========================
    @GetMapping("/history")
    public ResponseEntity<?> getDocumentHistory(
            @RequestParam("username") String username
    ) {

        System.out.println("📂 Fetching documents for: " + username);

        // ❌ Validate username
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Username is required");
        }

        try {

            List<Document> documents =
                    documentService.getDocumentsByUser(username);

            return ResponseEntity.ok(documents);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to fetch document history");
        }
    }
}