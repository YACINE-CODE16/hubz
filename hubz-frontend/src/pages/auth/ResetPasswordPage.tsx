import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { Lock, ArrowLeft, CheckCircle, XCircle, Loader2 } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Card from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import { authService } from '../../services/auth.service';

const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Le mot de passe doit contenir au moins 8 caractères'),
    confirmPassword: z.string().min(1, 'Confirmation requise'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Les mots de passe ne correspondent pas',
    path: ['confirmPassword'],
  });

type ResetPasswordForm = z.infer<typeof resetPasswordSchema>;

export default function ResetPasswordPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [checkingToken, setCheckingToken] = useState(true);
  const [tokenValid, setTokenValid] = useState(false);
  const [resetSuccess, setResetSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordForm>({ resolver: zodResolver(resetPasswordSchema) });

  useEffect(() => {
    const checkToken = async () => {
      if (!token) {
        setTokenValid(false);
        setCheckingToken(false);
        return;
      }

      try {
        await authService.checkResetTokenValid(token);
        setTokenValid(true);
      } catch {
        setTokenValid(false);
      } finally {
        setCheckingToken(false);
      }
    };

    checkToken();
  }, [token]);

  const onSubmit = async (data: ResetPasswordForm) => {
    if (!token) return;

    setError('');
    setLoading(true);
    try {
      await authService.resetPassword({
        token,
        newPassword: data.newPassword,
      });
      setResetSuccess(true);
    } catch {
      setError('Une erreur est survenue. Le lien est peut-être expiré.');
    } finally {
      setLoading(false);
    }
  };

  // Loading state while checking token
  if (checkingToken) {
    return (
      <AuthLayout>
        <Card className="w-full max-w-md p-8">
          <div className="flex flex-col items-center text-center">
            <Loader2 className="h-12 w-12 animate-spin text-accent" />
            <p className="mt-4 text-gray-500 dark:text-gray-400">
              Vérification du lien...
            </p>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  // Invalid or expired token
  if (!tokenValid) {
    return (
      <AuthLayout>
        <Card className="w-full max-w-md p-8">
          <div className="flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-error/10">
              <XCircle className="h-8 w-8 text-error" />
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-white">
              Lien invalide
            </h1>
            <p className="mb-6 text-gray-500 dark:text-gray-400">
              Ce lien de réinitialisation est invalide ou a expiré. Veuillez demander un
              nouveau lien.
            </p>
            <div className="flex flex-col gap-3">
              <Link to="/forgot-password">
                <Button className="w-full">Demander un nouveau lien</Button>
              </Link>
              <Link
                to="/login"
                className="flex items-center justify-center gap-2 text-sm font-medium text-accent hover:underline"
              >
                <ArrowLeft className="h-4 w-4" />
                Retour à la connexion
              </Link>
            </div>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  // Success state
  if (resetSuccess) {
    return (
      <AuthLayout>
        <Card className="w-full max-w-md p-8">
          <div className="flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-success/10">
              <CheckCircle className="h-8 w-8 text-success" />
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-white">
              Mot de passe modifié
            </h1>
            <p className="mb-6 text-gray-500 dark:text-gray-400">
              Votre mot de passe a été réinitialisé avec succès. Vous pouvez maintenant
              vous connecter avec votre nouveau mot de passe.
            </p>
            <Button onClick={() => navigate('/login')} className="w-full">
              Se connecter
            </Button>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  // Reset password form
  return (
    <AuthLayout>
      <Card className="w-full max-w-md p-8">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-accent">Hubz</h1>
          <p className="mt-2 text-lg font-semibold text-gray-900 dark:text-white">
            Nouveau mot de passe
          </p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Choisissez un nouveau mot de passe sécurisé
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          {error && (
            <p className="rounded-lg bg-error/10 px-4 py-2 text-sm text-error">{error}</p>
          )}

          <Input
            label="Nouveau mot de passe"
            type="password"
            placeholder="Au moins 8 caractères"
            icon={<Lock className="h-4 w-4" />}
            error={errors.newPassword?.message}
            {...register('newPassword')}
          />

          <Input
            label="Confirmer le mot de passe"
            type="password"
            placeholder="Confirmez votre mot de passe"
            icon={<Lock className="h-4 w-4" />}
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />

          <Button type="submit" loading={loading} className="mt-2 w-full">
            Réinitialiser le mot de passe
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
          <Link
            to="/login"
            className="flex items-center justify-center gap-2 font-medium text-accent hover:underline"
          >
            <ArrowLeft className="h-4 w-4" />
            Retour à la connexion
          </Link>
        </p>
      </Card>
    </AuthLayout>
  );
}
