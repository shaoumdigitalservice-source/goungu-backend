package com.goungue.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InscriptionRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "8 caractères minimum")
    private String motDePasse;

    @NotBlank
    private String prenom;

    @NotBlank
    private String nom;

    @NotBlank
    private String role;
}
