package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.RessourceRequestDTO;
import com.goungue.backend.model.Ressource;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.RessourceRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/ressources")
@RequiredArgsConstructor
public class RessourceController {

    private final RessourceRepository ressourceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    private static final String UPLOAD_DIR = "uploads";
    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of(
            ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx",
            ".jpg", ".jpeg", ".png", ".webp", ".mp4"
    );

    private ResponseEntity<?> verifierEstAdmin(String authHeader) {
        if (authHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        Utilisateur appelant = utilisateurRepository.findByEmail(email).orElse(null);
        if (appelant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"admin".equals(appelant.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux administrateurs");
        }
        return null;
    }

    // GET /api/ressources -> liste publique (actives uniquement)
    @GetMapping
    public List<Ressource> listeActives() {
        return ressourceRepository.findByActifTrueOrderByOrdreAffichageAsc();
    }

    // GET /api/ressources/admin -> liste complète (admin uniquement)
    @GetMapping("/admin")
    public ResponseEntity<?> listeComplete(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        return ResponseEntity.ok(ressourceRepository.findAll());
    }

    // POST /api/ressources/lien -> créer une ressource de type LIEN (admin uniquement)
    @PostMapping("/lien")
    public ResponseEntity<?> creerLien(@RequestHeader(value = "Authorization", required = false) String authHeader, @Valid @RequestBody RessourceRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        Ressource ressource = new Ressource();
        ressource.setTitre(dto.getTitre());
        ressource.setDescription(dto.getDescription());
        ressource.setType("LIEN");
        ressource.setUrl(dto.getUrl());
        ressource.setActif(dto.isActif());
        ressource.setOrdreAffichage(dto.getOrdreAffichage());
        ressource.setCategorie(dto.getCategorie());

        Ressource saved = ressourceRepository.save(ressource);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // POST /api/ressources/fichier -> créer une ressource de type FICHIER (admin uniquement)
    @PostMapping("/fichier")
    public ResponseEntity<?> creerFichier(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("titre") String titre,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "actif", defaultValue = "true") boolean actif,
            @RequestParam(value = "ordreAffichage", defaultValue = "0") Integer ordreAffichage,
            @RequestParam(value = "categorie", required = false) String categorie,
            @RequestParam("fichier") MultipartFile fichier
    ) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        String nomOriginal = fichier.getOriginalFilename();
        String extension = (nomOriginal != null && nomOriginal.contains("."))
                ? nomOriginal.substring(nomOriginal.lastIndexOf(".")).toLowerCase()
                : "";
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            return ResponseEntity.badRequest().body("Format de fichier non autorisé (pdf, doc(x), ppt(x), xls(x), jpg, jpeg, png, webp, mp4 uniquement)");
        }

        try {
            Path dossier = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dossier)) {
                Files.createDirectories(dossier);
            }

            String nomFichier = "ressource_" + UUID.randomUUID() + extension;

            Path chemin = dossier.resolve(nomFichier);
            Files.copy(fichier.getInputStream(), chemin);

            Ressource ressource = new Ressource();
            ressource.setTitre(titre);
            ressource.setDescription(description);
            ressource.setType("FICHIER");
            ressource.setUrl("/uploads/" + nomFichier);
            ressource.setNomFichier(nomOriginal);
            ressource.setActif(actif);
            ressource.setOrdreAffichage(ordreAffichage);
            ressource.setCategorie(categorie);

            Ressource saved = ressourceRepository.save(ressource);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload : " + e.getMessage());
        }
    }

    // PUT /api/ressources/{id} -> modifier titre/description/statut/ordre (admin uniquement)
    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id, @Valid @RequestBody RessourceRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        return ressourceRepository.findById(id)
                .map(r -> {
                    r.setTitre(dto.getTitre());
                    r.setDescription(dto.getDescription());
                    r.setActif(dto.isActif());
                    r.setOrdreAffichage(dto.getOrdreAffichage());
                    r.setCategorie(dto.getCategorie());
                    return ResponseEntity.ok(ressourceRepository.save(r));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/ressources/{id} -> supprimer (admin uniquement)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        if (!ressourceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ressourceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
