package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentEmbeddingRepository
        extends JpaRepository<DocumentEmbedding, Long> {

    // ✅ ONLY THIS (IMPORTANT)
    List<DocumentEmbedding> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}