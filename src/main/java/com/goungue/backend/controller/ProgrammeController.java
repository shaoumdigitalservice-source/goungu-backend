package com.goungue.backend.controller;

import com.goungue.backend.dto.ProgrammeRequestDTO;
import com.goungue.backend.model.Programme;
import com.goungue.backend.repository.ProgrammeRepository;
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

    // GET /api/programmes -> liste publique (actifs uniquement, triés)
    @GetMapping
    public List<Programme> listeActifs() {
        return programmeRepository.findByActifTrueOrderByOrdreAffichageAsc();
    }

    // GET /api/programmes/admin -> liste complète pour l'admin
    @GetMapping("/admin")
    public List<Programme> listeComplete() {
        return programmeRepository.findAll();
    }

    // GET /api/programmes/slug/{slug} -> détail par slug (pour les pages publiques)
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Programme> parSlug(@PathVariable String slug) {
        return programmeRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/programmes -> créer (admin)
    @PostMapping
    public ResponseEntity<Programme> creer(@Valid @RequestBody ProgrammeRequestDTO dto) {
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

    // PUT /api/programmes/{id} -> modifier (admin)
    @PutMapping("/{id}")
    public ResponseEntity<Programme> modifier(@PathVariable Long id, @Valid @RequestBody ProgrammeRequestDTO dto) {
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

    // DELETE /api/programmes/{id} -> supprimer (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (!programmeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        programmeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}