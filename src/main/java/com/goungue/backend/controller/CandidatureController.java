package com.goungue.backend.controller;

import com.goungue.backend.dto.CandidatureRequestDTO;
import com.goungue.backend.model.Candidature;
import com.goungue.backend.repository.CandidatureRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatures")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class CandidatureController {

    private final CandidatureRepository candidatureRepository;

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

    // GET /api/candidatures -> liste toutes les candidatures (pour l'admin, pas encore sécurisé)
    @GetMapping
    public List<Candidature> liste() {
        return candidatureRepository.findAll();
    }

    // GET /api/candidatures/{id} -> détail d'une candidature
    @GetMapping("/{id}")
    public ResponseEntity<Candidature> detail(@PathVariable Long id) {
        return candidatureRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/candidatures/{id}/statut -> changer le statut (accepter/refuser)
    @PutMapping("/{id}/statut")
    public ResponseEntity<Candidature> changerStatut(@PathVariable Long id, @RequestParam String statut) {
        return candidatureRepository.findById(id)
                .map(c -> {
                    c.setStatut(statut);
                    return ResponseEntity.ok(candidatureRepository.save(c));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}