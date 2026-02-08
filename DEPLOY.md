# 🚀 Guide de Déploiement Hubz

## Repo GitHub
https://github.com/YACINE-CODE16/hubz

---

## 📦 Architecture de Déploiement

- **Frontend:** Vercel (React + Vite)
- **Backend:** Render (Spring Boot + PostgreSQL)
- **AI:** Ollama (local uniquement - désactivé en prod)
- **Cache:** Redis (fourni par Render)

---

## 1️⃣ Déploiement Backend (Render)

### Étape 1: Créer le service

1. Aller sur https://dashboard.render.com
2. Cliquer "New +" → "Web Service"
3. Connecter le repo GitHub: `YACINE-CODE16/hubz`
4. Configuration:
   - **Name:** `hubz-backend`
   - **Region:** Frankfurt (ou plus proche)
   - **Branch:** `main`
   - **Root Directory:** `hubz-backend`
   - **Runtime:** Docker
   - **Plan:** Free (ou Starter $7/mois)

### Étape 2: Variables d'environnement

Ajouter ces variables dans Render Dashboard → Environment:

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Database (Render le génère automatiquement si vous créez une PostgreSQL database)
DATABASE_URL=postgresql://user:password@host/db
DB_USERNAME=hubz_user
DB_PASSWORD=generate-strong-password

# JWT Secret (générer une clé aléatoire sécurisée)
JWT_SECRET=your-256-bit-secret-key-here-must-be-very-long-and-random

# CORS (URL Vercel de votre frontend)
CORS_ALLOWED_ORIGINS=https://hubz.vercel.app

# Frontend URL
FRONTEND_URL=https://hubz.vercel.app

# Email SMTP (optionnel pour MVP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
MAIL_FROM=noreply@hubz.com

# Email Verification (désactiver pour MVP)
EMAIL_VERIFICATION_REQUIRED=false

# OAuth Google
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=https://hubz-backend.onrender.com/api/auth/oauth2/google/callback

# Redis (optionnel - Render peut le fournir)
REDIS_HOST=redis-hostname-from-render
REDIS_PORT=6379

# Ollama (désactiver en prod - sauf si serveur dédié)
OLLAMA_ENABLED=false
```

### Étape 3: Créer la base de données PostgreSQL

1. Dans Render Dashboard → "New +" → "PostgreSQL"
2. Name: `hubz-db`
3. Database: `hubzdb`
4. User: `hubz_user`
5. Region: Même région que le backend
6. Plan: Free
7. Créer → Copier l'URL de connexion
8. Coller `DATABASE_URL` dans les env vars du backend

### Étape 4: Déployer

1. Render va automatiquement détecter le `Dockerfile`
2. Le build va démarrer automatiquement
3. Attendre ~5-10 minutes
4. Vérifier: https://hubz-backend.onrender.com/actuator/health

---

## 2️⃣ Déploiement Frontend (Vercel)

### Étape 1: Import du projet

1. Aller sur https://vercel.com
2. "Add New..." → "Project"
3. Import Git Repository: `YACINE-CODE16/hubz`
4. Configuration:
   - **Framework Preset:** Vite
   - **Root Directory:** `hubz-frontend`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`

### Étape 2: Variables d'environnement

Ajouter dans Vercel → Settings → Environment Variables:

```bash
VITE_API_URL=https://hubz-backend.onrender.com
```

### Étape 3: Déployer

1. Click "Deploy"
2. Attendre ~2 minutes
3. Votre app est live! https://hubz.vercel.app (ou votre domaine)

---

## 3️⃣ Configuration Google OAuth (Production)

### Ajouter les URLs de production

1. Google Cloud Console: https://console.cloud.google.com
2. APIs & Services → Credentials
3. Modifier votre OAuth 2.0 Client ID
4. Ajouter dans "Authorized redirect URIs":
   ```
   https://hubz-backend.onrender.com/api/auth/oauth2/google/callback
   ```
5. Ajouter dans "Authorized JavaScript origins":
   ```
   https://hubz.vercel.app
   ```
6. Sauvegarder

---

## 4️⃣ Configuration Email SMTP (Production)

