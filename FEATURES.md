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
- ✅ Récupération de mot de passe (email)
  - ✅ POST /api/auth/forgot-password - Envoi d'email avec token
  - ✅ POST /api/auth/reset-password - Réinitialisation du mot de passe
  - ✅ GET /api/auth/reset-password/{token}/valid - Vérification de validité du token
  - ✅ Frontend: ForgotPasswordPage et ResetPasswordPage
  - ✅ Template email HTML
- ✅ Validation email lors de l'inscription
  - ✅ Envoi automatique d'email de vérification à l'inscription
  - ✅ GET /api/auth/verify-email/{token} - Vérification de l'email
  - ✅ POST /api/auth/resend-verification - Renvoi de l'email de vérification
  - ✅ Frontend: VerifyEmailPage
  - ✅ Champ emailVerified sur User (soft requirement - configurable)
- ✅ Authentification à deux facteurs (2FA)
  - ✅ POST /api/auth/2fa/setup - Génération du secret TOTP et QR code
  - ✅ POST /api/auth/2fa/verify - Vérification du code TOTP et activation de la 2FA
  - ✅ DELETE /api/auth/2fa/disable - Désactivation de la 2FA (requiert mot de passe + TOTP)
  - ✅ GET /api/auth/2fa/status - Statut de la 2FA pour l'utilisateur
  - ✅ Login modifié pour gérer la 2FA (retourne requires2FA si activée)
  - ✅ Frontend: SecuritySettingsPage avec configuration 2FA
  - ✅ Frontend: LoginPage avec saisie du code TOTP
  - ✅ Bibliothèque TOTP: dev.samstevens.totp
  - ✅ QR code généré en base64 pour scanning
  - ✅ Tests unitaires complets pour TwoFactorAuthService

### 1.2 Gestion des utilisateurs
- ✅ Profil utilisateur (firstName, lastName, email, description)
- ✅ Endpoint GET /api/auth/me
- ✅ Modification du profil utilisateur
- ✅ Changement de mot de passe
- ✅ Photo de profil
  - ✅ POST /api/users/me/photo - Upload de photo (max 5MB, jpg/png/gif/webp)
  - ✅ DELETE /api/users/me/photo - Suppression de photo
  - ✅ Stockage local dans uploads/profile-photos/
  - ✅ Affichage dans header et liste des membres
  - ✅ Page de parametres profil (frontend)
- ✅ Suppression de compte
  - ✅ DELETE /api/users/me - Suppression avec confirmation mot de passe
  - ✅ Transfert automatique de propriete des organisations
  - ✅ Suppression des organisations sans autres membres
  - ✅ Modal de confirmation avec saisie "SUPPRIMER"

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
- ✅ Changer le rôle d'un membre
- ✅ Transfert de propriété (OWNER)

### 2.3 Documents d'organisation
- ✅ Upload de documents (drag & drop)
- ✅ Liste des documents
- ✅ Téléchargement de documents
- ✅ Suppression de documents
- ✅ Stockage local des fichiers
- ✅ Prévisualisation des documents
  - ✅ Backend: GET /api/documents/{id}/preview - Metadonnees de previsualisation
  - ✅ Backend: GET /api/documents/{id}/preview/content - Contenu inline pour images/PDF
  - ✅ Support images (JPEG, PNG, GIF, WebP, SVG)
  - ✅ Support PDF (affichage inline dans iframe)
  - ✅ Support fichiers texte (txt, md, json, xml, code sources)
  - ✅ Frontend: DocumentPreviewModal avec mode plein ecran
  - ✅ Tests unitaires (14 nouveaux tests)
- ✅ Versioning des documents
  - ✅ Backend: DocumentVersion entity (id, documentId, versionNumber, filePath, uploadedBy, uploadedAt)
  - ✅ API: GET /api/documents/{id}/versions - Liste des versions d'un document
  - ✅ API: POST /api/documents/{id}/versions - Upload nouvelle version
  - ✅ API: GET /api/documents/{id}/versions/{versionNumber}/download - Telecharger une version
  - ✅ Conservation des versions precedentes lors d'upload
  - ✅ Frontend: DocumentVersionHistory component avec historique
  - ✅ Frontend: Upload nouvelle version depuis le modal de previsualisation
  - ✅ Frontend: Telechargement de versions specifiques
  - ✅ Tests unitaires (10 nouveaux tests versioning)
- ✅ Tags/catégories pour documents
  - ✅ Backend: Réutilisation de l'entité Tag existante
  - ✅ Many-to-many: document_tags table
  - ✅ API: GET/POST/PUT/DELETE /api/documents/{id}/tags
  - ✅ Frontend: Tag chips sur les documents
  - ✅ Frontend: Filtrage par tag
  - ✅ Frontend: Modal de gestion des tags
  - ✅ Tests unitaires TagService (12 nouveaux tests)

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
- ✅ Tags/labels
  - ✅ Backend: Tag entity (id, name, color, organizationId)
  - ✅ Many-to-many: task_tags table
  - ✅ API: CRUD for tags, assign/remove tags from tasks
  - ✅ Frontend: Tag chips display, tag selector in task modal
- ✅ Checklist dans une tâche
  - ✅ Backend: ChecklistItem entity (id, taskId, content, completed, position)
  - ✅ API: Add/update/delete/reorder checklist items
  - ✅ Frontend: Checklist component in task detail modal
  - ✅ Progress indicator (X/Y items completed)
- ✅ Pièces jointes
  - ✅ Backend: TaskAttachment entity (id, taskId, fileName, fileUrl, uploadedBy, createdAt)
  - ✅ API: Upload/download/delete attachments (FileStorageService)
  - ✅ Frontend: File upload in task modal (drag & drop), list of attachments
  - ✅ Support for images, PDFs, documents (25MB max)
- ✅ Commentaires sur les taches
  - ✅ CRUD commentaires (creation, modification, suppression)
  - ✅ Reponses imbriquees (threaded comments)
  - ✅ Affichage auteur et date
  - ✅ Indicateur "modifie"
  - ✅ Permissions (auteur ou admin peut supprimer)
- ✅ Historique des modifications
  - ✅ Backend: TaskHistory entity (id, taskId, userId, fieldChanged, oldValue, newValue, changedAt)
  - ✅ API: GET /api/tasks/{id}/history - Historique des modifications avec filtre par champ
  - ✅ Enregistrement automatique des changements (status, priority, assignee, due date, title, description, goal)
  - ✅ Frontend: TaskHistoryTimeline component dans le modal de detail
  - ✅ Affichage chronologique avec avatar utilisateur, champ modifie, ancienne/nouvelle valeur
  - ✅ Filtre par type de champ

### 4.3 Interface utilisateur
- ✅ Vue tableau (Kanban board)
- ✅ Drag & drop entre colonnes
- ✅ Filtres par statut, priorité, assigné
- ✅ Vue liste
  - ✅ Alternative to Kanban board - table/list view
  - ✅ Sortable columns (title, status, priority, due date, assignee, created)
  - ✅ Toggle between Kanban and List view
  - ✅ Filter by tag
- ✅ Vue calendrier
  - ✅ New view option alongside Kanban and List views
  - ✅ Display tasks on calendar based on due date
  - ✅ Monthly/weekly/daily views (reusing EventCalendar components)
  - ✅ Click on task to open detail modal
  - ✅ Color coding by priority or status (toggle switch)
  - ✅ Drag to change due date (week view)
