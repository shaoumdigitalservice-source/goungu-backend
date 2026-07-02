package com.goungue.backend.controller;

import com.goungue.backend.dto.MessageContactRequestDTO;
import com.goungue.backend.model.MessageContact;
import com.goungue.backend.repository.MessageContactRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class MessageContactController {

    private final MessageContactRepository messageContactRepository;

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

    // GET /api/contact -> liste tous les messages (pour l'admin)
    @GetMapping
    public List<MessageContact> liste() {
        return messageContactRepository.findAll();
    }

    // PUT /api/contact/{id}/lu -> marquer un message comme lu
    @PutMapping("/{id}/lu")
    public ResponseEntity<MessageContact> marquerLu(@PathVariable Long id) {
        return messageContactRepository.findById(id)
                .map(m -> {
                    m.setLu(true);
                    return ResponseEntity.ok(messageContactRepository.save(m));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}