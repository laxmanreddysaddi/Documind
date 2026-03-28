package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DocumentEmbeddingRepository
        extends JpaRepository<DocumentEmbedding, Long> {

    // ✅ Fetch embeddings
    @Query("SELECT d FROM DocumentEmbedding d WHERE d.documentId IN :docIds")
    List<DocumentEmbedding> findByDocumentIds(List<Long> docIds);

    // ✅ Delete embeddings safely (🔥 IMPORTANT FIX)
    @Transactional
    void deleteByDocumentId(Long documentId);
}