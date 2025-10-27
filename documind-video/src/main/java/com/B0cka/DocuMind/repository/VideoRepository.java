package com.B0cka.DocuMind.repository;

import com.B0cka.DocuMind.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    @Query(value = "SELECT v.text, v.vector <=> CAST(:vector AS vector) AS similarity " +
            "FROM video v " +
            "WHERE v.link = :link " +
            "ORDER BY similarity ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarVectors(@Param("vector") String vector,
                                      @Param("limit") int limit,
                                      @Param("link") String link);

}
