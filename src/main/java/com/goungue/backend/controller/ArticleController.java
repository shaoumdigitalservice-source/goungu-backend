package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.ArticleRequestDTO;
import com.goungue.backend.model.Admin;
import com.goungue.backend.model.Article;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.AdminRepository;
import com.goungue.backend.repository.ArticleRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AdminRepository adminRepository;
    private final JwtService jwtService;

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

    // GET /api/articles -> liste publique (seulement les articles publiés)
    @GetMapping
    public List<Article> listePubliee() {
        return articleRepository.findByPublieTrue();
    }

    // GET /api/articles/admin -> liste complète pour l'admin (publiés + brouillons)
    @GetMapping("/admin")
    public ResponseEntity<?> listeComplete(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        return ResponseEntity.ok(articleRepository.findAll());
    }

    // GET /api/articles/{id} -> détail d'un article (brouillon visible par l'admin uniquement)
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        if (!article.isPublie()) {
            ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
            if (erreur != null) return erreur;
        }
        return ResponseEntity.ok(article);
    }

    // POST /api/articles -> créer un article (admin)
    @PostMapping
    public ResponseEntity<?> creer(@RequestHeader(value = "Authorization", required = false) String authHeader, @Valid @RequestBody ArticleRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        Article article = new Article();
        article.setTitre(dto.getTitre());
        article.setCategorie(dto.getCategorie());
        article.setContenu(dto.getContenu());
        article.setImageUrl(dto.getImageUrl());
        article.setTempsLecture(dto.getTempsLecture());
        article.setPublie(dto.isPublie());

        Article saved = articleRepository.save(article);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /api/articles/{id} -> modifier un article (admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id, @Valid @RequestBody ArticleRequestDTO dto) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        return articleRepository.findById(id)
                .map(article -> {
                    article.setTitre(dto.getTitre());
                    article.setCategorie(dto.getCategorie());
                    article.setContenu(dto.getContenu());
                    article.setImageUrl(dto.getImageUrl());
                    article.setTempsLecture(dto.getTempsLecture());
                    article.setPublie(dto.isPublie());
                    return ResponseEntity.ok(articleRepository.save(article));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/articles/{id} -> supprimer un article (admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        if (!articleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        articleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
