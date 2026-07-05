package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.model.SessionFormation;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.SessionFormationRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions-formation")
@RequiredArgsConstructor
public class SessionFormationController {

    private final SessionFormationRepository sessionFormationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    private Utilisateur getUtilisateurConnecte(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    private Map<String, Object> versReponse(SessionFormation s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("formateurId", s.getFormateurId());
        map.put("titre", s.getTitre());
        map.put("dateHeure", s.getDateHeure());
        map.put("description", s.getDescription());
        map.put("statut", s.getStatut());
        return map;
    }

    // GET /api/sessions-formation/mes-sessions -> le formateur connecté voit ses sessions
    @GetMapping("/mes-sessions")
    public ResponseEntity<?> mesSessions(@RequestHeader("Authorization") String authHeader) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"formateur".equals(formateur.getRole()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux formateurs");
        }
        List<Map<String, Object>> liste = sessionFormationRepository.findByFormateurId(formateur.getId()).stream()
                .sorted((a, b) -> a.getDateHeure().compareTo(b.getDateHeure()))
                .map(this::versReponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(liste);
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

        String titre = body.get("titre");
        if (titre == null || titre.isBlank()) {
            return ResponseEntity.badRequest().body("Le titre est requis");
        }

        LocalDateTime dateHeure;
        try {
            dateHeure = LocalDateTime.parse(body.get("dateHeure"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Date/heure invalide");
        }

        SessionFormation session = new SessionFormation();
        session.setFormateurId(formateur.getId());
        session.setTitre(titre);
        session.setDateHeure(dateHeure);
        session.setDescription(body.get("description"));
        session.setStatut("PLANIFIE");
        session.setCreatedAt(LocalDateTime.now());
        sessionFormationRepository.save(session);

        return ResponseEntity.status(HttpStatus.CREATED).body(versReponse(session));
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changerStatut(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        SessionFormation session = sessionFormationRepository.findById(id).orElse(null);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session non trouvée");
        }
        if (!session.getFormateurId().equals(formateur.getId()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cette session ne vous appartient pas");
        }
        String nouveauStatut = body.get("statut");
        List<String> statutsValides = List.of("PLANIFIE", "TERMINE", "ANNULE");
        if (nouveauStatut == null || !statutsValides.contains(nouveauStatut)) {
            return ResponseEntity.badRequest().body("Statut invalide");
        }
        session.setStatut(nouveauStatut);
        sessionFormationRepository.save(session);
        return ResponseEntity.ok(versReponse(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Utilisateur formateur = getUtilisateurConnecte(authHeader);
        if (formateur == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        SessionFormation session = sessionFormationRepository.findById(id).orElse(null);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session non trouvée");
        }
        if (!session.getFormateurId().equals(formateur.getId()) && !"admin".equals(formateur.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cette session ne vous appartient pas");
        }
        sessionFormationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Session supprimée"));
    }
}
