package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    // 🔥 INSERT with CAST (CRITICAL FIX)
    @Query(value = """
        INSERT INTO document_embeddings (chunk_text, document_id, embedding)
        VALUES (:chunk, :docId, CAST(:embedding AS vector))
    """, nativeQuery = true)
    void insertEmbedding(
            @Param("chunk") String chunk,
            @Param("docId") Long docId,
            @Param("embedding") String embedding
    );

    // 🔥 VECTOR SEARCH
    @Query(value = """
        SELECT chunk_text
        FROM document_embeddings
        ORDER BY embedding <-> CAST(:vector AS vector)
        LIMIT 3
    """, nativeQuery = true)
    List<String> findTop3SimilarByUser(@Param("vector") String vector);
}