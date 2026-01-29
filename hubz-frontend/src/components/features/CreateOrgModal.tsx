import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Building2,
  Code,
  Briefcase,
  Rocket,
  BookOpen,
  Users,
  Gamepad2,
  Music,
  Heart,
  Star,
  type LucideIcon,
} from 'lucide-react';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import { cn } from '../../lib/utils';

const icons: { key: string; Icon: LucideIcon }[] = [
  { key: 'building', Icon: Building2 },
  { key: 'code', Icon: Code },
  { key: 'briefcase', Icon: Briefcase },
  { key: 'rocket', Icon: Rocket },
  { key: 'book', Icon: BookOpen },
  { key: 'users', Icon: Users },
  { key: 'gamepad', Icon: Gamepad2 },
  { key: 'music', Icon: Music },
  { key: 'heart', Icon: Heart },
  { key: 'star', Icon: Star },
];

const colors = [
  '#3B82F6',
  '#8B5CF6',
  '#EC4899',
  '#EF4444',
  '#F59E0B',
  '#22C55E',
  '#06B6D4',
  '#6366F1',
];

const schema = z.object({
  name: z.string().min(1, 'Nom requis'),
  description: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

interface CreateOrgModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; description?: string; icon: string; color: string }) => Promise<void>;
}

export default function CreateOrgModal({ isOpen, onClose, onSubmit }: CreateOrgModalProps) {
  const [selectedIcon, setSelectedIcon] = useState('building');
  const [selectedColor, setSelectedColor] = useState(colors[0]);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const handleFormSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      await onSubmit({ ...data, icon: selectedIcon, color: selectedColor });
      reset();
      setSelectedIcon('building');
      setSelectedColor(colors[0]);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvelle organisation">
      <form onSubmit={handleSubmit(handleFormSubmit)} className="flex flex-col gap-5">
        <Input
          label="Nom"
          placeholder="Mon organisation"
          error={errors.name?.message}
          {...register('name')}
        />

        <Input
          label="Description"
          placeholder="Une courte description (optionnel)"
          error={errors.description?.message}
          {...register('description')}
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Icône
          </label>
          <div className="flex flex-wrap gap-2">
            {icons.map(({ key, Icon }) => (
              <button
                key={key}
                type="button"
                onClick={() => setSelectedIcon(key)}
                className={cn(
                  'flex h-10 w-10 items-center justify-center rounded-lg border transition-colors',
                  selectedIcon === key
                    ? 'border-accent bg-accent/10 text-accent'
                    : 'border-gray-200 dark:border-white/10 text-gray-500 dark:text-gray-400 hover:bg-light-hover dark:hover:bg-dark-hover',
                )}
              >
                <Icon className="h-5 w-5" />
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Couleur
          </label>
          <div className="flex flex-wrap gap-2">
            {colors.map((color) => (
              <button
                key={color}
                type="button"
                onClick={() => setSelectedColor(color)}
                className={cn(
                  'h-8 w-8 rounded-full transition-transform',
                  selectedColor === color && 'scale-110 ring-2 ring-offset-2 ring-offset-light-card dark:ring-offset-dark-card',
                )}
                style={{ backgroundColor: color }}
              />
            ))}
          </div>
        </div>

        <Button type="submit" loading={loading} className="mt-1 w-full">
          Créer l'organisation
        </Button>
      </form>
    </Modal>
  );
}
