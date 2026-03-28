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
            @RequestParam String username
    ) {

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("❌ File is empty");
            }

            documentService.saveDocumentMetadata(file, username);

            return ResponseEntity.ok("✅ Uploaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Upload failed");
        }
    }

    // =========================
    // ✅ Get Document History
    // =========================
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam String username
    ) {
        try {
            List<Document> docs =
                    documentService.getDocumentsByUser(username);

            return ResponseEntity.ok(docs);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to fetch history");
        }
    }

    // =========================
    // ✅ Clear All Data
    // =========================
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAll() {
        try {
            documentService.clearAll();
            return ResponseEntity.ok("✅ All data cleared");

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Failed to clear");
        }
    }
    
    @DeleteMapping("/delete/{id}")
public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
    try {
        documentService.deleteDocument(id);
        return ResponseEntity.ok("✅ Document deleted");
    } catch (Exception e) {
        return ResponseEntity.internalServerError()
                .body("❌ Delete failed");
    }
}
    
    // =========================
    // ✅ Debug API
    // =========================
    @GetMapping("/debug")
    public ResponseEntity<?> debug() {
        try {
            return ResponseEntity.ok(documentService.debugData());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Debug failed");
        }
    }
}