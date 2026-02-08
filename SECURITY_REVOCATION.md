# 🔒 Actions de Révocation de Sécurité Urgentes

## ⚠️ Secrets Exposés et Révoqués

Les secrets suivants ont été **exposés publiquement** sur GitHub et **DOIVENT être révoqués immédiatement**:

### 1. Gmail App Password (SMTP)
**Email:** yacineallam00@gmail.com
**Password exposé:** `ygcc axhb tuul thrl`

**Actions à faire MAINTENANT:**
1. Aller sur https://myaccount.google.com/apppasswords
2. Supprimer le mot de passe d'application "Hubz" ou tout mot de passe suspect
3. Créer un **NOUVEAU** mot de passe d'application
4. Mettre à jour la variable `MAIL_PASSWORD` dans votre `.env` local
5. Mettre à jour la variable `SMTP_PASSWORD` dans Render (production)

### 2. Google OAuth Credentials
**Client ID exposé:** `440822837711-69iumithrv8hdcjtimslm9dl9m372c5a`
**Client Secret exposé:** `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`

**Actions à faire MAINTENANT:**
1. Aller sur https://console.cloud.google.com/apis/credentials
2. Trouver le Client ID OAuth 2.0 concerné
3. **SUPPRIMER** complètement ce client OAuth
4. **CRÉER** un nouveau Client ID OAuth 2.0:
   - Type: Application Web
   - Nom: Hubz Production
   - Authorized JavaScript origins:
     - `http://localhost:5173` (dev)
     - `https://hubz.vercel.app` (prod - ou votre domaine)
   - Authorized redirect URIs:
     - `http://localhost:8085/api/auth/oauth2/google/callback` (dev)
     - `https://hubz-backend.onrender.com/api/auth/oauth2/google/callback` (prod)
5. Copier le **nouveau** Client ID et Client Secret
6. Mettre à jour dans votre `.env` local:
   ```bash
   GOOGLE_CLIENT_ID=nouveau-client-id
   GOOGLE_CLIENT_SECRET=nouveau-client-secret
   ```
7. Mettre à jour dans Render (production):
   - Variable `GOOGLE_CLIENT_ID`
   - Variable `GOOGLE_CLIENT_SECRET`

---

## ✅ Actions de Nettoyage Effectuées

### Fichiers supprimés du repo:
- ✅ `fix-now.sql` (contenait email)
- ✅ `hubz-backend/backend.log` (fichier de log sensible)

### Fichiers nettoyés:
- ✅ `hubz-backend/src/main/resources/application.yml` - Secrets remplacés par placeholders
- ✅ `docker-compose.yml` - Mots de passe hardcodés remplacés par variables d'environnement

### Historique Git:
- ✅ **BFG Repo-Cleaner** a purgé tous les secrets de l'historique Git complet
- ✅ **14 commits** ont été nettoyés
- ✅ **27 object IDs** ont été modifiés
- ✅ Force push effectué vers GitHub

---

## 🔄 Si vous avez déjà cloné le repo

Si vous ou quelqu'un d'autre avez cloné le repo avant le nettoyage, vous DEVEZ:

1. **Supprimer** votre clone local:
   ```bash
   rm -rf hubz
   ```

2. **Re-cloner** le repo nettoyé:
   ```bash
   git clone https://github.com/YACINE-CODE16/hubz.git
   cd hubz
   ```

3. Copier votre `.env` avec les **NOUVEAUX** secrets (pas les anciens!)

---

## 📋 Checklist de Sécurité

- [ ] Gmail App Password révoqué et nouveau créé
- [ ] Variable `MAIL_PASSWORD` mise à jour (local et Render)
- [ ] OAuth Client supprimé et nouveau créé
- [ ] Variables `GOOGLE_CLIENT_ID` et `GOOGLE_CLIENT_SECRET` mises à jour (local et Render)
- [ ] Repo local supprimé et re-cloné (si applicable)
- [ ] Backend redémarré avec les nouveaux credentials
- [ ] Test de connexion OAuth Google fonctionne
- [ ] Test d'envoi d'email SMTP fonctionne

---

## 🚨 Pourquoi c'est Important

Les secrets exposés permettent à **N'IMPORTE QUI** de:
- Envoyer des emails depuis votre compte Gmail
- Se connecter à votre application via Google OAuth
- Potentiellement accéder aux données utilisateurs

**Agissez IMMÉDIATEMENT!**

---

**Date de révocation:** 08 février 2026
**Dernière mise à jour:** Après nettoyage Git avec BFG Repo-Cleaner
