package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    // ✅ FIXED QUERY (IMPORTANT)
    @Query("SELECT de FROM DocumentEmbedding de WHERE de.documentId IN :docIds")
    List<DocumentEmbedding> findByDocumentIds(List<Long> docIds);
}