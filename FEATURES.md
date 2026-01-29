# Hubz - Liste des Fonctionnalités

## Légende
- ✅ **Complété** - Fonctionnalité implémentée et testée
- 🚧 **En cours** - Fonctionnalité partiellement implémentée
- ⏳ **À faire** - Fonctionnalité planifiée mais non commencée
- 🔴 **Bloqué** - Fonctionnalité bloquée par une dépendance

---

## 1. Authentification & Autorisation

### 1.1 Authentification
- ✅ Inscription utilisateur (email + mot de passe)
- ✅ Connexion avec JWT
- ✅ Déconnexion
- ✅ Validation de session (token refresh)
- ✅ Hachage des mots de passe avec BCrypt
- ⏳ Récupération de mot de passe (email)
- ⏳ Validation email lors de l'inscription
- ⏳ Authentification à deux facteurs (2FA)

### 1.2 Gestion des utilisateurs
- ✅ Profil utilisateur (firstName, lastName, email, description)
- ✅ Endpoint GET /api/auth/me
- ✅ Modification du profil utilisateur
- ✅ Changement de mot de passe
- ⏳ Photo de profil
- ⏳ Suppression de compte

---

## 2. Organisations

### 2.1 CRUD Organisations
- ✅ Créer une organisation
- ✅ Lister mes organisations
- ✅ Voir les détails d'une organisation
- ✅ Modifier une organisation (nom, description, icon, color, readme)
- ✅ Supprimer une organisation (owner uniquement)

### 2.2 Membres d'organisation
- ✅ Lister les membres d'une organisation
- ✅ Ajouter un membre (par ID utilisateur)
- ✅ Retirer un membre
- ✅ Rôles: OWNER, ADMIN, MEMBER, VIEWER
- ✅ Vérification des permissions selon le rôle
- ✅ Système d'invitation par email/lien
  - ✅ Créer une invitation
  - ✅ Lister les invitations en attente
  - ✅ Accepter une invitation (/join/:token)
  - ✅ Supprimer une invitation
  - ✅ Expiration après 7 jours
  - ✅ Envoi automatique d'email (avec template HTML)
- ⏳ Changer le rôle d'un membre
- ⏳ Transfert de propriété (OWNER)

### 2.3 Documents d'organisation
- ✅ Upload de documents (drag & drop)
- ✅ Liste des documents
- ✅ Téléchargement de documents
- ✅ Suppression de documents
- ✅ Stockage local des fichiers
- ⏳ Prévisualisation des documents
- ⏳ Versioning des documents
- ⏳ Tags/catégories pour documents

---

## 3. Équipes (Teams)

### 3.1 CRUD Équipes
- ✅ Créer une équipe
- ✅ Lister les équipes d'une organisation
- ✅ Voir les détails d'une équipe
- ✅ Modifier une équipe
- ✅ Supprimer une équipe

### 3.2 Membres d'équipe
- ✅ Ajouter un membre à une équipe
- ✅ Retirer un membre d'une équipe
- ✅ Lister les membres d'une équipe
- ✅ UI pour gérer les membres (modal)

---

## 4. Tâches (Tasks)

### 4.1 CRUD Tâches
- ✅ Créer une tâche
- ✅ Lister les tâches d'une organisation
- ✅ Lister toutes mes tâches (across organizations)
- ✅ Voir les détails d'une tâche
- ✅ Modifier une tâche
- ✅ Supprimer une tâche
- ✅ Changer le statut (TODO, IN_PROGRESS, DONE)

### 4.2 Propriétés des tâches
- ✅ Titre & description
- ✅ Statut (TODO, IN_PROGRESS, DONE)
- ✅ Priorité (LOW, MEDIUM, HIGH, URGENT)
- ✅ Date d'échéance (due date)
- ✅ Assignation à un utilisateur
- ✅ Association à une équipe (optionnel)
- ✅ Créateur de la tâche
- ⏳ Tags/labels
- ⏳ Checklist dans une tâche
- ⏳ Pièces jointes
- ⏳ Commentaires sur les tâches
- ⏳ Historique des modifications

### 4.3 Interface utilisateur
- ✅ Vue tableau (Kanban board)
- ✅ Drag & drop entre colonnes
- ✅ Filtres par statut, priorité, assigné
- ⏳ Vue liste
- ⏳ Vue calendrier
- ⏳ Recherche de tâches

---

## 5. Objectifs (Goals)

