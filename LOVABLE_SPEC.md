# Hubz - Spécification complète pour Lovable

## 📋 Vue d'ensemble

**Hubz** est une application de productivité moderne permettant de gérer plusieurs organisations, équipes, tâches, objectifs et habitudes personnelles.

**Type:** Application web full-stack (SPA React + API REST Spring Boot)
**Utilisateurs cibles:** Équipes, organisations, individus
**Objectif:** Centraliser la gestion de projets, tâches, objectifs et productivité personnelle

---

## 🎨 Design System

### Palette de couleurs

**Mode Sombre (par défaut):**
```css
--dark-base: #0A0A0F
--dark-card: #12121A
--dark-hover: #1A1A24
--dark-border: #2A2A34
```

**Mode Clair:**
```css
--light-base: #F8FAFC
--light-card: #FFFFFF
--light-hover: #F1F5F9
--light-border: #E2E8F0
```

**Couleurs d'accent:**
```css
--primary: #3B82F6 (bleu)
--success: #22C55E (vert)
--warning: #F59E0B (orange)
--error: #EF4444 (rouge)
--info: #8B5CF6 (violet)
```

### Typographie

- **Font:** Inter
- **H1:** 32px / Bold
- **H2:** 24px / Semibold
- **H3:** 18px / Semibold
- **Body:** 16px / Regular
- **Small:** 14px / Medium

### Style des composants

- **Border radius:** 12px (rounded-xl)
- **Glassmorphism:** backdrop-blur avec transparence
- **Ombres:** Douces, subtiles
- **Animations:** 200-300ms, ease-in-out
- **Icons:** Lucide React (outline style)

---

## 🏗️ Architecture Backend

### URL de l'API
- **Dev:** `http://localhost:8085/api`
- **Prod:** `https://[backend-url]/api`

### Authentification
- **Type:** JWT Bearer Token
- **Header:** `Authorization: Bearer <token>`
- **Expiration:** 24 heures
- **Stockage:** localStorage (clé: `auth-token`)

### Format des réponses

**Succès:**
```json
{
  "id": "uuid",
  "field1": "value",
  ...
}
```

**Erreur:**
```json
{
  "timestamp": "2026-02-08T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Description de l'erreur",
  "path": "/api/endpoint"
}
```

---

## 📡 API Endpoints

### Authentication

```typescript
POST /api/auth/register
Body: {
  email: string,
  password: string,
  firstName: string,
  lastName: string
}
Response: AuthResponse

POST /api/auth/login
Body: {
  email: string,
  password: string
}
Response: AuthResponse

POST /api/auth/oauth2/google
Body: {
  token: string
}
Response: AuthResponse

GET /api/auth/me
Response: UserResponse

POST /api/auth/refresh
Response: AuthResponse
```

### Users

```typescript
GET /api/users/me
Response: UserResponse

PUT /api/users/me
Body: {
  firstName?: string,
  lastName?: string,
  description?: string,
  photoUrl?: string
}
Response: UserResponse

POST /api/users/me/upload-photo
Body: FormData (file: File)
Response: { photoUrl: string }

DELETE /api/users/me
Response: void
```

### Organizations

```typescript
GET /api/organizations
Response: OrganizationResponse[]

POST /api/organizations
Body: {
  name: string,
  description?: string,
  icon?: string,
  color?: string
}
Response: OrganizationResponse

GET /api/organizations/{id}
Response: OrganizationResponse

PUT /api/organizations/{id}
Body: {
  name?: string,
  description?: string,
  icon?: string,
  color?: string,
  readme?: string
}
Response: OrganizationResponse

DELETE /api/organizations/{id}
Response: void

GET /api/organizations/{id}/members
Response: MemberResponse[]

POST /api/organizations/{id}/members
Body: {
  userId: string,
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'
}
Response: MemberResponse

PUT /api/organizations/{id}/members/{userId}
Body: {
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'
}
Response: MemberResponse

DELETE /api/organizations/{id}/members/{userId}
Response: void
```

### Organization Invitations

```typescript
POST /api/organizations/{orgId}/invitations
Body: {
  email: string,
  role: 'ADMIN' | 'MEMBER' | 'VIEWER'
}
Response: OrganizationInvitationResponse

GET /api/organizations/{orgId}/invitations
Response: OrganizationInvitationResponse[]

GET /api/invitations/{token}/info
Response: OrganizationInvitationResponse

POST /api/invitations/{token}/accept
Response: OrganizationResponse

DELETE /api/invitations/{id}
Response: void
```

