import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2, ArrowRight } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import { authService } from '../../services/auth.service';

export default function VerifyEmailPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const [verifying, setVerifying] = useState(true);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const verifyEmail = async () => {
      if (!token) {
        setError('Token de vérification manquant');
        setVerifying(false);
        return;
      }

      try {
        await authService.verifyEmail(token);
        setSuccess(true);
      } catch (err: unknown) {
        const errorMessage =
          err instanceof Error
            ? err.message
            : 'Le lien de vérification est invalide ou a expiré.';
        setError(errorMessage);
      } finally {
        setVerifying(false);
      }
    };

    verifyEmail();
  }, [token]);

  // Loading state
  if (verifying) {
    return (
      <AuthLayout>
        <Card className="w-full max-w-md p-8">
          <div className="flex flex-col items-center text-center">
            <Loader2 className="h-12 w-12 animate-spin text-accent" />
            <p className="mt-4 text-lg font-medium text-gray-900 dark:text-white">
              Vérification en cours...
            </p>
            <p className="mt-2 text-gray-500 dark:text-gray-400">
              Veuillez patienter pendant que nous vérifions votre adresse email.
            </p>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  // Success state
  if (success) {
    return (
      <AuthLayout>
        <Card className="w-full max-w-md p-8">
          <div className="flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-success/10">
              <CheckCircle className="h-8 w-8 text-success" />
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-white">
              Email vérifié !
            </h1>
            <p className="mb-6 text-gray-500 dark:text-gray-400">
              Votre adresse email a été vérifiée avec succès. Vous pouvez maintenant
              profiter de toutes les fonctionnalités de Hubz.
            </p>
            <Button
              onClick={() => navigate('/hub')}
              className="flex w-full items-center justify-center gap-2"
            >
              Continuer vers Hubz
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  // Error state
  return (
    <AuthLayout>
      <Card className="w-full max-w-md p-8">
        <div className="flex flex-col items-center text-center">
          <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-error/10">
            <XCircle className="h-8 w-8 text-error" />
          </div>
          <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-white">
            Erreur de vérification
          </h1>
          <p className="mb-6 text-gray-500 dark:text-gray-400">{error}</p>
          <div className="flex w-full flex-col gap-3">
            <Link to="/login">
              <Button variant="outline" className="w-full">
                Retour à la connexion
              </Button>
            </Link>
          </div>
        </div>
      </Card>
    </AuthLayout>
  );
}
