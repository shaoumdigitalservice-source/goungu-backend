package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "images_site")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cle; // ex: "hero-accueil", "logo", "about-team"

    @Column(nullable = false)
    private String nomFichier; // ex: "hero-accueil_1234.jpg"

    @Column(nullable = false)
    private String url; // ex: "/uploads/hero-accueil_1234.jpg"

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}