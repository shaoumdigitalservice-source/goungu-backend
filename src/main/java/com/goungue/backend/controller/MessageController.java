package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.model.Message;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.MessageRepository;
import com.goungue.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    private Utilisateur getUtilisateurConnecte(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    // Vérifie que les deux utilisateurs sont bien liés par une relation mentor-jeune valide
    private boolean relationValide(Utilisateur moi, Utilisateur autre) {
        if (moi == null || autre == null) return false;

        if ("mentor".equals(moi.getRole()) && "jeune".equals(autre.getRole())) {
            return autre.getMentorId() != null && autre.getMentorId().equals(moi.getId());
        }
        if ("jeune".equals(moi.getRole()) && "mentor".equals(autre.getRole())) {
            return moi.getMentorId() != null && moi.getMentorId().equals(autre.getId());
        }
        return false;
    }

    // GET /api/messages/conversation/{autreUserId} -> historique du fil avec cette personne
    @GetMapping("/conversation/{autreUserId}")
    public ResponseEntity<?> conversation(@RequestHeader("Authorization") String authHeader, @PathVariable Long autreUserId) {
        Utilisateur moi = getUtilisateurConnecte(authHeader);
        if (moi == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        Utilisateur autre = utilisateurRepository.findById(autreUserId).orElse(null);
        if (!relationValide(moi, autre)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vous n'êtes pas autorisé à voir cette conversation");
        }

        List<Message> messages = messageRepository.findConversation(moi.getId(), autreUserId);
        return ResponseEntity.ok(messages);
    }

    // POST /api/messages -> envoyer un message (body: {destinataireId, contenu})
    @PostMapping
    public ResponseEntity<?> envoyer(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> body) {
        Utilisateur moi = getUtilisateurConnecte(authHeader);
        if (moi == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        Long destinataireId = Long.valueOf(String.valueOf(body.get("destinataireId")));
        String contenu = String.valueOf(body.get("contenu"));

        if (contenu == null || contenu.isBlank()) {
            return ResponseEntity.badRequest().body("Le message ne peut pas être vide");
        }

        Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElse(null);
        if (!relationValide(moi, destinataire)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vous n'êtes pas autorisé à écrire à cette personne");
        }

        Message message = new Message();
        message.setExpediteurId(moi.getId());
        message.setDestinataireId(destinataireId);
        message.setContenu(contenu);

        Message saved = messageRepository.save(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