### Tasks

```typescript
GET /api/organizations/{orgId}/tasks
Query: ?status=TODO&assigneeId=uuid&priority=HIGH&search=text
Response: TaskResponse[]

POST /api/organizations/{orgId}/tasks
Body: {
  title: string,
  description?: string,
  status: 'TODO' | 'IN_PROGRESS' | 'DONE',
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT',
  dueDate?: string,
  assigneeId?: string,
  teamId?: string,
  tags?: string[]
}
Response: TaskResponse

GET /api/tasks/{id}
Response: TaskResponse

PUT /api/tasks/{id}
Body: Partial<CreateTaskRequest>
Response: TaskResponse

PATCH /api/tasks/{id}/status
Body: {
  status: 'TODO' | 'IN_PROGRESS' | 'DONE'
}
Response: TaskResponse

DELETE /api/tasks/{id}
Response: void

GET /api/users/me/tasks
Response: TaskResponse[]
```

### Task Comments

```typescript
GET /api/tasks/{taskId}/comments
Response: TaskCommentResponse[]

POST /api/tasks/{taskId}/comments
Body: {
  content: string,
  parentId?: string
}
Response: TaskCommentResponse

PUT /api/tasks/{taskId}/comments/{commentId}
Body: {
  content: string
}
Response: TaskCommentResponse

DELETE /api/tasks/{taskId}/comments/{commentId}
Response: void
```

### Goals

```typescript
GET /api/organizations/{orgId}/goals
Response: GoalResponse[]

POST /api/organizations/{orgId}/goals
Body: {
  title: string,
  type: 'SHORT' | 'MEDIUM' | 'LONG',
  targetValue: number,
  currentValue?: number,
  deadline?: string
}
Response: GoalResponse

GET /api/users/me/goals
Response: GoalResponse[]

POST /api/users/me/goals
Body: CreateGoalRequest
Response: GoalResponse

PUT /api/goals/{id}
Body: Partial<CreateGoalRequest>
Response: GoalResponse

PATCH /api/goals/{id}/progress
Body: {
  currentValue: number
}
Response: GoalResponse

DELETE /api/goals/{id}
Response: void
```

### Events

```typescript
GET /api/organizations/{orgId}/events
Query: ?startDate=2026-02-01&endDate=2026-02-28
Response: EventResponse[]

POST /api/organizations/{orgId}/events
Body: {
  title: string,
  description?: string,
  startTime: string,
  endTime: string,
  objective?: string,
  recurrence?: {
    type: 'DAILY' | 'WEEKLY' | 'MONTHLY',
    endDate?: string
  }
}
Response: EventResponse

GET /api/users/me/events
Query: ?startDate=2026-02-01&endDate=2026-02-28
Response: EventResponse[]

POST /api/users/me/events
Body: CreateEventRequest
Response: EventResponse

PUT /api/events/{id}
Body: Partial<CreateEventRequest>
Response: EventResponse

DELETE /api/events/{id}
Response: void

POST /api/events/{id}/participants/invite
Body: {
  email: string
}
Response: EventParticipantResponse

POST /api/events/{id}/participants/respond
Body: {
  status: 'ACCEPTED' | 'DECLINED' | 'TENTATIVE'
}
Response: EventParticipantResponse
```

### Notes

```typescript
GET /api/organizations/{orgId}/notes
Query: ?category=meeting&search=text&folderId=uuid
Response: NoteResponse[]

POST /api/organizations/{orgId}/notes
Body: {
  title: string,
  content: string,
  category?: string,
  folderId?: string,
  tags?: string[]
}
Response: NoteResponse

GET /api/notes/{id}
Response: NoteResponse

PUT /api/notes/{id}
Body: Partial<CreateNoteRequest>
Response: NoteResponse

DELETE /api/notes/{id}
Response: void

GET /api/notes/{id}/versions
Response: NoteVersionResponse[]
```

### Notifications

```typescript
GET /api/notifications
Query: ?read=false&limit=50
Response: NotificationResponse[]

PATCH /api/notifications/{id}/read
Response: NotificationResponse

PATCH /api/notifications/read-all
Response: void

DELETE /api/notifications/{id}
Response: void
```

### Search

```typescript
GET /api/search
Query: ?q=keyword&type=tasks,notes,events&organizationId=uuid
Response: SearchResultsResponse
```