### 5.1 CRUD Objectifs
- ✅ Créer un objectif
- ✅ Lister les objectifs d'une organisation
- ✅ Lister mes objectifs personnels
- ✅ Modifier un objectif
- ✅ Supprimer un objectif
- ✅ Mettre à jour la progression

### 5.2 Types d'objectifs
- ✅ Objectifs personnels (userId, organizationId = null)
- ✅ Objectifs d'organisation (organizationId)
- ✅ Types: SHORT (court terme), MEDIUM (moyen terme), LONG (long terme)
- ✅ Valeur cible & valeur actuelle
- ✅ Date limite
- ⏳ Objectifs liés à des tâches
- ⏳ Calcul automatique de progression

### 5.3 Interface utilisateur
- ✅ Barres de progression
- ✅ Affichage par type (court/moyen/long terme)
- ⏳ Graphiques de progression
- ⏳ Notifications à l'approche de la deadline

---

## 6. Calendrier & Événements

### 6.1 CRUD Événements
- ✅ Créer un événement
- ✅ Lister les événements d'une organisation
- ✅ Lister mes événements personnels
- ✅ Modifier un événement
- ✅ Supprimer un événement

### 6.2 Propriétés des événements
- ✅ Titre & description
- ✅ Date/heure de début et fin
- ✅ Objectif de l'événement
- ✅ Type: personnel ou organisation
- ⏳ Participants
- ⏳ Lieu (physique ou lien visio)
- ⏳ Rappels/notifications
- ⏳ Événements récurrents

### 6.3 Interface calendrier
- ✅ Vue mensuelle
- ✅ Vue hebdomadaire
- ✅ Vue journalière
- ⏳ Intégration Google Calendar / Outlook
- ⏳ Export iCal

---

## 7. Notes

### 7.1 CRUD Notes
- ✅ Créer une note
- ✅ Lister les notes d'une organisation
- ✅ Modifier une note
- ✅ Supprimer une note
- ✅ Catégories de notes

### 7.2 Contenu des notes
- ✅ Titre & contenu (markdown)
- ✅ Catégorie
- ✅ Créateur
- ✅ Pièces jointes (par note)
- ⏳ Éditeur WYSIWYG
- ⏳ Collaboration en temps réel
- ⏳ Versioning des notes
- ⏳ Recherche full-text

### 7.3 Organisation
- ✅ Notes séparées des documents d'organisation
- ✅ Upload de fichiers attachés aux notes
- ⏳ Dossiers/arborescence
- ⏳ Tags

---

## 8. Espace Personnel

### 8.1 Habitudes (Habits)
- ✅ Créer une habitude
- ✅ Lister mes habitudes
- ✅ Modifier une habitude
- ✅ Supprimer une habitude
- ✅ Fréquence (DAILY, WEEKLY)
- ✅ Icône personnalisable

### 8.2 Suivi des habitudes (Habit Logs)
- ✅ Logger une habitude (marquer comme complétée)
- ✅ Historique des logs
- ✅ Contrainte unique (1 log par habitude par jour)
- 🚧 Interface visuelle du tracking
- ⏳ Streaks (séries)
- ⏳ Statistiques de complétion
- ⏳ Graphiques de progression

### 8.3 Récapitulatif personnel
- 🚧 Page récap avec vue d'ensemble
- 🚧 Objectifs personnels
- 🚧 Habitudes du jour
- 🚧 Tâches assignées
- ⏳ Statistiques de productivité
- ⏳ Insights & recommandations

---

## 9. Dashboard & Analytics

### 9.1 Dashboard Organisation
- ✅ Vue d'ensemble de l'organisation
- ✅ Statistiques de base (membres, tâches, objectifs)
- ✅ Activité récente
- ⏳ Graphiques de progression
- ⏳ KPIs personnalisables
- ⏳ Rapports exportables

### 9.2 Dashboard Personnel
- 🚧 Vue d'ensemble personnelle
- ⏳ Mes tâches du jour
- ⏳ Mes habitudes du jour
- ⏳ Mes prochains événements
- ⏳ Progression de mes objectifs

