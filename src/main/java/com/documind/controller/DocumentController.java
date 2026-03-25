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

        if (authentication == null) {
            return ResponseEntity.status(401).body("❌ User not authenticated");
        }

        String username = authentication.getName();

        documentService.saveDocumentMetadata(file, username);

        return ResponseEntity.ok("✅ Document uploaded successfully");
    }

    // ✅ Get Document History
    @GetMapping("/history")
    public ResponseEntity<?> getDocumentHistory(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).body("❌ User not authenticated");
        }

        List<Document> documents =
                documentService.getDocumentsByUser(authentication.getName());

        return ResponseEntity.ok(documents);
    }
}