# Audit — goungue-backend

Audit basé sur l'état du dépôt au 2026-07-04, branche `main`, commit `530d43a`.

**Légende des sévérités :** 🔴 Critique · 🟠 À corriger · 🟡 À surveiller · 🟢 Bon

---

## 1. Vue d'ensemble

**Goungué** est une API Spring Boot pure (pas de frontend dans ce dépôt) pour une plateforme d'accompagnement de jeunes : mise en relation avec des mentors, parents et formateurs, programmes (camps, formations), événements, candidatures publiques, et un contenu vitrine (articles, ressources) géré par un back-office admin.

| | |
|---|---|
| **Stack** | Spring Boot 3.5.15 · Java 17 · MySQL · JPA/Hibernate |
| **Auth** | JWT stateless maison (`JwtService` + `JwtAuthFilter`), 24h, pas de rôles Spring Security |
| **Base de données** | `goungue_db`, `hibernate.ddl-auto=update`, pas de migrations versionnées (Flyway/Liquibase) |
| **Port** | 8082 |
| **Tests** | 1 seul (`contextLoads`) — aucune couverture fonctionnelle |
| **Chiffres** | 15 contrôleurs · 14 entités JPA · 71 endpoints (30 GET · 19 POST · 13 PUT · 9 DELETE) · 5 rôles |

---

## 2. Modèle de données

15 entités. Pas de `@ManyToOne`/`@OneToMany` JPA : toutes les relations sont des identifiants bruts (`Long xId`) résolus manuellement dans les contrôleurs.

**Utilisateurs — deux systèmes distincts.** `Admin` est une table à part (email + mot de passe haché). `Utilisateur` porte un champ `role` en `String` libre : `jeune`, `parent`, `mentor`, `formateur`, ou `admin` — un `Utilisateur` peut donc lui aussi être admin. Les deux se connectent sur des endpoints différents (`/api/auth/login` vs `/api/utilisateurs/login`) mais partagent le même `JwtService` — un token ne dit jamais de quelle table il vient, seulement l'email.

