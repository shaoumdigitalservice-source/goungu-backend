package com.goungue.backend.controller;

import com.goungue.backend.model.Admin;
import com.goungue.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    // Endpoint protégé par une clé secrète - à supprimer après usage
    @PostMapping("/create-admin")
    public String creerAdmin(
            @RequestParam String email,
            @RequestParam String motDePasse,
            @RequestParam String cleSecrete) {

        if (!cleSecrete.equals(cleSetupAttendue)) {
            return "Non autorisé";
        }

        if (adminRepository.findByEmail(email).isPresent()) {
            return "Un admin existe déjà avec cet email";
        }

        Admin admin = new Admin();
        admin.setEmail(email);
        admin.setMotDePasse(passwordEncoder.encode(motDePasse));
        adminRepository.save(admin);

        return "Admin créé avec succès : " + email;
    }
}
