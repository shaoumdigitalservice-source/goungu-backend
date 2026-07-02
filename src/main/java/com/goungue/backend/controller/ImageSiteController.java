package com.goungue.backend.controller;

import com.goungue.backend.model.ImageSite;
import com.goungue.backend.repository.ImageSiteRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class ImageSiteController {

    private final ImageSiteRepository imageSiteRepository;

    private static final String UPLOAD_DIR = "uploads";

    // POST /api/images -> upload ou remplace une image pour une clé donnée
    @PostMapping
    public ResponseEntity<?> uploadImage(
            @RequestParam("cle") String cle,
            @RequestParam("fichier") MultipartFile fichier
    ) {
        try {
            // Crée le dossier uploads s'il n'existe pas
            Path dossier = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dossier)) {
                Files.createDirectories(dossier);
            }

            // Génère un nom de fichier unique
            String extension = "";
            String nomOriginal = fichier.getOriginalFilename();
            if (nomOriginal != null && nomOriginal.contains(".")) {
                extension = nomOriginal.substring(nomOriginal.lastIndexOf("."));
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