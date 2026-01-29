import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Plus } from 'lucide-react';
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
import type { Task, TaskStatus, TaskPriority } from '../../types/task';
import type { Member } from '../../types/organization';
import type { Goal } from '../../types/goal';
import Button from '../../components/ui/Button';
import TaskColumn from '../../components/features/TaskColumn';
import CreateTaskModal from '../../components/features/CreateTaskModal';
import TaskDetailModal from '../../components/features/TaskDetailModal';
import TaskCard from '../../components/features/TaskCard';

const columns: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

export default function TasksPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [activeTaskId, setActiveTaskId] = useState<string | null>(null);

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
      toast.error('Erreur lors du chargement des tâches');
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

  useEffect(() => {
    fetchTasks();
    fetchMembers();
    fetchGoals();
  }, [fetchTasks, fetchMembers, fetchGoals]);

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
    const updated = await taskService.updateStatus(id, { status });
    setTasks((prev) => prev.map((t) => (t.id === id ? updated : t)));
    setSelectedTask(updated);
  };

  const handleDelete = async (id: string) => {
    await taskService.delete(id);
    setTasks((prev) => prev.filter((t) => t.id !== id));
    setSelectedTask(null);
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
      toast.success('Tâche déplacée');
    } catch (error) {
      toast.error('Erreur lors du déplacement de la tâche');
      console.error(error);
    }
  };

  const tasksByStatus = (status: TaskStatus) => tasks.filter((t) => t.status === status);

  const activeTask = activeTaskId ? tasks.find((t) => t.id === activeTaskId) : null;

  return (
    <div className="flex h-full flex-col">
      {/* Toolbar */}
      <div className="flex items-center justify-end px-6 py-3">
        <Button size="sm" onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4" />
          Nouvelle tache
        </Button>
      </div>

      {/* Kanban */}
      <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <div className="flex-1 overflow-x-auto px-6 pb-6">
          <div className="grid h-full grid-cols-3 gap-4">
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
        />
      )}
    </div>
  );
}
