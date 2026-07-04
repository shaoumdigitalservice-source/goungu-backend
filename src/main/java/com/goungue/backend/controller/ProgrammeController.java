package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.ProgrammeRequestDTO;
import com.goungue.backend.model.Admin;
import com.goungue.backend.model.Programme;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.AdminRepository;
import com.goungue.backend.repository.ProgrammeRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programmes")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class ProgrammeController {

    private final ProgrammeRepository programmeRepository;
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

    // GET /api/programmes -> liste publique (actifs uniquement, triés)
    @GetMapping
    public List<Programme> listeActifs() {
        return programmeRepository.findByActifTrueOrderByOrdreAffichageAsc();
    }

    // GET /api/programmes/admin -> liste complète (admin uniquement)
    @GetMapping("/admin")
    public ResponseEntity<?> listeComplete(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        return ResponseEntity.ok(programmeRepository.findAll());
    }

    // GET /api/programmes/slug/{slug} -> détail par slug (public)
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Programme> parSlug(@PathVariable String slug) {
        return programmeRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/programmes -> créer (admin uniquement)
    @PostMapping
    public ResponseEntity<?> creer(@RequestHeader(value = "Authorization", required = false) String authHeader, @Valid @RequestBody ProgrammeRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        Programme programme = new Programme();
        programme.setTitre(dto.getTitre());
        programme.setTag(dto.getTag());
        programme.setSlug(dto.getSlug());
        programme.setDescription(dto.getDescription());
        programme.setImageUrl(dto.getImageUrl());
        programme.setActif(dto.isActif());
        programme.setOrdreAffichage(dto.getOrdreAffichage());

        Programme saved = programmeRepository.save(programme);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /api/programmes/{id} -> modifier (admin uniquement)
    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id, @Valid @RequestBody ProgrammeRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        return programmeRepository.findById(id)
                .map(p -> {
                    p.setTitre(dto.getTitre());
                    p.setTag(dto.getTag());
                    p.setSlug(dto.getSlug());
                    p.setDescription(dto.getDescription());
                    p.setImageUrl(dto.getImageUrl());
                    p.setActif(dto.isActif());
                    p.setOrdreAffichage(dto.getOrdreAffichage());
                    return ResponseEntity.ok(programmeRepository.save(p));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/programmes/{id} -> supprimer (admin uniquement)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        if (!programmeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        programmeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
