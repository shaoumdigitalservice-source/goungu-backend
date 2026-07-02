package com.goungue.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    @NotBlank(message = "La catégorie est obligatoire")
    private String categorie;

    private String contenu;

    private String imageUrl;

    private String tempsLecture;

    private boolean publie = true;
}