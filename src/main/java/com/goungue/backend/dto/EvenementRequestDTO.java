package com.goungue.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvenementRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "La date est obligatoire")
    private LocalDateTime dateEvenement;

    @NotBlank(message = "Le lieu est obligatoire")
    private String lieu;

    private boolean actif = true;
}