### Analytics

```typescript
GET /api/analytics/personal-dashboard
Response: PersonalDashboardResponse

GET /api/analytics/productivity-stats
Query: ?startDate=2026-02-01&endDate=2026-02-28
Response: ProductivityStatsResponse

GET /api/analytics/activity-heatmap
Query: ?year=2026
Response: ActivityHeatmapResponse

GET /api/analytics/calendar
Query: ?startDate=2026-02-01&endDate=2026-02-28
Response: CalendarAnalyticsResponse
```

### Chatbot

```typescript
POST /api/chatbot/message
Body: {
  message: string,
  organizationId?: string
}
Response: {
  intent: string,
  response: string,
  extractedEntities: object,
  quickActions?: string[],
  actionExecuted?: boolean,
  createdResourceId?: string,
  usedOllama?: boolean,
  ollamaModel?: string
}
```

---

## 📦 Types TypeScript

### Auth Types

```typescript
interface AuthResponse {
  token: string;
  user: UserResponse;
}

interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  description?: string;
  photoUrl?: string;
  createdAt: string;
  updatedAt: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}
```

### Organization Types

```typescript
interface OrganizationResponse {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  readme?: string;
  ownerId: string;
  createdAt: string;
}

interface MemberResponse {
  id: string;
  userId: string;
  userEmail: string;
  userFirstName: string;
  userLastName: string;
  userPhotoUrl?: string;
  organizationId: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';
  joinedAt: string;
}

interface OrganizationInvitationResponse {
  id: string;
  organizationId: string;
  organizationName: string;
  email: string;
  role: 'ADMIN' | 'MEMBER' | 'VIEWER';
  token: string;
  createdBy: string;
  createdByName: string;
  createdAt: string;
  expiresAt: string;
  used: boolean;
  acceptedBy?: string;
  acceptedAt?: string;
}
```

### Task Types

```typescript
interface TaskResponse {
  id: string;
  title: string;
  description?: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  dueDate?: string;
  organizationId: string;
  teamId?: string;
  assigneeId?: string;
  assigneeName?: string;
  assigneePhotoUrl?: string;
  createdById: string;
  createdByName: string;
  tags?: TagResponse[];
  attachments?: TaskAttachmentResponse[];
  checklist?: ChecklistItemResponse[];
  createdAt: string;
  updatedAt: string;
}

interface TaskCommentResponse {
  id: string;
  content: string;
  taskId: string;
  userId: string;
  userName: string;
  userPhotoUrl?: string;
  parentId?: string;
  replies?: TaskCommentResponse[];
  createdAt: string;
  updatedAt: string;
}

interface TagResponse {
  id: string;
  name: string;
  color: string;
}

interface TaskAttachmentResponse {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  fileType: string;
  uploadedBy: string;
  uploadedByName: string;
  uploadedAt: string;
}

interface ChecklistItemResponse {
  id: string;
  title: string;
  completed: boolean;
  order: number;
}
```

### Goal Types

```typescript
interface GoalResponse {
  id: string;
  title: string;
  type: 'SHORT' | 'MEDIUM' | 'LONG';
  targetValue: number;
  currentValue: number;
  deadline?: string;
  organizationId?: string;
  userId: string;
  createdAt: string;
}
```

### Event Types

```typescript
interface EventResponse {
  id: string;
  title: string;
  description?: string;
  startTime: string;
  endTime: string;
  objective?: string;
  recurrence?: {
    type: 'DAILY' | 'WEEKLY' | 'MONTHLY';
    endDate?: string;
  };
  organizationId?: string;
  userId: string;
  participants?: EventParticipantResponse[];
  createdAt: string;
}

interface EventParticipantResponse {
  id: string;
  eventId: string;
  userId: string;
  userEmail: string;
  userName: string;
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'TENTATIVE';
  invitedAt: string;
  respondedAt?: string;
}
```

### Note Types

```typescript
interface NoteResponse {
  id: string;
  title: string;
  content: string;
  category?: string;
  organizationId: string;
  folderId?: string;
  tags?: NoteTagResponse[];
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

interface NoteTagResponse {
  id: string;
  name: string;
  color: string;
}

interface NoteVersionResponse {
  id: string;
  noteId: string;
  content: string;
  createdAt: string;
  createdBy: string;
  createdByName: string;
}
```

