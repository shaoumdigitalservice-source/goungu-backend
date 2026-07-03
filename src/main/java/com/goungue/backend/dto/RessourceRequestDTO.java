package com.goungue.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RessourceRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotBlank(message = "L'URL est obligatoire")
    private String url;

    private boolean actif = true;

    private Integer ordreAffichage = 0;

    private String categorie;
}
