# Infrastructure VPS — guide complet (DNS → VPS → Caddy central → projets)

Ce document décrit l'architecture d'hébergement mise en place sur ce VPS, et comment
l'utiliser pour maintenir les projets existants ou en ajouter un nouveau, **sans assistance
extérieure**. Il part de zéro (achat de domaine, VPS vierge) jusqu'à un site en HTTPS.

Le VPS héberge plusieurs projets indépendants derrière **un seul Caddy central partagé**.
C'est le point le plus important à comprendre avant de toucher à quoi que ce soit :

```
Internet (443/80)
        │
        ▼
┌─────────────────────────────┐
│   Caddy central (~/infra)   │   ← seul à publier les ports 80/443 sur l'hôte
│   sites/shaolin.caddy        │
│   sites/goungu.caddy         │
│   sites/<futur-projet>.caddy │
└───────────┬─────────────────┘
            │  réseau Docker externe "edge"
    ┌───────┼────────────┬─────────────────┐
    ▼       ▼             ▼                 ▼
 shaolin  shaolin       goungu-app      goungu-frontend
-frontend -backend      (Spring Boot)   (Caddy interne, fichiers statiques)
```

Chaque projet vit dans son propre dossier / dépôt Git, avec son propre `docker-compose.yml`,
et ne connaît rien des autres — il rejoint juste le réseau partagé `edge` pour devenir
atteignable par le Caddy central.

---

## Partie A — Nom de domaine (une fois par domaine)

