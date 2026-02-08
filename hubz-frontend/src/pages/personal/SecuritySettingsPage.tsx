import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Shield, ShieldCheck, ShieldOff, Copy, Check, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import Modal from '../../components/ui/Modal';
import { authService } from '../../services/auth.service';
import { useAuthStore } from '../../stores/authStore';
import type { TwoFactorSetupResponse } from '../../types/auth';

const verifySchema = z.object({
  code: z
    .string()
    .min(6, 'Le code doit contenir 6 chiffres')
    .max(6, 'Le code doit contenir 6 chiffres')
    .regex(/^\d{6}$/, 'Le code doit contenir uniquement des chiffres'),
});

const disableSchema = z.object({
  password: z.string().min(1, 'Le mot de passe est requis'),
  code: z
    .string()
    .min(6, 'Le code doit contenir 6 chiffres')
    .max(6, 'Le code doit contenir 6 chiffres')
    .regex(/^\d{6}$/, 'Le code doit contenir uniquement des chiffres'),
});

type VerifyForm = z.infer<typeof verifySchema>;
type DisableForm = z.infer<typeof disableSchema>;

export default function SecuritySettingsPage() {
  const { user, setUser } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [is2FAEnabled, setIs2FAEnabled] = useState(user?.twoFactorEnabled ?? false);
  const [setupData, setSetupData] = useState<TwoFactorSetupResponse | null>(null);
  const [showSetupModal, setShowSetupModal] = useState(false);
  const [showDisableModal, setShowDisableModal] = useState(false);
  const [secretCopied, setSecretCopied] = useState(false);

  const {
    register: registerVerify,
    handleSubmit: handleSubmitVerify,
    formState: { errors: verifyErrors },
    reset: resetVerify,
  } = useForm<VerifyForm>({ resolver: zodResolver(verifySchema) });

  const {
    register: registerDisable,
    handleSubmit: handleSubmitDisable,
    formState: { errors: disableErrors },
    reset: resetDisable,
  } = useForm<DisableForm>({ resolver: zodResolver(disableSchema) });

  useEffect(() => {
    // Fetch current 2FA status
    const fetchStatus = async () => {
      try {
        const status = await authService.get2FAStatus();
        setIs2FAEnabled(status.enabled);
      } catch (error) {
        console.error('Failed to fetch 2FA status:', error);
      }
    };
    fetchStatus();
  }, []);

  const handleSetup2FA = async () => {
    setLoading(true);
    try {
      const data = await authService.setup2FA();
      setSetupData(data);
      setShowSetupModal(true);
    } catch (error) {
      toast.error('Erreur lors de la configuration de la 2FA');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleVerify2FA = async (data: VerifyForm) => {
    setLoading(true);
    try {
      await authService.verify2FA({ code: data.code });
      setIs2FAEnabled(true);
      setShowSetupModal(false);
      setSetupData(null);
      resetVerify();
      if (user) {
        setUser({ ...user, twoFactorEnabled: true });
      }
      toast.success('Authentification a deux facteurs activee !');
    } catch (error) {
      toast.error('Code invalide. Veuillez reessayer.');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleDisable2FA = async (data: DisableForm) => {
    setLoading(true);
    try {
      await authService.disable2FA({ password: data.password, code: data.code });
      setIs2FAEnabled(false);
      setShowDisableModal(false);
      resetDisable();
      if (user) {
        setUser({ ...user, twoFactorEnabled: false });
      }
      toast.success('Authentification a deux facteurs desactivee');
    } catch (error) {
      toast.error('Mot de passe ou code invalide');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const copySecret = async () => {
    if (setupData?.secret) {
      await navigator.clipboard.writeText(setupData.secret);
      setSecretCopied(true);
      setTimeout(() => setSecretCopied(false), 2000);
      toast.success('Secret copie !');
    }
  };

  const closeSetupModal = () => {
    setShowSetupModal(false);
    setSetupData(null);
    resetVerify();
  };

  const closeDisableModal = () => {
    setShowDisableModal(false);
    resetDisable();
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6 p-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
        Parametres de securite
      </h1>

      {/* 2FA Section */}
      <Card className="p-6">
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-4">
            <div
              className={`flex h-12 w-12 items-center justify-center rounded-full ${
                is2FAEnabled ? 'bg-success/10' : 'bg-warning/10'
              }`}
            >
              {is2FAEnabled ? (
                <ShieldCheck className="h-6 w-6 text-success" />
              ) : (
                <Shield className="h-6 w-6 text-warning" />
              )}
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                Authentification a deux facteurs (2FA)
              </h2>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {is2FAEnabled
                  ? 'Votre compte est protege par une authentification a deux facteurs.'
                  : 'Ajoutez une couche de securite supplementaire a votre compte.'}
              </p>
              <div className="mt-2">
                <span
                  className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                    is2FAEnabled
                      ? 'bg-success/10 text-success'
                      : 'bg-warning/10 text-warning'
                  }`}
                >
                  {is2FAEnabled ? 'Active' : 'Desactive'}
                </span>
              </div>
            </div>
          </div>
          <div>
            {is2FAEnabled ? (
              <Button
                variant="outline"
                onClick={() => setShowDisableModal(true)}
                className="text-error border-error hover:bg-error/10"
              >
                <ShieldOff className="mr-2 h-4 w-4" />
                Desactiver
              </Button>
            ) : (
              <Button onClick={handleSetup2FA} loading={loading}>
                <Shield className="mr-2 h-4 w-4" />
                Activer la 2FA
              </Button>
            )}
          </div>
        </div>

        <div className="mt-6 rounded-lg bg-gray-50 p-4 dark:bg-gray-800/50">
          <h3 className="text-sm font-medium text-gray-900 dark:text-white">
            Comment ca fonctionne ?
          </h3>
          <ul className="mt-2 space-y-2 text-sm text-gray-500 dark:text-gray-400">
            <li className="flex items-start gap-2">
              <span className="mt-1 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-accent/10 text-xs font-medium text-accent">
                1
              </span>
              <span>
                Telechargez une application d'authentification (Google Authenticator, Authy, etc.)
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="mt-1 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-accent/10 text-xs font-medium text-accent">
                2
              </span>
              <span>Scannez le QR code ou entrez le secret manuellement</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="mt-1 flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-accent/10 text-xs font-medium text-accent">
                3
              </span>
              <span>
                Entrez le code a 6 chiffres pour verifier la configuration
              </span>
            </li>
          </ul>
        </div>
      </Card>

      {/* Setup 2FA Modal */}
      <Modal
        isOpen={showSetupModal}
        onClose={closeSetupModal}
        title="Configurer l'authentification a deux facteurs"
      >
        <div className="space-y-6">
          {setupData && (
            <>
              {/* QR Code */}
              <div className="flex flex-col items-center">
                <p className="mb-4 text-center text-sm text-gray-500 dark:text-gray-400">
                  Scannez ce QR code avec votre application d'authentification
                </p>
                <div className="rounded-lg bg-white p-4">
                  <img
                    src={setupData.qrCodeImage}
                    alt="QR Code pour 2FA"
                    className="h-48 w-48"
                  />
                </div>
              </div>

              {/* Manual Entry */}
              <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-800/50">
                <p className="mb-2 text-sm font-medium text-gray-700 dark:text-gray-300">
                  Ou entrez ce secret manuellement :
                </p>
                <div className="flex items-center gap-2">
                  <code className="flex-1 rounded bg-gray-200 px-3 py-2 font-mono text-sm dark:bg-gray-700">
                    {setupData.secret}
                  </code>
                  <button
                    type="button"
                    onClick={copySecret}
                    className="rounded-lg p-2 text-gray-500 hover:bg-gray-200 dark:hover:bg-gray-700"
                    title="Copier le secret"
                  >
                    {secretCopied ? (
                      <Check className="h-5 w-5 text-success" />
                    ) : (
                      <Copy className="h-5 w-5" />
                    )}
                  </button>
                </div>
              </div>

              {/* Warning */}
              <div className="flex items-start gap-3 rounded-lg bg-warning/10 p-4">
                <AlertTriangle className="h-5 w-5 flex-shrink-0 text-warning" />
                <p className="text-sm text-warning">
                  Conservez ce secret en lieu sur. Vous en aurez besoin si vous perdez
                  l'acces a votre application d'authentification.
                </p>
              </div>

              {/* Verification Form */}
              <form onSubmit={handleSubmitVerify(handleVerify2FA)} className="space-y-4">
                <Input
                  label="Code de verification"
                  type="text"
                  placeholder="000000"
                  icon={<Shield className="h-4 w-4" />}
                  error={verifyErrors.code?.message}
                  maxLength={6}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  {...registerVerify('code')}
                />
                <div className="flex justify-end gap-3">
                  <Button type="button" variant="outline" onClick={closeSetupModal}>
                    Annuler
                  </Button>
                  <Button type="submit" loading={loading}>
                    Activer la 2FA
                  </Button>
                </div>
              </form>
            </>
          )}
        </div>
      </Modal>

      {/* Disable 2FA Modal */}
      <Modal
        isOpen={showDisableModal}
        onClose={closeDisableModal}
        title="Desactiver l'authentification a deux facteurs"
      >
        <div className="space-y-6">
          <div className="flex items-start gap-3 rounded-lg bg-error/10 p-4">
            <AlertTriangle className="h-5 w-5 flex-shrink-0 text-error" />
            <p className="text-sm text-error">
              La desactivation de la 2FA reduira la securite de votre compte. Etes-vous
              sur de vouloir continuer ?
            </p>
          </div>

          <form onSubmit={handleSubmitDisable(handleDisable2FA)} className="space-y-4">
            <Input
              label="Mot de passe actuel"
              type="password"
              placeholder="********"
              error={disableErrors.password?.message}
              {...registerDisable('password')}
            />
            <Input
              label="Code de verification"
              type="text"
              placeholder="000000"
              icon={<Shield className="h-4 w-4" />}
              error={disableErrors.code?.message}
              maxLength={6}
              inputMode="numeric"
              autoComplete="one-time-code"
              {...registerDisable('code')}
            />
            <div className="flex justify-end gap-3">
              <Button type="button" variant="outline" onClick={closeDisableModal}>
                Annuler
              </Button>
              <Button
                type="submit"
                loading={loading}
                className="bg-error hover:bg-error/90"
              >
                Desactiver la 2FA
              </Button>
            </div>
          </form>
        </div>
      </Modal>
    </div>
  );
}
