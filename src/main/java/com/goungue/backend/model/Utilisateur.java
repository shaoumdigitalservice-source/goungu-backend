package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "utilisateurs")
@Data
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String role;

    private String telephone;
    private String ville;
    private String dateNaissance;

    @Column(length = 1000)
    private String bio;

    // Champs pour la réinitialisation de mot de passe
    private String resetToken;
    private java.time.LocalDateTime resetTokenExpiration;

    // Pour un utilisateur de rôle "jeune" : id du mentor qui lui est assigné (nullable)
    private Long mentorId;

    // Pour un utilisateur de rôle "jeune" : id du parent qui lui est associé (nullable)
    private Long parentId;
}