### Option 1: Gmail (simple)
1. Activer 2FA sur votre compte Google
2. Créer un App Password: https://myaccount.google.com/apppasswords
3. Utiliser ce mot de passe dans `MAIL_PASSWORD`

### Option 2: SendGrid (recommandé pour prod)
1. Créer un compte: https://sendgrid.com
2. Gratuit: 100 emails/jour
3. API Key → Utiliser dans `MAIL_PASSWORD`
4. `MAIL_HOST=smtp.sendgrid.net`
5. `MAIL_USERNAME=apikey`

---

## 5️⃣ Vérification du Déploiement

### Backend Health Check
```bash
curl https://hubz-backend.onrender.com/actuator/health
```

**Réponse attendue:**
```json
{
  "status": "UP"
}
```

### Frontend
Ouvrir: https://hubz.vercel.app

### Test Complet
1. Register un compte
2. Login
3. Créer une organisation
4. Créer une tâche
5. Tester le chatbot

---

## 6️⃣ Domaine Personnalisé (Optionnel)

### Frontend (Vercel)
1. Vercel Dashboard → Settings → Domains
2. Add Domain: `hubz.votredomaine.com`
3. Configurer les DNS selon les instructions

### Backend (Render)
1. Render Dashboard → Settings → Custom Domain
2. Add Custom Domain: `api.hubz.votredomaine.com`
3. Configurer les DNS
4. Mettre à jour `CORS_ALLOWED_ORIGINS` et `GOOGLE_REDIRECT_URI`

---

## 7️⃣ Monitoring & Logs

### Render (Backend)
- Logs: Dashboard → Logs
- Metrics: Dashboard → Metrics
- Health: /actuator/health

### Vercel (Frontend)
- Analytics: Dashboard → Analytics
- Logs: Dashboard → Deployments → View Function Logs
- Speed Insights: Dashboard → Speed Insights

---

## 8️⃣ CI/CD (Automatique)

### GitHub Actions
Le projet inclut déjà des workflows CI/CD:

- **backend-tests.yml**: Tests backend sur chaque push
- **frontend-tests.yml**: Tests frontend sur chaque push
- **e2e-tests.yml**: Tests E2E sur chaque PR
- **build.yml**: Build verification
- **deploy.yml**: Déploiement automatique sur tags `v*.*.*`

### Déployer une nouvelle version
```bash
# Créer un tag de version
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions va automatiquement:
# 1. Lancer tous les tests
# 2. Build les images Docker
# 3. Déployer sur Render et Vercel
```

---

## 9️⃣ Coûts Estimés

### Plan Gratuit (MVP)
- Vercel: Gratuit (100GB bandwidth, hobby plan)
- Render: Gratuit (750h/mois, sleep après inactivité)
- PostgreSQL: Gratuit (limité)
- **Total: 0€/mois**

### Plan Production (recommandé)
- Vercel Pro: $20/mois
- Render Starter: $7/mois (backend)
- PostgreSQL: $7/mois
- Redis: $10/mois
- **Total: ~$44/mois**

---

## 🔟 Troubleshooting

### Backend ne démarre pas
1. Vérifier les logs Render
2. Vérifier `DATABASE_URL` est correct
3. Vérifier `JWT_SECRET` est défini
4. Vérifier `SPRING_PROFILES_ACTIVE=prod`

### Frontend 404 sur les routes
1. Vercel → Settings → Rewrites
2. Ajouter: `/*` → `/index.html` (déjà configuré dans vercel.json)

### CORS Errors
1. Vérifier `CORS_ALLOWED_ORIGINS` inclut l'URL Vercel exacte
2. Pas de slash à la fin
3. Protocole HTTPS en prod

### OAuth Google ne marche pas
1. Vérifier redirect URIs dans Google Cloud Console
2. Vérifier `GOOGLE_REDIRECT_URI` dans Render env vars
3. Vérifier `FRONTEND_URL` est correct

---

## 📞 Support

- **Issues:** https://github.com/YACINE-CODE16/hubz/issues
- **Docs:** Voir README.md et CONFIGURATION.md

---

**Dernière mise à jour:** 08 février 2026
**Version:** 1.0.0
