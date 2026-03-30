package com.documind.controller;

import com.documind.model.Document;
import com.documind.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // =========================
    // ✅ UPLOAD
    // =========================
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam MultipartFile file,
            @RequestParam String username
    ) {
        documentService.saveDocumentMetadata(file, username);
        return ResponseEntity.ok("✅ Uploaded");
    }

    // =========================
    // 📂 HISTORY
    // =========================
    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam String username) {

        List<Document> docs =
                documentService.getDocumentsByUser(username);

        return ResponseEntity.ok(docs);
    }

    // =========================
    // 🗑 DELETE
    // =========================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {

        documentService.deleteDocument(id);
        return ResponseEntity.ok("✅ Deleted");
    }

    // =========================
    // 🧹 CLEAR ALL
    // =========================
    @DeleteMapping("/clear")
    public ResponseEntity<?> clear() {

        documentService.clearAll();
        return ResponseEntity.ok("✅ Cleared");
    }

    // =========================
    // 🛠 DEBUG
    // =========================
    @GetMapping("/debug")
    public ResponseEntity<?> debug() {
        return ResponseEntity.ok(documentService.debugData());
    }
}