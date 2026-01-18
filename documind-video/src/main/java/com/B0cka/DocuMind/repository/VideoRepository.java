package com.B0cka.DocuMind.repository;

import com.B0cka.DocuMind.model.VideoChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<VideoChunk, Long> {

    @Query(value = "SELECT v.text, v.start_time, v.end_time " +
            "FROM video_chunks v " +
            "WHERE v.link = :link " +
            "ORDER BY v.vector <=> CAST(:vector AS vector) " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarChunksNative(@Param("vector") String vectorStr,
                                           @Param("limit") int limit,
                                           @Param("link") String link);

    List<VideoChunk> findByLinkOrderByStartTimeAsc(String link);
}