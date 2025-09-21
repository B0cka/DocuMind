package com.B0cka.DocuMind.reposiroty;

import com.B0cka.DocuMind.models.Vectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface WebRepository extends JpaRepository<Vectors, Long> {

    @Query(value = "SELECT v.text, v.vector <=> CAST(:vector AS vector) AS similarity " +
            "FROM vectors v " +
            "WHERE v.doc_id = :docId " +
            "ORDER BY similarity ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarVectors(@Param("vector") String vector,
                                      @Param("limit") int limit,
                                      @Param("docId") String docId);
}