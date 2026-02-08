import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Card from '../../components/ui/Card';
import { authService } from '../../services/auth.service';

export default function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    const oauthError = searchParams.get('oauth_error');

    if (oauthError) {
      setError(decodeURIComponent(oauthError));
      setTimeout(() => navigate('/login'), 3000);
      return;
    }

    if (!token) {
      setError('Aucun token recu. Veuillez reessayer.');
      setTimeout(() => navigate('/login'), 3000);
      return;
    }

    const handleCallback = async () => {
      try {
        await authService.handleOAuthCallback(token);
        navigate('/hub');
      } catch {
        setError('Echec de la connexion. Veuillez reessayer.');
        setTimeout(() => navigate('/login'), 3000);
      }
    };

    handleCallback();
  }, [searchParams, navigate]);

  if (error) {
    return (
      <AuthLayout>
        <Card className="w-full max-w-md p-8">
          <div className="text-center">
            <p className="rounded-lg bg-error/10 px-4 py-3 text-sm text-error">
              {error}
            </p>
            <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">
              Redirection vers la page de connexion...
            </p>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <Card className="w-full max-w-md p-8">
        <div className="flex flex-col items-center gap-4 text-center">
          <Loader2 className="h-8 w-8 animate-spin text-accent" />
          <p className="text-gray-600 dark:text-gray-300">
            Connexion en cours...
          </p>
        </div>
      </Card>
    </AuthLayout>
  );
}
