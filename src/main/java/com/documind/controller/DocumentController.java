package com.documind.controller;

import com.documind.service.DocumentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:3000") // 🔥 FIX CORS
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        System.out.println("🔥 Upload API called");

        String username = "anonymous";

        if (authentication != null) {
            username = authentication.getName();
        }

        documentService.saveDocumentMetadata(file, username);

        return "✅ Document uploaded successfully";
    }

    @GetMapping("/history")
    public List<String> getDocumentHistory(Authentication authentication) {

        if (authentication == null) {
            throw new RuntimeException("❌ User not authenticated");
        }

        return documentService.getDocumentHistory(authentication.getName());
    }
}