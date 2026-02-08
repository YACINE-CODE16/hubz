# Configuration Manuelle - Hubz

Ce document liste toutes les configurations manuelles requises pour les fonctionnalités qui ne peuvent pas être automatisées par les agents.

---

## 1. Configuration SMTP (Email)

### Étapes:
1. Créer un compte Gmail ou utiliser un service SMTP (SendGrid, Mailgun, etc.)
2. Pour Gmail:
   - Aller sur https://myaccount.google.com/apppasswords
   - Générer un "App Password" pour "Mail"
   - Copier le mot de passe généré

3. Configurer les variables d'environnement:
```bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=ton.email@gmail.com
export MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx  # App Password
export MAIL_FROM=noreply@hubz.com
```

4. Ou modifier `hubz-backend/src/main/resources/application.yml`:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ton.email@gmail.com
    password: ton-app-password
```

### Vérification:
```bash
cd hubz-backend && ./mvnw spring-boot:run
# Tester l'inscription - un email de vérification devrait être envoyé
```

---

## 2. CI/CD - GitHub Actions

### Étapes:

1. **Créer le dossier workflows**:
```bash
mkdir -p .github/workflows
```

2. **Créer le fichier `.github/workflows/ci.yml`**:
```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: cd hubz-backend && ./mvnw test

      - name: Build
        run: cd hubz-backend && ./mvnw clean package -DskipTests

  frontend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: hubz-frontend/package-lock.json

      - name: Install dependencies
        run: cd hubz-frontend && npm ci

      - name: Build
        run: cd hubz-frontend && npm run build
```

3. **Pusher sur GitHub**:
```bash
git add .github/
git commit -m "ci: add GitHub Actions workflow"
git push origin main
```

4. **Vérifier**: Aller sur GitHub > Actions pour voir le pipeline

---

## 3. Déploiement Production

### Option A: VPS (DigitalOcean, Hetzner, OVH)

1. **Louer un VPS** (~5-10€/mois):
   - DigitalOcean: https://www.digitalocean.com
   - Hetzner: https://www.hetzner.com
   - OVH: https://www.ovh.com

2. **Installer Docker sur le VPS**:
```bash
ssh root@ton-ip
curl -fsSL https://get.docker.com | sh
apt install docker-compose
```

3. **Cloner le projet**:
```bash
git clone https://github.com/ton-user/hubz.git
cd hubz
```

4. **Créer `.env.prod`**:
```bash
# Database
DB_USERNAME=hubz
DB_PASSWORD=ton-mot-de-passe-securise

# JWT
JWT_SECRET=une-cle-secrete-de-256-bits-minimum

# Mail
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=ton.email@gmail.com
MAIL_PASSWORD=ton-app-password
MAIL_FROM=noreply@hubz.com

# URLs
FRONTEND_URL=https://hubz.ton-domaine.com
CORS_ALLOWED_ORIGINS=https://hubz.ton-domaine.com
```

5. **Créer `docker-compose.prod.yml`**:
```yaml
version: '3.8'
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: hubzdb
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: always

  backend:
    build: ./hubz-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      MAIL_FROM: ${MAIL_FROM}
      FRONTEND_URL: ${FRONTEND_URL}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
    depends_on:
      - db
    restart: always

  frontend:
    build: ./hubz-frontend
    restart: always

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - /etc/letsencrypt:/etc/letsencrypt
    depends_on:
      - backend
      - frontend
    restart: always

volumes:
  postgres_data:
```

6. **Configurer SSL avec Let's Encrypt**:
```bash
apt install certbot
certbot certonly --standalone -d hubz.ton-domaine.com
```

7. **Lancer**:
```bash
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

### Option B: Vercel (Frontend) + Railway/Render (Backend)

1. **Frontend sur Vercel**:
   - Créer compte sur https://vercel.com
   - Importer le repo GitHub
   - Root directory: `hubz-frontend`
   - Build command: `npm run build`
   - Output: `dist`

2. **Backend sur Railway**:
   - Créer compte sur https://railway.app
   - New Project > Deploy from GitHub
   - Sélectionner le repo, root: `hubz-backend`
   - Ajouter PostgreSQL addon
   - Configurer les variables d'environnement

---

## 4. OAuth Providers (Optionnel)

### Google OAuth
1. Aller sur https://console.cloud.google.com
2. Créer un projet
3. APIs & Services > Credentials > Create OAuth Client ID
4. Configurer:
   - Authorized origins: `http://localhost:5173`, `https://hubz.ton-domaine.com`
   - Authorized redirects: `http://localhost:8085/api/auth/oauth2/callback/google`
5. Copier Client ID et Client Secret

### GitHub OAuth
1. Aller sur https://github.com/settings/developers
2. New OAuth App
3. Configurer:
   - Homepage: `https://hubz.ton-domaine.com`
   - Callback: `http://localhost:8085/api/auth/oauth2/callback/github`
4. Copier Client ID et Client Secret

---

## 5. Stockage Cloud (Optionnel)

### AWS S3
1. Créer compte AWS: https://aws.amazon.com
2. IAM > Create User > Programmatic access
3. Attacher policy: `AmazonS3FullAccess`
4. Créer un bucket S3
5. Configurer:
```bash
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
export AWS_S3_BUCKET=hubz-uploads
export AWS_REGION=eu-west-3
```

---

## 6. Intégrations (Optionnel)

### Slack
1. Créer app sur https://api.slack.com/apps
2. OAuth & Permissions > Add scopes: `chat:write`, `channels:read`
3. Install to Workspace
4. Copier Bot Token

### GitHub (Issues/PRs)
1. Settings > Developer settings > Personal access tokens
2. Generate token avec scopes: `repo`, `read:org`

---

## Checklist Résumé

| Tâche | Priorité | Temps estimé |
|-------|----------|--------------|
| SMTP Gmail | Haute | 10 min |
| GitHub Actions CI | Haute | 15 min |
| VPS + Docker | Moyenne | 1-2h |
| SSL Let's Encrypt | Moyenne | 15 min |
| OAuth Google | Basse | 30 min |
| AWS S3 | Basse | 30 min |

---

**Dernière mise à jour:** 03 février 2026
