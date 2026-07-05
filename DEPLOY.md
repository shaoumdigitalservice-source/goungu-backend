# Déploiement — goungue-backend sur un VPS

Ce guide part d'un VPS vierge (Ubuntu/Debian) jusqu'à l'API accessible en HTTPS sur ton domaine.

## 0. Prérequis

- Un VPS avec un accès SSH (root ou sudo).
- Un nom de domaine (ou sous-domaine, ex. `api.tondomaine.com`) dont l'enregistrement DNS **A** pointe vers l'IP publique du VPS. Vérifie que la propagation DNS est faite (`dig +short api.tondomaine.com`) avant de lancer Caddy, sinon la génération du certificat HTTPS échouera.

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

```bash
git clone <url-de-ton-depot> goungue-backend
cd goungue-backend
```

## 4. Configurer les secrets

**Jamais via Git.** Directement sur le serveur :

```bash
cp .env.example .env
nano .env
```

Remplir :
- `DB_PASSWORD` — un mot de passe fort pour MySQL
- `JWT_SECRET` — générer avec `openssl rand -base64 32`
- `SETUP_SECRET_KEY` — générer avec `openssl rand -base64 32` (différente du JWT_SECRET)
- `CORS_ALLOWED_ORIGINS` — l'URL de ton frontend en prod (ex. `https://tondomaine.com`)
- `DOMAIN` — le domaine de l'API elle-même (ex. `api.tondomaine.com`)

## 5. Lancer la stack

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Ça démarre `db` (MySQL), `app` (l'API, non exposée sur l'hôte) et `caddy` (reverse proxy, obtient et renouvelle le certificat Let's Encrypt automatiquement au premier accès).

Vérifier :
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f caddy
curl -I https://ton-domaine.com/api/programmes
```

## 6. Créer le premier compte admin

```bash
curl -X POST "https://ton-domaine.com/api/setup/create-admin" \
  --data-urlencode "email=toi@exemple.com" \
  --data-urlencode "motDePasse=un-mot-de-passe-fort" \
  --data-urlencode "cleSecrete=<la valeur de SETUP_SECRET_KEY dans ton .env>"
```

Une fois cet admin créé, désactive l'accès à `/api/setup/create-admin` (retire la règle `permitAll` correspondante dans `SecurityConfig.java` et redéploie) — il n'a plus de raison d'être exposé.

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
