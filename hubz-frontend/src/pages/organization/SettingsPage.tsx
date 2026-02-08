import { useState, useRef, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import {
  Settings,
  Camera,
  Trash2,
  Save,
  Building2,
  Palette,
} from 'lucide-react';
import toast from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import { organizationService } from '../../services/organization.service';
import type { Organization, UpdateOrganizationRequest } from '../../types/organization';
import { cn } from '../../lib/utils';
import { iconMap } from '../../components/features/SpaceCard';

interface SettingsFormData {
  name: string;
  description: string;
  icon: string;
  color: string;
  readme: string;
}

const iconOptions = Object.keys(iconMap);

const colorOptions = [
  '#3B82F6', // blue
  '#6366F1', // indigo
  '#8B5CF6', // violet
  '#EC4899', // pink
  '#EF4444', // red
  '#F97316', // orange
  '#F59E0B', // amber
  '#22C55E', // green
  '#14B8A6', // teal
  '#06B6D4', // cyan
];

export default function SettingsPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [organization, setOrganization] = useState<Organization | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploadingLogo, setIsUploadingLogo] = useState(false);
  const [isDeletingLogo, setIsDeletingLogo] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);

  const form = useForm<SettingsFormData>({
    defaultValues: {
      name: '',
      description: '',
      icon: 'building',
      color: '#3B82F6',
      readme: '',
    },
  });

  useEffect(() => {
    if (!orgId) return;

    const fetchOrganization = async () => {
      try {
        const org = await organizationService.getById(orgId);
        setOrganization(org);
        form.reset({
          name: org.name,
          description: org.description || '',
          icon: org.icon || 'building',
          color: org.color || '#3B82F6',
          readme: org.readme || '',
        });
      } catch (error) {
        toast.error('Erreur lors du chargement de l\'organisation');
        console.error(error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchOrganization();
  }, [orgId, form]);

  const handleLogoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !orgId) return;

    // Validate file type
    const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      toast.error('Type de fichier non supporte. Utilisez JPG, PNG, GIF ou WebP.');
      return;
    }

    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Le logo doit faire moins de 5 Mo.');
      return;
    }

    setIsUploadingLogo(true);
    try {
      const updatedOrg = await organizationService.uploadLogo(orgId, file);
      setOrganization(updatedOrg);
      toast.success('Logo mis a jour');
    } catch (error) {
      toast.error('Erreur lors du telechargement du logo');
      console.error(error);
    } finally {
      setIsUploadingLogo(false);
      // Reset input
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDeleteLogo = async () => {
    if (!organization?.logoUrl || !orgId) return;

    setIsDeletingLogo(true);
    try {
      const updatedOrg = await organizationService.deleteLogo(orgId);
      setOrganization(updatedOrg);
      toast.success('Logo supprime');
    } catch (error) {
      toast.error('Erreur lors de la suppression du logo');
      console.error(error);
    } finally {
      setIsDeletingLogo(false);
    }
  };

  const handleUpdateSettings = async (data: SettingsFormData) => {
    if (!orgId) return;

    setIsUpdating(true);
    try {
      const updateData: UpdateOrganizationRequest = {
        name: data.name,
        description: data.description || undefined,
        icon: data.icon,
        color: data.color,
        readme: data.readme || undefined,
      };

      const updatedOrg = await organizationService.update(orgId, updateData);
      setOrganization(updatedOrg);
      toast.success('Parametres mis a jour');
    } catch (error) {
      toast.error('Erreur lors de la mise a jour');
      console.error(error);
    } finally {
      setIsUpdating(false);
    }
  };

  const getLogoUrl = () => {
    if (!organization?.logoUrl) return null;
    // Handle relative URLs from the backend
    if (organization.logoUrl.startsWith('http')) {
      return organization.logoUrl;
    }
    return `/uploads/${organization.logoUrl}`;
  };

  const selectedIcon = form.watch('icon');
  const selectedColor = form.watch('color');
  const SelectedIconComponent = iconMap[selectedIcon] || Building2;

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          Parametres de l'organisation
        </h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Gerez les informations et l'apparence de votre organisation
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Logo Section */}
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
            <Camera className="h-5 w-5 text-gray-400" />
            Logo de l'organisation
          </h3>

          <div className="flex items-center gap-6">
            {/* Logo Preview */}
            <div className="relative">
              {organization?.logoUrl ? (
                <img
                  src={getLogoUrl() || ''}
                  alt="Organization logo"
                  className="h-24 w-24 rounded-xl object-cover border-2 border-gray-200 dark:border-gray-700"
                />
              ) : (
                <div
                  className="flex h-24 w-24 items-center justify-center rounded-xl text-white"
                  style={{ backgroundColor: selectedColor }}
                >
                  <SelectedIconComponent className="h-10 w-10" />
                </div>
              )}

              {/* Upload overlay */}
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploadingLogo}
                className={cn(
                  'absolute inset-0 flex items-center justify-center rounded-xl bg-black/50 opacity-0 transition-opacity hover:opacity-100',
                  isUploadingLogo && 'opacity-100'
                )}
              >
                {isUploadingLogo ? (
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
                onChange={handleLogoUpload}
                className="hidden"
              />

              <Button
                variant="secondary"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                loading={isUploadingLogo}
              >
                Changer le logo
              </Button>

              {organization?.logoUrl && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleDeleteLogo}
                  loading={isDeletingLogo}
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

        {/* General Information */}
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
            <Settings className="h-5 w-5 text-gray-400" />
            Informations generales
          </h3>

          <form onSubmit={form.handleSubmit(handleUpdateSettings)} className="space-y-4">
            <Input
              label="Nom de l'organisation"
              {...form.register('name', { required: 'Nom requis' })}
              error={form.formState.errors.name?.message}
            />

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Description
              </label>
              <textarea
                {...form.register('description')}
                rows={3}
                className="w-full rounded-lg border bg-white/60 dark:bg-white/5 backdrop-blur-sm px-3 py-2 text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-colors border-gray-200 dark:border-white/10 focus:border-accent dark:focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 resize-none"
                placeholder="Decrivez votre organisation..."
              />
            </div>

            <Button type="submit" loading={isUpdating}>
              <Save className="h-4 w-4 mr-1" />
              Enregistrer
            </Button>
          </form>
        </Card>

        {/* Icon Selection */}
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
            <Building2 className="h-5 w-5 text-gray-400" />
            Icone
          </h3>

          <div className="grid grid-cols-5 gap-2">
            {iconOptions.map((iconKey) => {
              const IconComponent = iconMap[iconKey];
              return (
                <button
                  key={iconKey}
                  type="button"
                  onClick={() => form.setValue('icon', iconKey)}
                  className={cn(
                    'flex h-12 w-12 items-center justify-center rounded-lg border-2 transition-colors',
                    selectedIcon === iconKey
                      ? 'border-accent bg-accent/10 text-accent'
                      : 'border-gray-200 dark:border-gray-700 text-gray-500 hover:border-gray-300 dark:hover:border-gray-600'
                  )}
                >
                  <IconComponent className="h-5 w-5" />
                </button>
              );
            })}
          </div>
        </Card>

        {/* Color Selection */}
        <Card className="p-6">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
            <Palette className="h-5 w-5 text-gray-400" />
            Couleur
          </h3>

          <div className="flex flex-wrap gap-2">
            {colorOptions.map((color) => (
              <button
                key={color}
                type="button"
                onClick={() => form.setValue('color', color)}
                className={cn(
                  'h-10 w-10 rounded-lg transition-transform',
                  selectedColor === color && 'ring-2 ring-offset-2 ring-accent scale-110'
                )}
                style={{ backgroundColor: color }}
              />
            ))}
          </div>

          <div className="mt-4 flex items-center gap-2">
            <label className="text-sm text-gray-600 dark:text-gray-400">
              Ou choisissez une couleur personnalisee:
            </label>
            <input
              type="color"
              value={selectedColor}
              onChange={(e) => form.setValue('color', e.target.value)}
              className="h-8 w-8 cursor-pointer rounded border-0"
            />
          </div>
        </Card>

        {/* README / About */}
        <Card className="p-6 lg:col-span-2">
          <h3 className="mb-4 font-semibold text-gray-900 dark:text-gray-100">
            A propos (README)
          </h3>

          <form onSubmit={form.handleSubmit(handleUpdateSettings)} className="space-y-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Contenu (Markdown supporte)
              </label>
              <textarea
                {...form.register('readme')}
                rows={10}
                className="w-full rounded-lg border bg-white/60 dark:bg-white/5 backdrop-blur-sm px-3 py-2 text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-colors border-gray-200 dark:border-white/10 focus:border-accent dark:focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20 resize-none font-mono"
                placeholder="# Bienvenue dans notre organisation&#10;&#10;Decrivez ici les objectifs, les regles et les informations importantes..."
              />
            </div>

            <Button type="submit" loading={isUpdating}>
              <Save className="h-4 w-4 mr-1" />
              Enregistrer
            </Button>
          </form>
        </Card>
      </div>
    </div>
  );
}
