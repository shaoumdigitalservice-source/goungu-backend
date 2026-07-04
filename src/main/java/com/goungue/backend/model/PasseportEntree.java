package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "passeport_entrees")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasseportEntree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jeune_id", nullable = false)
    private Long jeuneId;

    @Column(nullable = false)
    private String type; // "COMPETENCE" ou "REALISATION"

    @Column(nullable = false)
    private String titre;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
