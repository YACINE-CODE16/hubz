import { useDroppable } from '@dnd-kit/core';
import type { Task, TaskStatus } from '../../types/task';
import type { Member } from '../../types/organization';
import TaskCard from './TaskCard';
import { cn } from '../../lib/utils';

const columnConfig: Record<TaskStatus, { title: string; accent: string }> = {
  TODO: { title: 'A faire', accent: 'bg-gray-400' },
  IN_PROGRESS: { title: 'En cours', accent: 'bg-accent' },
  DONE: { title: 'Termine', accent: 'bg-success' },
};

interface TaskColumnProps {
  status: TaskStatus;
  tasks: Task[];
  onTaskClick: (task: Task) => void;
  members: Member[];
}

export default function TaskColumn({ status, tasks, onTaskClick, members }: TaskColumnProps) {
  const config = columnConfig[status];
  const { setNodeRef, isOver } = useDroppable({
    id: status,
  });

  return (
    <div
      ref={setNodeRef}
      className={cn(
        'flex flex-col rounded-xl bg-light-hover/50 dark:bg-dark-hover/50 p-3 transition-colors',
        isOver && 'ring-2 ring-accent/50 bg-accent/5',
      )}
    >
      {/* Header */}
      <div className="mb-3 flex items-center gap-2 px-1">
        <div className={cn('h-2.5 w-2.5 rounded-full', config.accent)} />
        <h3 className="text-sm font-semibold text-gray-800 dark:text-gray-200">
          {config.title}
        </h3>
        <span className="ml-auto rounded-full bg-gray-200 dark:bg-white/10 px-2 py-0.5 text-xs font-medium text-gray-600 dark:text-gray-400">
          {tasks.length}
        </span>
      </div>

      {/* Tasks */}
      <div className="flex flex-1 flex-col gap-2 overflow-y-auto">
        {tasks.map((task) => (
          <TaskCard key={task.id} task={task} onClick={() => onTaskClick(task)} members={members} />
        ))}

        {tasks.length === 0 && (
          <p className="py-8 text-center text-xs text-gray-400 dark:text-gray-500">
            Aucune tache
          </p>
        )}
      </div>
    </div>
  );
}
