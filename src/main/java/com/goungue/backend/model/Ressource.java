package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ressources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ressource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String type; // "FICHIER" ou "LIEN"

    @Column(nullable = false)
    private String url;

    @Column(name = "nom_fichier")
    private String nomFichier; // nom original, uniquement pour type FICHIER

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "ordre_affichage")
    private Integer ordreAffichage = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
