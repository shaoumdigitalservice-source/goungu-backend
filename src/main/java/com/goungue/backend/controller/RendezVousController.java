package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.model.RendezVous;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.RendezVousRepository;
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
@RequestMapping("/api/rendez-vous")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class RendezVousController {

    private final RendezVousRepository rendezVousRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    private Utilisateur getUtilisateurConnecte(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    private Map<String, Object> versReponse(RendezVous r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("mentorId", r.getMentorId());
        map.put("jeuneId", r.getJeuneId());
        map.put("dateHeure", r.getDateHeure());
        map.put("sujet", r.getSujet());
        map.put("notes", r.getNotes());
        map.put("statut", r.getStatut());

        utilisateurRepository.findById(r.getJeuneId()).ifPresent(j -> {
            map.put("jeunePrenom", j.getPrenom());
            map.put("jeuneNom", j.getNom());
        });
        utilisateurRepository.findById(r.getMentorId()).ifPresent(m -> {
            map.put("mentorPrenom", m.getPrenom());
            map.put("mentorNom", m.getNom());
        });

        return map;
    }

    // GET /api/rendez-vous/mentor -> tous les rendez-vous du mentor connecté
    @GetMapping("/mentor")
    public ResponseEntity<?> mesRendezVousMentor(@RequestHeader("Authorization") String authHeader) {
        Utilisateur mentor = getUtilisateurConnecte(authHeader);
        if (mentor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"mentor".equals(mentor.getRole()) && !"admin".equals(mentor.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux mentors");
        }

        List<Map<String, Object>> liste = rendezVousRepository.findByMentorId(mentor.getId()).stream()
                .sorted((a, b) -> a.getDateHeure().compareTo(b.getDateHeure()))
                .map(this::versReponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(liste);
    }

    // GET /api/rendez-vous/jeune -> tous les rendez-vous du jeune connecté
    @GetMapping("/jeune")
    public ResponseEntity<?> mesRendezVousJeune(@RequestHeader("Authorization") String authHeader) {
        Utilisateur jeune = getUtilisateurConnecte(authHeader);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        List<Map<String, Object>> liste = rendezVousRepository.findByJeuneId(jeune.getId()).stream()
                .sorted((a, b) -> a.getDateHeure().compareTo(b.getDateHeure()))
                .map(this::versReponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(liste);
    }

    // POST /api/rendez-vous -> le mentor crée un rendez-vous avec un de ses jeunes
    @PostMapping
    public ResponseEntity<?> creer(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, String> body) {
        Utilisateur mentor = getUtilisateurConnecte(authHeader);
        if (mentor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"mentor".equals(mentor.getRole()) && !"admin".equals(mentor.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux mentors");
        }

        Long jeuneId;
        try {
            jeuneId = Long.parseLong(body.get("jeuneId"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("jeuneId invalide");
        }

        Utilisateur jeune = utilisateurRepository.findById(jeuneId).orElse(null);
        if (jeune == null || !"jeune".equals(jeune.getRole())) {
            return ResponseEntity.badRequest().body("Jeune invalide");
        }
        if (!mentor.getId().equals(jeune.getMentorId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ce jeune ne vous est pas assigné");
        }

        String sujet = body.get("sujet");
        if (sujet == null || sujet.isBlank()) {
            return ResponseEntity.badRequest().body("Le sujet est requis");
        }

        LocalDateTime dateHeure;
        try {
            dateHeure = LocalDateTime.parse(body.get("dateHeure"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Date/heure invalide");
        }

        RendezVous rdv = new RendezVous();
        rdv.setMentorId(mentor.getId());
        rdv.setJeuneId(jeuneId);
        rdv.setDateHeure(dateHeure);
        rdv.setSujet(sujet);
        rdv.setNotes(body.get("notes"));
        rdv.setStatut("PLANIFIE");
        rdv.setCreatedAt(LocalDateTime.now());

        rendezVousRepository.save(rdv);

        return ResponseEntity.status(HttpStatus.CREATED).body(versReponse(rdv));
    }

    // PUT /api/rendez-vous/{id}/statut -> changer le statut (mentor propriétaire uniquement)
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changerStatut(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Utilisateur mentor = getUtilisateurConnecte(authHeader);
        if (mentor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        RendezVous rdv = rendezVousRepository.findById(id).orElse(null);
        if (rdv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rendez-vous non trouvé");
        }
        if (!rdv.getMentorId().equals(mentor.getId()) && !"admin".equals(mentor.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ce rendez-vous ne vous appartient pas");
        }

        String nouveauStatut = body.get("statut");
        List<String> statutsValides = List.of("PLANIFIE", "TERMINE", "ANNULE");
        if (nouveauStatut == null || !statutsValides.contains(nouveauStatut)) {
            return ResponseEntity.badRequest().body("Statut invalide");
        }

        rdv.setStatut(nouveauStatut);
        rendezVousRepository.save(rdv);

        return ResponseEntity.ok(versReponse(rdv));
    }

    // DELETE /api/rendez-vous/{id} -> supprimer (mentor propriétaire uniquement)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Utilisateur mentor = getUtilisateurConnecte(authHeader);
        if (mentor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        RendezVous rdv = rendezVousRepository.findById(id).orElse(null);
        if (rdv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Rendez-vous non trouvé");
        }
        if (!rdv.getMentorId().equals(mentor.getId()) && !"admin".equals(mentor.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ce rendez-vous ne vous appartient pas");
        }

        rendezVousRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Rendez-vous supprimé"));
    }
}
