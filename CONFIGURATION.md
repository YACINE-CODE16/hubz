# 🚀 Guide de Configuration Hubz

## 1. OAuth Google (5 minutes)

### Google Cloud Console
1. Aller sur https://console.cloud.google.com
2. Créer un projet "Hubz"
3. Activer **Google+ API**
4. Créer des credentials OAuth 2.0:
   - Type: Application Web
   - Nom: Hubz
   - Authorized redirect URIs:
     ```
     http://localhost:8085/api/auth/oauth2/google/callback
     https://votre-backend.onrender.com/api/auth/oauth2/google/callback
     ```

5. Copier **Client ID** et **Client Secret**

### Backend (application.yml)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: VOTRE_CLIENT_ID_ICI
            client-secret: VOTRE_CLIENT_SECRET_ICI
            scope: profile, email
```

---

## 2. Variables d'Environnement

### Vercel (Frontend)
```bash
# Via Vercel Dashboard ou CLI
vercel env add VITE_API_URL
# Valeur: https://votre-backend.onrender.com
```

### Render (Backend)
```bash
# Via Render Dashboard, ajouter ces variables:

# Base de données (généré automatiquement par Render)
DATABASE_URL=postgresql://...

# JWT
JWT_SECRET=votre-secret-aleatoire-securise-minimum-32-caracteres

# CORS
CORS_ALLOWED_ORIGINS=https://votre-app.vercel.app

# Email SMTP (optionnel pour v1)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=votre-email@gmail.com
SMTP_PASSWORD=votre-mot-de-passe-application
SMTP_FROM=noreply@hubz.com

# OAuth Google
GOOGLE_CLIENT_ID=votre-client-id
GOOGLE_CLIENT_SECRET=votre-client-secret
GOOGLE_REDIRECT_URI=https://votre-backend.onrender.com/api/auth/oauth2/google/callback

# Ollama (optionnel - désactiver en prod)
OLLAMA_ENABLED=false
# Ou si vous avez un serveur Ollama dédié:
# OLLAMA_URL=http://votre-serveur-ollama:11434
# OLLAMA_ENABLED=true
```

---

## 3. Configuration Email SMTP (Optionnel)

### Gmail (le plus simple)
1. Activer "2-Step Verification" sur votre compte Google
2. Générer un "App Password": https://myaccount.google.com/apppasswords
3. Utiliser ce mot de passe dans `SMTP_PASSWORD`

### Autres providers
- **SendGrid**: Gratuit 100 emails/jour
- **Mailgun**: Gratuit 5000 emails/mois
- **AWS SES**: Très bon marché

---

## 4. Déploiement

### Frontend (Vercel)
```bash
cd hubz-frontend
npm run build  # Test local
vercel --prod  # Déploiement
```

### Backend (Render)
1. Connecter votre repo GitHub
2. Render détecte automatiquement `render.yaml`
3. Configurer les variables d'environnement (voir section 2)
4. Déployer!

---

## 5. Installation Ollama (Local uniquement)

**macOS:**
```bash
brew install ollama
ollama pull llama3.1
ollama serve
```

**Linux:**
```bash
curl -fsSL https://ollama.ai/install.sh | sh
ollama pull llama3.1
ollama serve
```

**Windows:**
Télécharger depuis https://ollama.ai

---

## 6. Premier Déploiement - Checklist

- [ ] OAuth Google configuré
- [ ] Variables d'environnement Vercel configurées
- [ ] Variables d'environnement Render configurées
- [ ] Frontend build réussi localement (`npm run build`)
- [ ] Backend tests passent (`./mvnw test`)
- [ ] Déployé sur Vercel
- [ ] Déployé sur Render
- [ ] Test login/register en production
- [ ] Test création organisation en production
- [ ] Test création tâche en production

---

## 7. URLs de Production

**Frontend:** https://votre-app.vercel.app
**Backend:** https://votre-backend.onrender.com
**Swagger (dev):** http://localhost:8085/swagger-ui.html

---

## 🔧 Troubleshooting

### OAuth Google ne fonctionne pas
- Vérifier que les redirect URIs sont exactement les mêmes
- Vérifier que Google+ API est activée
- Vérifier client-id et client-secret dans application.yml

### Base de données erreurs
- Vérifier DATABASE_URL dans Render
- Vérifier que PostgreSQL est démarré

### CORS erreurs
- Vérifier CORS_ALLOWED_ORIGINS inclut l'URL Vercel exacte
- Pas de slash à la fin de l'URL

### Emails ne partent pas
- Vérifier SMTP credentials
- Vérifier firewall/ports (587 pour TLS)
- Tester avec Gmail App Password d'abord

---

## 📞 Support

Si problème, vérifier:
1. Logs Vercel (Deployments → Logs)
2. Logs Render (Dashboard → Logs)
3. Browser DevTools (Network tab)
4. Backend logs (actuator/health, actuator/info)
