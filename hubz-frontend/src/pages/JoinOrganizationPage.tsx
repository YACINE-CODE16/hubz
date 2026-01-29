import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { UserPlus, CheckCircle, XCircle, Clock } from 'lucide-react';
import toast from 'react-hot-toast';
import { invitationService } from '../services/invitation.service';
import { useAuth } from '../hooks/useAuth';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';
import type { InvitationInfo } from '../types/invitation';

export default function JoinOrganizationPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();
  const [invitation, setInvitation] = useState<InvitationInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setError('Invalid invitation link');
      setLoading(false);
      return;
    }

    fetchInvitationInfo();
  }, [token]);

  const fetchInvitationInfo = async () => {
    if (!token) return;

    try {
      const data = await invitationService.getInvitationInfo(token);
      setInvitation(data);
    } catch (error: any) {
      setError(error.response?.data?.error || 'Invalid or expired invitation');
    } finally {
      setLoading(false);
    }
  };

  const handleAcceptInvitation = async () => {
    if (!token) return;

    setAccepting(true);
    try {
      await invitationService.acceptInvitation(token);
      toast.success('Invitation acceptée! Bienvenue dans l\'organisation.');

      // Redirect to organization after 1 second
      setTimeout(() => {
        navigate(`/organization/${invitation?.organizationId}/tasks`);
      }, 1000);
    } catch (error: any) {
      toast.error(error.response?.data?.error || 'Erreur lors de l\'acceptation de l\'invitation');
    } finally {
      setAccepting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-light-base dark:bg-dark-base flex items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-light-base dark:bg-dark-base flex items-center justify-center p-6">
        <Card className="max-w-md w-full p-8">
          <div className="flex flex-col items-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/30">
              <XCircle className="h-8 w-8 text-red-600 dark:text-red-400" />
            </div>
            <h2 className="mt-4 text-xl font-semibold text-gray-900 dark:text-gray-100">
              Invitation invalide
            </h2>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              {error}
            </p>
            <Button onClick={() => navigate('/hub')} className="mt-6">
              Retour à l'accueil
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  if (!invitation) return null;

  if (invitation.used) {
    return (
      <div className="min-h-screen bg-light-base dark:bg-dark-base flex items-center justify-center p-6">
        <Card className="max-w-md w-full p-8">
          <div className="flex flex-col items-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
              <CheckCircle className="h-8 w-8 text-gray-400" />
            </div>
            <h2 className="mt-4 text-xl font-semibold text-gray-900 dark:text-gray-100">
              Invitation déjà utilisée
            </h2>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Cette invitation a déjà été acceptée.
            </p>
            <Button onClick={() => navigate('/hub')} className="mt-6">
              Retour à l'accueil
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  const isExpired = new Date(invitation.expiresAt) < new Date();
  if (isExpired) {
    return (
      <div className="min-h-screen bg-light-base dark:bg-dark-base flex items-center justify-center p-6">
        <Card className="max-w-md w-full p-8">
          <div className="flex flex-col items-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-orange-100 dark:bg-orange-900/30">
              <Clock className="h-8 w-8 text-orange-600 dark:text-orange-400" />
            </div>
            <h2 className="mt-4 text-xl font-semibold text-gray-900 dark:text-gray-100">
              Invitation expirée
            </h2>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Cette invitation a expiré le{' '}
              {new Date(invitation.expiresAt).toLocaleDateString('fr-FR')}.
            </p>
            <p className="mt-4 text-sm text-gray-600 dark:text-gray-400">
              Demandez à l'administrateur de l'organisation de vous envoyer une nouvelle invitation.
            </p>
            <Button onClick={() => navigate('/hub')} className="mt-6">
              Retour à l'accueil
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-light-base dark:bg-dark-base flex items-center justify-center p-6">
        <Card className="max-w-md w-full p-8">
          <div className="flex flex-col items-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30">
              <UserPlus className="h-8 w-8 text-blue-600 dark:text-blue-400" />
            </div>
            <h2 className="mt-4 text-xl font-semibold text-gray-900 dark:text-gray-100">
              Invitation à rejoindre une organisation
            </h2>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              Vous avez été invité à rejoindre une organisation en tant que{' '}
              <span className="font-medium">{invitation.role}</span>.
            </p>
            <div className="mt-6 space-y-3 w-full">
              <Button onClick={() => navigate('/login', { state: { invitationToken: token } })} className="w-full">
                Se connecter
              </Button>
              <Button
                onClick={() => navigate('/register', { state: { invitationToken: token, email: invitation.email } })}
                variant="outline"
                className="w-full"
              >
                Créer un compte
              </Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-light-base dark:bg-dark-base flex items-center justify-center p-6">
      <Card className="max-w-md w-full p-8">
        <div className="flex flex-col items-center text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
            <UserPlus className="h-8 w-8 text-green-600 dark:text-green-400" />
          </div>
          <h2 className="mt-4 text-xl font-semibold text-gray-900 dark:text-gray-100">
            Rejoindre l'organisation
          </h2>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
            Vous avez été invité en tant que{' '}
            <span className="font-medium">{invitation.role}</span>.
          </p>
          <div className="mt-6 w-full space-y-4">
            <div className="rounded-lg bg-gray-50 dark:bg-gray-800/50 p-4">
              <p className="text-xs text-gray-500 dark:text-gray-400">Connecté en tant que</p>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mt-1">
                {user?.email}
              </p>
            </div>
            <Button
              onClick={handleAcceptInvitation}
              disabled={accepting}
              className="w-full"
            >
              {accepting ? 'Acceptation...' : 'Accepter l\'invitation'}
            </Button>
            <Button onClick={() => navigate('/hub')} variant="ghost" className="w-full">
              Annuler
            </Button>
          </div>
          <p className="mt-6 text-xs text-gray-500 dark:text-gray-400">
            Expire le {new Date(invitation.expiresAt).toLocaleDateString('fr-FR')}
          </p>
        </div>
      </Card>
    </div>
  );
}
