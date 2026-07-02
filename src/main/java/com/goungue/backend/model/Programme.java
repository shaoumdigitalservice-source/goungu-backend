package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "programmes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Programme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String tag; // ex: "15 jours · Lac Rose"

    @Column(name = "slug", nullable = false, unique = true)
    private String slug; // ex: "camp-lac-rose"

    @Column(length = 3000)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "ordre_affichage")
    private Integer ordreAffichage = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}