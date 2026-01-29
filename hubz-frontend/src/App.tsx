import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { useAuthStore } from './stores/authStore';
import ProtectedRoute from './components/auth/ProtectedRoute';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import HubPage from './pages/hub/HubPage';
import OrganizationLayout from './pages/organization/OrganizationLayout';
import DashboardPage from './pages/organization/DashboardPage';
import TasksPage from './pages/organization/TasksPage';
import TeamsPage from './pages/organization/TeamsPage';
import MembersPage from './pages/organization/MembersPage';
import GoalsPage from './pages/organization/GoalsPage';
import CalendarPage from './pages/organization/CalendarPage';
import NotesPage from './pages/organization/NotesPage';
import PersonalLayout from './pages/personal/PersonalLayout';
import HabitsPage from './pages/personal/HabitsPage';
import PersonalGoalsPage from './pages/personal/PersonalGoalsPage';
import PersonalCalendarPage from './pages/personal/PersonalCalendarPage';
import PersonalRecapPage from './pages/personal/PersonalRecapPage';
import JoinOrganizationPage from './pages/JoinOrganizationPage';

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
      <BrowserRouter>
        <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
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
          <Route index element={<Navigate to="recap" replace />} />
          <Route path="recap" element={<PersonalRecapPage />} />
          <Route path="calendar" element={<PersonalCalendarPage />} />
          <Route path="goals" element={<PersonalGoalsPage />} />
          <Route path="habits" element={<HabitsPage />} />
        </Route>
        </Routes>
      </BrowserRouter>
    </>
  );
}

export default App;
