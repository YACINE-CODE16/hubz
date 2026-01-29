# Configuration de l'envoi d'emails

## Vue d'ensemble

Le système d'invitation envoie automatiquement des emails HTML aux personnes invitées. Pour activer cette fonctionnalité, vous devez configurer un serveur SMTP.

## Option 1: Gmail (Recommandé pour le développement)

### 1. Créer un mot de passe d'application Gmail

1. Allez sur [https://myaccount.google.com/security](https://myaccount.google.com/security)
2. Activez la validation en 2 étapes si ce n'est pas déjà fait
3. Allez dans "Mots de passe des applications"
4. Générez un nouveau mot de passe d'application pour "Mail"
5. Copiez le mot de passe généré (16 caractères)

### 2. Configurer les variables d'environnement

Créez un fichier `.env` à la racine du projet backend:

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre.email@gmail.com
MAIL_PASSWORD=votre-mot-de-passe-application
MAIL_FROM=votre.email@gmail.com
FRONTEND_URL=http://localhost:5175
```

### 3. Exporter les variables (alternative sans .env)

```bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=votre.email@gmail.com
export MAIL_PASSWORD=votre-mot-de-passe-application
export MAIL_FROM=votre.email@gmail.com
export FRONTEND_URL=http://localhost:5175
```

## Option 2: SendGrid (Recommandé pour la production)

### 1. Créer un compte SendGrid

1. Allez sur [https://sendgrid.com/](https://sendgrid.com/)
2. Créez un compte gratuit (100 emails/jour)
3. Vérifiez votre identité d'expéditeur
4. Créez une clé API

### 2. Configuration

```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=votre-cle-api-sendgrid
MAIL_FROM=noreply@votredomaine.com
FRONTEND_URL=https://votredomaine.com
```

## Option 3: Mailgun

```bash
MAIL_HOST=smtp.mailgun.org
MAIL_PORT=587
MAIL_USERNAME=votre-username-mailgun
MAIL_PASSWORD=votre-password-mailgun
MAIL_FROM=noreply@votredomaine.com
FRONTEND_URL=https://votredomaine.com
```

## Option 4: Mailtrap (Pour les tests uniquement)

Mailtrap capture tous les emails sans les envoyer réellement. Parfait pour tester.

1. Créez un compte sur [https://mailtrap.io/](https://mailtrap.io/)
2. Créez une inbox
3. Copiez les credentials SMTP

```bash
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=587
MAIL_USERNAME=votre-username-mailtrap
MAIL_PASSWORD=votre-password-mailtrap
MAIL_FROM=test@hubz.com
FRONTEND_URL=http://localhost:5175
```

## Redémarrer le backend

Après avoir configuré les variables d'environnement:

```bash
cd hubz-backend
./mvnw spring-boot:run
```

## Comportement en cas d'échec

Si l'envoi d'email échoue:
- L'invitation sera quand même créée dans la base de données
- Une erreur sera loggée dans la console
- L'administrateur pourra toujours copier le lien manuellement depuis l'interface

## Vérifier que ça fonctionne

1. Créez une invitation depuis l'interface web
2. Vérifiez les logs du backend pour voir si l'email a été envoyé
3. Vérifiez la boîte de réception du destinataire

## Template d'email

L'email envoyé contient:
- Logo Hubz
- Nom de l'organisation
- Rôle assigné
- Bouton "Accepter l'invitation"
- Notice d'expiration (7 jours)
- Lien de secours si le bouton ne fonctionne pas

## Dépannage

### Erreur: "Authentication failed"
- Vérifiez que vous utilisez un mot de passe d'application (pas votre mot de passe Gmail normal)
- Vérifiez que la validation en 2 étapes est activée

### Erreur: "Could not connect to SMTP host"
- Vérifiez que le port est correct (587 pour STARTTLS, 465 pour SSL)
- Vérifiez votre pare-feu/antivirus

### Les emails arrivent dans les spams
- Configurez SPF/DKIM pour votre domaine
- Utilisez un service professionnel comme SendGrid en production
- Ajoutez votre adresse d'expédition aux contacts du destinataire

## Mode développement sans email

Si vous voulez désactiver temporairement l'envoi d'emails:
- Ne configurez pas les variables MAIL_*
- Le système continuera de fonctionner
- Les administrateurs pourront copier les liens manuellement
