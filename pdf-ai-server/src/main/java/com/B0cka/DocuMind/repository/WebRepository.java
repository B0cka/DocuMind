package com.B0cka.DocuMind.repository;

import com.B0cka.DocuMind.models.Vectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebRepository extends JpaRepository<Vectors, Long> {
}
