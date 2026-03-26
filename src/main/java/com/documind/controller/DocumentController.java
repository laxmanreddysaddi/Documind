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

    // ✅ Upload (TEMP: no auth to avoid failure)
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {

        System.out.println("🔥 Upload API called");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("❌ File is empty");
        }

        try {
            String username = "testuser"; // 🔥 TEMP FIX

            documentService.saveDocumentMetadata(file, username);

            return ResponseEntity.ok("✅ Uploaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Upload failed: " + e.getMessage());
        }
    }

    // ✅ Document history
    @GetMapping("/history")
    public ResponseEntity<?> getDocumentHistory() {

        String username = "testuser"; // 🔥 TEMP FIX

        List<Document> docs = documentService.getDocumentsByUser(username);

        return ResponseEntity.ok(docs);
    }
}