### Analytics Types

```typescript
interface PersonalDashboardResponse {
  upcomingTasks: TaskResponse[];
  recentActivity: ActivityResponse[];
  goalProgress: GoalProgressResponse[];
  todayEvents: EventResponse[];
  insights: InsightResponse[];
}

interface ProductivityStatsResponse {
  tasksCompleted: number;
  tasksCreated: number;
  averageCompletionTime: number;
  goalCompletionRate: number;
  mostProductiveDay: string;
  mostProductiveHour: number;
}

interface ActivityHeatmapResponse {
  date: string;
  count: number;
}[]

interface CalendarAnalyticsResponse {
  busyHours: number[];
  eventTypes: { type: string; count: number }[];
  collaborationScore: number;
}
```

### Notification Types

```typescript
interface NotificationResponse {
  id: string;
  type: 'TASK_ASSIGNED' | 'TASK_COMMENT' | 'GOAL_DEADLINE' | 'EVENT_INVITATION' | 'ORGANIZATION_INVITATION' | 'MENTION';
  title: string;
  message: string;
  data?: object;
  read: boolean;
  userId: string;
  createdAt: string;
}
```

---

## 🗺️ Structure des pages

### Pages publiques (non authentifié)
1. **Login Page** (`/login`)
   - Formulaire email/password
   - Bouton "Se connecter avec Google"
   - Lien "Mot de passe oublié"
   - Lien vers Register

2. **Register Page** (`/register`)
   - Formulaire: email, password, firstName, lastName
   - Lien vers Login

3. **Forgot Password** (`/forgot-password`)
   - Formulaire email
   - Email envoyé confirmation

4. **Reset Password** (`/reset-password/:token`)
   - Nouveau mot de passe
   - Confirmation

5. **Email Verification** (`/verify-email/:token`)
   - Message de vérification
   - Redirection auto

6. **Accept Invitation** (`/invitations/:token`)
   - Affichage des détails de l'invitation
   - Bouton "Accepter" (nécessite login/register)

### Pages protégées (authentifié)

#### Hub (Home)
7. **Hub Page** (`/hub`)
   - Liste des organisations (cartes)
   - Bouton "Créer une organisation"
   - Lien vers espace personnel
   - Notifications récentes
   - Quick actions

#### Organisation
8. **Organization Layout** (`/organizations/:orgId`)
   - Sidebar avec navigation:
     - Vue d'ensemble
     - Tâches
     - Objectifs
     - Calendrier
     - Notes
     - Documents
     - Membres
     - Paramètres

9. **Organization Overview** (`/organizations/:orgId`)
   - Statistiques clés
   - Activité récente
   - Membres actifs
   - Tâches en cours

10. **Tasks Page** (`/organizations/:orgId/tasks`)
    - Vue Kanban (par défaut): colonnes TODO, IN_PROGRESS, DONE
    - Vue Liste
    - Vue Calendrier
    - Filtres: statut, priorité, assigné à, tags
    - Recherche
    - Bouton "Créer une tâche"
    - Drag & drop entre colonnes

11. **Task Detail Modal**
    - Titre, description
    - Statut, priorité, échéance
    - Assigné à
    - Tags
    - Checklist
    - Commentaires (avec replies)
    - Pièces jointes
    - Historique des modifications

12. **Goals Page** (`/organizations/:orgId/goals`)
    - Liste des objectifs
    - Barre de progression
    - Type (Court/Moyen/Long terme)
    - Deadline
    - Bouton "Créer un objectif"

13. **Calendar Page** (`/organizations/:orgId/calendar`)
    - Vue mensuelle/hebdomadaire/journalière
    - Événements
    - Tâches avec échéance
    - Bouton "Créer un événement"
    - Filtres
    - Export iCal

14. **Notes Page** (`/organizations/:orgId/notes`)
    - Liste/Grille de notes
    - Éditeur WYSIWYG (Tiptap)
    - Catégories
    - Tags
    - Recherche
    - Versionning

15. **Documents Page** (`/organizations/:orgId/documents`)
    - Upload de fichiers
    - Preview
    - Versionning
    - Tags
    - Recherche

16. **Members Page** (`/organizations/:orgId/members`)
    - Liste des membres avec rôle
    - Bouton "Inviter un membre"
    - Gestion des rôles (si admin/owner)
    - Liste des invitations en attente

