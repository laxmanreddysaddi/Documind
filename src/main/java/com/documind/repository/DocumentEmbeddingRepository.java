package com.documind.repository;

import com.documind.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    // TEMP method (no vector search yet)
    default List<String> findTop3SimilarByUser(String vector) {
        return List.of(
                "DocuMind processes documents using AI.",
                "Embeddings help find similar content.",
                "RAG improves answer quality."
        );
    }
}