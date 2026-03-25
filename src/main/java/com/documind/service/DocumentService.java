package com.documind.service;

import com.documind.model.Document;
import com.documind.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // ✅ Save document metadata
    public void saveDocumentMetadata(MultipartFile file, String username) {

        Document doc = new Document();

        doc.setFileName(file.getOriginalFilename());
        doc.setFileSize(file.getSize());
        doc.setUserUsername(username);
        doc.setUploadedAt(LocalDateTime.now());

        documentRepository.save(doc);
    }

    // ✅ Get documents by user
    public List<Document> getDocumentsByUser(String username) {
        return documentRepository.findByUserUsername(username);
    }
}