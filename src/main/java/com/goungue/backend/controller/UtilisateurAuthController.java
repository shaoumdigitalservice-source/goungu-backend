package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.InscriptionRequestDTO;
import com.goungue.backend.dto.ProfilUpdateRequestDTO;
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

    private Utilisateur getUtilisateurConnecte(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    private Map<String, Object> versReponse(Utilisateur u, String token) {
        Map<String, Object> response = new HashMap<>();
        if (token != null) response.put("token", token);
        response.put("email", u.getEmail());
        response.put("prenom", u.getPrenom());
        response.put("nom", u.getNom());
        response.put("role", u.getRole());
        response.put("telephone", u.getTelephone());
        response.put("ville", u.getVille());
        response.put("dateNaissance", u.getDateNaissance());
        response.put("bio", u.getBio());
        return response;
    }

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
        return ResponseEntity.status(HttpStatus.CREATED).body(versReponse(utilisateur, token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UtilisateurLoginRequestDTO dto) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(dto.getEmail()).orElse(null);

        if (utilisateur == null || !passwordEncoder.matches(dto.getMotDePasse(), utilisateur.getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        String token = jwtService.genererToken(utilisateur.getEmail());
        return ResponseEntity.ok(versReponse(utilisateur, token));
    }

    @GetMapping("/moi")
    public ResponseEntity<?> profil(@RequestHeader("Authorization") String authHeader) {
        Utilisateur utilisateur = getUtilisateurConnecte(authHeader);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        return ResponseEntity.ok(versReponse(utilisateur, null));
    }

    @PutMapping("/moi")
    public ResponseEntity<?> modifierProfil(@RequestHeader("Authorization") String authHeader, @RequestBody ProfilUpdateRequestDTO dto) {
        Utilisateur utilisateur = getUtilisateurConnecte(authHeader);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }

        if (dto.getPrenom() != null) utilisateur.setPrenom(dto.getPrenom());
        if (dto.getNom() != null) utilisateur.setNom(dto.getNom());
        if (dto.getTelephone() != null) utilisateur.setTelephone(dto.getTelephone());
        if (dto.getVille() != null) utilisateur.setVille(dto.getVille());
        if (dto.getDateNaissance() != null) utilisateur.setDateNaissance(dto.getDateNaissance());
        if (dto.getBio() != null) utilisateur.setBio(dto.getBio());

        utilisateurRepository.save(utilisateur);
        return ResponseEntity.ok(versReponse(utilisateur, null));
    }
}
