package com.documind.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<Object, Long> {

    // TEMP FIX: Return simple text (no vector query)
    default List<String> findTop3SimilarByUser(String vector) {
        return List.of(
                "Sample document chunk 1",
                "Sample document chunk 2",
                "Sample document chunk 3"
        );
    }
}