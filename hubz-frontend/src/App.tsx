import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { useAuthStore } from './stores/authStore';
import ProtectedRoute from './components/auth/ProtectedRoute';
import { KeyboardShortcutsProvider } from './components/providers/KeyboardShortcutsProvider';
import LoadingPage from './components/ui/LoadingPage';
import ErrorBoundary from './components/ui/ErrorBoundary';
import InstallPWAPrompt from './components/features/InstallPWAPrompt';
import OfflineIndicator from './components/features/OfflineIndicator';
import PWAUpdateNotification from './components/features/PWAUpdateNotification';

// Landing page (critical path - no lazy load)
import LandingPage from './pages/LandingPage';

// Auth pages - auth.chunk.js
const LoginPage = lazy(() => import(/* webpackChunkName: "auth" */ './pages/auth/LoginPage'));
const RegisterPage = lazy(() => import(/* webpackChunkName: "auth" */ './pages/auth/RegisterPage'));
const ForgotPasswordPage = lazy(() => import(/* webpackChunkName: "auth" */ './pages/auth/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import(/* webpackChunkName: "auth" */ './pages/auth/ResetPasswordPage'));
const VerifyEmailPage = lazy(() => import(/* webpackChunkName: "auth" */ './pages/auth/VerifyEmailPage'));
const OAuthCallbackPage = lazy(() => import(/* webpackChunkName: "auth" */ './pages/auth/OAuthCallbackPage'));

// Hub pages - hub.chunk.js
const HubPage = lazy(() => import(/* webpackChunkName: "hub" */ './pages/hub/HubPage'));
const JoinOrganizationPage = lazy(() => import(/* webpackChunkName: "hub" */ './pages/JoinOrganizationPage'));

// Organization pages - organization.chunk.js
const OrganizationLayout = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/OrganizationLayout'));
const DashboardPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/DashboardPage'));
const TasksPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/TasksPage'));
const TeamsPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/TeamsPage'));
const MembersPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/MembersPage'));
const GoalsPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/GoalsPage'));
const CalendarPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/CalendarPage'));
const NotesPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/NotesPage'));
const AnalyticsPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/AnalyticsPage'));
const OrganizationSettingsPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/SettingsPage'));
const WebhookIntegrationsPage = lazy(() => import(/* webpackChunkName: "organization" */ './pages/organization/WebhookIntegrationsPage'));

// Personal pages - personal.chunk.js
const PersonalLayout = lazy(() => import(/* webpackChunkName: "personal" */ './pages/personal/PersonalLayout'));
const HabitsPage = lazy(() => import(/* webpackChunkName: "personal" */ './pages/personal/HabitsPage'));
const PersonalGoalsPage = lazy(() => import(/* webpackChunkName: "personal" */ './pages/personal/PersonalGoalsPage'));
const PersonalCalendarPage = lazy(() => import(/* webpackChunkName: "personal" */ './pages/personal/PersonalCalendarPage'));
const PersonalDashboardPage = lazy(() => import(/* webpackChunkName: "personal" */ './pages/personal/PersonalDashboardPage'));
const DirectMessagesPage = lazy(() => import(/* webpackChunkName: "personal" */ './pages/personal/DirectMessagesPage'));

// Settings pages - settings.chunk.js
const ProfileSettingsPage = lazy(() => import(/* webpackChunkName: "settings" */ './pages/personal/ProfileSettingsPage'));
const SecuritySettingsPage = lazy(() => import(/* webpackChunkName: "settings" */ './pages/personal/SecuritySettingsPage'));
const PreferencesSettingsPage = lazy(() => import(/* webpackChunkName: "settings" */ './pages/personal/PreferencesSettingsPage'));

function App() {
  useEffect(() => {
    const token = useAuthStore.getState().token;
    if (token) {
      useAuthStore.getState().validateSession();
    }
  }, []);

  return (
    <>
      <Toaster position="top-right" />
      <OfflineIndicator />
      <InstallPWAPrompt />
      <PWAUpdateNotification />
      <BrowserRouter>
        <KeyboardShortcutsProvider>
          <ErrorBoundary>
            <Suspense fallback={<LoadingPage />}>
              <Routes>
                {/* Landing page - not lazy loaded for faster initial render */}
                <Route path="/" element={<LandingPage />} />

                {/* Auth routes */}
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
                <Route path="/verify-email/:token" element={<VerifyEmailPage />} />
                <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

                {/* Hub routes */}
                <Route path="/join/:token" element={<JoinOrganizationPage />} />
                <Route
                  path="/hub"
                  element={
                    <ProtectedRoute>
                      <HubPage />
                    </ProtectedRoute>
                  }
                />

                {/* Organization space */}
                <Route
                  path="/organization/:orgId"
                  element={
                    <ProtectedRoute>
                      <OrganizationLayout />
                    </ProtectedRoute>
                  }
                >
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

                {/* Personal space */}
                <Route
                  path="/personal"
                  element={
                    <ProtectedRoute>
                      <PersonalLayout />
                    </ProtectedRoute>
                  }
                >
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
          </ErrorBoundary>
        </KeyboardShortcutsProvider>
      </BrowserRouter>
    </>
  );
}

export default App;
