package com.B0cka.DocuMind.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Vectors {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    private float[] vector;

    @Column(columnDefinition = "TEXT")
    private String text;

}