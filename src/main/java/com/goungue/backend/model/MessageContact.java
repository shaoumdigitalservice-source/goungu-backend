package com.goungue.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String subject;

    @Column(length = 3000, nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean lu = false; // pour que l'admin sache s'il a déjà lu le message

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}