- ✅ Recherche de tâches
  - ✅ Search input in task list view
  - ✅ Search by title, description, tag name
  - ✅ Filter results in real-time

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
- ✅ Objectifs liés à des tâches
  - ✅ Champ goalId sur Task (optionnel, nullable)
  - ✅ Sélecteur d'objectif dans le modal de création/édition de tâche
  - ✅ API: GET /api/goals/{id}/tasks - Liste des tâches liées à un objectif
  - ✅ API: GET /api/goals/{id} - Détails d'un objectif avec compteurs
  - ✅ Affichage du nombre de tâches liées sur les cartes d'objectifs
- ✅ Calcul automatique de progression
  - ✅ Comptage automatique: totalTasks et completedTasks dans GoalResponse
  - ✅ Progression = (tâches DONE / total tâches) * 100
  - ✅ Mise à jour automatique quand le statut d'une tâche change
  - ✅ Barre de progression visuelle sur les cartes d'objectifs

### 5.3 Interface utilisateur
- ✅ Barres de progression
- ✅ Affichage par type (court/moyen/long terme)
- ⏳ Graphiques de progression
- ✅ Notifications à l'approche de la deadline
  - ✅ Backend: @Scheduled job quotidien (8h) pour verifier les echeances
  - ✅ Backend: GoalDeadlineScheduler avec notifications a 7, 3 et 1 jour(s)
  - ✅ Backend: GoalDeadlineNotification entity pour eviter les doublons
  - ✅ Backend: Integration avec NotificationService existant
  - ✅ Frontend: GoalCard component avec badges d'alerte
  - ✅ Frontend: Badges colores selon urgence (rouge/orange/jaune)
  - ✅ Tests unitaires complets (GoalDeadlineSchedulerTest)

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
- ✅ Participants aux événements
  - ✅ EventParticipant entity (eventId, userId, status: INVITED/ACCEPTED/DECLINED)
  - ✅ API pour inviter des utilisateurs aux événements
  - ✅ API pour accepter/refuser une invitation
  - ✅ Liste des participants dans les détails de l'événement
- ✅ Lieu (physique ou lien visio)
- ✅ Rappels/notifications (NONE, 15min, 30min, 1h, 2h, 1 jour, 2 jours, 1 semaine)
- ✅ Evenements recurrents
  - ✅ Backend: RecurrenceType enum (NONE, DAILY, WEEKLY, MONTHLY, YEARLY)
  - ✅ Backend: Event fields (recurrenceType, recurrenceInterval, recurrenceEndDate, parentEventId)
  - ✅ Backend: Occurrence generation algorithm (virtual occurrences)
  - ✅ API: GET /api/events/{id}/occurrences - Get occurrences in time range
  - ✅ API: DELETE /api/events/{id}?deleteAllOccurrences=true/false
  - ✅ API: PUT with updateAllOccurrences option
  - ✅ Frontend: Recurrence options in event creation modal
  - ✅ Frontend: Recurring event icon (Repeat) in calendar views
  - ✅ Frontend: Delete confirmation dialog for recurring events
  - ✅ Unit tests for recurrence logic

### 6.3 Interface calendrier
- ✅ Vue mensuelle
- ✅ Vue hebdomadaire
- ✅ Vue journalière
- ⏳ Intégration Google Calendar / Outlook
- ✅ Export iCal
  - ✅ GET /api/events/{id}/ical - Export d'un événement en .ics
  - ✅ GET /api/users/me/events/ical - Export des événements personnels
  - ✅ GET /api/users/me/all-events/ical - Export de tous les événements

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
- ✅ Éditeur WYSIWYG
  - ✅ TipTap editor avec toolbar complete
  - ✅ Formatage: Gras, Italique, Souligne, Barre
  - ✅ Titres: H1, H2, H3
  - ✅ Listes: a puces, numerotees
  - ✅ Blocs de code avec coloration syntaxique
  - ✅ Liens et images (via URL)
  - ✅ Citations (blockquote)
  - ✅ Ligne horizontale
  - ✅ Mode edition/apercu
  - ✅ Support contenu HTML dans les notes
  - ✅ Retrocompatibilite avec notes texte existantes
- ✅ Collaboration en temps réel
  - ✅ Backend: spring-boot-starter-websocket dependency
  - ✅ Backend: WebSocketConfig avec @EnableWebSocketMessageBroker
  - ✅ Backend: STOMP endpoints (/ws, /topic, /app)
  - ✅ Backend: NoteCollaborationController avec @MessageMapping
    - ✅ /note/join - Utilisateur rejoint l'edition
    - ✅ /note/edit - Envoi des changements en temps reel
    - ✅ /note/cursor - Position du curseur
    - ✅ /note/leave - Utilisateur quitte
    - ✅ /note/typing - Indicateur de frappe
  - ✅ Backend: NoteCollaborationService (session management)
  - ✅ Backend: Domain models (NoteSession, NoteCollaborator, CursorPosition, NoteEditOperation)
  - ✅ Backend: Detection basique des conflits (version-based)
  - ✅ Backend: Broadcast vers /topic/note/{noteId} pour collaborateurs
  - ✅ API REST: GET /api/notes/{id}/collaborators - Liste des collaborateurs
  - ✅ API REST: GET /api/notes/{id}/collaborators/count - Nombre de collaborateurs
  - ✅ API REST: GET /api/notes/{id}/session - Session active
  - ✅ Frontend: @stomp/stompjs et sockjs-client packages
  - ✅ Frontend: useWebSocket hook (connexion WebSocket)
  - ✅ Frontend: useNoteCollaboration hook (joinNote, sendEdit, sendCursor, leaveNote)
  - ✅ Frontend: NoteCollaborators component (avatars, couleurs)
  - ✅ Frontend: CollaborationBadge component (X personnes en ligne)
  - ✅ Frontend: CollaborativeNoteEditor component
  - ✅ Frontend: Indicateur "Utilisateur X est en train d'ecrire..."
  - ✅ Frontend: Reconnexion automatique WebSocket
  - ✅ Tests unitaires NoteCollaborationServiceTest (15 tests)
- ✅ Versioning des notes
  - ✅ Backend: NoteVersion entity (id, noteId, versionNumber, title, content, createdById, createdAt)
  - ✅ Backend: Auto-save version on note update (when content changes)
  - ✅ API: GET /api/notes/{id}/versions - List all versions
  - ✅ API: GET /api/notes/{noteId}/versions/{versionId} - Get specific version
  - ✅ API: POST /api/notes/{id}/restore/{versionId} - Restore to version
  - ✅ Frontend: Version history panel in note view modal
  - ✅ Frontend: Version preview and restore functionality
  - ✅ Tests unitaires NoteVersionService (21 tests)
- ✅ Recherche full-text
  - ✅ GET /api/organizations/{orgId}/notes/search?q=query
  - ✅ Recherche par titre et contenu (case-insensitive)
  - ✅ Frontend: Input de recherche dans la page notes
  - ✅ Recherche debounced (300ms)
  - ✅ Affichage des resultats en temps reel

### 7.3 Organisation
- ✅ Notes séparées des documents d'organisation
- ✅ Upload de fichiers attachés aux notes
- ✅ Dossiers/arborescence
  - ✅ Backend: NoteFolder entity (id, name, parentFolderId, organizationId, createdById)
  - ✅ Many-to-many: note_folders table avec arborescence parent/enfant
  - ✅ API: CRUD for folders, nested folder tree structure
  - ✅ API: PATCH /api/notes/{id}/folder - Move note to folder
  - ✅ Frontend: Folder tree sidebar with expand/collapse
  - ✅ Frontend: Filter notes by folder
  - ✅ Frontend: Create/rename/delete folders
  - ✅ Frontend: Move notes between folders
