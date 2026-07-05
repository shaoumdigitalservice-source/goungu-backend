package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.LoginRequestDTO;
import com.goungue.backend.model.Admin;
import com.goungue.backend.repository.AdminRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO dto) {
        Admin admin = adminRepository.findByEmail(dto.getEmail()).orElse(null);

        if (admin == null || !passwordEncoder.matches(dto.getMotDePasse(), admin.getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        String token = jwtService.genererToken(admin.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", admin.getEmail());
        return ResponseEntity.ok(response);
    }
}
