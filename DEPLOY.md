# Déploiement — goungué (backend + frontend) sur un VPS

Ce guide part d'un VPS vierge (Ubuntu/Debian) jusqu'au site complet accessible en HTTPS
sur ton domaine. Architecture mono-domaine : un seul Caddy sert le frontend à la racine
et proxifie `/api` et `/uploads` vers le backend (voir `Caddyfile`) — pas de CORS à
gérer en production puisque tout est servi depuis la même origine.

## 0. Prérequis

- Un VPS avec un accès SSH (root ou sudo).
- Un nom de domaine dont l'enregistrement DNS **A** pointe vers l'IP publique du VPS. Vérifie que la propagation DNS est faite (`dig +short tondomaine.com`) avant de lancer Caddy, sinon la génération du certificat HTTPS échouera.
- Les deux dépôts clonés en dossiers **frères** (`docker-compose.prod.yml` référence le frontend via `../goungu-your-launchpad`) :
  ```
  ~/goungue/goungu-backend/
  ~/goungue/goungu-your-launchpad/
  ```

## 1. Installer Docker sur le VPS

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# se reconnecter (ou "newgrp docker") pour que le groupe soit pris en compte
```

## 2. Pare-feu

Seuls 22 (SSH), 80 et 443 (HTTP/HTTPS) doivent être ouverts. Le port de l'app (8082) et celui de MySQL (3306) ne sont **jamais** exposés en prod (voir `docker-compose.prod.yml` — seul Caddy publie des ports).

```bash
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw enable
```

## 3. Récupérer le code sur le serveur

Cloner les deux dépôts côte à côte :

```bash
mkdir -p ~/goungue && cd ~/goungue
git clone <url-du-depot-backend> goungu-backend
git clone <url-du-depot-frontend> goungu-your-launchpad
cd goungu-backend
```

## 4. Configurer les secrets

**Jamais via Git.** Directement sur le serveur, dans `goungu-backend/` :

```bash
cp .env.example .env
nano .env
```

Remplir :
- `DB_PASSWORD` — un mot de passe fort pour MySQL
- `JWT_SECRET` — générer avec `openssl rand -base64 32`
- `SETUP_SECRET_KEY` — générer avec `openssl rand -base64 32` (différente du JWT_SECRET)
- `DOMAIN` — le domaine du site (ex. `tondomaine.com`), sert à la fois au frontend et à l'API via `/api`
- `CORS_ALLOWED_ORIGINS` — laisser la valeur par défaut ; n'a plus d'effet en production mono-domaine, seulement utile en dev local (frontend et backend sur des ports différents)

## 5. Lancer la stack

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Ça démarre `db` (MySQL), `app` (l'API), `frontend` (le build Vite servi par un Caddy interne) — aucun des trois n'est exposé sur l'hôte — et `caddy` (reverse proxy public, obtient et renouvelle le certificat Let's Encrypt automatiquement au premier accès).

Vérifier :
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f caddy
curl -I https://tondomaine.com/            # doit servir le frontend
curl -I https://tondomaine.com/api/programmes   # doit répondre depuis le backend
```

## 6. Créer le premier compte admin

```bash
curl -X POST "https://tondomaine.com/api/setup/create-admin" \
  --data-urlencode "email=toi@exemple.com" \
  --data-urlencode "motDePasse=un-mot-de-passe-fort" \
  --data-urlencode "cleSecrete=<la valeur de SETUP_SECRET_KEY dans ton .env>"
```

Une fois cet admin créé, désactive l'accès à `/api/setup/create-admin` (retire la règle `permitAll` correspondante dans `SecurityConfig.java` et redéploie) — il n'a plus de raison d'être exposé.

Ce compte (table `Admin`) peut se connecter sur `/api/auth/login` et gérer articles, programmes, images et messages de contact. Pour accéder à la console `/espace/admin` du frontend (utilisateurs, candidatures, ressources, événements), il doit en plus promouvoir un compte inscrit normalement (`/api/utilisateurs/inscription`) au rôle admin via `PUT /api/utilisateurs/{id}/role` — c'est la seule façon de faire exister le premier admin de ce second système.

## 7. Sauvegardes de la base

Aucune sauvegarde automatique n'est configurée par défaut. À minima, un cron quotidien :

```bash
# crontab -e
0 3 * * * docker exec $(docker ps -qf "name=goungue-backend-db-1") sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" goungue_db' > /home/$USER/backups/goungue_$(date +\%F).sql
```

Pense à copier ces fichiers hors du VPS de temps en temps (autre machine, stockage objet) — une sauvegarde qui vit sur le même disque que la donnée qu'elle protège ne protège pas grand-chose.

## 8. Mettre à jour l'application

```bash
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Les données (MySQL, fichiers uploadés) survivent au redéploiement — elles vivent dans des volumes Docker nommés (`db_data`, `uploads_data`), pas dans le conteneur applicatif.

## 9. Redémarrage du serveur

Tous les services ont `restart: unless-stopped` : si le VPS redémarre, Docker (démarré automatiquement au boot par défaut après l'installation via `get.docker.com`) relance les conteneurs tout seul. Rien à faire manuellement.