### 9.3 Analytics - Tâches
- ⏳ Nombre de tâches créées par période (jour/semaine/mois)
- ⏳ Nombre de tâches complétées par période
- ⏳ Taux de complétion des tâches (complétées / total)
- ⏳ Temps moyen de complétion d'une tâche (création → DONE)
- ⏳ Temps moyen par statut (temps en TODO, temps en IN_PROGRESS)
- ⏳ Répartition par priorité (LOW/MEDIUM/HIGH/URGENT) - Pie chart
- ⏳ Répartition par statut (TODO/IN_PROGRESS/DONE) - Pie chart
- ⏳ Tâches en retard (dépassant la due date)
- ⏳ Taux de retard (tâches en retard / tâches avec due date)
- ⏳ Burndown chart (tâches restantes vs temps)
- ⏳ Burnup chart (tâches complétées cumulées vs temps)
- ⏳ Velocity chart (tâches complétées par sprint/semaine)
- ⏳ Throughput chart (tâches terminées par jour - rolling average)
- ⏳ Cycle time distribution (histogramme du temps de complétion)
- ⏳ Lead time (temps entre création et complétion)
- ⏳ Tâches bloquées trop longtemps (alertes)
- ⏳ Tendance de création vs complétion (balance de flux)
- ⏳ Cumulative flow diagram (CFD)
- ⏳ Work in progress (WIP) chart - tâches actives en parallèle

