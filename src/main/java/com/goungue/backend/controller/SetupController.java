package com.goungue.backend.controller;

import com.goungue.backend.model.Admin;
import com.goungue.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${setup.secret-key}")
    private String cleSetupAttendue;

    @Value("${setup.enabled}")
    private boolean setupActive;

    // Endpoint protégé par une clé secrète, et désactivable via SETUP_ENABLED
    // une fois le premier admin créé sur un environnement donné.
    @PostMapping("/create-admin")
    public ResponseEntity<String> creerAdmin(
            @RequestParam String email,
            @RequestParam String motDePasse,
            @RequestParam String cleSecrete) {

        if (!setupActive) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Non disponible");
        }

        if (!cleSecrete.equals(cleSetupAttendue)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non autorisé");
        }

        if (adminRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Un admin existe déjà avec cet email");
        }

        Admin admin = new Admin();
        admin.setEmail(email);
        admin.setMotDePasse(passwordEncoder.encode(motDePasse));
        adminRepository.save(admin);

        return ResponseEntity.ok("Admin créé avec succès : " + email);
    }
}
