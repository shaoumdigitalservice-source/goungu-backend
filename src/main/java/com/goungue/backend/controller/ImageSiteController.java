package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.model.Admin;
import com.goungue.backend.model.ImageSite;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.AdminRepository;
import com.goungue.backend.repository.ImageSiteRepository;
import com.goungue.backend.repository.UtilisateurRepository;
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
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageSiteController {

    private final ImageSiteRepository imageSiteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AdminRepository adminRepository;
    private final JwtService jwtService;

    private static final String UPLOAD_DIR = "uploads";
    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg");
    private static final Pattern CLE_VALIDE = Pattern.compile("^[a-zA-Z0-9_-]+$");

    // Accepte un token venant SOIT de la table Admin, SOIT d'un Utilisateur avec role = admin
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

    // POST /api/images -> upload ou remplace une image pour une clé donnée (admin uniquement)
    @PostMapping
    public ResponseEntity<?> uploadImage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("cle") String cle,
            @RequestParam("fichier") MultipartFile fichier
    ) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        if (cle == null || !CLE_VALIDE.matcher(cle).matches()) {
            return ResponseEntity.badRequest().body("Clé invalide (lettres, chiffres, - et _ uniquement)");
        }

        String nomOriginal = fichier.getOriginalFilename();
        String extension = (nomOriginal != null && nomOriginal.contains("."))
                ? nomOriginal.substring(nomOriginal.lastIndexOf(".")).toLowerCase()
                : "";
        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            return ResponseEntity.badRequest().body("Format d'image non autorisé (jpg, jpeg, png, webp, gif, svg uniquement)");
        }

        try {
            // Crée le dossier uploads s'il n'existe pas
            Path dossier = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dossier)) {
                Files.createDirectories(dossier);
            }

            String nomFichier = cle + "_" + UUID.randomUUID() + extension;

            // Sauvegarde le fichier sur le disque
            Path chemin = dossier.resolve(nomFichier);
            Files.copy(fichier.getInputStream(), chemin);

            String url = "/uploads/" + nomFichier;

            // Cherche si une image existe déjà pour cette clé
            ImageSite image = imageSiteRepository.findByCle(cle).orElse(new ImageSite());
            image.setCle(cle);
            image.setNomFichier(nomFichier);
            image.setUrl(url);
            image.setUpdatedAt(java.time.LocalDateTime.now());

            ImageSite saved = imageSiteRepository.save(image);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'upload : " + e.getMessage());
        }
    }

    // GET /api/images -> liste toutes les images
    @GetMapping
    public List<ImageSite> liste() {
        return imageSiteRepository.findAll();
    }

    // GET /api/images/{cle} -> récupère l'image par sa clé
    @GetMapping("/{cle}")
    public ResponseEntity<ImageSite> parCle(@PathVariable String cle) {
        return imageSiteRepository.findByCle(cle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}