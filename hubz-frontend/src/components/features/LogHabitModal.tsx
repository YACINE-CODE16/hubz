import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import type { HabitLog } from '../../types/habit';

const schema = z.object({
  notes: z.string().optional(),
  duration: z.number().min(0, 'La durée doit être positive').optional().or(z.literal('')),
});

type FormData = z.infer<typeof schema>;

interface LogHabitModalProps {
  isOpen: boolean;
  onClose: () => void;
  habitName: string;
  date: string;
  existingLog?: HabitLog;
  onSubmit: (data: { date: string; completed: boolean; notes?: string; duration?: number }) => Promise<void>;
}

export default function LogHabitModal({
  isOpen,
  onClose,
  habitName,
  date,
  existingLog,
  onSubmit,
}: LogHabitModalProps) {
  const [completed, setCompleted] = useState(false);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (existingLog) {
      setCompleted(existingLog.completed);
      setValue('notes', existingLog.notes || '');
      setValue('duration', existingLog.duration as any || '');
    } else {
      setCompleted(false);
      setValue('notes', '');
      setValue('duration', '' as any);
    }
  }, [existingLog, setValue, isOpen]);

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  };

  const handleFormSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      await onSubmit({
        date,
        completed,
        notes: data.notes || undefined,
        duration: data.duration ? Number(data.duration) : undefined,
      });
      reset();
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Logger: ${habitName}`}>
      <form onSubmit={handleSubmit(handleFormSubmit)} className="flex flex-col gap-4">
        <div className="rounded-lg bg-gray-50 dark:bg-gray-800 p-3">
          <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
            {formatDate(date)}
          </p>
        </div>

        <div className="flex items-center gap-3 rounded-lg border border-gray-300 dark:border-gray-700 p-3">
          <input
            type="checkbox"
            id="completed"
            checked={completed}
            onChange={(e) => setCompleted(e.target.checked)}
            className="h-5 w-5 rounded border-gray-300 text-accent focus:ring-accent focus:ring-offset-0"
          />
          <label htmlFor="completed" className="text-sm font-medium text-gray-900 dark:text-gray-100">
            Séance complétée
          </label>
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Notes sur la séance
          </label>
          <textarea
            placeholder="Ex: 30 min de cardio, révision chapitre 5, yoga du matin..."
            rows={4}
            className="w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 py-2 text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20"
            {...register('notes')}
          />
          {errors.notes && (
            <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors.notes.message}</p>
          )}
        </div>

        <Input
          label="Durée (minutes)"
          type="number"
          placeholder="Ex: 30"
          error={errors.duration?.message}
          {...register('duration', { valueAsNumber: false })}
        />

        <div className="flex gap-2 mt-2">
          <Button type="button" onClick={onClose} variant="secondary" className="flex-1">
            Annuler
          </Button>
          <Button type="submit" loading={loading} className="flex-1">
            Sauvegarder
          </Button>
        </div>
      </form>
    </Modal>
  );
}
