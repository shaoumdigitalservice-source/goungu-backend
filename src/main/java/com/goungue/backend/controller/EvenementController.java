package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.EvenementRequestDTO;
import com.goungue.backend.model.Evenement;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.model.Admin;
import com.goungue.backend.repository.AdminRepository;
import com.goungue.backend.repository.EvenementRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evenements")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class EvenementController {

    private final EvenementRepository evenementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AdminRepository adminRepository;
    private final JwtService jwtService;

    private ResponseEntity<?> verifierEstAdmin(String authHeader) {
        if (authHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);

        Admin admin = adminRepository.findByEmail(email).orElse(null);
        if (admin != null) {
            return null;
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

    // GET /api/evenements -> liste publique (actifs uniquement, triés par date)
    @GetMapping
    public List<Evenement> listeActifs() {
        return evenementRepository.findByActifTrueOrderByDateEvenementAsc();
    }

    // GET /api/evenements/admin -> liste complète (admin uniquement)
    @GetMapping("/admin")
    public ResponseEntity<?> listeComplete(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        return ResponseEntity.ok(evenementRepository.findAll());
    }

    // POST /api/evenements -> créer (admin uniquement)
    @PostMapping
    public ResponseEntity<?> creer(@RequestHeader(value = "Authorization", required = false) String authHeader, @Valid @RequestBody EvenementRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        Evenement evenement = new Evenement();
        evenement.setTitre(dto.getTitre());
        evenement.setDescription(dto.getDescription());
        evenement.setDateEvenement(dto.getDateEvenement());
        evenement.setLieu(dto.getLieu());
        evenement.setActif(dto.isActif());

        Evenement saved = evenementRepository.save(evenement);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /api/evenements/{id} -> modifier (admin uniquement)
    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id, @Valid @RequestBody EvenementRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        return evenementRepository.findById(id)
                .map(e -> {
                    e.setTitre(dto.getTitre());
                    e.setDescription(dto.getDescription());
                    e.setDateEvenement(dto.getDateEvenement());
                    e.setLieu(dto.getLieu());
                    e.setActif(dto.isActif());
                    return ResponseEntity.ok(evenementRepository.save(e));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/evenements/{id} -> supprimer (admin uniquement)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        if (!evenementRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        evenementRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
