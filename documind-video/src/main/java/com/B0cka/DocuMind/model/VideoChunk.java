package com.B0cka.DocuMind.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "video_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoChunk {

    public VideoChunk(Long id, String link, String text, float[] vector) {
        this.id = id;
        this.link = link;
        this.text = text;
        this.vector = vector;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] vector;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "link")
    private String link;

    @Column(name = "start_time")
    private double startTime;

    @Column(name = "end_time")
    private double endTime;
}
