package com.documind.controller;

import com.documind.model.Document;
import com.documind.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    // ✅ Upload Document
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        System.out.println("🔥 Upload API called");

        // ❌ Not logged in
        if (authentication == null) {
            return ResponseEntity.status(401).body("❌ User not authenticated");
        }

        // ❌ Empty file check
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ File is empty");
        }

        // ❌ File size limit (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("❌ File too large (max 5MB)");
        }

        String username = authentication.getName();

        try {
            documentService.saveDocumentMetadata(file, username);
            return ResponseEntity.ok("✅ Document uploaded and processed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to process document");
        }
    }

    // ✅ Get Document History
    @GetMapping("/history")
    public ResponseEntity<?> getDocumentHistory(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).body("❌ User not authenticated");
        }

        try {
            List<Document> documents =
                    documentService.getDocumentsByUser(authentication.getName());

            return ResponseEntity.ok(documents);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to fetch documents");
        }
    }
}