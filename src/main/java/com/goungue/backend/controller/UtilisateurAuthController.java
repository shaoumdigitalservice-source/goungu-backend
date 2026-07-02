package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.InscriptionRequestDTO;
import com.goungue.backend.dto.UtilisateurLoginRequestDTO;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class UtilisateurAuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/inscription")
    public ResponseEntity<?> inscription(@Valid @RequestBody InscriptionRequestDTO dto) {
        if (utilisateurRepository.findByEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setNom(dto.getNom());
        utilisateur.setRole(dto.getRole());
        utilisateurRepository.save(utilisateur);

        String token = jwtService.genererToken(utilisateur.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UtilisateurLoginRequestDTO dto) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(dto.getEmail()).orElse(null);

        if (utilisateur == null || !passwordEncoder.matches(dto.getMotDePasse(), utilisateur.getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        String token = jwtService.genererToken(utilisateur.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/moi")
    public ResponseEntity<?> profil(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("email", utilisateur.getEmail());
        response.put("prenom", utilisateur.getPrenom());
        response.put("nom", utilisateur.getNom());
        response.put("role", utilisateur.getRole());
        return ResponseEntity.ok(response);
    }
}
