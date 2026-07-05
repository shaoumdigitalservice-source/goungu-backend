package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.CandidatureRequestDTO;
import com.goungue.backend.model.Admin;
import com.goungue.backend.model.Candidature;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.AdminRepository;
import com.goungue.backend.repository.CandidatureRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
public class CandidatureController {

    private final CandidatureRepository candidatureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AdminRepository adminRepository;
    private final JwtService jwtService;

    // Accepte un token venant SOIT de la table Admin, SOIT d'un Utilisateur avec role = admin
    private ResponseEntity<?> verifierEstAdmin(String authHeader) {
        if (authHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);

        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin != null) {
            return null; // C'est un admin de la table Admin, accès autorisé
        }

        Utilisateur appelant = utilisateurRepository.findByEmail(email).orElse(null);
        if (appelant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"admin".equals(appelant.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux administrateurs");
        }
        return null;
    }

    // POST /api/candidatures -> créer une candidature (public, depuis le formulaire)
    @PostMapping
    public ResponseEntity<Candidature> creer(@Valid @RequestBody CandidatureRequestDTO dto) {
        Candidature candidature = new Candidature();
        candidature.setName(dto.getName());
        candidature.setEmail(dto.getEmail());
        candidature.setPhone(dto.getPhone());
        candidature.setProgramme(dto.getProgramme());
        candidature.setMotivation(dto.getMotivation());

        Candidature saved = candidatureRepository.save(candidature);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // GET /api/candidatures -> liste toutes les candidatures (admin uniquement)
    @GetMapping
    public ResponseEntity<?> liste(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        return ResponseEntity.ok(candidatureRepository.findAll());
    }

    // GET /api/candidatures/{id} -> détail d'une candidature (admin uniquement)
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        return candidatureRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/candidatures/{id}/statut -> changer le statut (admin uniquement)
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changerStatut(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id, @RequestParam String statut) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        List<String> statutsValides = List.of("EN_ATTENTE", "ACCEPTEE", "REFUSEE");
        if (!statutsValides.contains(statut)) {
            return ResponseEntity.badRequest().body("Statut invalide");
        }

        return candidatureRepository.findById(id)
                .map(c -> {
                    c.setStatut(statut);
                    return ResponseEntity.ok(candidatureRepository.save(c));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
