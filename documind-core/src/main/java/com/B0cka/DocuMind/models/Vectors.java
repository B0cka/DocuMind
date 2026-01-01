package com.B0cka.DocuMind.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table(name = "vectors")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Vectors {

    @Id
    private Long id;
    private float[] vector;
    private String text;
    private String docId;

}