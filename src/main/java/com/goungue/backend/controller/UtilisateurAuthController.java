package com.goungue.backend.controller;

import com.goungue.backend.config.JwtService;
import com.goungue.backend.dto.InscriptionRequestDTO;
import com.goungue.backend.dto.ProfilUpdateRequestDTO;
import com.goungue.backend.dto.UtilisateurLoginRequestDTO;
import com.goungue.backend.model.Utilisateur;
import com.goungue.backend.repository.UtilisateurRepository;
import com.goungue.backend.repository.RendezVousRepository;
import com.goungue.backend.model.RendezVous;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173"})
public class UtilisateurAuthController {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurAuthController.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RendezVousRepository rendezVousRepository;

    private Utilisateur getUtilisateurConnecte(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtService.extraireEmail(token);
        return utilisateurRepository.findByEmail(email).orElse(null);
    }

    private Map<String, Object> versReponse(Utilisateur u, String token) {
        Map<String, Object> response = new HashMap<>();
        if (token != null) response.put("token", token);
        response.put("id", u.getId());
        response.put("email", u.getEmail());
        response.put("prenom", u.getPrenom());
        response.put("nom", u.getNom());
        response.put("role", u.getRole());
        response.put("telephone", u.getTelephone());
        response.put("ville", u.getVille());
        response.put("dateNaissance", u.getDateNaissance());
        response.put("bio", u.getBio());
        response.put("mentorId", u.getMentorId());
        response.put("parentId", u.getParentId());
        return response;
    }

