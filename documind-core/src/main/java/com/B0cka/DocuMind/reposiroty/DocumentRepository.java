package com.B0cka.DocuMind.reposiroty;

import com.B0cka.DocuMind.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, String> {
    boolean existsById(String id);
}