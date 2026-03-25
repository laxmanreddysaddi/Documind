package com.documind.model;

import jakarta.persistence.*;

@Entity
@Table(name = "document_embeddings")
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 actual text chunk
    @Column(columnDefinition = "TEXT")
    private String chunkText;

    // 🔹 embedding vector stored as string
    @Column(columnDefinition = "TEXT")
    private String embedding;

    // 🔹 link to document
    private Long documentId;

    // ✅ GETTERS & SETTERS

    public Long getId() {
        return id;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
}