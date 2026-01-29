import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import type { HabitFrequency } from '../../types/habit';
import { cn } from '../../lib/utils';

const frequencies: { value: HabitFrequency; label: string }[] = [
  { value: 'DAILY', label: 'Quotidien' },
  { value: 'WEEKLY', label: 'Hebdomadaire' },
];

const iconSuggestions = ['🏃', '📚', '🧘', '💪', '💧', '🎯', '✍️', '🎨'];

const schema = z.object({
  name: z.string().min(1, 'Nom requis'),
  icon: z.string().min(1, 'Icône requise'),
});

type FormData = z.infer<typeof schema>;

interface CreateHabitModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; icon: string; frequency: HabitFrequency }) => Promise<void>;
}

export default function CreateHabitModal({ isOpen, onClose, onSubmit }: CreateHabitModalProps) {
  const [selectedFrequency, setSelectedFrequency] = useState<HabitFrequency>('DAILY');
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const selectedIcon = watch('icon');

  const handleFormSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      await onSubmit({
        name: data.name,
        icon: data.icon,
        frequency: selectedFrequency,
      });
      reset();
      setSelectedFrequency('DAILY');
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvelle habitude">
      <form onSubmit={handleSubmit(handleFormSubmit)} className="flex flex-col gap-4">
        <Input
          label="Nom"
          placeholder="Ex: Faire du sport"
          error={errors.name?.message}
          {...register('name')}
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Icône
          </label>
          <div className="mb-2 flex flex-wrap gap-2">
            {iconSuggestions.map((icon) => (
              <button
                key={icon}
                type="button"
                onClick={() => setValue('icon', icon)}
                className={cn(
                  'h-10 w-10 rounded-lg text-2xl transition-all',
                  selectedIcon === icon
                    ? 'bg-accent/20 ring-2 ring-accent'
                    : 'bg-light-hover dark:bg-dark-hover hover:bg-gray-200 dark:hover:bg-white/10'
                )}
              >
                {icon}
              </button>
            ))}
          </div>
          <Input
            placeholder="Ou tapez un emoji"
            error={errors.icon?.message}
            {...register('icon')}
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Fréquence
          </label>
          <div className="flex gap-2">
            {frequencies.map((freq) => (
              <button
                key={freq.value}
                type="button"
                onClick={() => setSelectedFrequency(freq.value)}
                className={cn(
                  'flex-1 rounded-lg px-4 py-2 text-sm font-medium transition-all',
                  selectedFrequency === freq.value
                    ? 'bg-accent text-white'
                    : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10'
                )}
              >
                {freq.label}
              </button>
            ))}
          </div>
        </div>

        <Button type="submit" loading={loading} className="mt-1 w-full">
          Créer l'habitude
        </Button>
      </form>
    </Modal>
  );
}
