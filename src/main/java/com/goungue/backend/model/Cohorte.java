package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cohortes")
@Data
public class Cohorte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long formateurId;

    @Column(nullable = false)
    private String nom;

    @Column(length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "cohorte_membres", joinColumns = @JoinColumn(name = "cohorte_id"))
    @Column(name = "jeune_id")
    private List<Long> jeuneIds = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
