package com.B0cka.DocuMind.reposiroty;

import com.B0cka.DocuMind.models.Vectors;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebRepository extends CrudRepository<Vectors, Long> {

    @Query("SELECT text " +
            "FROM vectors " +
            "WHERE doc_id = :docId " +
            "ORDER BY vector <=> cast(:vectorStr as vector) " +
            "LIMIT :limit")
    List<String> findSimilar(@Param("vectorStr") String vectorStr,
                             @Param("limit") int limit,
                             @Param("docId") String docId);

    @Modifying
    @Query("INSERT INTO vectors (doc_id, text, vector) VALUES (:docId, :text, cast(:vectorStr as vector))")
    void saveNative(@Param("docId") String docId,
                    @Param("text") String text,
                    @Param("vectorStr") String vectorStr);
}