- ✅ Tags
  - ✅ Backend: NoteTag entity (id, name, color, organizationId)
  - ✅ Many-to-many: note_note_tags join table
  - ✅ API: CRUD for note tags, assign/remove tags from notes
  - ✅ Frontend: Tag chips on note cards
  - ✅ Frontend: Tag filter sidebar
  - ✅ Frontend: Tag selector in note create/edit modal
  - ✅ Frontend: Note tag management modal

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
- ✅ Interface visuelle du tracking
- ✅ Streaks (séries)
- ✅ Statistiques de complétion
- ✅ Graphiques de progression

### 8.3 Récapitulatif personnel
- ✅ Page récap avec vue d'ensemble
- ✅ Objectifs personnels
- ✅ Habitudes du jour
- ✅ Tâches assignées
- ✅ Statistiques de productivité
  - ✅ GET /api/users/me/productivity-stats - Métriques personnelles
  - ✅ Tâches complétées cette semaine/ce mois
  - ✅ Temps moyen de complétion
  - ✅ Série de jours productifs (streak)
  - ✅ Comparaison avec période précédente
  - ✅ Score de productivité (0-100)
  - ✅ Graphique: tâches complétées sur 30 jours
  - ✅ Répartition par priorité
  - ✅ Jour le plus productif
  - ✅ Insights personnalisés
- ✅ Insights & recommandations
  - ✅ Backend: InsightType enum (PRODUCTIVITY_TIP, HABIT_SUGGESTION, GOAL_ALERT, WORKLOAD_WARNING, CELEBRATION, PATTERN_DETECTED)
  - ✅ Backend: Insight domain model (id, type, title, message, priority, actionable, actionUrl, createdAt)
  - ✅ Backend: InsightService avec generation d'insights basee sur:
    - ✅ Patterns de productivite (jour le plus productif)
    - ✅ Streaks d'habitudes (7 jours, 30 jours, a risque)
    - ✅ Progression des objectifs (a risque, presque termine, en retard)
    - ✅ Alertes de charge de travail (semaine chargee, taches en retard)
    - ✅ Celebrations (25, 50, 100 taches ce mois)
    - ✅ Detection de patterns (comparaison jours, constance)
  - ✅ API: GET /api/users/me/insights
  - ✅ Frontend: InsightCard component avec icones et couleurs par type
  - ✅ Frontend: InsightsPanel component avec liste d'insights
  - ✅ Frontend: Integration dans PersonalDashboardPage
  - ✅ Frontend: Insights dismissables (stockage localStorage 24h)
  - ✅ Tests unitaires InsightService (35 tests)

---

## 9. Dashboard & Analytics

### 9.1 Dashboard Organisation
- ✅ Vue d'ensemble de l'organisation
- ✅ Statistiques de base (membres, tâches, objectifs)
- ✅ Activité récente
- ✅ Graphiques de progression
- ✅ KPIs (score de sante, tendances, croissance mensuelle)
- ⏳ Rapports exportables

### 9.2 Dashboard Personnel
- ✅ Vue d'ensemble personnelle
- ✅ Mes tâches du jour
- ✅ Mes habitudes du jour
- ✅ Mes prochains événements
- ✅ Progression de mes objectifs

### 9.3 Analytics - Tâches
- ✅ Nombre de tâches créées par période (jour/semaine/mois)
- ✅ Nombre de tâches complétées par période
- ✅ Taux de complétion des tâches (complétées / total)
- ✅ Temps moyen de complétion d'une tâche (création → DONE)
- ✅ Temps moyen par statut (temps en TODO, temps en IN_PROGRESS)
- ✅ Répartition par priorité (LOW/MEDIUM/HIGH/URGENT) - Pie chart
- ✅ Répartition par statut (TODO/IN_PROGRESS/DONE) - Pie chart
- ✅ Tâches en retard (dépassant la due date)
- ✅ Taux de retard (tâches en retard / tâches avec due date)
- ✅ Burndown chart (tâches restantes vs temps)
- ✅ Burnup chart (tâches complétées cumulées vs scope total)
- ✅ Velocity chart (tâches complétées par sprint/semaine)
- ✅ Throughput chart (tâches terminées par jour avec moyenne mobile 7 jours)
- ✅ Cycle time distribution (histogramme du temps de complétion en 6 buckets)
- ✅ Lead time (temps entre création et complétion avec tendance par semaine)
- ⏳ Tâches bloquées trop longtemps (alertes)
- ⏳ Tendance de création vs complétion (balance de flux)
- ✅ Cumulative flow diagram (CFD)
- ✅ Work in progress (WIP) chart - tâches actives en parallèle avec WIP moyen

