import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Plus, LayoutGrid, List, Calendar } from 'lucide-react';
import toast from 'react-hot-toast';
import {
  DndContext,
  DragOverlay,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import type { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { taskService } from '../../services/task.service';
import { organizationService } from '../../services/organization.service';
import { goalService } from '../../services/goal.service';
import { tagService } from '../../services/tag.service';
import type { Task, TaskStatus, TaskPriority } from '../../types/task';
import type { Member } from '../../types/organization';
import type { Goal } from '../../types/goal';
import type { Tag } from '../../types/tag';
import Button from '../../components/ui/Button';
import TaskColumn from '../../components/features/TaskColumn';
import TaskListView from '../../components/features/TaskListView';
import TaskCalendarView from '../../components/features/TaskCalendarView';
import CreateTaskModal from '../../components/features/CreateTaskModal';
import TaskDetailModal from '../../components/features/TaskDetailModal';
import TaskCard from '../../components/features/TaskCard';
import { cn } from '../../lib/utils';

const columns: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

type ViewMode = 'kanban' | 'list' | 'calendar';

export default function TasksPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [activeTaskId, setActiveTaskId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>(() => {
    // Load from localStorage or default to kanban
    const saved = localStorage.getItem('hubz-tasks-view-mode');
    return (saved as ViewMode) || 'kanban';
  });

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
  );

  const fetchTasks = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await taskService.getByOrganization(orgId);
      setTasks(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des taches');
      console.error(error);
    }
  }, [orgId]);

  const fetchMembers = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await organizationService.getMembers(orgId);
      setMembers(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des membres');
      console.error(error);
    }
  }, [orgId]);

  const fetchGoals = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await goalService.getByOrganization(orgId);
      setGoals(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des objectifs');
      console.error(error);
    }
  }, [orgId]);

  const fetchTags = useCallback(async () => {
    if (!orgId) return;
    try {
      const data = await tagService.getByOrganization(orgId);
      setTags(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des tags');
      console.error(error);
    }
  }, [orgId]);

  useEffect(() => {
    fetchTasks();
    fetchMembers();
    fetchGoals();
    fetchTags();
  }, [fetchTasks, fetchMembers, fetchGoals, fetchTags]);

  const handleViewModeChange = (mode: ViewMode) => {
    setViewMode(mode);
    localStorage.setItem('hubz-tasks-view-mode', mode);
  };

  const handleCreate = async (data: {
    title: string;
    description?: string;
    priority?: TaskPriority;
    dueDate?: string;
    assigneeId?: string;
  }) => {
    if (!orgId) return;
    await taskService.create(orgId, data);
    await fetchTasks();
  };

  const handleUpdate = async (
    id: string,
    data: { title?: string; description?: string; priority?: TaskPriority; dueDate?: string },
  ) => {
    const updated = await taskService.update(id, data);
    setTasks((prev) => prev.map((t) => (t.id === id ? updated : t)));
    setSelectedTask(updated);
  };

  const handleStatusChange = async (id: string, status: TaskStatus) => {
    try {
      const updated = await taskService.updateStatus(id, { status });
      setTasks((prev) => prev.map((t) => (t.id === id ? updated : t)));
      if (selectedTask?.id === id) {
        setSelectedTask(updated);
      }
    } catch (error) {
      toast.error('Erreur lors de la mise a jour du statut');
      console.error(error);
    }
  };

  const handleDelete = async (id: string) => {
    await taskService.delete(id);
    setTasks((prev) => prev.filter((t) => t.id !== id));
    setSelectedTask(null);
  };

  const handleTagsChange = async (taskId: string, newTags: Tag[]) => {
    try {
      await tagService.setTaskTags(taskId, newTags.map((t) => t.id));
      // Update the task in state with new tags
      setTasks((prev) =>
        prev.map((t) => (t.id === taskId ? { ...t, tags: newTags } : t))
      );
      if (selectedTask?.id === taskId) {
        setSelectedTask({ ...selectedTask, tags: newTags });
      }
    } catch (error) {
      toast.error('Erreur lors de la mise a jour des tags');
      console.error(error);
    }
  };

  const handleCreateTag = async (name: string, color: string): Promise<Tag> => {
    if (!orgId) throw new Error('Organization ID is required');
    const newTag = await tagService.create(orgId, { name, color });
    setTags((prev) => [...prev, newTag]);
    return newTag;
  };

  const handleDueDateChange = async (taskId: string, newDueDate: string) => {
    try {
      const updated = await taskService.update(taskId, { dueDate: newDueDate });
      setTasks((prev) => prev.map((t) => (t.id === taskId ? updated : t)));
      if (selectedTask?.id === taskId) {
        setSelectedTask(updated);
      }
      toast.success('Date d\'echeance mise a jour');
    } catch (error) {
      toast.error('Erreur lors de la mise a jour de la date');
      console.error(error);
    }
  };

  const handleDragStart = (event: DragStartEvent) => {
    setActiveTaskId(event.active.id as string);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveTaskId(null);

    if (!over) return;

    const taskId = active.id as string;
    const newStatus = over.id as TaskStatus;

    const task = tasks.find((t) => t.id === taskId);
    if (!task || task.status === newStatus) return;

    try {
      await handleStatusChange(taskId, newStatus);
      toast.success('Tache deplacee');
    } catch (error) {
      toast.error('Erreur lors du deplacement de la tache');
      console.error(error);
    }
  };

  const tasksByStatus = (status: TaskStatus) => tasks.filter((t) => t.status === status);

  const activeTask = activeTaskId ? tasks.find((t) => t.id === activeTaskId) : null;

  return (
    <div className="flex h-full flex-col">
      {/* Toolbar */}
      <div className="flex items-center justify-between gap-2 px-4 py-2 border-b border-gray-200 dark:border-gray-700 sm:px-6 sm:py-3">
        {/* View mode toggle */}
        <div className="flex items-center gap-0.5 rounded-lg border border-gray-200 p-0.5 dark:border-gray-700 sm:gap-1 sm:p-1">
          <button
            onClick={() => handleViewModeChange('kanban')}
            className={cn(
              'flex items-center gap-1 rounded-md px-2 py-1.5 text-xs font-medium transition-colors sm:gap-1.5 sm:px-3 sm:text-sm',
              viewMode === 'kanban'
                ? 'bg-accent text-white'
                : 'text-gray-600 hover:bg-light-hover dark:text-gray-400 dark:hover:bg-dark-hover'
            )}
          >
            <LayoutGrid className="h-4 w-4" />
            <span className="hidden sm:inline">Kanban</span>
          </button>
          <button
            onClick={() => handleViewModeChange('list')}
            className={cn(
              'flex items-center gap-1 rounded-md px-2 py-1.5 text-xs font-medium transition-colors sm:gap-1.5 sm:px-3 sm:text-sm',
              viewMode === 'list'
                ? 'bg-accent text-white'
                : 'text-gray-600 hover:bg-light-hover dark:text-gray-400 dark:hover:bg-dark-hover'
            )}
          >
            <List className="h-4 w-4" />
            <span className="hidden sm:inline">Liste</span>
          </button>
          <button
            onClick={() => handleViewModeChange('calendar')}
            className={cn(
              'flex items-center gap-1 rounded-md px-2 py-1.5 text-xs font-medium transition-colors sm:gap-1.5 sm:px-3 sm:text-sm',
              viewMode === 'calendar'
                ? 'bg-accent text-white'
                : 'text-gray-600 hover:bg-light-hover dark:text-gray-400 dark:hover:bg-dark-hover'
            )}
          >
            <Calendar className="h-4 w-4" />
            <span className="hidden sm:inline">Calendrier</span>
          </button>
        </div>

        <Button size="sm" onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4" />
          <span className="hidden sm:inline">Nouvelle tache</span>
        </Button>
      </div>

      {/* View Content */}
      {viewMode === 'kanban' && (
        <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
          <div className="flex-1 overflow-x-auto px-4 pb-4 pt-3 sm:px-6 sm:pb-6 sm:pt-4">
            <div className="flex h-full gap-3 sm:grid sm:grid-cols-3 sm:gap-4" style={{ minWidth: '720px' }}>
              {columns.map((status) => (
                <TaskColumn
                  key={status}
                  status={status}
                  tasks={tasksByStatus(status)}
                  onTaskClick={setSelectedTask}
                  members={members}
                />
              ))}
            </div>
          </div>

          <DragOverlay>
            {activeTask && (
              <div className="rotate-3 opacity-80">
                <TaskCard task={activeTask} onClick={() => {}} members={members} />
              </div>
            )}
          </DragOverlay>
        </DndContext>
      )}

      {viewMode === 'list' && (
        <TaskListView
          tasks={tasks}
          members={members}
          availableTags={tags}
          onTaskClick={setSelectedTask}
          onStatusChange={handleStatusChange}
        />
      )}

      {viewMode === 'calendar' && (
        <div className="flex-1 overflow-auto px-4 pb-4 pt-3 sm:px-6 sm:pb-6 sm:pt-4">
          <TaskCalendarView
            tasks={tasks}
            members={members}
            onTaskClick={setSelectedTask}
            onDueDateChange={handleDueDateChange}
          />
        </div>
      )}

      {/* Modals */}
      <CreateTaskModal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        onSubmit={handleCreate}
        members={members}
        goals={goals}
      />

      {selectedTask && (
        <TaskDetailModal
          task={selectedTask}
          isOpen={!!selectedTask}
          onClose={() => setSelectedTask(null)}
          onUpdate={handleUpdate}
          onStatusChange={handleStatusChange}
          onDelete={handleDelete}
          goals={goals}
          availableTags={tags}
          onTagsChange={handleTagsChange}
          onCreateTag={handleCreateTag}
        />
      )}
    </div>
  );
}