17. **Organization Settings** (`/organizations/:orgId/settings`)
    - Nom, description, icon, couleur
    - README (markdown)
    - Transfert de propriété
    - Supprimer l'organisation

#### Espace Personnel
18. **Personal Dashboard** (`/personal`)
    - Vue d'ensemble personnelle
    - Tâches assignées (toutes organisations)
    - Objectifs personnels
    - Événements du jour
    - Insights IA
    - Heatmap d'activité

19. **Personal Tasks** (`/personal/tasks`)
    - Toutes mes tâches (multi-organisations)
    - Filtres avancés
    - Tri personnalisé

20. **Personal Goals** (`/personal/goals`)
    - Objectifs personnels uniquement
    - Progress tracking
    - Analytics

21. **Personal Calendar** (`/personal/calendar`)
    - Tous mes événements
    - Tâches avec échéance
    - Vue d'ensemble

22. **Personal Analytics** (`/personal/analytics`)
    - Statistiques de productivité
    - Graphiques de performance
    - Heatmap d'activité
    - Insights IA

#### Paramètres utilisateur
23. **Profile Settings** (`/settings/profile`)
    - Photo de profil
    - Nom, prénom, description
    - Email (non modifiable)

24. **Security Settings** (`/settings/security`)
    - Changer le mot de passe
    - Authentification à deux facteurs (2FA)
    - Sessions actives

25. **Preferences Settings** (`/settings/preferences`)
    - Langue (FR/EN)
    - Thème (dark/light)
    - Format de date
    - Notifications préférences
    - Weekly digest

26. **Account Settings** (`/settings/account`)
    - Exporter mes données
    - Supprimer mon compte

---

## 🎭 Composants UI nécessaires

### Layout
- `Header`: Logo, navigation, search, notifications, user menu
- `Sidebar`: Navigation contextuelle (org/personal)
- `SpaceLayout`: Layout pour organisations
- `Footer`: Copyright, liens

### Navigation
- `Breadcrumbs`: Fil d'Ariane
- `Tabs`: Onglets de navigation
- `NavLink`: Lien de navigation avec état actif

### Forms
- `Input`: Champ texte, email, password
- `Textarea`: Zone de texte multi-ligne
- `Select`: Menu déroulant
- `Checkbox`: Case à cocher
- `Radio`: Bouton radio
- `DatePicker`: Sélecteur de date
- `TimePicker`: Sélecteur d'heure
- `FileUpload`: Upload de fichier avec drag & drop
- `FormField`: Wrapper avec label et erreur

### Data Display
- `Card`: Carte avec header/body/footer
- `SpaceCard`: Carte d'organisation
- `TaskCard`: Carte de tâche
- `GoalCard`: Carte d'objectif
- `Table`: Tableau avec tri/filtres
- `Badge`: Badge/chip
- `Avatar`: Photo de profil
- `Tag`: Tag coloré
- `ProgressBar`: Barre de progression
- `Heatmap`: Contribution heatmap

### Feedback
- `Modal`: Modale avec overlay
- `ConfirmDialog`: Dialogue de confirmation
- `Toast`: Notification temporaire
- `Tooltip`: Info-bulle
- `LoadingSpinner`: Indicateur de chargement
- `Skeleton`: Placeholder de chargement
- `EmptyState`: État vide avec illustration

### Interactive
- `Button`: Bouton avec variantes
- `IconButton`: Bouton icône
- `Dropdown`: Menu déroulant
- `ContextMenu`: Menu contextuel
- `CommandPalette`: Palette de commandes (Ctrl+K)
- `KanbanBoard`: Tableau Kanban avec drag & drop
- `Calendar`: Calendrier avec événements
- `WysiwygEditor`: Éditeur de texte riche
- `ChatbotPanel`: Panel chatbot IA

### Charts (Recharts)
- `LineChart`: Graphique linéaire
- `BarChart`: Graphique en barres
- `PieChart`: Graphique circulaire
- `RadarChart`: Graphique radar
- `AreaChart`: Graphique en aires

---

## 🔄 Flux utilisateur clés

### Flux d'authentification
1. Utilisateur non connecté → `/login`
2. Login réussi → Redirection vers `/hub`
3. Première connexion → Onboarding optionnel
4. Logout → Retour à `/login`

### Flux de création d'organisation
1. Hub → Bouton "Créer une organisation"
2. Modal avec formulaire (nom, description, icon, couleur)
3. Submit → Création
4. Redirection vers `/organizations/{newOrgId}`