### 9.4 Analytics - Membres & Productivité
- ✅ Tâches complétées par membre (classement)
- ✅ Charge de travail par membre (tâches assignées actives)
- ✅ Répartition de la charge (heatmap par membre)
- ✅ Taux de complétion par membre
- ✅ Temps moyen de complétion par membre
- ✅ Historique d'activité par membre (contributions/jour)
- ✅ Score de productivité individuel (tâches pondérées par priorité)
- ✅ Contribution heatmap (style GitHub - grille d'activité annuelle)
  - ✅ Backend: ActivityHeatmapService aggregant taches, objectifs, habitudes
  - ✅ Backend: ActivityHeatmapRepositoryPort et adapter pour requetes DB
  - ✅ Backend: GET /api/users/me/activity-heatmap - 12 mois d'activite
  - ✅ Backend: GET /api/organizations/{orgId}/activity-heatmap - equipe
  - ✅ Backend: GET /api/organizations/{orgId}/members/{memberId}/activity-heatmap
  - ✅ Frontend: ContributionHeatmap component avec grille 52 semaines x 7 jours
  - ✅ Frontend: Intensite de couleur basee sur le niveau d'activite (0-4)
  - ✅ Frontend: Tooltip au survol avec date et nombre de contributions
  - ✅ Frontend: Labels des mois en haut, jours de la semaine a gauche
  - ✅ Frontend: Statistiques (serie actuelle, meilleure serie, jours actifs, moyenne)
  - ✅ Frontend: Integration dans PersonalDashboardPage
  - ✅ Tests unitaires ActivityHeatmapServiceTest (28 tests)
- ✅ Comparaison de performance entre équipes
- ✅ Top performers de la semaine/du mois
- ✅ Indicateur de surcharge (membres avec trop de tâches)
- ✅ Membres inactifs (aucune activité depuis X jours)

### 9.5 Analytics - Objectifs
- ✅ Progression globale des objectifs (agrégée)
- ✅ Taux d'objectifs atteints vs en cours vs échoués
- ✅ Progression par type (SHORT/MEDIUM/LONG)
- ✅ Courbe de progression dans le temps par objectif
- ✅ Objectifs à risque (progression faible + deadline proche)
- ✅ Vélocité de progression (vitesse d'avancement)
- ✅ Prédiction de complétion (estimation basée sur la vélocité)
- ✅ Historique des objectifs complétés par mois
- ✅ Corrélation objectifs ↔ tâches (combien de tâches contribuent)
- ⏳ Score d'alignement stratégique (objectifs liés entre eux)

### 9.6 Analytics - Habitudes & Bien-être
- ✅ Taux de complétion quotidien des habitudes
- ✅ Taux de complétion hebdomadaire
- ✅ Taux de complétion mensuel
- ✅ Streaks actuels (jours consécutifs de complétion)
- ✅ Record de streak par habitude
- ✅ Heatmap calendrier de complétion (style GitHub contributions)
- ✅ Jour de la semaine le plus productif
- ✅ Tendance sur 30/60/90 jours
- ⏳ Score de constance (régularité dans le temps)
- ✅ Habitudes les plus/moins respectées (classement)
- ⏳ Corrélation habitudes ↔ productivité
- ✅ Graphique radar (vue d'ensemble de toutes les habitudes)
  - ✅ RadarChartCard avec completion par habitude
  - ✅ Mode comparaison (taux actuel vs tendance historique)
- ✅ Comparaison semaine actuelle vs semaine précédente
  - ✅ PeriodComparisonPanel avec metriques cles et indicateurs de changement
- ⏳ Alerte de rupture de streak

### 9.7 Analytics - Calendrier & Temps
- ✅ Nombre d'événements par semaine/mois
- ✅ Répartition du temps par type d'événement
- ✅ Heures occupées vs disponibles (taux d'occupation)
- ✅ Jours les plus chargés (heatmap)
- ✅ Créneaux les plus utilisés (distribution horaire)
- ✅ Temps passé en réunions vs travail individuel
- ✅ Tendance du nombre de réunions
- ✅ Conflits d'agenda détectés
- ✅ Score de disponibilité
- ✅ Prévision de charge pour la semaine suivante

### 9.8 Analytics - Organisation (Vue globale)
- ✅ Score de santé de l'organisation (composite)
- ✅ Nombre total de tâches actives
- ✅ Nombre de membres actifs (derniers 7 jours)
- ⏳ Activité globale (événements + tâches + notes + objectifs)
- ⏳ Tendance d'activité sur 12 mois
- ⏳ Répartition de l'activité par équipe
- ✅ Taux de croissance (nouveaux membres/mois)
- ⏳ Score de collaboration (tâches cross-team)
- ⏳ Flux d'activité en temps réel (timeline)
- ⏳ Graphique réseau de collaboration (qui travaille avec qui)
- ⏳ Indicateur de santé par équipe (traffic light system)
- ✅ Comparaison mois par mois (MoM growth)

### 9.9 Rapports & Exports
- ⏳ Rapport hebdomadaire automatique (digest)
- ⏳ Rapport mensuel d'activité
- ✅ Export PDF des dashboards
- ✅ Export CSV des données brutes
- ✅ Export Excel avec graphiques
- ⏳ Rapport de productivité par membre
- ✅ Rapport de progression des objectifs
- ⏳ Planning prévisionnel (basé sur la velocity)
- ⏳ Rapport d'audit (activités par utilisateur)
- ⏳ Rapports personnalisables (choisir les métriques)
- ⏳ Envoi automatique par email (scheduled reports)
- ⏳ API pour connecter des outils BI externes (Metabase, Tableau)

### 9.10 Visualisation & Graphiques
- ✅ Librairie de graphiques (Recharts)
- ✅ Line charts (tendances dans le temps)
- ✅ Bar charts (comparaisons)
- ✅ Pie charts (répartitions)
- ✅ Area charts (cumulative flow)
- ✅ Heatmaps (activité calendrier)
- ✅ Radar charts (vue multi-dimensionnelle)
  - ✅ SimpleRadarChart et ComparisonRadarChart dans Charts.tsx
  - ✅ RadarChartCard component pour vue d'ensemble des habitudes
  - ✅ Mode comparaison avec toggle (taux actuel vs tendance historique)
  - ✅ Integration dans PersonalDashboardPage
- ✅ Gauge charts (KPIs avec seuils)
- ✅ Treemaps (répartition hiérarchique)
  - ✅ SimpleTreemap component avec rendu personnalise des cellules
  - ✅ TreemapCard component pour distribution des taches par membre
  - ✅ Integration dans AnalyticsPage (onglet Membres)
- ✅ Graphiques interactifs (zoom, tooltip, drill-down)
- ⏳ Widgets de dashboard repositionnables (drag & drop)
- ✅ Filtres dynamiques (date range, équipe, membre)
  - ✅ Backend: AnalyticsFilterRequest DTO + applyFilters dans AnalyticsService
  - ✅ Backend: @RequestParam filters sur endpoints task/member/goal analytics
  - ✅ Frontend: DashboardFilters component (date range, status, priority, member)
  - ✅ Frontend: useAnalyticsFilters hook avec persistance URL search params
  - ✅ Frontend: Integration dans AnalyticsPage avec refetch automatique
  - ✅ Tests unitaires backend (16 tests pour les filtres dynamiques)
- ✅ Thème sombre/clair pour les graphiques
- ✅ Mode plein écran pour chaque graphique
  - ✅ ChartFullscreenButton integre dans ChartContainer (prop fullscreenContent)
  - ✅ ChartFullscreenModal standalone pour usage personnalise
  - ✅ Support fermeture par bouton, clic exterieur, et touche Escape
  - ✅ Graphiques en taille large (height 500px) en mode plein ecran
  - ✅ Active sur tous les graphiques de l'AnalyticsPage
- ✅ Comparaison de périodes (cette semaine vs semaine dernière)
  - ✅ PeriodComparisonPanel component avec metriques cles
  - ✅ Indicateurs de changement en pourcentage (hausse/baisse/stable)
  - ✅ Comparaison taches, taux de completion, habitudes, mensuel
  - ✅ PeriodComparisonIndicator reutilisable dans Charts.tsx
  - ✅ Vue expandable avec comparaisons detaillees
  - ✅ Integration dans PersonalDashboardPage

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
- ✅ Recherche globale
- ✅ Raccourcis clavier
  - ✅ Hook useKeyboardShortcuts avec gestion des sequences de touches
  - ✅ Ctrl/Cmd+K pour ouvrir la recherche
  - ✅ ? pour afficher l'aide des raccourcis
  - ✅ Escape pour fermer les modals
  - ✅ G+H pour aller au Hub
  - ✅ G+T pour aller aux Taches
  - ✅ G+C pour aller au Calendrier
  - ✅ G+D pour aller au Dashboard
  - ✅ G+G pour aller aux Objectifs
  - ✅ G+N pour aller aux Notes
  - ✅ G+M pour aller aux Membres
  - ✅ G+A pour aller aux Analytics
  - ✅ G+P pour aller a l'espace personnel
  - ✅ G+S pour aller aux parametres
  - ✅ KeyboardShortcutsModal avec affichage groupe par categorie
  - ✅ Bouton clavier dans le header pour decouvrir les raccourcis

### 10.3 Responsive
- ✅ Design mobile-friendly
  - ✅ Modal: Plein ecran sur mobile, bottom-sheet style, prop compact
  - ✅ Sidebar: Overlay avec transition, plus large sur mobile (w-64), header espace personnel sur mobile
  - ✅ Header: Recherche mobile overlay, boutons icon-only, espacement adaptatif
  - ✅ SpaceCard: Padding, tailles et troncature adaptatifs
  - ✅ HubPage: Header responsive, nom masque sur mobile, salutation adaptative
  - ✅ TaskCard: Padding compact, tags limites, initiales assignee sur mobile
  - ✅ TaskDetailModal: Largeur responsive, boutons pleine largeur, actions empilees
  - ✅ TaskListView: Layout cartes sur mobile (md:hidden), table sur desktop (hidden md:block)
  - ✅ TasksPage: Toolbar responsive, kanban en scroll horizontal sur mobile
  - ✅ CalendarView: Vue jour par defaut sur mobile, controles compacts, dots au lieu de texte
  - ✅ GoalsPage: Header empile, bouton pleine largeur, padding responsive
  - ✅ GoalCard: Boutons d'action toujours visibles sur mobile, cibles tactiles agrandies
  - ✅ ContributionHeatmap: Cellules plus petites, labels jour masques, stats compactes
  - ✅ ProductivityStatsSection: Header empile, jauge adaptative, grilles responsives
- ✅ Sidebar responsive (collapse sur mobile)
- ✅ PWA (Progressive Web App)
  - ✅ Manifest avec metadata application (nom, icones, couleurs)
  - ✅ Service Worker avec strategies de cache (NetworkFirst, CacheFirst, StaleWhileRevalidate)
  - ✅ Icones PWA (192x192, 512x512, maskable)
  - ✅ Meta tags iOS et Android
  - ✅ InstallPWAPrompt component (installation native)
  - ✅ OfflineIndicator component (detection hors-ligne)
  - ✅ PWAUpdateNotification component (mises a jour)
  - ✅ Shortcuts pour Hub et Personal Space
- ⏳ Application mobile native

---

## 11. Notifications & Communication

### 11.1 Système de notifications
- ✅ Notifications in-app
  - ✅ Backend: NotificationService, NotificationController
  - ✅ Types: TASK_ASSIGNED, TASK_COMPLETED, ORGANIZATION_INVITE, ROLE_CHANGED, etc.
  - ✅ Endpoints: GET/POST/DELETE notifications, mark read, count
  - ✅ Frontend: NotificationCenter component in header
  - ✅ Badge de compteur non-lus
  - ✅ Polling automatique (30s)
- ✅ Notifications email
  - ✅ Backend: NotificationPreferences domain model
  - ✅ Backend: NotificationPreferencesRepositoryPort et adapter
  - ✅ Backend: NotificationPreferencesService (get/update preferences)
  - ✅ Backend: sendNotificationEmail method dans EmailService
  - ✅ Backend: Integration avec NotificationService (verification preferences avant envoi)
  - ✅ API: GET/PUT /api/notifications/preferences
  - ✅ Template email HTML responsive avec icones par type
  - ✅ Tests unitaires NotificationPreferencesServiceTest
- ⏳ Notifications push (PWA)
- ✅ Centre de notifications
- ✅ Parametres de notification
  - ✅ Backend: NotificationPreferences entity (emailEnabled, taskAssigned, taskCompleted, mentions, etc.)
  - ✅ Frontend: NotificationPreferencesModal component
  - ✅ Frontend: Bouton Settings dans NotificationCenter
  - ✅ Toggle master pour activer/desactiver les emails
  - ✅ Toggles individuels par type de notification
  - ✅ Sauvegarde automatique des preferences

### 11.2 Emails automatiques
- ✅ Email d'invitation à une organisation
- ✅ Email de vérification d'adresse email
- ✅ Email de récupération de mot de passe
- ✅ Email de bienvenue
  - ✅ Template HTML avec message de bienvenue et fonctionnalites Hubz
  - ✅ Envoi automatique apres inscription reussie
  - ✅ Tests unitaires
- ✅ Digest hebdomadaire
  - ✅ WeeklyDigestService pour generer les statistiques
  - ✅ Template HTML avec taches, objectifs, habitudes, evenements
  - ✅ WeeklyDigestScheduler (@Scheduled chaque lundi a 9h)
  - ✅ Preference digestEnabled dans UserPreferences (default: true)
  - ✅ Toggle dans PreferencesSettingsPage avec apercu du contenu
  - ✅ Tests unitaires complets
- ✅ Rappels d'echeance
  - ✅ DeadlineReminderScheduler: @Scheduled job quotidien a 8h
  - ✅ DeadlineReminderService: generation des rappels par type (taches, objectifs, evenements)
  - ✅ Template HTML groupe par urgence (Aujourd'hui, Cette semaine, Semaine prochaine)
  - ✅ Preference reminderEnabled dans UserPreferences (default: true)
  - ✅ Preference reminderFrequency (ONE_DAY, THREE_DAYS, ONE_WEEK)
  - ✅ Toggle et selecteur de frequence dans PreferencesSettingsPage
  - ✅ Tests unitaires complets (DeadlineReminderServiceTest, DeadlineReminderSchedulerTest)

### 11.3 Communication interne
- ✅ Commentaires sur taches
- ✅ Mentions (@user)
  - ✅ Backend: MentionService pour parser et resoudre les @mentions
  - ✅ Backend: Notification automatique aux utilisateurs mentionnes (type MENTION)
  - ✅ API: GET /api/organizations/{orgId}/mentions/users pour autocomplete
  - ✅ Frontend: MentionInput avec autocomplete dropdown
  - ✅ Frontend: MentionText pour afficher les mentions en surbrillance
  - ✅ Support @prenom, @nom, ou @prenom.nom
- ✅ Chat d'équipe
  - ✅ Backend: Message domain model (id, teamId, userId, content, deleted, createdAt, editedAt)
  - ✅ Backend: MessageRepositoryPort + MessageRepositoryAdapter + MessageJpaRepository
  - ✅ Backend: TeamChatService (send, get paginated, edit, delete, count)
  - ✅ Backend: Soft delete (marque supprime, remplace contenu)
  - ✅ Backend: Permissions (auteur peut edit/delete, admin peut delete)
  - ✅ API: POST /api/teams/{teamId}/messages - Envoyer un message
  - ✅ API: GET /api/teams/{teamId}/messages?page=0&size=50 - Messages pagines
  - ✅ API: PUT /api/teams/{teamId}/messages/{messageId} - Modifier un message
  - ✅ API: DELETE /api/teams/{teamId}/messages/{messageId} - Supprimer un message
  - ✅ API: GET /api/teams/{teamId}/messages/count - Nombre de messages
  - ✅ Frontend: TeamChatPanel component avec panel lateral fixe
  - ✅ Frontend: Liste de messages avec regroupement par date (Aujourd'hui/Hier/date)
  - ✅ Frontend: Auto-scroll vers le bas, bouton scroll-to-bottom flottant
  - ✅ Frontend: Edition inline et suppression avec confirmation
  - ✅ Frontend: Chargement de messages plus anciens (pagination)
  - ✅ Frontend: Polling toutes les 5 secondes pour nouveaux messages
  - ✅ Frontend: Integration dans TeamsPage (bouton chat par equipe)
  - ✅ Tests unitaires TeamChatServiceTest (22 tests)
  - ✅ Tests controleur TeamChatControllerTest (11 tests)
- ✅ Messagerie directe
  - ✅ Backend: DirectMessage domain model (id, senderId, receiverId, content, read, deleted, createdAt, editedAt)
  - ✅ Backend: DirectMessageRepositoryPort + DirectMessageRepositoryAdapter + DirectMessageJpaRepository
  - ✅ Backend: DirectMessageService (send, getConversation paginated, edit, delete, markAsRead, getConversations, getUnreadCount)
  - ✅ Backend: Soft delete (marque supprime, remplace contenu)
  - ✅ Backend: Permissions (auteur peut edit/delete, seul le destinataire peut marquer comme lu)
  - ✅ Backend: Notification automatique au destinataire (type DIRECT_MESSAGE)
  - ✅ API: POST /api/messages - Envoyer un message
  - ✅ API: GET /api/messages/conversations - Liste des conversations
  - ✅ API: GET /api/messages/conversation/{userId}?page=0&size=50 - Messages pagines
  - ✅ API: PUT /api/messages/{messageId} - Modifier un message
  - ✅ API: DELETE /api/messages/{messageId} - Supprimer un message
  - ✅ API: POST /api/messages/{messageId}/read - Marquer comme lu
  - ✅ API: POST /api/messages/conversation/{userId}/read - Marquer conversation comme lue
  - ✅ API: GET /api/messages/unread/count - Nombre de messages non lus
  - ✅ Frontend: DirectMessagesPage avec liste conversations a gauche et messages a droite
  - ✅ Frontend: Bulles de messages avec indicateurs lu/non-lu (check/double-check)
  - ✅ Frontend: Auto-scroll vers le bas, bouton scroll-to-bottom flottant
  - ✅ Frontend: Edition inline et suppression de messages
  - ✅ Frontend: Chargement de messages plus anciens (pagination)
  - ✅ Frontend: Polling toutes les 5 secondes pour nouveaux messages
  - ✅ Frontend: Badge unread count sur les conversations
  - ✅ Frontend: Lien depuis la page Membres (bouton Message sur chaque membre)
  - ✅ Frontend: Navigation Messages dans la sidebar espace personnel
  - ✅ Tests unitaires DirectMessageServiceTest (24 tests)

### 11.4 Agent Conversationnel (Chatbot IA)
- ✅ Interface Chat
  - ✅ Bouton flottant en bas a droite (icone robot violet)
  - ✅ Panel coulissant avec mode expand/collapse
  - ✅ Historique de conversation avec localStorage (50 messages max)
  - ✅ Auto-scroll vers le dernier message
  - ✅ Indicateur de frappe (typing indicator avec animation)
  - ✅ Exemples de commandes affichees quand historique vide
  - ✅ Bouton effacer l'historique
- ✅ NLP (Natural Language Processing)
  - ✅ Backend: ChatbotService avec parseMessage() (regex-based NLP)
  - ✅ Backend: ChatbotIntent enum (CREATE_TASK, CREATE_EVENT, CREATE_GOAL, CREATE_NOTE, QUERY_TASKS, QUERY_STATS, UNKNOWN)
  - ✅ Backend: ChatbotParsedMessage domain model
  - ✅ Detection d'intentions via patterns regex
  - ✅ Extraction d'entites:
    - ✅ Dates ("le 18", "demain", "aujourd'hui", "dans X jours", "lundi/mardi/...", "prochain")
    - ✅ Heures ("a 13h", "a 14h30", "midi", "matin", "soir", "apres-midi")
    - ✅ Titres (apres deux-points ou entre guillemets)
    - ✅ Priorites ("urgent", "importante", "pas urgent", "basse priorite")
  - ✅ Exemples de messages supportes:
    - "J'ai un rdv demain a 14h avec le client"
    - "Creer une tache urgente: finir le rapport"
    - "Note: idee pour le projet"
    - "Objectif: courir 3 fois cette semaine"
    - "Quelles sont mes taches?"
    - "Mes statistiques"
- ✅ Reponses intelligentes
  - ✅ Confirmation textuelle avec entites extraites
  - ✅ Boutons d'action rapide (quick actions)
  - ✅ Lien "Voir le resultat" vers la ressource creee
  - ✅ Indicateur de succes/erreur
  - ✅ Resume des resultats de requete (count)
- ✅ Commandes avancees
  - ✅ "Quelles sont mes taches pour aujourd'hui?" (QUERY_TASKS)
  - ✅ "Mes statistiques" / "Ma productivite" (QUERY_STATS)
- ✅ Historique des conversations
  - ✅ Sauvegarde automatique dans localStorage
  - ✅ Limite a 50 messages (rotation)
  - ✅ Persistance entre sessions
- ✅ API Endpoint
  - ✅ POST /api/chatbot/message - Traiter un message
  - ✅ Request: { message: string, organizationId?: string }
  - ✅ Response: { intent, extractedEntities, confirmationText, actionUrl, actionExecuted, createdResourceId, quickActions, queryResults }
- ✅ Tests unitaires ChatbotServiceTest (48 tests)
- ✅ Integration Ollama LLM (Intelligence Artificielle locale)
  - ✅ Backend: OllamaPort interface (application layer)
  - ✅ Backend: OllamaAdapter implementation (infrastructure layer)
  - ✅ Backend: ConversationHistory domain model (10 messages max par utilisateur)
  - ✅ Backend: Configuration Ollama dans application.yml (url, model, timeout, enabled)
  - ✅ Backend: Fallback automatique vers regex si Ollama non disponible
  - ✅ Backend: Cache de disponibilite Ollama (60 secondes)
  - ✅ Backend: Prompt systeme en francais avec exemples
  - ✅ Backend: Parsing JSON des reponses Ollama avec extraction d'entites
  - ✅ API: POST /api/chatbot/message - Response contient usedOllama et ollamaModel
  - ✅ Frontend: Badge "AI" dans l'en-tete du chat quand Ollama est actif
  - ✅ Frontend: Indicateur "Powered by AI (llama3.1)" sous le titre
  - ✅ Frontend: Badge Sparkles sur les messages traites par Ollama
  - ✅ Tests unitaires OllamaAdapterTest (11 tests)
  - ✅ Tests unitaires ChatbotServiceTest Ollama (12 tests supplementaires)
- ✅ Mode contextuel
  - ✅ Historique de conversation stocke en memoire (10 messages max)
  - ✅ Contexte envoye a Ollama pour comprendre le fil de discussion
  - ✅ Methode clearConversationHistory pour reset
- ⏳ Ameliorations futures
  - Reconnaissance vocale (Web Speech API)
  - Feedback vocal (synthese vocale)
  - Mode mains-libres (activation par mot-cle)

### 11.5 Assistant Vocal & IA
- ⏳ Reconnaissance vocale
  - Web Speech API pour capture audio
  - Bouton micro dans header
  - Transcription en temps réel
  - Support multi-langues (FR, EN)
  - Réutilise le même backend que le chatbot textuel
- ⏳ Feedback vocal (optionnel)
  - Synthèse vocale pour confirmer l'action
  - "J'ai créé un événement le 18 janvier à 13h"
- ⏳ Mode mains-libres
  - Activation par mot-clé ("Hey Hubz")
  - Écoute continue
  - Feedback sonore

---

## 12. Intégrations

### 12.1 Stockage de fichiers
- ✅ Stockage local (uploads/)
- ⏳ AWS S3
- ⏳ Google Drive
- ⏳ Dropbox

### 12.2 Authentification
- ✅ OAuth Google
  - ✅ Backend: spring-boot-starter-oauth2-client dependency
  - ✅ Backend: GoogleOAuth2Port interface (application layer)
  - ✅ Backend: GoogleOAuth2Adapter (infrastructure layer - token exchange + user info)
  - ✅ Backend: OAuth2Service (create/login user with Google account)
  - ✅ Backend: OAuth2Controller (GET /api/auth/oauth2/google, GET /api/auth/oauth2/google/callback)
  - ✅ Backend: User domain model + UserEntity with oauthProvider/oauthProviderId fields
  - ✅ Backend: OAuth2AuthenticationException + GlobalExceptionHandler
  - ✅ Backend: Configuration OAuth2 Google dans application.yml (client-id, client-secret, redirect-uri)
  - ✅ Backend: Liaison compte Google a un utilisateur existant (meme email)
  - ✅ Backend: Email de bienvenue pour nouveaux utilisateurs OAuth
  - ✅ Frontend: Bouton "Se connecter avec Google" sur LoginPage (icone Google officielle)
  - ✅ Frontend: OAuthCallbackPage pour gerer le token apres redirection
  - ✅ Frontend: Route /oauth/callback dans App.tsx
  - ✅ Frontend: Gestion des erreurs OAuth avec redirection vers login
  - ✅ Tests unitaires OAuth2ServiceTest (16 tests)
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
- ✅ Préférences de langue (FR, EN)
- ✅ Timezone (fuseaux horaires communs)
- ✅ Format de date (DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD)
- ✅ Thème par défaut (Systeme, Clair, Sombre)
  - ✅ Backend: UserPreferences entity, repository, service
  - ✅ API: GET/PUT /api/users/me/preferences
  - ✅ Frontend: PreferencesSettingsPage
  - ✅ Zustand store pour persistance locale
  - ✅ Application automatique du theme

### 13.2 Paramètres organisation
- ✅ Modifier informations de base
- ✅ Logo personnalisé
  - ✅ POST /api/organizations/{id}/logo - Upload de logo (max 5MB, jpg/png/gif/webp)
  - ✅ DELETE /api/organizations/{id}/logo - Suppression de logo
  - ✅ Stockage local dans uploads/organization-logos/
  - ✅ Affichage dans sidebar, hub page, space cards
  - ✅ Page de parametres organisation (frontend)
  - ✅ Tests unitaires (12 nouveaux tests)
- ⏳ Domaine personnalisé
- ⏳ Rôles personnalisés
- ⏳ Permissions granulaires

### 13.3 Audit & Sécurité
- ⏳ Logs d'audit (qui a fait quoi et quand)
- ⏳ Historique des connexions
- ⏳ Sessions actives
- ⏳ Expiration automatique de session
- ✅ Rate limiting
  - ✅ Bucket4j + Caffeine cache en mémoire
  - ✅ Auth endpoints: 5 requests/minute (login, register, forgot-password)
  - ✅ API endpoints: 100 requests/minute par utilisateur authentifié
  - ✅ Public endpoints: 20 requests/minute par IP
  - ✅ Headers de réponse: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
  - ✅ HTTP 429 Too Many Requests avec Retry-After header
  - ✅ Exclusions: /actuator/**, /swagger-ui/**, /api-docs/**, /ws/**
  - ✅ Tests unitaires (19 tests)
- ⏳ GDPR compliance (export/suppression de données)

---

## 14. Performance & Optimisation

### 14.1 Backend
- ✅ Pagination des listes
- ✅ Indexes sur colonnes fréquemment requêtées
- ⏳ Cache Redis
- ⏳ Requêtes optimisées (N+1 prevention)
- ✅ Background jobs (emails, exports)
  - ✅ BackgroundJob domain model (id, type, status, payload, retryCount, error, createdAt, executedAt)
  - ✅ JobType enum: EMAIL_SEND, REPORT_EXPORT, WEBHOOK_CALL, DATA_CLEANUP
  - ✅ JobStatus enum: PENDING, RUNNING, COMPLETED, FAILED
  - ✅ BackgroundJobRepositoryPort + BackgroundJobRepositoryAdapter
  - ✅ BackgroundJobEntity avec indexes (status, type, createdAt)
  - ✅ BackgroundJobService: scheduleJob, executeJob, retryFailedJobs, cleanupOldJobs, processPendingJobs
  - ✅ JobExecutor interface avec 4 implementations:
    - ✅ EmailJobExecutor - envoi emails async via payload JSON
    - ✅ ReportExportJobExecutor - generation rapports async
    - ✅ WebhookJobExecutor - appels HTTP webhook avec timeout
    - ✅ DataCleanupJobExecutor - nettoyage DB (vieux jobs, notifications, tokens)
  - ✅ BackgroundJobScheduler: @Scheduled toutes les minutes pour jobs pending, retry toutes les 15min, cleanup quotidien a 3h
  - ✅ API admin: GET /api/admin/jobs, GET /api/admin/jobs/{id}, POST /api/admin/jobs/{id}/retry, POST /api/admin/jobs/retry-all, POST /api/admin/jobs/cleanup
  - ✅ Retry automatique (max 3 tentatives) avec resetForRetry
  - ✅ Nettoyage automatique des jobs > 30 jours
  - ✅ Tests unitaires BackgroundJobServiceTest (10 tests)
  - ✅ Tests unitaires EmailJobExecutorTest (5 tests)

### 14.2 Frontend
- ✅ Hot Module Replacement (HMR)
- ✅ Code splitting
- ✅ Lazy loading des routes
  - ✅ React.lazy() pour tous les composants de page
  - ✅ Suspense avec LoadingPage comme fallback
  - ✅ ErrorBoundary pour gestion des erreurs de chargement
  - ✅ Chunks groupes: auth, hub, organization, personal, settings
  - ✅ Vendor chunks: react, ui, forms, charts, editor, utils
  - ✅ Configuration Vite optimisee pour le cache
- ⏳ Image optimization
- ✅ Service Worker (offline mode)
  - ✅ Custom sw.js with intelligent caching strategies
  - ✅ NetworkFirst for API calls (with cache fallback)
  - ✅ CacheFirst for static assets (JS, CSS, images, fonts)
  - ✅ StaleWhileRevalidate for HTML pages
  - ✅ Offline fallback page (offline.html)
  - ✅ Cache versioning and automatic cleanup
  - ✅ Skip waiting for immediate updates
  - ✅ Background sync support (extensible)
  - ✅ Improved OfflineIndicator with retry button
  - ✅ Improved PWAUpdateNotification with auto-refresh countdown

---

## 15. Tests & Qualité

### 15.1 Tests Backend
- ✅ Tests unitaires des services (1018 tests, 44% couverture globale)
  - ✅ AuthService: 82.6%
  - ✅ UserService: 93.1%
  - ✅ OrganizationService: 97.8%
  - ✅ TeamService: 98.4%
  - ✅ TaskService: 84.7%
  - ✅ GoalService: 100%
  - ✅ EventService: 66.6%
  - ✅ HabitService: 100%
  - ✅ NoteService: 100%
  - ✅ NotificationService: 94.9%
  - ✅ NoteAttachmentService: 87.1%
  - ✅ OrganizationDocumentService: 100%
  - ✅ TwoFactorAuthService: 94.1%
- ✅ Tests d'intégration des contrôleurs
- ✅ Tests de repository
- ⏳ Tests de sécurité
- 🚧 Couverture de code > 70% (actuellement ~44%, services > 80%)

### 15.2 Tests Frontend
- 🚧 Tests unitaires des composants
  - ✅ Infrastructure Vitest configuree (vitest, jsdom, @testing-library/react, @testing-library/jest-dom, @testing-library/user-event)
  - ✅ setupTests.ts avec mocks globaux (matchMedia, IntersectionObserver, ResizeObserver)
  - ✅ vite.config.ts configure avec test environment jsdom et coverage v8
  - ✅ Scripts: test, test:ui, test:coverage, test:run
  - ✅ Tests composants UI: Button (16 tests), Card (7 tests), Input (15 tests), Modal (10 tests), LoadingPage (5 tests)
  - ✅ Tests composants features: SpaceCard (10 tests)
  - ✅ Tests stores Zustand: authStore (7 tests), preferencesStore (17 tests), formatDate (6 tests), getTranslation (2 tests)
  - ✅ Tests services API: auth.service (13 tests), organization.service (12 tests)
  - ✅ Tests utilitaires: cn/utils (11 tests)
  - Total: ~131 tests frontend
- ⏳ Tests d'intégration
- ✅ Tests E2E (Playwright)
  - ✅ @playwright/test installe avec support multi-navigateurs
  - ✅ Configuration playwright.config.ts (chromium, firefox, webkit)
  - ✅ Scripts npm: test:e2e, test:e2e:ui, test:e2e:headed, test:e2e:debug
  - ✅ Page Object Model pattern implemente
    - ✅ BasePage: Fonctionnalites communes (navigation, wait, toast)
    - ✅ LoginPage: Formulaire login, 2FA, navigation
    - ✅ RegisterPage: Formulaire inscription, validation
    - ✅ HubPage: Liste organisations, creation, navigation
    - ✅ OrganizationPage: Sidebar, navigation sections, settings
    - ✅ TasksPage: Kanban, list view, CRUD taches, drag-drop
  - ✅ Tests d'authentification (auth.spec.ts)
    - ✅ Inscription utilisateur
    - ✅ Connexion avec credentials valides/invalides
    - ✅ Deconnexion
    - ✅ Protection des routes
    - ✅ Persistence de session
  - ✅ Tests des organisations (organizations.spec.ts)
    - ✅ Creation d'organisation
    - ✅ Navigation vers organisation
    - ✅ Mise a jour nom/description
    - ✅ Suppression avec confirmation
  - ✅ Tests des taches (tasks.spec.ts)
    - ✅ Creation de tache (titre, priorite, date echeance)
    - ✅ Vue Kanban/Liste/Calendrier
    - ✅ Modification de tache
    - ✅ Changement de statut
    - ✅ Drag-and-drop entre colonnes
    - ✅ Suppression de tache
    - ✅ Recherche de taches
  - ✅ Tests de navigation (navigation.spec.ts)
    - ✅ Hub vers organisation
    - ✅ Hub vers espace personnel
    - ✅ Navigation sidebar organisation
    - ✅ Navigation sidebar espace personnel
    - ✅ Boutons back/forward navigateur
    - ✅ Deep linking
    - ✅ Raccourcis clavier (Ctrl+K, Escape)
  - ✅ Fixtures et helpers (fixtures.ts)
    - ✅ Page objects pre-configures
    - ✅ Generateurs de donnees de test
    - ✅ API helpers pour setup/teardown
  - ✅ Auth setup automatique (auth.setup.ts)
  - ✅ GitHub Actions (e2e-tests.yml)
    - ✅ Tests paralleles sur chromium, firefox, webkit
    - ✅ Artifacts: rapports, screenshots, videos
    - ✅ Summary job avec statut par navigateur
  - Total: ~50 tests E2E couvrant les parcours critiques

### 15.3 CI/CD
- ✅ GitHub Actions
  - ✅ backend-tests.yml: Java 21 setup, Maven cache, tests + JaCoCo coverage, Codecov upload
  - ✅ frontend-tests.yml: Node.js 20 setup, npm cache, Vitest + coverage, Codecov upload
  - ✅ e2e-tests.yml: Tests Playwright multi-navigateurs (chromium, firefox, webkit)
    - ✅ Backend + Frontend demarres automatiquement
    - ✅ Tests paralleles par navigateur
    - ✅ Upload rapports et screenshots on failure
    - ✅ Summary job avec statut global
  - ✅ build.yml: Backend JAR build, Frontend dist build, Docker image build verification
  - ✅ deploy.yml: Triggered on version tags (v*.*.*), Docker push to GHCR, deployment placeholder
  - ✅ pr-checks.yml: Clean architecture validation, ESLint, TypeScript type check, security scan, PR size check
  - ✅ Dependabot: Auto-updates for Maven, npm, and GitHub Actions dependencies (weekly)
  - ✅ README badges: Build status, test status, Codecov coverage
- ✅ Tests automatiques sur PR
- ✅ Build automatique
- ✅ Déploiement automatique (pipeline pret, cible a configurer)

---

## 16. Déploiement & DevOps

### 16.1 Conteneurisation
- ✅ Docker Compose (dev)
- ✅ Dockerfile production
  - ✅ Backend: Multi-stage build (Maven + JRE Alpine), non-root user, healthcheck, JVM tuning
  - ✅ Frontend: Multi-stage build (Node + Nginx Alpine), healthcheck
  - ✅ Nginx: Gzip compression, cache headers, security headers, SPA fallback
  - ✅ docker-compose.prod.yml: PostgreSQL, Redis, volumes, networks, resource limits
  - ✅ .dockerignore pour backend et frontend
  - ✅ .env.example avec variables d'environnement
  - ✅ README-DEPLOY.md avec instructions completes
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
- ✅ Documentation API (Swagger/OpenAPI)
  - ✅ springdoc-openapi-starter-webmvc-ui (v2.3.0)
  - ✅ OpenApiConfig avec @OpenAPIDefinition et JWT security scheme
  - ✅ Tags pour grouper les endpoints (Authentication, Organizations, Tasks, etc.)
  - ✅ Annotations @Operation, @ApiResponse, @Parameter sur les controleurs
  - ✅ AuthController documente (8 endpoints)
  - ✅ OrganizationController documente (11 endpoints)
  - ✅ TaskController documente (6 endpoints)
  - ✅ UserController documente (5 endpoints)
  - ✅ GoalController documente (9 endpoints)
  - ✅ EventController documente (14 endpoints)
  - ✅ NoteController documente (10 endpoints)
  - ✅ Swagger UI accessible a /swagger-ui.html (dev uniquement)
  - ✅ OpenAPI spec accessible a /api-docs
  - ✅ Desactive en production (application-prod.yml)

### 17.2 Documentation utilisateur
- ⏳ Guide de démarrage
- ⏳ Tutoriels vidéo
- ⏳ FAQ
- ⏳ Base de connaissances

---

## Récapitulatif par statut

### ✅ Fonctionnalités complètes (estimé)
- Authentification de base (JWT, login, register, password reset, email verification)
- CRUD Organisations complètes
- Système d'invitation avec email
- Gestion des roles de membres et transfert de propriete
- Recherche globale (organisations, taches, objectifs, evenements, notes, membres)
- Notifications in-app (backend + frontend, centre de notifications)
- Preferences de notification et notifications email
- Chat d'equipe (messages, edition, suppression, pagination, polling)
- CRUD Équipes
- CRUD Tâches avec Kanban
- CRUD Objectifs
- CRUD Événements avec participants, lieu et rappels
- Export iCal des événements
- CRUD Notes
- CRUD Habitudes avec logs et interface visuelle de tracking
- Documents d'organisation
- UI/UX de base avec dark mode
- Analytics & Dashboards avances (KPIs, graphiques)
- Analytics taches (burndown, burnup, velocity, throughput, cycle time, lead time, WIP, CFD)
- Analytics membres (productivite, charge, top performers, completion time, inactive members, team comparison, workload heatmap)
- Analytics habitudes (streaks, heatmap, tendances)
- Analytics objectifs (progression, predictions, risques)
- Dashboard personnel complet (taches, habitudes, evenements, objectifs)
- Photo de profil (upload, suppression, affichage)
- Suppression de compte avec transfert de propriete
- Design mobile responsive (14 composants adaptes avec Tailwind responsive classes)
- CI/CD GitHub Actions (tests, build, deploy, PR checks, Dependabot)
- Messagerie directe (envoi, edition, suppression, conversations, badges unread, polling 5s)
- Collaboration temps reel sur les notes (WebSocket, STOMP, sessions, curseurs, conflits)

### 🚧 Fonctionnalités en cours
- Interface calendrier
- Tests unitaires des services (1042+ tests backend, couverture services > 80%)
- Tests unitaires frontend (~131 tests, infrastructure Vitest configuree)

### ⏳ Fonctionnalités prioritaires à venir
1. ~~Analytics & Dashboards avancés~~ (DONE)
2. ~~Analytics tâches (burndown, velocity, cycle time, CFD)~~ (DONE)
3. ~~Analytics membres (productivité, charge, heatmaps)~~ (DONE)
4. ~~Analytics habitudes (streaks, heatmap calendrier, tendances)~~ (DONE)
5. ~~Analytics objectifs (progression, prédictions, risques)~~ (DONE)
6. ~~Rapports exportables (PDF, CSV, Excel)~~ (DONE)
7. ~~Changement de rôle des membres~~ (DONE)
8. ~~Modification du profil utilisateur~~ (DONE)
9. ~~Recherche globale~~ (DONE)
10. ~~Notifications in-app~~ (DONE)
11. ~~Tests complets~~ (EN COURS - 1003 tests, services > 80% couverture)
12. ~~CI/CD~~ (DONE)
13. Déploiement production

---

**Derniere mise a jour:** 08 fevrier 2026 - Integration Ollama LLM pour chatbot IA (59 tests chatbot+ollama)
**Progression globale:** ~75% complete
