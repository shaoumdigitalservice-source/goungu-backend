package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions_formation")
@Data
public class SessionFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long formateurId;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String statut;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
