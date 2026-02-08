import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  User,
  Camera,
  Trash2,
  AlertTriangle,
  Save,
  Lock,
  LogOut,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import Modal from '../../components/ui/Modal';
import { useAuthStore } from '../../stores/authStore';
import { userService } from '../../services/user.service';
import { cn } from '../../lib/utils';

interface ProfileFormData {
  firstName: string;
  lastName: string;
  description: string;
}

interface PasswordFormData {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

interface DeleteAccountFormData {
  password: string;
  confirmation: string;
}

export default function ProfileSettingsPage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false);
  const [isDeletingPhoto, setIsDeletingPhoto] = useState(false);
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);

  const profileForm = useForm<ProfileFormData>({
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      description: user?.description || '',
    },
  });

  const passwordForm = useForm<PasswordFormData>({
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const deleteForm = useForm<DeleteAccountFormData>({
    defaultValues: {
      password: '',
      confirmation: '',
    },
  });

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      toast.error('Type de fichier non supporte. Utilisez JPG, PNG, GIF ou WebP.');
      return;
    }

    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      toast.error('La photo doit faire moins de 5 Mo.');
      return;
    }

    setIsUploadingPhoto(true);
    try {
      await userService.uploadProfilePhoto(file);
      toast.success('Photo de profil mise a jour');
    } catch (error) {
      toast.error('Erreur lors du telechargement de la photo');
      console.error(error);
    } finally {
      setIsUploadingPhoto(false);
      // Reset input
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDeletePhoto = async () => {
    if (!user?.profilePhotoUrl) return;

    setIsDeletingPhoto(true);
    try {
      await userService.deleteProfilePhoto();
      toast.success('Photo de profil supprimee');
    } catch (error) {
      toast.error('Erreur lors de la suppression de la photo');
      console.error(error);
    } finally {
      setIsDeletingPhoto(false);
    }
  };

  const handleUpdateProfile = async (data: ProfileFormData) => {
    setIsUpdatingProfile(true);
    try {
      await userService.updateProfile({
        firstName: data.firstName,
        lastName: data.lastName,
        description: data.description || undefined,
      });
      toast.success('Profil mis a jour');
    } catch (error) {
      toast.error('Erreur lors de la mise a jour du profil');
      console.error(error);
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const handleChangePassword = async (data: PasswordFormData) => {
    if (data.newPassword !== data.confirmPassword) {
      passwordForm.setError('confirmPassword', {
        message: 'Les mots de passe ne correspondent pas',
      });
      return;
    }

    if (data.newPassword.length < 8) {
      passwordForm.setError('newPassword', {
        message: 'Le mot de passe doit contenir au moins 8 caracteres',
      });
      return;
    }

    setIsChangingPassword(true);
    try {
      await userService.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      toast.success('Mot de passe modifie');
      passwordForm.reset();
    } catch (error: unknown) {
      const err = error as { response?: { status?: number } };
      if (err.response?.status === 400) {
        toast.error('Mot de passe actuel incorrect');
      } else {
        toast.error('Erreur lors du changement de mot de passe');
      }
      console.error(error);
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleDeleteAccount = async (data: DeleteAccountFormData) => {
    if (data.confirmation !== 'SUPPRIMER') {
      deleteForm.setError('confirmation', {
        message: 'Veuillez taper SUPPRIMER pour confirmer',
      });
      return;
    }

    setIsDeletingAccount(true);
    try {
      await userService.deleteAccount({ password: data.password });
      toast.success('Compte supprime');
      navigate('/');
    } catch (error: unknown) {
      const err = error as { response?: { status?: number } };
      if (err.response?.status === 400) {
        toast.error('Mot de passe incorrect');
      } else {
        toast.error('Erreur lors de la suppression du compte');
      }
      console.error(error);
    } finally {
      setIsDeletingAccount(false);
    }
  };

  const getPhotoUrl = () => {
    if (!user?.profilePhotoUrl) return null;
    // Handle relative URLs from the backend
    if (user.profilePhotoUrl.startsWith('http')) {
      return user.profilePhotoUrl;
    }
    return `/uploads/${user.profilePhotoUrl}`;
  };

  const initials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`
    : '??';

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          Parametres du profil
        </h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Gerez votre compte et vos preferences
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Profile Photo Section */}
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
            <Camera className="h-5 w-5 text-gray-400" />
            Photo de profil
          </h3>

          <div className="flex items-center gap-6">
            {/* Photo Preview */}
            <div className="relative">
              {user?.profilePhotoUrl ? (
                <img
                  src={getPhotoUrl() || ''}
                  alt="Profile"
                  className="h-24 w-24 rounded-full object-cover border-2 border-gray-200 dark:border-gray-700"
                />
              ) : (
                <div className="flex h-24 w-24 items-center justify-center rounded-full bg-accent text-2xl font-bold text-white">
                  {initials}
                </div>
              )}

              {/* Upload overlay */}
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploadingPhoto}
                className={cn(
                  'absolute inset-0 flex items-center justify-center rounded-full bg-black/50 opacity-0 transition-opacity hover:opacity-100',
                  isUploadingPhoto && 'opacity-100'
                )}
              >
                {isUploadingPhoto ? (
                  <div className="h-6 w-6 animate-spin rounded-full border-2 border-white border-t-transparent" />
                ) : (
                  <Camera className="h-6 w-6 text-white" />
                )}
              </button>
            </div>

            <div className="flex flex-col gap-2">
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                onChange={handlePhotoUpload}
                className="hidden"
              />

              <Button
                variant="secondary"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                loading={isUploadingPhoto}
              >
                Changer la photo
              </Button>

              {user?.profilePhotoUrl && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleDeletePhoto}
                  loading={isDeletingPhoto}
                  className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                >
                  <Trash2 className="h-4 w-4 mr-1" />
                  Supprimer
                </Button>
              )}

              <p className="text-xs text-gray-500 dark:text-gray-400">
                JPG, PNG, GIF ou WebP. Max 5 Mo.
              </p>
            </div>
          </div>
        </Card>

        {/* Profile Information Section */}
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
            <User className="h-5 w-5 text-gray-400" />
            Informations du profil
          </h3>

          <form onSubmit={profileForm.handleSubmit(handleUpdateProfile)} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Prenom"
                {...profileForm.register('firstName', { required: 'Prenom requis' })}
                error={profileForm.formState.errors.firstName?.message}
              />
              <Input
                label="Nom"
                {...profileForm.register('lastName', { required: 'Nom requis' })}
                error={profileForm.formState.errors.lastName?.message}
              />
            </div>

            <Input
              label="Email"
              value={user?.email || ''}
              disabled
              className="bg-gray-100 dark:bg-gray-800 cursor-not-allowed"
            />

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Description
              </label>
              <textarea
                {...profileForm.register('description')}
                rows={3}
                className="w-full rounded-lg border bg-white/60 dark:bg-white/5 backdrop-blur-sm px-3 py-2 text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-colors border-gray-200 dark:border-white/10 focus:border-accent dark:focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 resize-none"
                placeholder="Parlez-nous de vous..."
              />
            </div>

            <Button type="submit" loading={isUpdatingProfile}>
              <Save className="h-4 w-4 mr-1" />
              Enregistrer
            </Button>
          </form>
        </Card>

        {/* Change Password Section */}
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
            <Lock className="h-5 w-5 text-gray-400" />
            Changer le mot de passe
          </h3>

          <form onSubmit={passwordForm.handleSubmit(handleChangePassword)} className="space-y-4">
            <Input
              label="Mot de passe actuel"
              type="password"
              {...passwordForm.register('currentPassword', { required: 'Requis' })}
              error={passwordForm.formState.errors.currentPassword?.message}
            />

            <Input
              label="Nouveau mot de passe"
              type="password"
              {...passwordForm.register('newPassword', { required: 'Requis' })}
              error={passwordForm.formState.errors.newPassword?.message}
            />

            <Input
              label="Confirmer le mot de passe"
              type="password"
              {...passwordForm.register('confirmPassword', { required: 'Requis' })}
              error={passwordForm.formState.errors.confirmPassword?.message}
            />

            <Button type="submit" loading={isChangingPassword}>
              <Lock className="h-4 w-4 mr-1" />
              Changer le mot de passe
            </Button>
          </form>
        </Card>

        {/* Danger Zone */}
        <Card className="p-6 border-red-200 dark:border-red-900/50">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-red-600 dark:text-red-400">
            <AlertTriangle className="h-5 w-5" />
            Zone de danger
          </h3>

          <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
            La suppression de votre compte est irreversible. Toutes vos donnees seront supprimees.
            Si vous etes proprietaire d'une organisation, la propriete sera transferee a un administrateur.
          </p>

          <Button
            variant="danger"
            onClick={() => setShowDeleteModal(true)}
          >
            <Trash2 className="h-4 w-4 mr-1" />
            Supprimer mon compte
          </Button>
        </Card>
      </div>

      {/* Delete Account Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => {
          setShowDeleteModal(false);
          deleteForm.reset();
        }}
        title="Supprimer le compte"
        className="max-w-md"
      >
        <form onSubmit={deleteForm.handleSubmit(handleDeleteAccount)} className="space-y-4">
          <div className="rounded-lg bg-red-50 dark:bg-red-900/20 p-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <h4 className="font-medium text-red-800 dark:text-red-300">
                  Cette action est irreversible
                </h4>
                <p className="mt-1 text-sm text-red-700 dark:text-red-400">
                  Votre compte et toutes vos donnees seront definitivement supprimes.
                  Vous serez retire de toutes les organisations.
                </p>
              </div>
            </div>
          </div>

          <Input
            label="Mot de passe"
            type="password"
            {...deleteForm.register('password', { required: 'Mot de passe requis' })}
            error={deleteForm.formState.errors.password?.message}
            placeholder="Entrez votre mot de passe"
          />

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Tapez <span className="font-bold text-red-600">SUPPRIMER</span> pour confirmer
            </label>
            <Input
              {...deleteForm.register('confirmation', { required: 'Confirmation requise' })}
              error={deleteForm.formState.errors.confirmation?.message}
              placeholder="SUPPRIMER"
            />
          </div>

          <div className="flex gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              className="flex-1"
              onClick={() => {
                setShowDeleteModal(false);
                deleteForm.reset();
              }}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              variant="danger"
              className="flex-1"
              loading={isDeletingAccount}
            >
              <LogOut className="h-4 w-4 mr-1" />
              Supprimer
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
