package com.egov2025.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class Cupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cod;
    private double procent;
    private boolean activ;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
