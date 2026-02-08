import { useState } from 'react';
import { Trash2, Calendar, AlertTriangle, Target } from 'lucide-react';
import type { Task, TaskStatus, TaskPriority } from '../../types/task';
import type { Goal } from '../../types/goal';
import type { Tag } from '../../types/tag';
import Modal from '../ui/Modal';
import Input from '../ui/Input';
import Button from '../ui/Button';
import TaskComments from './TaskComments';
import TaskChecklist from './TaskChecklist';
import TaskAttachments from './TaskAttachments';
import TaskHistoryTimeline from './TaskHistoryTimeline';
import TagSelector from './TagSelector';
import { cn } from '../../lib/utils';

const statusOptions: { value: TaskStatus; label: string; accent: string }[] = [
  { value: 'TODO', label: 'A faire', accent: 'bg-gray-400' },
  { value: 'IN_PROGRESS', label: 'En cours', accent: 'bg-accent' },
  { value: 'DONE', label: 'Termine', accent: 'bg-success' },
];

const priorityOptions: { value: TaskPriority; label: string; className: string }[] = [
  { value: 'LOW', label: 'Basse', className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400' },
  { value: 'MEDIUM', label: 'Moyenne', className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400' },
  { value: 'HIGH', label: 'Haute', className: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-400' },
  { value: 'URGENT', label: 'Urgente', className: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400' },
];

interface TaskDetailModalProps {
  task: Task;
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (id: string, data: { title?: string; description?: string; priority?: TaskPriority; goalId?: string; dueDate?: string }) => Promise<void>;
  onStatusChange: (id: string, status: TaskStatus) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
  goals: Goal[];
  availableTags?: Tag[];
  onTagsChange?: (taskId: string, tags: Tag[]) => Promise<void>;
  onCreateTag?: (name: string, color: string) => Promise<Tag>;
}

export default function TaskDetailModal({
  task,
  isOpen,
  onClose,
  onUpdate,
  onStatusChange,
  onDelete,
  goals,
  availableTags = [],
  onTagsChange,
  onCreateTag,
}: TaskDetailModalProps) {
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(task.title);
  const [description, setDescription] = useState(task.description || '');
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'DONE';

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await onUpdate(task.id, {
        title,
        description: description || undefined,
      });
      setEditing(false);
    } catch (err) {
      setError('Erreur lors de la sauvegarde de la tâche');
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    setDeleting(true);
    setError(null);
    try {
      await onDelete(task.id);
      onClose();
    } catch (err) {
      setError('Erreur lors de la suppression de la tâche');
      console.error(err);
    } finally {
      setDeleting(false);
    }
  };

  const formatDate = (date: string) =>
    new Date(date).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={editing ? 'Modifier la tache' : undefined} className="sm:max-w-lg">
      <div className="flex flex-col gap-4 sm:gap-5">
        {/* Error message */}
        {error && (
          <div className="rounded-lg bg-error/10 border border-error/20 px-4 py-3 text-sm text-error">
            {error}
          </div>
        )}

        {/* Title */}
        {editing ? (
          <Input
            label="Titre"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        ) : (
          <h2
            onClick={() => setEditing(true)}
            className="cursor-pointer text-lg font-semibold text-gray-900 dark:text-gray-100 hover:text-accent transition-colors"
          >
            {task.title}
          </h2>
        )}

        {/* Status buttons */}
        <div>
          <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
            Statut
          </label>
          <div className="flex flex-wrap gap-2">
            {statusOptions.map((s) => (
              <button
                key={s.value}
                onClick={() => onStatusChange(task.id, s.value)}
                className={cn(
                  'flex flex-1 items-center justify-center gap-1.5 rounded-lg px-3 py-2 text-xs font-medium transition-colors sm:flex-none sm:py-1.5',
                  task.status === s.value
                    ? 'bg-accent/10 text-accent ring-1 ring-accent/30'
                    : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10',
                )}
              >
                <div className={cn('h-2 w-2 rounded-full', s.accent)} />
                {s.label}
              </button>
            ))}
          </div>
        </div>

        {/* Description */}
        {editing ? (
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full rounded-lg border border-gray-200 dark:border-white/10 bg-white/60 dark:bg-white/5 backdrop-blur-sm px-3 py-2 text-sm text-gray-900 dark:text-gray-100 placeholder:text-gray-400 focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/20"
              placeholder="Ajouter une description..."
            />
          </div>
        ) : (
          task.description && (
            <div>
              <label className="mb-1 block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                Description
              </label>
              <p
                onClick={() => setEditing(true)}
                className="cursor-pointer text-sm text-gray-700 dark:text-gray-300 hover:text-accent transition-colors whitespace-pre-wrap"
              >
                {task.description}
              </p>
            </div>
          )
        )}

        {/* Priority */}
        <div>
          <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
            Priorite
          </label>
          <div className="flex flex-wrap gap-2">
            {priorityOptions.map((p) => (
              <button
                key={p.value}
                onClick={() => onUpdate(task.id, { priority: p.value })}
                className={cn(
                  'rounded-full px-3 py-1 text-xs font-medium transition-all',
                  p.className,
                  task.priority === p.value && 'ring-2 ring-offset-1 ring-offset-1 ring-offset-light-card dark:ring-offset-dark-card ring-current',
                )}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>

        {/* Tags */}
        {(availableTags.length > 0 || onCreateTag) && (
          <div>
            <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
              Tags
            </label>
            <TagSelector
              availableTags={availableTags}
              selectedTags={task.tags || []}
              onTagsChange={(tags) => onTagsChange?.(task.id, tags)}
              onCreateTag={onCreateTag}
            />
          </div>
        )}

        {/* Goal */}
        {goals.length > 0 && (
          <div>
            <label className="mb-2 block text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wide">
              Objectif lie
            </label>
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => onUpdate(task.id, { goalId: undefined })}
                className={cn(
                  'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                  !task.goalId
                    ? 'bg-accent/10 text-accent ring-1 ring-accent/30'
                    : 'bg-light-hover dark:bg-dark-hover text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-white/10',
                )}
              >
                <Target className="h-3.5 w-3.5" />
                Aucun
              </button>
              {goals.map((goal) => (
                <button
                  key={goal.id}
                  onClick={() => onUpdate(task.id, { goalId: goal.id })}
                  className={cn(
                    'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                    task.goalId === goal.id
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

        {/* Due date */}
        {task.dueDate && (
          <div className={cn('flex items-center gap-2 text-sm', isOverdue ? 'text-error' : 'text-gray-600 dark:text-gray-400')}>
            {isOverdue && <AlertTriangle className="h-4 w-4" />}
            <Calendar className="h-4 w-4" />
            <span>{formatDate(task.dueDate)}</span>
            {isOverdue && <span className="text-xs font-medium">(en retard)</span>}
          </div>
        )}

        {/* Meta */}
        <p className="text-xs text-gray-400 dark:text-gray-500">
          Creee le {formatDate(task.createdAt)}
        </p>

        {/* Checklist */}
        <TaskChecklist taskId={task.id} />

        {/* Attachments */}
        <TaskAttachments taskId={task.id} />

        {/* Comments */}
        <TaskComments taskId={task.id} organizationId={task.organizationId || ''} />

        {/* History */}
        <TaskHistoryTimeline taskId={task.id} />

        {/* Actions */}
        <div className="flex flex-col gap-2 border-t border-gray-200/50 dark:border-white/10 pt-4 sm:flex-row">
          {editing ? (
            <>
              <Button onClick={handleSave} loading={saving} className="flex-1">
                Enregistrer
              </Button>
              <Button variant="ghost" onClick={() => { setEditing(false); setTitle(task.title); setDescription(task.description || ''); }} className="w-full sm:w-auto">
                Annuler
              </Button>
            </>
          ) : (
            <>
              <Button variant="ghost" size="sm" onClick={() => setEditing(true)} className="w-full sm:w-auto">
                Modifier
              </Button>
              <Button
                variant="ghost"
                size="sm"
                loading={deleting}
                onClick={handleDelete}
                className="w-full text-error hover:bg-error/10 sm:ml-auto sm:w-auto"
              >
                <Trash2 className="h-4 w-4" />
                Supprimer
              </Button>
            </>
          )}
        </div>
      </div>
    </Modal>
  );
}
