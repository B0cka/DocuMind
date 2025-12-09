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
@Table(name = "video")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    public Video(Long id, String link, String text, float[] vector) {
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
    @Array(length = 768)
    private float[] vector;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "link")
    private String link;
}