1. Acheter/posséder le domaine chez le registrar de son choix (ici Hostinger).
2. Dans le tableau de bord du registrar → **DNS / Zone DNS** → ajouter ou modifier
   l'enregistrement **A** :
   - **Type** : `A`
   - **Nom/Host** : `@` (racine du domaine)
   - **Valeur** : l'adresse IP publique du VPS
   - **TTL** : 3600 ou 14400 (pas besoin d'un TTL très court en usage normal)
3. Un `CNAME` `www` → `<domaine racine>` fait suivre `www.<domaine>` automatiquement.
4. Vérifier la propagation avant de continuer :
   ```bash
   dig +short <ton-domaine>
   ```
   Doit renvoyer l'IP du VPS. Ça peut prendre de quelques minutes à quelques heures.

*(Fait pour `goungueincub.com` → `89.117.49.91`, avec un CNAME `www` déjà présent.)*

---

## Partie B — Préparer le VPS (une fois par VPS)

### B.1 Accès et vérifications de base
```bash
ssh <utilisateur>@<ip-du-vps>
docker --version          # si Docker est deja installe, ne PAS relancer le script d'install
docker compose version
```

Si Docker n'est pas installé :
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# se reconnecter (ou "newgrp docker") pour que le groupe soit pris en compte
```
Vérifier ensuite que ça fonctionne sans `sudo` : `docker ps`.

### B.2 Pare-feu
Seuls SSH/HTTP/HTTPS doivent être ouverts. Rien d'autre (ni le port d'une app, ni celui
d'une base de données) ne doit jamais être exposé directement sur l'hôte.
```bash
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw enable
sudo ufw status
```

### B.3 Le réseau Docker partagé
Un seul réseau Docker, créé une bonne fois pour toutes, que tous les projets rejoindront :
```bash
docker network create edge
```
Personne ne "possède" ce réseau — chaque projet (y compris le Caddy central) le référence
en tant que réseau **externe**. Si le VPS est reconstruit un jour, cette commande est la
toute première chose à relancer.

---

## Partie C — Le Caddy central (une fois par VPS)

Dossier dédié, qui n'appartient à aucun projet applicatif — c'est important : un projet
peut être arrêté, redéployé ou supprimé sans jamais toucher au Caddy central ni couper
les autres sites.

```bash
mkdir -p ~/infra/caddy/sites
cd ~/infra/caddy

cat > docker-compose.yml << 'EOF'
services:
  caddy:
    image: caddy:2
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - ./sites:/etc/caddy/sites:ro
      - caddy_data:/data
      - caddy_config:/config
    networks:
      - edge

networks:
  edge:
    external: true
    name: edge

volumes:
  caddy_data:
  caddy_config:
EOF

cat > Caddyfile << 'EOF'
import sites/*.caddy
EOF

docker compose up -d
docker compose ps
```

`Caddyfile` ne contient qu'une ligne : il importe tous les fichiers du dossier `sites/`.
**Pour ajouter un projet, on ajoute un fichier dans `sites/` — on ne touche jamais aux
fichiers des autres projets.**

*Conseil pour la suite : mettre `~/infra` sous Git (`git init`, un commit), même sans le
pousser où que ce soit, juste pour garder un historique local des changements de config.*

---

## Partie D — Déployer un projet sur ce VPS (à répéter pour chaque projet, y compris les futurs)

Cette partie est **générique** — c'est la procédure à suivre pour n'importe quel nouveau
projet, pas seulement Goungué.

### D.1 Le projet doit exposer un service HTTP interne
Peu importe la techno (Node, Spring Boot, Next.js, fichiers statiques...), le conteneur de
l'app ne doit **pas** publier de port sur l'hôte (pas de `ports: "xxxx:xxxx"`), seulement
`expose` en interne. C'est le Caddy central qui parlera à ce conteneur par le réseau Docker,
jamais directement depuis l'extérieur.

### D.2 Rejoindre le réseau `edge` avec un alias stable
Dans le `docker-compose.yml` du projet, chaque service qui doit être atteint par Caddy
rejoint `edge` en plus de son réseau habituel, avec un **alias** explicite (nom stable,
indépendant du nom réel du conteneur) :

```yaml
services:
  monservice:
    build: .
    expose: ["3000"]
    networks:
      default:          # reseau prive du projet (ex. pour parler a sa propre DB)
      edge:
        aliases:
          - monprojet-app   # <- c'est ce nom que Caddy utilisera

networks:
  edge:
    external: true
    name: edge
```

### D.3 Lancer le projet
```bash
cd ~/<dossier-du-projet>
docker compose up -d --build
docker network inspect edge --format '{{range .Containers}}{{.Name}} {{end}}'
```
Le conteneur du projet doit apparaître dans la liste, aux côtés de tous les autres.

### D.4 Ajouter le site au Caddy central
Créer **un nouveau fichier** (jamais modifier ceux des autres projets) :
```bash
cat > ~/infra/caddy/sites/<nom-du-projet>.caddy << 'EOF'
tondomaine.com, www.tondomaine.com {
	reverse_proxy monprojet-app:3000
}
EOF

cd ~/infra/caddy
docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile
```
`caddy reload` recharge la config **sans coupure** des autres sites déjà en ligne — c'est
tout l'intérêt du Caddy central : ajouter un projet ne redémarre jamais les autres.

### D.5 Vérifier
```bash
curl -I https://tondomaine.com/
```
Premier accès un peu plus lent : Caddy obtient automatiquement le certificat Let's Encrypt
à la volée (le DNS doit déjà pointer vers ce VPS, sinon le certificat échoue).

---

## Partie E — Cas concret : comment Goungué a été branché

Ce qui suit est l'application exacte de la Partie D à ce projet précis, pour référence.

### E.1 Dépôts clonés en frères
```bash
mkdir -p ~/goungue && cd ~/goungue
git clone https://github.com/shaoumdigitalservice-source/goungu-backend.git
git clone https://github.com/shaoumdigitalservice-source/goungu-your-launchpad.git
```
Le frontend est un dépôt public (clone HTTPS direct suffit). S'il redevenait privé, il
faudrait soit une deploy key SSH (accès lecture seule limité à ce dépôt), soit un token
d'accès personnel.

### E.2 Secrets (`goungu-backend/.env`)
```bash
cd ~/goungue/goungu-backend
cp .env.example .env

JWT_SECRET=$(openssl rand -base64 32)
SETUP_SECRET_KEY=$(openssl rand -base64 32)
DB_PASSWORD=$(openssl rand -base64 24)

sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" .env
sed -i "s|^SETUP_SECRET_KEY=.*|SETUP_SECRET_KEY=${SETUP_SECRET_KEY}|" .env
sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=${DB_PASSWORD}|" .env
sed -i "s|^DOMAIN=.*|DOMAIN=goungueincub.com|" .env
sed -i "s|^EXTERNAL_CADDY_NETWORK=.*|EXTERNAL_CADDY_NETWORK=edge|" .env
```
`EXTERNAL_CADDY_NETWORK=edge` **doit** être dans le `.env` (voir le piège en Partie F.2).

### E.3 Lancement (VPS mutualisé → `docker-compose.shared-caddy.yml`, pas `docker-compose.prod.yml`)
```bash
docker compose -f docker-compose.yml -f docker-compose.shared-caddy.yml up -d --build
docker network inspect edge --format '{{range .Containers}}{{.Name}} {{end}}'
```
Doit démarrer `db`, `app` et `frontend` (pas de `caddy` — c'est le Caddy central qui
s'en charge), et faire apparaître `goungu-backend-app-1` / `goungu-backend-frontend-1`
dans le réseau `edge`, avec les alias `goungu-app` / `goungu-frontend`.

### E.4 Site Caddy (`~/infra/caddy/sites/goungu.caddy`)
```caddyfile
goungueincub.com, www.goungueincub.com {
	handle /api/* {
		reverse_proxy goungu-app:8082
	}
	handle /uploads/* {
		reverse_proxy goungu-app:8082
	}
	handle {
		reverse_proxy goungu-frontend:80
	}
}
```
Différence avec un projet "simple" (Partie D) : ici deux services partagent un seul nom de
domaine, routés par chemin (`/api`, `/uploads` → backend ; tout le reste → frontend).

### E.5 Premier compte admin, puis fermeture de l'endpoint
```bash
curl -X POST "https://goungueincub.com/api/setup/create-admin" \
  --data-urlencode "email=toi@exemple.com" \
  --data-urlencode "motDePasse=un-mot-de-passe-fort" \
  --data-urlencode "cleSecrete=<SETUP_SECRET_KEY du .env>"
```
Puis, une fois l'admin créé :
```bash
sed -i "s|^SETUP_ENABLED=.*|SETUP_ENABLED=false|" .env
# si la ligne n'existe pas encore : echo "SETUP_ENABLED=false" >> .env
docker compose -f docker-compose.yml -f docker-compose.shared-caddy.yml up -d --build app
```
Vérifier que l'endpoint répond `404` ensuite (n'importe qui, avec n'importe quelle clé) :
```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://goungueincub.com/api/setup/create-admin \
  --data-urlencode "email=x@x.com" --data-urlencode "motDePasse=x" --data-urlencode "cleSecrete=x"
```

Ce compte (table `Admin`) gère articles/programmes/images/messages via `/api/auth/login`.
Pour la console `/espace/admin` du site (utilisateurs, candidatures, ressources,
événements), il faut en plus : s'inscrire normalement sur `/auth` (rôle "jeune" par ex.),
puis promouvoir ce compte en admin avec le compte `Admin` :
```bash
TOKEN=$(curl -s -X POST https://goungueincub.com/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"toi@exemple.com","motDePasse":"un-mot-de-passe-fort"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
curl -X PUT https://goungueincub.com/api/utilisateurs/<id-du-compte>/role \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"role":"admin"}'
```

---

## Partie F — Pièges rencontrés (et comment les éviter)

### F.1 Port 80/443 déjà utilisé
Symptôme : `Bind for 0.0.0.0:80 failed: port is already allocated`.
Cause : un autre Caddy (ou nginx/apache) tourne déjà. Vérifier avec :
```bash
sudo ss -tlnp | grep -E ':80|:443'
docker ps -a
```
**Solution : ne jamais faire tourner un second Caddy qui publie ces ports.** Toujours passer
par le Caddy central (Partie C/D), jamais par `docker-compose.prod.yml` sur ce VPS.

### F.2 `EXTERNAL_CADDY_NETWORK` oublié → 502 après un redéploiement
Symptôme : le site tournait, puis après un `docker compose ... up -d --build`, tout
répond `502 Bad Gateway`.
Cause : `docker-compose.shared-caddy.yml` a une valeur par défaut pour le réseau externe ;
si `EXTERNAL_CADDY_NETWORK` n'est pas dans le `.env` et qu'on ne le préfixe pas non plus sur
la ligne de commande, le conteneur reconstruit rejoint le **mauvais** réseau — le Caddy
central ne le retrouve plus.
**Solution : `EXTERNAL_CADDY_NETWORK=edge` doit être dans le `.env` du projet, pas seulement
tapé une fois sur la ligne de commande.** Vérifier après chaque redéploiement :
```bash
docker network inspect edge --format '{{range .Containers}}{{.Name}} {{end}}'
```

### F.3 Un projet existant doit rejoindre le Caddy central après coup
Si un projet avait déjà son propre Caddy (ce qui était le cas de "shaolin" au départ) :
1. Ajouter le réseau `edge` (aliases) à chaque service à exposer dans son `docker-compose.yml`.
2. Retirer entièrement son service `caddy` du fichier (plus de `ports: 80/443` nulle part
   ailleurs que dans `~/infra/caddy`).
3. `docker compose up -d --remove-orphans` — recrée les services sur le nouveau réseau et
   supprime automatiquement l'ancien conteneur Caddy devenu orphelin.
4. Ajouter son bloc de site dans `~/infra/caddy/sites/`, recharger le Caddy central.

Coupure attendue : quelques secondes, le temps que l'ancien Caddy s'arrête et que le Caddy
central prenne le relais.

### F.4 Rôle admin auto-attribuable
Ne jamais permettre à un endpoint d'inscription public d'accepter un rôle "admin" envoyé
par le client sans vérification serveur — c'est un endpoint séparé, protégé par clé secrète
(`/api/setup/create-admin`) et désactivable (`SETUP_ENABLED=false`), qui doit servir à ça.

---

## Partie G — Opérations courantes

### Mettre à jour un projet (nouveau code poussé sur GitHub)
```bash
cd ~/<dossier-du-projet>
git pull
docker compose -f docker-compose.yml -f docker-compose.shared-caddy.yml up -d --build
docker network inspect edge --format '{{range .Containers}}{{.Name}} {{end}}'   # sanity check, voir F.2
```

### Ajouter un tout nouveau projet plus tard
Reprendre uniquement la **Partie D** (le Caddy central et le réseau `edge` existent déjà,
inutile de refaire les parties A/B/C).

### Sauvegardes de la base de données (Goungué)
Aucune sauvegarde automatique par défaut. À minima, un cron quotidien :
```bash
# crontab -e
0 3 * * * docker exec $(docker ps -qf "name=goungu-backend-db-1") sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" goungue_db' > /home/$USER/backups/goungue_$(date +\%F).sql
```
Copier ces fichiers hors du VPS de temps en temps (autre machine, stockage objet).

### Redémarrage du serveur
Tous les services ont `restart: unless-stopped` : un redémarrage du VPS relance tout
automatiquement (Docker démarre au boot par défaut). Rien à faire manuellement.

### Nettoyage de volumes orphelins
Après avoir retiré l'ancien Caddy d'un projet (F.3), ses volumes (`caddy_data`,
`caddy_config` de ce projet) ne servent plus à rien :
```bash
docker volume ls   # reperer les volumes orphelins
docker volume rm <nom-du-volume>
```

---

## Checklist — ajouter un nouveau projet from scratch

- [ ] Domaine acheté, enregistrement A pointé vers l'IP du VPS, propagation vérifiée (`dig`)
- [ ] Dépôt(s) clonés dans leur propre dossier sous `~/`
- [ ] `.env` créé avec de vrais secrets (jamais les valeurs par défaut d'un `.env.example`)
- [ ] Le service applicatif n'expose aucun port directement sur l'hôte (`expose`, pas `ports`)
- [ ] Le service rejoint `edge` avec un alias stable
- [ ] `docker network inspect edge` liste bien le nouveau conteneur
- [ ] Un fichier dédié ajouté dans `~/infra/caddy/sites/`, aucun autre fichier modifié
- [ ] `docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile` (depuis `~/infra/caddy`)
- [ ] `curl -I https://<domaine>/` répond 200 avec un certificat valide