### Flux de gestion de tâches
1. Organization Tasks → Vue Kanban
2. Clic sur tâche → Modal de détail
3. Modifier statut → Drag & drop ou dropdown
4. Assigner → Sélecteur de membre
5. Ajouter commentaire → Formulaire dans modal
6. Fermer → Mise à jour en temps réel

### Flux d'invitation
1. Members page → "Inviter un membre"
2. Formulaire: email, rôle
3. Email envoyé → Notification
4. Invité clique sur lien → `/invitations/{token}`
5. Si non connecté → Register/Login
6. Accept → Ajout à l'organisation

### Flux de recherche globale
1. Ctrl+K ou clic sur search bar
2. Command palette s'ouvre
3. Taper requête
4. Résultats groupés par type (tasks, notes, events)
5. Clic → Redirection vers la ressource

---

## 🔌 WebSocket (temps réel)

**Endpoint:** `ws://localhost:8085/ws`

**Events à écouter:**
- `notification`: Nouvelle notification
- `task.updated`: Tâche modifiée
- `task.comment.new`: Nouveau commentaire
- `organization.member.joined`: Nouveau membre

**Format:**
```typescript
{
  type: 'notification' | 'task.updated' | ...,
  payload: object
}
```

---

## 🌐 Internationalisation (i18n)

**Langues supportées:**
- Français (par défaut)
- Anglais

**Clés de traduction:**
- Auth: `auth.login`, `auth.register`, `auth.logout`, ...
- Tasks: `tasks.title`, `tasks.create`, `tasks.status.todo`, ...
- Organizations: `organizations.create`, `organizations.members`, ...
- Common: `common.save`, `common.cancel`, `common.delete`, ...

---

## 📱 Responsive Design

**Breakpoints:**
- Mobile: < 640px
- Tablet: 640px - 1024px
- Desktop: > 1024px

**Adaptations mobiles:**
- Sidebar → Drawer
- Tables → Cards verticales
- Kanban → Liste
- Multi-colonnes → Simple colonne

---

## ♿ Accessibilité

- **ARIA labels** sur tous les boutons/liens
- **Focus visible** sur tous les éléments interactifs
- **Contraste minimum** WCAG AA (4.5:1)
- **Navigation clavier** complète
- **Screen reader** compatible

---

## 🔐 Sécurité Frontend

- **Pas de données sensibles** dans localStorage (sauf token)
- **XSS protection:** Sanitize user input (DOMPurify)
- **CSRF protection:** Backend gère
- **Content Security Policy** configurée
- **HTTPS uniquement** en production

---

## ⚡ Performance

- **Code splitting:** React.lazy()
- **Lazy loading:** Images, routes
- **Memoization:** React.memo, useMemo
- **Virtual scrolling:** Pour grandes listes
- **Debouncing:** Recherche, auto-save
- **Service Worker:** Cache, offline mode
- **Optimistic updates:** UI réactive

---

## 🧪 Tests requis

- **Unit tests** (Vitest): Composants, hooks, utils
- **Integration tests**: Flux complets
- **E2E tests** (Playwright): Parcours utilisateur critiques
- **Coverage minimum:** 70%

---

## 🚀 Déploiement

**Platform:** Vercel
**Build command:** `npm run build`
**Output:** `dist/`
**Environment variables:**
```
VITE_API_URL=https://backend-url
```

---

## 📝 Notes importantes

1. **Utiliser TailwindCSS** pour TOUT le styling
2. **Zustand** pour l'état global (authStore, preferencesStore)
3. **React Query** pour le cache des données API
4. **React Hook Form** pour tous les formulaires
5. **Zod** pour la validation
6. **Lucide React** pour les icônes
7. **date-fns** pour la manipulation de dates
8. **Recharts** pour les graphiques
9. **Framer Motion** pour les animations complexes
10. **DOMPurify** pour sanitize le HTML

---

## 🎯 Priorités de développement

### Phase 1 (MVP)
1. Auth (login/register)
2. Hub
3. Organization CRUD
4. Tasks (Kanban)
5. Members

### Phase 2
6. Goals
7. Calendar/Events
8. Notes
9. Search

### Phase 3
10. Analytics
11. Chatbot
12. Notifications temps réel
13. Settings avancés

---

**Version:** 1.0.0
**Date:** 08 février 2026
**Contact:** [Votre email]