`Utilisateur` porte aussi `mentorId` et `parentId` (liens vers d'autres lignes de la même table), `resetToken`/`resetTokenExpiration` pour le mot de passe oublié, et `createdAt` pour la frise "mon parcours".

**Domaine métier :**

| Entité | Rôle |
|---|---|
| `Candidature` | formulaire public → statut `EN_ATTENTE/ACCEPTEE/REFUSEE` |
| `Programme` | vitrine publique par `slug`, ordre d'affichage |
| `Evenement` | liste publique triée par date |
| `Article` / `Ressource` | contenu éditorial, catégorie "parentalité" |
| `Cohorte` | formateur → liste de jeunes (`@ElementCollection`) |
| `SessionFormation` | agenda du formateur |
| `RendezVous` | mentor ↔ jeune, visible aussi par le parent |
| `Message` | fil mentor ↔ jeune, autorisé seulement si liés |
| `PasseportEntree` | compétences/réalisations du jeune ("Passeport Avenir") |
| `MessageContact` / `ImageSite` | formulaire de contact du site, gestion des visuels |

---

## 3. Rôles & contrôle d'accès

Il n'y a pas de rôles Spring Security. `JwtAuthFilter` authentifie n'importe quel token valide avec une liste d'autorités **vide**. Spring Security ne sait faire qu'une distinction : *authentifié* ou pas. Toute notion de rôle est réimplémentée à la main dans chaque contrôleur via un motif copié-collé (`verifierEstAdmin` / `getUtilisateurConnecte`), qui existe en **6 variantes légèrement différentes** — et qui est **absent** dans 3 contrôleurs qui en auraient besoin (voir §5).

---

## 4. Carte du système — qui peut faire quoi

```
SANS COMPTE                    COMPTE (JWT)                    SELON LE RÔLE
────────────                   ────────────                    ─────────────
Contact & candidature    →     Utilisateur                →    jeune      : mon-mentor, mon-parcours,
(formulaires publics)          (un seul champ `role`)           passeport avenir, messages avec mentor
                                │
Contenu vitrine           →    ├─ mentorId  ──────────┐   →    parent     : mon-enfant, rendez-vous
(lecture seule)                └─ parentId ────────┐  │             de l'enfant (lecture seule)
                                                      │  │
Inscription / connexion   →    relations portées     │  │   →    mentor     : mes-jeunes, rendez-vous,
                                par ID, pas de         │  │            messagerie — ses jeunes assignés
                                table de jointure      │  │
                                                        │  →    formateur  : cohortes, sessions —
                                                        │            les siennes uniquement
                                                        └  →    admin      : gestion comptes/rôles,
                                                                     contenu vitrine, candidatures
                                                                     (⚠ en principe, cf. §5)
```

---

## 5. Failles de sécurité

### 🔴 Critique — secret JWT en dur dans le code source
**`config/JwtService.java:16-18`**
La clé qui signe *tous* les tokens (admin compris) est une chaîne fixe commitée dans le dépôt. Quiconque a accès au code peut forger un JWT avec `subject = email-de-l-admin` et obtenir un accès admin instantané, sans mot de passe.
> **Correctif :** déplacer la clé en variable d'environnement, générer une clé aléatoire par environnement, faire tourner la clé maintenant qu'elle est exposée dans l'historique Git.

### 🔴 Critique — trois contrôleurs sans aucune vérification de rôle
**`ArticleController`, `ImageSiteController`, `MessageContactController`**
`POST/PUT/DELETE /api/articles/*` et `POST /api/images` n'appellent jamais `verifierEstAdmin` — Spring Security exige juste un JWT valide, donc **n'importe quel compte** (jeune, parent…) peut publier/modifier/supprimer un article ou remplacer une image du site. `GET /api/contact` et `PUT /api/contact/{id}/lu` ont le même trou : tout compte connecté peut lire les messages de contact (PII) destinés à l'admin.
> **Correctif :** reproduire le motif `verifierEstAdmin` déjà utilisé dans `RessourceController`/`ProgrammeController` — ou en faire un service partagé (§6).

### 🔴 Critique — bug de pattern dans la config Spring Security
**`config/SecurityConfig.java:62`**
`requestMatchers(GET, "/api/articles/*")` visait `/api/articles/{id}`, mais le wildcard `*` matche aussi `/api/articles/admin` (un seul segment). Résultat : la liste admin des articles, censée être réservée à l'admin, est **publique** avant même d'atteindre le contrôleur. Les brouillons non publiés sont visibles sans aucune authentification.
> **Correctif :** retirer le wildcard générique, faire correspondre `{id}` par une regex numérique (`/api/articles/{id:[0-9]+}`).

### 🟠 À corriger — clé de setup admin par défaut, committée
**`application.properties:12`, `SetupController.java`**
`setup.secret-key=change-moi-cle-super-secrete-2026` est versionnée. Si elle n'a pas été changée en production, `POST /api/setup/create-admin` permet à quiconque connaît cette valeur de créer un compte admin.
> **Correctif :** variable d'environnement uniquement, désactiver l'endpoint après le premier usage.

### 🟠 À corriger — élévation de privilège à l'inscription
**`InscriptionRequestDTO.role`, `UtilisateurAuthController.inscription`**
Le rôle est un champ libre envoyé par le client — rien n'empêche `"role": "admin"` dans le corps de `POST /api/utilisateurs/inscription`.
> **Correctif :** whitelister les rôles auto-attribuables (`jeune/parent/mentor/formateur`), exclure `admin`.

### 🟠 À corriger — uploads sans validation
**`ImageSiteController.uploadImage`, `RessourceController.creerFichier`**
Aucune limite de taille, aucune vérification d'extension/MIME avant écriture sur disque dans `uploads/`.
> **Correctif :** whitelist d'extensions/MIME, taille max via `spring.servlet.multipart.max-file-size`.

### 🟡 À surveiller — CORS et absence de rate limiting
Origines CORS figées sur `localhost`, aucune limite de tentatives sur login/inscription/mot-de-passe-oublié.
> **Correctif :** ajouter l'origine de prod, limiter le débit sur les routes d'auth.

### 🟢 Bon — ce qui est déjà solide
- **Hashage des mots de passe** : BCrypt partout, y compris table `Admin`.
- **Validation des entrées publiques** : Bean Validation (`@NotBlank`, `@Email`) sur candidature, contact, inscription.
- **Réinitialisation de mot de passe** : token à usage unique, expiration 1h, message générique anti-énumération.
- **Relations mentor↔jeune** : `relationValide` et les checks de rendez-vous vérifient l'assignation réelle avant d'autoriser une action.
- **Propriété des ressources** : cohorte, session de formation, passeport — chaque suppression/modif vérifie que l'appelant est propriétaire.

---

## 6. Dette technique

| Aspect | Constat |
|---|---|
| **Couche service absente** | 🟠 Logique métier, autorisation et persistance mélangées dans les contrôleurs — pas testable sans démarrer tout Spring. |
| **Rôles/statuts en `String` libre** | 🟠 Pas d'`enum`, pas de contrainte DB — une faute de frappe casse silencieusement les contrôles d'accès. |
| **Couverture de tests** | 🟠 Un seul test (`contextLoads`) — aucune règle d'autorisation testée. |
| **Contrat de réponse API** | 🟡 Certains endpoints renvoient l'entité JPA brute, d'autres un `Map<String,Object>` fait main. Pas de doc OpenAPI possible en l'état. |
| **Migrations de schéma** | 🟡 `ddl-auto=update` sans Flyway/Liquibase. |
| **Double système admin** | 🟡 Table `Admin` et `Utilisateur.role="admin"` coexistent, reconnues de façon incohérente selon le contrôleur. |
| **Pas de pagination** | Listes admin (`findAll()` direct) chargeront tout en mémoire dès que le volume grossit. |
| **Conventions REST & historique Git** | 🟢 Routes cohérentes, un commit = une feature complète, progression incrémentale lisible. |

---

## 7. Historique des livraisons

Lecture du `git log` — 13 commits, développement séquentiel feature par feature :

1. `b918bb0` — Premier commit, sauvegarde du backend
2. `d50655c` — Système utilisateurs (jeune/parent/mentor/formateur)
3. `6eb164d` — Champs de profil + endpoint de mise à jour
4. `786fa2d` — Retrait de `uploads/` du suivi Git
5. `a45b204` — Lien parent–jeune (assignation, mon-enfant)
6. `6667bf9` — Système de rendez-vous mentor–jeune
7. `09443b3` — Route rendez-vous côté parent
8. `dd6ba03` — Sessions de formation (agenda formateur)
9. `d1d999d` — Catégorie sur les ressources (filtre parentalité)
10. `b925a72` — Système de cohortes (formateur)
11. `7054af4` — Messagerie mentor–jeune
12. `2cbdf30` — Frise "Mon parcours"
13. `bee49c9` — Passeport Avenir (compétences/réalisations)
14. `590aa08` / `530d43a` — Correctifs : Candidature/Programme/Evenement acceptent aussi les admins de la table `Admin`

---

## 8. Verdict & recommandations priorisées

**Fonctionnellement, le projet est riche et cohérent** : le parcours jeune/mentor/parent/formateur est complet, avec de vraies vérifications de relation là où ça compte. Le socle technique (BCrypt, JWT stateless, validation des entrées publiques) est correct.

**Le risque est concentré, pas diffus** : 3 endpoints sur 71 (les écritures d'`ArticleController`, `ImageSiteController`, `MessageContactController`) et 1 clé en dur portent l'essentiel du risque de sécurité. Le reste — architecture sans couche service, rôles en `String`, absence de tests — est de la dette normale pour un projet à ce stade.

| # | Action | Effort |
|---|---|---|
| 1 | Corriger les 3 failles critiques (secret JWT, contrôleurs sans check de rôle, pattern `/api/articles/*`) | Quelques heures |
| 2 | Extraire l'authentification/autorisation dans un service partagé | Moyen |
| 3 | Passer `role` et les statuts en `enum`, brancher sur `GrantedAuthority` | Moyen |
| 4 | Trancher entre `Admin` et `Utilisateur.role=admin` — fusionner vers un seul système | Moyen |
| 5 | Introduire Flyway avant que le schéma ne grossisse | Faible |
| 6 | Tests d'intégration sur les règles d'accès (mentor↔jeune, parent↔enfant, formateur↔cohorte) | Moyen-élevé |
| 7 | Pagination sur les listes admin | Faible |
