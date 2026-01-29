import { useState, useEffect } from 'react';
import { Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import Button from '../../components/ui/Button';
import HabitCard from '../../components/features/HabitCard';
import CreateHabitModal from '../../components/features/CreateHabitModal';
import EditHabitModal from '../../components/features/EditHabitModal';
import { habitService } from '../../services/habit.service';
import type { Habit, HabitLog, CreateHabitRequest, UpdateHabitRequest } from '../../types/habit';

export default function HabitsPage() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [habitLogs, setHabitLogs] = useState<Record<string, HabitLog[]>>({});
  const [loading, setLoading] = useState(true);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedHabit, setSelectedHabit] = useState<Habit | null>(null);

  useEffect(() => {
    loadHabits();
  }, []);

  const loadHabits = async () => {
    try {
      setLoading(true);
      const data = await habitService.getUserHabits();
      setHabits(data);

      const logsPromises = data.map(async (habit) => {
        const logs = await habitService.getHabitLogs(habit.id);
        return { habitId: habit.id, logs };
      });

      const logsResults = await Promise.all(logsPromises);
      const logsMap: Record<string, HabitLog[]> = {};
      logsResults.forEach(({ habitId, logs }) => {
        logsMap[habitId] = logs;
      });
      setHabitLogs(logsMap);
    } catch (error) {
      toast.error('Erreur lors du chargement des habitudes');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (data: CreateHabitRequest) => {
    try {
      await habitService.create(data);
      toast.success('Habitude créée avec succès');
      await loadHabits();
    } catch (error) {
      toast.error("Erreur lors de la création de l'habitude");
      throw error;
    }
  };

  const handleUpdate = async (id: string, data: UpdateHabitRequest) => {
    try {
      await habitService.update(id, data);
      toast.success('Habitude mise à jour avec succès');
      await loadHabits();
    } catch (error) {
      toast.error("Erreur lors de la mise à jour de l'habitude");
      throw error;
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette habitude ?')) return;

    try {
      await habitService.delete(id);
      toast.success('Habitude supprimée avec succès');
      await loadHabits();
    } catch (error) {
      toast.error("Erreur lors de la suppression de l'habitude");
      console.error(error);
    }
  };

  const handleLogHabit = async (habitId: string, data: { date: string; completed: boolean; notes?: string; duration?: number }) => {
    try {
      const result = await habitService.logHabit(habitId, data);

      setHabitLogs((prev) => {
        const logs = prev[habitId] || [];
        const existingLogIndex = logs.findIndex((log) => log.date === data.date);

        if (existingLogIndex >= 0) {
          const updatedLogs = [...logs];
          updatedLogs[existingLogIndex] = result;
          return { ...prev, [habitId]: updatedLogs };
        } else {
          return {
            ...prev,
            [habitId]: [...logs, result],
          };
        }
      });

      toast.success('Séance enregistrée avec succès');
    } catch (error) {
      toast.error("Erreur lors de l'enregistrement");
      console.error(error);
    }
  };

  const handleEdit = (habit: Habit) => {
    setSelectedHabit(habit);
    setIsEditModalOpen(true);
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-accent border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Mes habitudes</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Suivez vos habitudes quotidiennes et hebdomadaires
          </p>
        </div>
        <Button onClick={() => setIsCreateModalOpen(true)}>
          <Plus className="h-4 w-4" />
          Nouvelle habitude
        </Button>
      </div>

      {habits.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-gray-300 dark:border-gray-700 bg-light-card/50 dark:bg-dark-card/50 p-12 text-center">
          <p className="text-lg font-medium text-gray-900 dark:text-gray-100">
            Aucune habitude pour le moment
          </p>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Créez votre première habitude pour commencer le suivi
          </p>
          <Button onClick={() => setIsCreateModalOpen(true)} className="mt-4">
            <Plus className="h-4 w-4" />
            Créer une habitude
          </Button>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {habits.map((habit) => (
            <HabitCard
              key={habit.id}
              habit={habit}
              logs={habitLogs[habit.id] || []}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onLogHabit={handleLogHabit}
            />
          ))}
        </div>
      )}

      <CreateHabitModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSubmit={handleCreate}
      />

      <EditHabitModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        habit={selectedHabit}
        onSubmit={handleUpdate}
      />
    </div>
  );
}
