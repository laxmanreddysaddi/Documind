package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    @Query(value = """
    SELECT de.chunk_text
    FROM document_embeddings de
    ORDER BY de.embedding <-> (:vector)::vector
    LIMIT 3
    """, nativeQuery = true)
    List<String> findTop3SimilarByUser(@Param("vector") String vector);
}