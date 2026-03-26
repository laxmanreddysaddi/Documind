package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    // ✅ ADD THIS (IMPORTANT)
    List<DocumentEmbedding> findByDocumentIdIn(List<Long> documentIds);
}