import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { UserCircle2, Target } from 'lucide-react';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import type { TaskPriority } from '../../types/task';
import type { Member } from '../../types/organization';
import type { Goal } from '../../types/goal';
import { cn } from '../../lib/utils';

const priorities: { value: TaskPriority; label: string; className: string }[] = [
  { value: 'LOW', label: 'Basse', className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400' },
  { value: 'MEDIUM', label: 'Moyenne', className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400' },
  { value: 'HIGH', label: 'Haute', className: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-400' },
  { value: 'URGENT', label: 'Urgente', className: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400' },
];

const schema = z.object({
  title: z.string().min(1, 'Titre requis'),
  description: z.string().optional(),
  dueDate: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

interface CreateTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { title: string; description?: string; priority?: TaskPriority; goalId?: string; dueDate?: string; assigneeId?: string }) => Promise<void>;
  members: Member[];
  goals: Goal[];
}

export default function CreateTaskModal({ isOpen, onClose, onSubmit, members, goals }: CreateTaskModalProps) {
  const [selectedPriority, setSelectedPriority] = useState<TaskPriority>('MEDIUM');
  const [selectedAssignee, setSelectedAssignee] = useState<string | undefined>(undefined);
  const [selectedGoal, setSelectedGoal] = useState<string | undefined>(undefined);
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
      await onSubmit({
        title: data.title,
        description: data.description || undefined,
        priority: selectedPriority,
        goalId: selectedGoal,
        dueDate: data.dueDate || undefined,
        assigneeId: selectedAssignee,
      });
      reset();
      setSelectedPriority('MEDIUM');
      setSelectedAssignee(undefined);
      setSelectedGoal(undefined);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvelle tache">
      <form onSubmit={handleSubmit(handleFormSubmit)} className="flex flex-col gap-4">
        <Input
          label="Titre"
          placeholder="Que faut-il faire ?"
          error={errors.title?.message}
          {...register('title')}
        />

        <Input
          label="Description"
          placeholder="Details supplementaires (optionnel)"
          error={errors.description?.message}
          {...register('description')}
        />

        <Input
          label="Date limite"
          type="datetime-local"
          {...register('dueDate')}
        />

        {goals.length > 0 && (
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Objectif lie (optionnel)
            </label>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => setSelectedGoal(undefined)}
                className={cn(
                  'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                  selectedGoal === undefined
                    ? 'bg-accent/10 text-accent ring-1 ring-accent/30'
                    : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10',
                )}
              >
                <Target className="h-3.5 w-3.5" />
                Aucun objectif
              </button>
              {goals.map((goal) => (
                <button
                  key={goal.id}
                  type="button"
                  onClick={() => setSelectedGoal(goal.id)}
                  className={cn(
                    'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                    selectedGoal === goal.id
                      ? 'bg-accent/10 text-accent ring-1 ring-accent/30'
                      : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10',
                  )}
                >
                  <Target className="h-3.5 w-3.5" />
                  {goal.title}
                </button>
              ))}
            </div>
          </div>
        )}

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Assigner a
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setSelectedAssignee(undefined)}
              className={cn(
                'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                selectedAssignee === undefined
                  ? 'bg-accent/10 text-accent ring-1 ring-accent/30'
                  : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10',
              )}
            >
              <UserCircle2 className="h-3.5 w-3.5" />
              Non assigne
            </button>
            {members.map((member) => (
              <button
                key={member.userId}
                type="button"
                onClick={() => setSelectedAssignee(member.userId)}
                className={cn(
                  'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                  selectedAssignee === member.userId
                    ? 'bg-accent/10 text-accent ring-1 ring-accent/30'
                    : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10',
                )}
              >
                {member.firstName} {member.lastName}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Priorite
          </label>
          <div className="flex flex-wrap gap-2">
            {priorities.map((p) => (
              <button
                key={p.value}
                type="button"
                onClick={() => setSelectedPriority(p.value)}
                className={cn(
                  'rounded-full px-3 py-1 text-xs font-medium transition-all',
                  p.className,
                  selectedPriority === p.value && 'ring-2 ring-offset-1 ring-offset-light-card dark:ring-offset-dark-card ring-current',
                )}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>

        <Button type="submit" loading={loading} className="mt-1 w-full">
          Creer la tache
        </Button>
      </form>
    </Modal>
  );
}
