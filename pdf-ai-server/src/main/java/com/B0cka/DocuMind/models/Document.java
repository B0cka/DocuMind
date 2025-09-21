package com.B0cka.DocuMind.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    private String id;

    private String filename;
    private String originalFilename;
    private Long fileSize;
    private Integer totalChunks;
    private Instant uploadedAt;
}