### 9.4 Analytics - Membres & Productivité
- ⏳ Tâches complétées par membre (classement)
- ⏳ Charge de travail par membre (tâches assignées actives)
- ⏳ Répartition de la charge (heatmap par membre)
- ⏳ Taux de complétion par membre
- ⏳ Temps moyen de complétion par membre
- ⏳ Historique d'activité par membre (contributions/jour)
- ⏳ Score de productivité individuel (tâches pondérées par priorité)
- ⏳ Contribution heatmap (style GitHub - grille d'activité annuelle)
- ⏳ Comparaison de performance entre équipes
- ⏳ Top performers de la semaine/du mois
- ⏳ Indicateur de surcharge (membres avec trop de tâches)
- ⏳ Membres inactifs (aucune activité depuis X jours)

### 9.5 Analytics - Objectifs
- ⏳ Progression globale des objectifs (agrégée)
- ⏳ Taux d'objectifs atteints vs en cours vs échoués
- ⏳ Progression par type (SHORT/MEDIUM/LONG)
- ⏳ Courbe de progression dans le temps par objectif
- ⏳ Objectifs à risque (progression faible + deadline proche)
- ⏳ Vélocité de progression (vitesse d'avancement)
- ⏳ Prédiction de complétion (estimation basée sur la vélocité)
- ⏳ Historique des objectifs complétés par mois
- ⏳ Corrélation objectifs ↔ tâches (combien de tâches contribuent)
- ⏳ Score d'alignement stratégique (objectifs liés entre eux)

### 9.6 Analytics - Habitudes & Bien-être
- ⏳ Taux de complétion quotidien des habitudes
- ⏳ Taux de complétion hebdomadaire
- ⏳ Taux de complétion mensuel
- ⏳ Streaks actuels (jours consécutifs de complétion)
- ⏳ Record de streak par habitude
- ⏳ Heatmap calendrier de complétion (style GitHub contributions)
- ⏳ Jour de la semaine le plus productif
- ⏳ Tendance sur 30/60/90 jours
- ⏳ Score de constance (régularité dans le temps)
- ⏳ Habitudes les plus/moins respectées (classement)
- ⏳ Corrélation habitudes ↔ productivité
- ⏳ Graphique radar (vue d'ensemble de toutes les habitudes)
- ⏳ Comparaison semaine actuelle vs semaine précédente
- ⏳ Alerte de rupture de streak

### 9.7 Analytics - Calendrier & Temps
- ⏳ Nombre d'événements par semaine/mois
- ⏳ Répartition du temps par type d'événement
- ⏳ Heures occupées vs disponibles (taux d'occupation)
- ⏳ Jours les plus chargés (heatmap)
- ⏳ Créneaux les plus utilisés (distribution horaire)
- ⏳ Temps passé en réunions vs travail individuel
- ⏳ Tendance du nombre de réunions
- ⏳ Conflits d'agenda détectés
- ⏳ Score de disponibilité
- ⏳ Prévision de charge pour la semaine suivante

### 9.8 Analytics - Organisation (Vue globale)
- ⏳ Score de santé de l'organisation (composite)
- ⏳ Nombre total de tâches actives
- ⏳ Nombre de membres actifs (derniers 7 jours)
- ⏳ Activité globale (événements + tâches + notes + objectifs)
- ⏳ Tendance d'activité sur 12 mois
- ⏳ Répartition de l'activité par équipe
- ⏳ Taux de croissance (nouveaux membres/mois)
- ⏳ Score de collaboration (tâches cross-team)
- ⏳ Flux d'activité en temps réel (timeline)
- ⏳ Graphique réseau de collaboration (qui travaille avec qui)
- ⏳ Indicateur de santé par équipe (traffic light system)
- ⏳ Comparaison mois par mois (MoM growth)

### 9.9 Rapports & Exports
- ⏳ Rapport hebdomadaire automatique (digest)
- ⏳ Rapport mensuel d'activité
- ⏳ Export PDF des dashboards
- ⏳ Export CSV des données brutes
- ⏳ Export Excel avec graphiques
- ⏳ Rapport de productivité par membre
- ⏳ Rapport de progression des objectifs
- ⏳ Planning prévisionnel (basé sur la velocity)
- ⏳ Rapport d'audit (activités par utilisateur)
- ⏳ Rapports personnalisables (choisir les métriques)
- ⏳ Envoi automatique par email (scheduled reports)
- ⏳ API pour connecter des outils BI externes (Metabase, Tableau)

### 9.10 Visualisation & Graphiques
- ⏳ Librairie de graphiques (Recharts / Chart.js / D3.js)
- ⏳ Line charts (tendances dans le temps)
- ⏳ Bar charts (comparaisons)
- ⏳ Pie charts (répartitions)
- ⏳ Area charts (cumulative flow)
- ⏳ Heatmaps (activité calendrier)
- ⏳ Radar charts (vue multi-dimensionnelle)
- ⏳ Gauge charts (KPIs avec seuils)
- ⏳ Treemaps (répartition hiérarchique)
- ⏳ Graphiques interactifs (zoom, tooltip, drill-down)
- ⏳ Widgets de dashboard repositionnables (drag & drop)
- ⏳ Filtres dynamiques (date range, équipe, membre)
- ⏳ Thème sombre/clair pour les graphiques
- ⏳ Mode plein écran pour chaque graphique
- ⏳ Comparaison de périodes (cette semaine vs semaine dernière)

---

## 10. Interface Utilisateur

### 10.1 Design System
- ✅ Dark mode / Light mode
- ✅ Thème personnalisable par organisation (couleur)
- ✅ Composants UI réutilisables (Button, Card, Input, Modal)
- ✅ Design glassmorphism
- ✅ Icônes Lucide React
- ✅ Animations subtiles (200-300ms)

### 10.2 Navigation
- ✅ Hub central (liste des organisations)
- ✅ Sidebar avec navigation organisation
- ✅ Sidebar avec navigation personnelle
- ✅ Header avec titre et actions
- ✅ Breadcrumbs implicites
- ⏳ Recherche globale
- ⏳ Raccourcis clavier

### 10.3 Responsive
- 🚧 Design mobile-friendly
- ✅ Sidebar responsive (collapse sur mobile)
- ⏳ PWA (Progressive Web App)
- ⏳ Application mobile native

---

## 11. Notifications & Communication

### 11.1 Système de notifications
- ⏳ Notifications in-app
- ⏳ Notifications email
- ⏳ Notifications push (PWA)
- ⏳ Centre de notifications
- ⏳ Paramètres de notification

### 11.2 Emails automatiques
- ✅ Email d'invitation à une organisation
- ⏳ Email de bienvenue
- ⏳ Email de récupération de mot de passe
- ⏳ Digest hebdomadaire
- ⏳ Rappels d'échéance

### 11.3 Communication interne
- ⏳ Commentaires sur tâches
- ⏳ Mentions (@user)
- ⏳ Chat d'équipe
- ⏳ Messagerie directe

---

## 12. Intégrations

### 12.1 Stockage de fichiers
- ✅ Stockage local (uploads/)
- ⏳ AWS S3
- ⏳ Google Drive
- ⏳ Dropbox

### 12.2 Authentification
- ⏳ OAuth Google
- ⏳ OAuth GitHub
- ⏳ OAuth Microsoft
- ⏳ SSO (SAML)

### 12.3 Outils externes
- ⏳ Slack integration
- ⏳ GitHub integration
- ⏳ Jira sync
- ⏳ Zapier webhooks

---

## 13. Administration & Configuration

### 13.1 Paramètres utilisateur
- ⏳ Préférences de langue
- ⏳ Timezone
- ⏳ Format de date
- ⏳ Thème par défaut

### 13.2 Paramètres organisation
- ✅ Modifier informations de base
- ⏳ Logo personnalisé
- ⏳ Domaine personnalisé
- ⏳ Rôles personnalisés
- ⏳ Permissions granulaires

### 13.3 Audit & Sécurité
- ⏳ Logs d'audit (qui a fait quoi et quand)
- ⏳ Historique des connexions
- ⏳ Sessions actives
- ⏳ Expiration automatique de session
- ⏳ Rate limiting
- ⏳ GDPR compliance (export/suppression de données)

---

## 14. Performance & Optimisation

### 14.1 Backend
- ✅ Pagination des listes
- ✅ Indexes sur colonnes fréquemment requêtées
- ⏳ Cache Redis
- ⏳ Requêtes optimisées (N+1 prevention)
- ⏳ Background jobs (emails, exports)

### 14.2 Frontend
- ✅ Hot Module Replacement (HMR)
- ✅ Code splitting
- ⏳ Lazy loading des routes
- ⏳ Image optimization
- ⏳ Service Worker (offline mode)

---

## 15. Tests & Qualité

### 15.1 Tests Backend
- 🚧 Tests unitaires des services
- 🚧 Tests d'intégration des contrôleurs
- 🚧 Tests de repository
- ⏳ Tests de sécurité
- ⏳ Couverture de code > 70%

### 15.2 Tests Frontend
- ⏳ Tests unitaires des composants
- ⏳ Tests d'intégration
- ⏳ Tests E2E (Playwright/Cypress)

### 15.3 CI/CD
- ⏳ GitHub Actions
- ⏳ Tests automatiques sur PR
- ⏳ Build automatique
- ⏳ Déploiement automatique

---

## 16. Déploiement & DevOps

### 16.1 Conteneurisation
- ✅ Docker Compose (dev)
- ⏳ Dockerfile production
- ⏳ Kubernetes manifests
- ⏳ Helm charts

### 16.2 Hosting
- ⏳ Backend déployé (AWS/GCP/Azure)
- ⏳ Frontend déployé (Vercel/Netlify)
- ⏳ Base de données PostgreSQL (prod)
- ⏳ CDN pour les assets statiques

### 16.3 Monitoring
- ⏳ Logs centralisés (ELK/CloudWatch)
- ⏳ Métriques (Prometheus/Grafana)
- ⏳ Alertes
- ⏳ Health checks

---

## 17. Documentation

### 17.1 Documentation technique
- ✅ README.md
- ✅ CLAUDE.md (contexte du projet)
- ✅ FEATURES.md (cette liste)
- ✅ EMAIL_CONFIG.md (configuration SMTP)
- ⏳ Architecture Decision Records (ADR)
- ⏳ Documentation API (Swagger/OpenAPI)

### 17.2 Documentation utilisateur
- ⏳ Guide de démarrage
- ⏳ Tutoriels vidéo
- ⏳ FAQ
- ⏳ Base de connaissances

---

## Récapitulatif par statut

### ✅ Fonctionnalités complètes (estimé)
- Authentification de base (JWT, login, register)
- CRUD Organisations complètes
- Système d'invitation avec email
- CRUD Équipes
- CRUD Tâches avec Kanban
- CRUD Objectifs
- CRUD Événements
- CRUD Notes
- CRUD Habitudes avec logs
- Documents d'organisation
- UI/UX de base avec dark mode

### 🚧 Fonctionnalités en cours
- Dashboard organisation/personnel
- Interface calendrier
- Suivi visuel des habitudes
- Tests (en partie)

### ⏳ Fonctionnalités prioritaires à venir
1. **Analytics & Dashboards avancés** (KPIs, graphiques, rapports)
2. Analytics tâches (burndown, velocity, cycle time, CFD)
3. Analytics membres (productivité, charge, heatmaps)
4. Analytics habitudes (streaks, heatmap calendrier, tendances)
5. Analytics objectifs (progression, prédictions, risques)
6. Rapports exportables (PDF, CSV, Excel)
7. Changement de rôle des membres
8. ~~Modification du profil utilisateur~~ (DONE)
9. Recherche globale
10. Notifications in-app
11. Tests complets (>70% coverage)
12. CI/CD
13. Déploiement production

---

**Dernière mise à jour:** 29 janvier 2026
**Progression globale:** ~40% complété
