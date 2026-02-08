import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react';
import AuthLayout from '../../components/layout/AuthLayout';
import Card from '../../components/ui/Card';
import Input from '../../components/ui/Input';
import Button from '../../components/ui/Button';
import { authService } from '../../services/auth.service';

const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email requis').email('Email invalide'),
});

type ForgotPasswordForm = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [emailSent, setEmailSent] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    getValues,
  } = useForm<ForgotPasswordForm>({ resolver: zodResolver(forgotPasswordSchema) });

  const onSubmit = async (data: ForgotPasswordForm) => {
    setError('');
    setLoading(true);
    try {
      await authService.forgotPassword(data);
      setEmailSent(true);
    } catch {
      setError('Une erreur est survenue. Veuillez réessayer.');
    } finally {
      setLoading(false);
    }
  };

  if (emailSent) {
    return (
      <AuthLayout>
        <Card className="w-full max-w-md p-8">
          <div className="flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-success/10">
              <CheckCircle className="h-8 w-8 text-success" />
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-white">
              Email envoyé
            </h1>
            <p className="mb-6 text-gray-500 dark:text-gray-400">
              Si un compte existe avec l'adresse{' '}
              <span className="font-medium text-accent">{getValues('email')}</span>, vous
              recevrez un email avec les instructions pour réinitialiser votre mot de passe.
            </p>
            <p className="mb-6 text-sm text-gray-400 dark:text-gray-500">
              Le lien expire dans 1 heure. Vérifiez également vos spams si vous ne trouvez
              pas l'email.
            </p>
            <Link
              to="/login"
              className="flex items-center gap-2 text-sm font-medium text-accent hover:underline"
            >
              <ArrowLeft className="h-4 w-4" />
              Retour à la connexion
            </Link>
          </div>
        </Card>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <Card className="w-full max-w-md p-8">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-accent">Hubz</h1>
          <p className="mt-2 text-lg font-semibold text-gray-900 dark:text-white">
            Mot de passe oublié ?
          </p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Entrez votre email pour recevoir un lien de réinitialisation
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          {error && (
            <p className="rounded-lg bg-error/10 px-4 py-2 text-sm text-error">{error}</p>
          )}

          <Input
            label="Email"
            type="email"
            placeholder="vous@exemple.com"
            icon={<Mail className="h-4 w-4" />}
            error={errors.email?.message}
            {...register('email')}
          />

          <Button type="submit" loading={loading} className="mt-2 w-full">
            Envoyer le lien
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
