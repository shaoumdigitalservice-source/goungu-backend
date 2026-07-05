package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.MessageContactRequestDTO;
import com.goungue.backend.model.Admin;
import com.goungue.backend.model.MessageContact;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.AdminRepository;
import com.goungue.backend.repository.MessageContactRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class MessageContactController {

    private final MessageContactRepository messageContactRepository;
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

    // POST /api/contact -> envoyer un message (public, depuis le formulaire)
    @PostMapping
    public ResponseEntity<MessageContact> creer(@Valid @RequestBody MessageContactRequestDTO dto) {
        MessageContact message = new MessageContact();
        message.setName(dto.getName());
        message.setEmail(dto.getEmail());
        message.setSubject(dto.getSubject());
        message.setMessage(dto.getMessage());

        MessageContact saved = messageContactRepository.save(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // GET /api/contact -> liste tous les messages (admin uniquement)
    @GetMapping
    public ResponseEntity<?> liste(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        return ResponseEntity.ok(messageContactRepository.findAll());
    }

    // PUT /api/contact/{id}/lu -> marquer un message comme lu (admin uniquement)
    @PutMapping("/{id}/lu")
    public ResponseEntity<?> marquerLu(@RequestHeader(value = "Authorization", required = false) String authHeader, @PathVariable Long id) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        return messageContactRepository.findById(id)
                .map(m -> {
                    m.setLu(true);
                    return ResponseEntity.ok(messageContactRepository.save(m));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
