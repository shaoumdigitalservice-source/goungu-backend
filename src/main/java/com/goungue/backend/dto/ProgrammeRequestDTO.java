package com.goungue.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProgrammeRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotBlank(message = "Le tag est obligatoire")
    private String tag;

    @NotBlank(message = "Le slug est obligatoire")
    private String slug;

    private String description;

    private String imageUrl;

    private boolean actif = true;

    private Integer ordreAffichage = 0;
}