package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.model.PasseportEntree;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.PasseportEntreeRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passeport")
@RequiredArgsConstructor
public class PasseportController {

    private final PasseportEntreeRepository passeportEntreeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    private Utilisateur getUtilisateurConnecte(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    // GET /api/passeport/mon-passeport -> toutes les entrées du jeune connecté
    @GetMapping("/mon-passeport")
    public ResponseEntity<?> monPasseport(@RequestHeader("Authorization") String authHeader) {
        Utilisateur jeune = getUtilisateurConnecte(authHeader);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        List<PasseportEntree> entrees = passeportEntreeRepository.findByJeuneIdOrderByCreatedAtAsc(jeune.getId());
        return ResponseEntity.ok(entrees);
    }

    // POST /api/passeport -> ajouter une entrée (body: {type, titre, description})
    @PostMapping
    public ResponseEntity<?> ajouter(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, String> body) {
        Utilisateur jeune = getUtilisateurConnecte(authHeader);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        String type = body.get("type");
        String titre = body.get("titre");
        String description = body.get("description");

        if (!"COMPETENCE".equals(type) && !"REALISATION".equals(type)) {
            return ResponseEntity.badRequest().body("Type invalide");
        }
        if (titre == null || titre.isBlank()) {
            return ResponseEntity.badRequest().body("Le titre est obligatoire");
        }

        PasseportEntree entree = new PasseportEntree();
        entree.setJeuneId(jeune.getId());
        entree.setType(type);
        entree.setTitre(titre);
        entree.setDescription(description);

        PasseportEntree saved = passeportEntreeRepository.save(entree);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // DELETE /api/passeport/{id} -> supprimer une entrée (uniquement la sienne)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Utilisateur jeune = getUtilisateurConnecte(authHeader);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        PasseportEntree entree = passeportEntreeRepository.findById(id).orElse(null);
        if (entree == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Entrée non trouvée");
        }
        if (!entree.getJeuneId().equals(jeune.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cette entrée ne vous appartient pas");
        }

        passeportEntreeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Entrée supprimée"));
    }
}
