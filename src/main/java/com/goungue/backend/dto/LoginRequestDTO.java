package com.goungue.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String motDePasse;
}