import { Calendar, AlertTriangle, UserCircle2 } from 'lucide-react';
import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import type { Task, TaskPriority } from '../../types/task';
import type { Member } from '../../types/organization';
import Card from '../ui/Card';
import { cn } from '../../lib/utils';

const priorityConfig: Record<TaskPriority, { label: string; className: string }> = {
  LOW: { label: 'Basse', className: 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400' },
  MEDIUM: { label: 'Moyenne', className: 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400' },
  HIGH: { label: 'Haute', className: 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-400' },
  URGENT: { label: 'Urgente', className: 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400' },
};

interface TaskCardProps {
  task: Task;
  onClick: () => void;
  members: Member[];
}

export default function TaskCard({ task, onClick, members }: TaskCardProps) {
  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'DONE';
  const priority = task.priority ? priorityConfig[task.priority] : null;
  const assignee = task.assigneeId ? members.find((m) => m.userId === task.assigneeId) : null;

  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
  });

  const style = {
    transform: CSS.Translate.toString(transform),
  };

  const formatDate = (date: string) =>
    new Date(date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      <Card
        onClick={onClick}
        className={cn(
          'cursor-grab active:cursor-grabbing p-4 transition-all hover:shadow-md',
          isOverdue && 'border-error/50',
          isDragging && 'opacity-50',
        )}
      >
      <h4 className="text-sm font-medium text-gray-900 dark:text-gray-100 line-clamp-2">
        {task.title}
      </h4>

      {task.description && (
        <p className="mt-1.5 text-xs text-gray-500 dark:text-gray-400 line-clamp-2">
          {task.description}
        </p>
      )}

      <div className="mt-3 flex flex-wrap items-center gap-2">
        {priority && (
          <span className={cn('rounded-full px-2 py-0.5 text-xs font-medium', priority.className)}>
            {priority.label}
          </span>
        )}

        {task.dueDate && (
          <span
            className={cn(
              'inline-flex items-center gap-1 text-xs',
              isOverdue
                ? 'font-medium text-error'
                : 'text-gray-500 dark:text-gray-400',
            )}
          >
            {isOverdue && <AlertTriangle className="h-3 w-3" />}
            <Calendar className="h-3 w-3" />
            {formatDate(task.dueDate)}
          </span>
        )}

        {assignee && (
          <span className="inline-flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
            <UserCircle2 className="h-3 w-3" />
            {assignee.firstName} {assignee.lastName}
          </span>
        )}
      </div>
    </Card>
    </div>
  );
}
