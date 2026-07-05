package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.model.Cohorte;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.CohorteRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cohortes")
@RequiredArgsConstructor
public class CohorteController {

    private final CohorteRepository cohorteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    private Utilisateur getUtilisateurConnecte(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    private Map<String, Object> versReponse(Cohorte c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("formateurId", c.getFormateurId());
        map.put("nom", c.getNom());
        map.put("description", c.getDescription());
        map.put("jeuneIds", c.getJeuneIds());

        List<Map<String, Object>> membres = new ArrayList<>();
        for (Long jeuneId : c.getJeuneIds()) {
            utilisateurRepository.findById(jeuneId).ifPresent(j -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", j.getId());
                m.put("prenom", j.getPrenom());
                m.put("nom", j.getNom());
                m.put("email", j.getEmail());
                membres.add(m);
            });
        }
        map.put("membres", membres);
        return map;
    }

    // GET /api/cohortes/mes-cohortes -> le formateur connecté voit ses cohortes
    @GetMapping("/mes-cohortes")
    public ResponseEntity<?> mesCohortes(@RequestHeader("Authorization") String authHeader) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"formateur".equals(formateur.getRole()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux formateurs");
        }
        List<Map<String, Object>> liste = cohorteRepository.findByFormateurId(formateur.getId()).stream()
                .map(this::versReponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(liste);
    }

    // GET /api/cohortes/jeunes-disponibles -> liste tous les jeunes (pour choisir qui ajouter)
    @GetMapping("/jeunes-disponibles")
    public ResponseEntity<?> jeunesDisponibles(@RequestHeader("Authorization") String authHeader) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"formateur".equals(formateur.getRole()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux formateurs");
        }
        List<Map<String, Object>> jeunes = utilisateurRepository.findAll().stream()
                .filter(u -> "jeune".equals(u.getRole()))
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("prenom", u.getPrenom());
                    m.put("nom", u.getNom());
                    m.put("email", u.getEmail());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(jeunes);
    }

    @PostMapping
    public ResponseEntity<?> creer(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, String> body) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"formateur".equals(formateur.getRole()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux formateurs");
        }
        String nom = body.get("nom");
        if (nom == null || nom.isBlank()) {
            return ResponseEntity.badRequest().body("Le nom est requis");
        }
        Cohorte cohorte = new Cohorte();
        cohorte.setFormateurId(formateur.getId());
        cohorte.setNom(nom);
        cohorte.setDescription(body.get("description"));
        cohorte.setCreatedAt(LocalDateTime.now());
        cohorteRepository.save(cohorte);
        return ResponseEntity.status(HttpStatus.CREATED).body(versReponse(cohorte));
    }

    // PUT /api/cohortes/{id}/membres -> ajouter/retirer un jeune (body: {"jeuneId": 1, "action": "ajouter"|"retirer"})
    @PutMapping("/{id}/membres")
    public ResponseEntity<?> gererMembre(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        Cohorte cohorte = cohorteRepository.findById(id).orElse(null);
        if (cohorte == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cohorte non trouvée");
        }
        if (!cohorte.getFormateurId().equals(formateur.getId()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cette cohorte ne vous appartient pas");
        }
        Long jeuneId;
        try {
            jeuneId = Long.parseLong(body.get("jeuneId"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("jeuneId invalide");
        }
        String action = body.get("action");
        if ("ajouter".equals(action)) {
            if (!cohorte.getJeuneIds().contains(jeuneId)) {
                cohorte.getJeuneIds().add(jeuneId);
            }
        } else if ("retirer".equals(action)) {
            cohorte.getJeuneIds().remove(jeuneId);
        } else {
            return ResponseEntity.badRequest().body("Action invalide (ajouter ou retirer)");
        }
        cohorteRepository.save(cohorte);
        return ResponseEntity.ok(versReponse(cohorte));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        Cohorte cohorte = cohorteRepository.findById(id).orElse(null);
        if (cohorte == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cohorte non trouvée");
        }
        if (!cohorte.getFormateurId().equals(formateur.getId()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cette cohorte ne vous appartient pas");
        }
        cohorteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Cohorte supprimée"));
    }
}
