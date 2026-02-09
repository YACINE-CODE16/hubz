import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import ProtectedRoute from './components/auth/ProtectedRoute';

// Landing page (critical path - no lazy load)
import LandingPage from './pages/LandingPage';

// Simple loading fallback
function Loading() {
  return (
    <div className="min-h-screen bg-[#0A0A0F] flex items-center justify-center">
      <div className="w-8 h-8 border-2 border-accent border-t-transparent rounded-full animate-spin" />
    </div>
  );
}

// Auth pages
const LoginPage = lazy(() => import('./pages/auth/LoginPage'));
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('./pages/auth/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('./pages/auth/ResetPasswordPage'));
const VerifyEmailPage = lazy(() => import('./pages/auth/VerifyEmailPage'));
const OAuthCallbackPage = lazy(() => import('./pages/auth/OAuthCallbackPage'));

// Hub pages
const HubPage = lazy(() => import('./pages/hub/HubPage'));
const JoinOrganizationPage = lazy(() => import('./pages/JoinOrganizationPage'));

// Organization pages
const OrganizationLayout = lazy(() => import('./pages/organization/OrganizationLayout'));
const DashboardPage = lazy(() => import('./pages/organization/DashboardPage'));
const TasksPage = lazy(() => import('./pages/organization/TasksPage'));
const TeamsPage = lazy(() => import('./pages/organization/TeamsPage'));
const MembersPage = lazy(() => import('./pages/organization/MembersPage'));
const GoalsPage = lazy(() => import('./pages/organization/GoalsPage'));
const CalendarPage = lazy(() => import('./pages/organization/CalendarPage'));
const NotesPage = lazy(() => import('./pages/organization/NotesPage'));
const AnalyticsPage = lazy(() => import('./pages/organization/AnalyticsPage'));
const OrganizationSettingsPage = lazy(() => import('./pages/organization/SettingsPage'));
const WebhookIntegrationsPage = lazy(() => import('./pages/organization/WebhookIntegrationsPage'));

// Personal pages
const PersonalLayout = lazy(() => import('./pages/personal/PersonalLayout'));
const HabitsPage = lazy(() => import('./pages/personal/HabitsPage'));
const PersonalGoalsPage = lazy(() => import('./pages/personal/PersonalGoalsPage'));
const PersonalCalendarPage = lazy(() => import('./pages/personal/PersonalCalendarPage'));
const PersonalDashboardPage = lazy(() => import('./pages/personal/PersonalDashboardPage'));
const DirectMessagesPage = lazy(() => import('./pages/personal/DirectMessagesPage'));

// Settings pages
const ProfileSettingsPage = lazy(() => import('./pages/personal/ProfileSettingsPage'));
const SecuritySettingsPage = lazy(() => import('./pages/personal/SecuritySettingsPage'));
const PreferencesSettingsPage = lazy(() => import('./pages/personal/PreferencesSettingsPage'));

function App() {
  return (
    <>
      <Toaster position="top-right" />
      <BrowserRouter>
        <Suspense fallback={<Loading />}>
          <Routes>
            <Route path="/" element={<LandingPage />} />

            {/* Auth */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
            <Route path="/verify-email/:token" element={<VerifyEmailPage />} />
            <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

            {/* Hub */}
            <Route path="/join/:token" element={<JoinOrganizationPage />} />
            <Route path="/hub" element={<ProtectedRoute><HubPage /></ProtectedRoute>} />

            {/* Organization */}
            <Route path="/organization/:orgId" element={<ProtectedRoute><OrganizationLayout /></ProtectedRoute>}>
              <Route index element={<Navigate to="dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="tasks" element={<TasksPage />} />
              <Route path="teams" element={<TeamsPage />} />
              <Route path="members" element={<MembersPage />} />
              <Route path="goals" element={<GoalsPage />} />
              <Route path="calendar" element={<CalendarPage />} />
              <Route path="notes" element={<NotesPage />} />
              <Route path="analytics" element={<AnalyticsPage />} />
              <Route path="settings" element={<OrganizationSettingsPage />} />
              <Route path="webhooks" element={<WebhookIntegrationsPage />} />
            </Route>

            {/* Personal */}
            <Route path="/personal" element={<ProtectedRoute><PersonalLayout /></ProtectedRoute>}>
              <Route index element={<Navigate to="dashboard" replace />} />
              <Route path="dashboard" element={<PersonalDashboardPage />} />
              <Route path="calendar" element={<PersonalCalendarPage />} />
              <Route path="goals" element={<PersonalGoalsPage />} />
              <Route path="habits" element={<HabitsPage />} />
              <Route path="messages" element={<DirectMessagesPage />} />
              <Route path="settings" element={<ProfileSettingsPage />} />
              <Route path="security" element={<SecuritySettingsPage />} />
              <Route path="preferences" element={<PreferencesSettingsPage />} />
            </Route>
          </Routes>
        </Suspense>
      </BrowserRouter>
    </>
  );
}

export default App;