    @PostMapping("/inscription")
    public ResponseEntity<?> inscription(@Valid @RequestBody InscriptionRequestDTO dto) {
        if (utilisateurRepository.findByEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setNom(dto.getNom());
        utilisateur.setRole(dto.getRole());
        utilisateurRepository.save(utilisateur);

        String token = jwtService.genererToken(utilisateur.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(versReponse(utilisateur, token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UtilisateurLoginRequestDTO dto) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(dto.getEmail()).orElse(null);

        if (utilisateur == null || !passwordEncoder.matches(dto.getMotDePasse(), utilisateur.getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }

        String token = jwtService.genererToken(utilisateur.getEmail());
        return ResponseEntity.ok(versReponse(utilisateur, token));
    }

    @GetMapping("/moi")
    public ResponseEntity<?> profil(@RequestHeader("Authorization") String authHeader) {
        Utilisateur utilisateur = getUtilisateurConnecte(authHeader);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        return ResponseEntity.ok(versReponse(utilisateur, null));
    }

    @PutMapping("/moi")
    public ResponseEntity<?> modifierProfil(@RequestHeader("Authorization") String authHeader, @RequestBody ProfilUpdateRequestDTO dto) {
        Utilisateur utilisateur = getUtilisateurConnecte(authHeader);
        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }

        if (dto.getPrenom() != null) utilisateur.setPrenom(dto.getPrenom());
        if (dto.getNom() != null) utilisateur.setNom(dto.getNom());
        if (dto.getTelephone() != null) utilisateur.setTelephone(dto.getTelephone());
        if (dto.getVille() != null) utilisateur.setVille(dto.getVille());
        if (dto.getDateNaissance() != null) utilisateur.setDateNaissance(dto.getDateNaissance());
        if (dto.getBio() != null) utilisateur.setBio(dto.getBio());

        utilisateurRepository.save(utilisateur);
        return ResponseEntity.ok(versReponse(utilisateur, null));
    }

    // Étape 1 : demande de réinitialisation - génère un code et le loggue (mode test, pas d'email réel)
    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<?> motDePasseOublie(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);

        String messageGenerique = "Si un compte existe avec cet email, un lien de réinitialisation a été généré.";

        if (utilisateur == null) {
            return ResponseEntity.ok(Map.of("message", messageGenerique));
        }

        String code = UUID.randomUUID().toString();
        utilisateur.setResetToken(code);
        utilisateur.setResetTokenExpiration(LocalDateTime.now().plusHours(1));
        utilisateurRepository.save(utilisateur);

        String lienReset = "http://localhost:8080/reinitialiser-mot-de-passe?token=" + code;

        logger.info("=====================================================");
        logger.info("LIEN DE RÉINITIALISATION (mode test, pas d'email envoyé)");
        logger.info("Pour : {}", email);
        logger.info("Lien : {}", lienReset);
        logger.info("Valable jusqu'à : {}", utilisateur.getResetTokenExpiration());
        logger.info("=====================================================");

        return ResponseEntity.ok(Map.of("message", messageGenerique));
    }

    // Étape 2 : vérifie le code et change le mot de passe
    @PostMapping("/reinitialiser-mot-de-passe")
    public ResponseEntity<?> reinitialiserMotDePasse(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String nouveauMotDePasse = body.get("nouveauMotDePasse");

        if (token == null || nouveauMotDePasse == null || nouveauMotDePasse.length() < 6) {
            return ResponseEntity.badRequest().body("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        Utilisateur utilisateur = utilisateurRepository.findByResetToken(token).orElse(null);

        if (utilisateur == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lien de réinitialisation invalide");
        }

        if (utilisateur.getResetTokenExpiration() == null || utilisateur.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lien de réinitialisation expiré, veuillez refaire une demande");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setResetToken(null);
        utilisateur.setResetTokenExpiration(null);
        utilisateurRepository.save(utilisateur);

        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }

    // ===================== ROUTES ADMIN (gestion des utilisateurs) =====================

    private ResponseEntity<?> verifierEstAdmin(String authHeader) {
        Utilisateur appelant = getUtilisateurConnecte(authHeader);
        if (appelant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"admin".equals(appelant.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux administrateurs");
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> listerUtilisateurs(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        List<Map<String, Object>> liste = utilisateurRepository.findAll().stream()
                .map(u -> versReponse(u, null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(liste);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> changerRole(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody Map<String, String> body) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        String nouveauRole = body.get("role");
        List<String> rolesValides = List.of("jeune", "parent", "mentor", "formateur", "admin");
        if (nouveauRole == null || !rolesValides.contains(nouveauRole)) {
            return ResponseEntity.badRequest().body("Rôle invalide");
        }

        Utilisateur cible = utilisateurRepository.findById(id).orElse(null);
        if (cible == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }

        cible.setRole(nouveauRole);
        utilisateurRepository.save(cible);

        return ResponseEntity.ok(versReponse(cible, null));
    }

    // GET /api/utilisateurs/mon-parcours -> frise chronologique du jeune connecté
    @GetMapping("/mon-parcours")
    public ResponseEntity<?> monParcours(@RequestHeader("Authorization") String authHeader) {
        Utilisateur jeune = getUtilisateurConnecte(authHeader);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        List<Map<String, Object>> evenements = new java.util.ArrayList<>();

        if (jeune.getCreatedAt() != null) {
            Map<String, Object> inscription = new HashMap<>();
            inscription.put("type", "INSCRIPTION");
            inscription.put("titre", "Inscription sur la plateforme Goungué");
            inscription.put("date", jeune.getCreatedAt());
            evenements.add(inscription);
        }

        List<RendezVous> rdvsTermines = rendezVousRepository.findByJeuneId(jeune.getId()).stream()
                .filter(r -> "TERMINE".equals(r.getStatut()))
                .toList();

        for (RendezVous r : rdvsTermines) {
            Map<String, Object> etape = new HashMap<>();
            etape.put("type", "RENDEZ_VOUS");
            etape.put("titre", r.getSujet());
            etape.put("date", r.getDateHeure());
            if (r.getNotes() != null) etape.put("description", r.getNotes());
            evenements.add(etape);
        }

        evenements.sort((a, b) -> ((java.time.LocalDateTime) a.get("date"))
                .compareTo((java.time.LocalDateTime) b.get("date")));

        return ResponseEntity.ok(evenements);
    }

    @GetMapping("/stats-roles")
    public ResponseEntity<?> statsParRole(@RequestHeader("Authorization") String authHeader) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        Map<String, Long> stats = utilisateurRepository.findAll().stream()
                .collect(Collectors.groupingBy(Utilisateur::getRole, Collectors.counting()));

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimerUtilisateur(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        Utilisateur appelant = getUtilisateurConnecte(authHeader);
        if (appelant.getId().equals(id)) {
            return ResponseEntity.badRequest().body("Vous ne pouvez pas supprimer votre propre compte");
        }

        if (!utilisateurRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }

        utilisateurRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }

    // ===================== MENTOR (lien jeune <-> mentor) =====================

    // PUT /api/utilisateurs/{id}/mentor -> assigne un mentor à un jeune (admin uniquement)
    @PutMapping("/{id}/mentor")
    public ResponseEntity<?> assignerMentor(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody Map<String, Long> body) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;

        Long mentorId = body.get("mentorId");

        Utilisateur jeune = utilisateurRepository.findById(id).orElse(null);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        if (!"jeune".equals(jeune.getRole())) {
            return ResponseEntity.badRequest().body("Cet utilisateur n'a pas le rôle jeune");
        }

        if (mentorId != null) {
            Utilisateur mentor = utilisateurRepository.findById(mentorId).orElse(null);
            if (mentor == null || !"mentor".equals(mentor.getRole())) {
                return ResponseEntity.badRequest().body("Mentor invalide");
            }
        }

        jeune.setMentorId(mentorId);
        utilisateurRepository.save(jeune);

        return ResponseEntity.ok(versReponse(jeune, null));
    }


    // ===================== PARENT (lien jeune <-> parent) =====================
    // PUT /api/utilisateurs/{id}/parent -> associe un parent à un jeune (admin uniquement)
    @PutMapping("/{id}/parent")
    public ResponseEntity<?> assignerParent(@RequestHeader("Authorization") String authHeader, @PathVariable Long id, @RequestBody Map<String, Long> body) {
        ResponseEntity<?> erreur = verifierEstAdmin(authHeader);
        if (erreur != null) return erreur;
        Long parentId = body.get("parentId");
        Utilisateur jeune = utilisateurRepository.findById(id).orElse(null);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        if (!"jeune".equals(jeune.getRole())) {
            return ResponseEntity.badRequest().body("Cet utilisateur n'a pas le rôle jeune");
        }
        if (parentId != null) {
            Utilisateur parent = utilisateurRepository.findById(parentId).orElse(null);
            if (parent == null || !"parent".equals(parent.getRole())) {
                return ResponseEntity.badRequest().body("Parent invalide");
            }
        }
        jeune.setParentId(parentId);
        utilisateurRepository.save(jeune);
        return ResponseEntity.ok(versReponse(jeune, null));
    }

    // GET /api/utilisateurs/mon-enfant -> le parent connecté récupère les infos de son/ses jeune(s)
    @GetMapping("/mon-enfant")
    public ResponseEntity<?> monEnfant(@RequestHeader("Authorization") String authHeader) {
        Utilisateur parent = getUtilisateurConnecte(authHeader);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        List<Map<String, Object>> enfants = utilisateurRepository.findAll().stream()
                .filter(u -> parent.getId().equals(u.getParentId()))
                .map(u -> versReponse(u, null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(enfants);
    }
    // GET /api/utilisateurs/mon-mentor -> le jeune connecté récupère les infos de son mentor
    @GetMapping("/mon-mentor")
    public ResponseEntity<?> monMentor(@RequestHeader("Authorization") String authHeader) {
        Utilisateur jeune = getUtilisateurConnecte(authHeader);
        if (jeune == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        if (jeune.getMentorId() == null) {
            return ResponseEntity.ok(Map.of("assigne", false));
        }

        Utilisateur mentor = utilisateurRepository.findById(jeune.getMentorId()).orElse(null);
        if (mentor == null) {
            return ResponseEntity.ok(Map.of("assigne", false));
        }

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("assigne", true);
        reponse.put("id", mentor.getId());
        reponse.put("prenom", mentor.getPrenom());
        reponse.put("nom", mentor.getNom());
        reponse.put("email", mentor.getEmail());
        reponse.put("telephone", mentor.getTelephone());
        reponse.put("bio", mentor.getBio());

        return ResponseEntity.ok(reponse);
    }

    // GET /api/utilisateurs/mes-jeunes -> le mentor connecté récupère la liste de ses jeunes assignés
    @GetMapping("/mes-jeunes")
    public ResponseEntity<?> mesJeunes(@RequestHeader("Authorization") String authHeader) {
        Utilisateur mentor = getUtilisateurConnecte(authHeader);
        if (mentor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }
        if (!"mentor".equals(mentor.getRole()) && !"admin".equals(mentor.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux mentors");
        }

        List<Map<String, Object>> jeunes = utilisateurRepository.findAll().stream()
                .filter(u -> mentor.getId().equals(u.getMentorId()))
                .map(u -> versReponse(u, null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(jeunes);
    }
}
