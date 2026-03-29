package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DocumentEmbeddingRepository
        extends JpaRepository<DocumentEmbedding, Long> {

    // ✅ Fetch embeddings for ONE document
    List<DocumentEmbedding> findByDocumentId(Long documentId);

    // ✅ Delete embeddings when document deleted
    @Transactional
    void deleteByDocumentId(Long documentId);
}