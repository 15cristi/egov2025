package com.egov2025.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Plata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String telefon;
    private String inmatriculare;
    private int durata;
    private double suma;
    private double tva;
    private double total;

    private double reducere;
    private String codReducere;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String xmlContent;
}
