package com.goungue.backend.controller;

import com.goungue.backend.dto.ArticleRequestDTO;
import com.goungue.backend.model.Article;
import com.goungue.backend.repository.ArticleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class ArticleController {

    private final ArticleRepository articleRepository;

    // GET /api/articles -> liste publique (seulement les articles publiés)
    @GetMapping
    public List<Article> listePubliee() {
        return articleRepository.findByPublieTrue();
    }

    // GET /api/articles/admin -> liste complète pour l'admin (publiés + brouillons)
    @GetMapping("/admin")
    public List<Article> listeComplete() {
        return articleRepository.findAll();
    }

    // GET /api/articles/{id} -> détail d'un article
    @GetMapping("/{id}")
    public ResponseEntity<Article> detail(@PathVariable Long id) {
        return articleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/articles -> créer un article (admin)
    @PostMapping
    public ResponseEntity<Article> creer(@Valid @RequestBody ArticleRequestDTO dto) {
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
    public ResponseEntity<Article> modifier(@PathVariable Long id, @Valid @RequestBody ArticleRequestDTO dto) {
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
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (!articleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        articleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}