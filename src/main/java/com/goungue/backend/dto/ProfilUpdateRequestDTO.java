package com.goungue.backend.dto;

import lombok.Data;

@Data
public class ProfilUpdateRequestDTO {
    private String prenom;
    private String nom;
    private String telephone;
    private String ville;
    private String dateNaissance;
    private String bio;
}
