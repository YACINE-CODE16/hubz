import { useEffect, useState } from 'react';
import { Target, Plus } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Card from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Modal from '../../components/ui/Modal';
import Input from '../../components/ui/Input';
import GoalAnalytics from '../../components/features/GoalAnalytics';
import GoalCard from '../../components/features/GoalCard';
import { goalService } from '../../services/goal.service';
import type { Goal, GoalType, CreateGoalRequest, UpdateGoalRequest } from '../../types/goal';

export default function PersonalGoalsPage() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedGoal, setSelectedGoal] = useState<Goal | null>(null);
  const [analyticsKey, setAnalyticsKey] = useState(0);

  useEffect(() => {
    fetchGoals();
  }, []);

  const fetchGoals = async () => {
    try {
      setLoading(true);
      const data = await goalService.getPersonalGoals();
      setGoals(data);
    } catch (error) {
      toast.error('Erreur lors du chargement des objectifs');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (data: CreateGoalRequest) => {
    try {
      await goalService.createPersonalGoal(data);
      toast.success('Objectif cree');
      setIsCreateModalOpen(false);
      fetchGoals();
      setAnalyticsKey((prev) => prev + 1);
    } catch (error) {
      toast.error('Erreur lors de la creation');
    }
  };

  const handleUpdate = async (data: UpdateGoalRequest) => {
    if (!selectedGoal) return;
    try {
      await goalService.update(selectedGoal.id, data);
      toast.success('Objectif mis a jour');
      setIsEditModalOpen(false);
      setSelectedGoal(null);
      fetchGoals();
      setAnalyticsKey((prev) => prev + 1);
    } catch (error) {
      toast.error('Erreur lors de la mise a jour');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Supprimer cet objectif ?')) return;
    try {
      await goalService.delete(id);
      toast.success('Objectif supprime');
      fetchGoals();
      setAnalyticsKey((prev) => prev + 1);
    } catch (error) {
      toast.error('Erreur lors de la suppression');
    }
  };

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-gray-500 dark:text-gray-400">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Mes objectifs</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Gérez vos objectifs personnels. La progression se calcule automatiquement en fonction des tâches associées.
          </p>
        </div>
        <Button onClick={() => setIsCreateModalOpen(true)}>
          <Plus className="h-4 w-4" />
          Nouvel objectif
        </Button>
      </div>

      {/* Goals Grid */}
      {goals.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 dark:bg-gray-800">
            <Target className="h-8 w-8 text-gray-400" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-gray-900 dark:text-gray-100">
            Aucun objectif
          </h3>
          <p className="mt-2 text-center text-sm text-gray-500 dark:text-gray-400">
            Commencez par créer votre premier objectif. Vous pourrez ensuite y associer des tâches.
          </p>
          <Button onClick={() => setIsCreateModalOpen(true)} className="mt-4">
            <Plus className="h-4 w-4" />
            Créer un objectif
          </Button>
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {goals.map((goal) => (
            <GoalCard
              key={goal.id}
              goal={goal}
              onEdit={(goal) => {
                setSelectedGoal(goal);
                setIsEditModalOpen(true);
              }}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      {/* Create Modal */}
      <CreateGoalModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onCreate={handleCreate}
      />

      {/* Edit Modal */}
      {selectedGoal && (
        <EditGoalModal
          isOpen={isEditModalOpen}
          onClose={() => {
            setIsEditModalOpen(false);
            setSelectedGoal(null);
          }}
          onUpdate={handleUpdate}
          goal={selectedGoal}
        />
      )}

      {/* Analytics Section */}
      <GoalAnalytics refreshKey={analyticsKey} />
    </div>
  );
}

interface CreateGoalModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateGoalRequest) => void;
}

function CreateGoalModal({ isOpen, onClose, onCreate }: CreateGoalModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<GoalType>('SHORT');
  const [deadline, setDeadline] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    onCreate({
      title: title.trim(),
      description: description.trim() || undefined,
      type,
      deadline: deadline || undefined,
    });

    // Reset form
    setTitle('');
    setDescription('');
    setType('SHORT');
    setDeadline('');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Nouvel objectif">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Titre"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ex: Augmenter les ventes de 20%"
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Description
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Détails de l'objectif..."
            rows={3}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Type
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['SHORT', 'MEDIUM', 'LONG'] as GoalType[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                className={`rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                  type === t
                    ? 'border-accent bg-accent/10 text-accent'
                    : 'border-gray-300 text-gray-700 hover:border-gray-400 dark:border-gray-600 dark:text-gray-300 dark:hover:border-gray-500'
                }`}
              >
                {t === 'SHORT' ? 'Court' : t === 'MEDIUM' ? 'Moyen' : 'Long'} terme
              </button>
            ))}
          </div>
        </div>

        <Input
          label="Échéance (optionnel)"
          type="date"
          value={deadline}
          onChange={(e) => setDeadline(e.target.value)}
        />

        <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-3">
          <p className="text-xs text-blue-800 dark:text-blue-300">
            💡 La progression de cet objectif sera calculée automatiquement en fonction des tâches qui lui seront associées.
          </p>
        </div>

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Créer
          </Button>
        </div>
      </form>
    </Modal>
  );
}

interface EditGoalModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (data: UpdateGoalRequest) => void;
  goal: Goal;
}

function EditGoalModal({ isOpen, onClose, onUpdate, goal }: EditGoalModalProps) {
  const [title, setTitle] = useState(goal.title);
  const [description, setDescription] = useState(goal.description || '');
  const [type, setType] = useState<GoalType>(goal.type);
  const [deadline, setDeadline] = useState(
    goal.deadline ? goal.deadline.split('T')[0] : '',
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    onUpdate({
      title: title.trim(),
      description: description.trim() || undefined,
      type,
      deadline: deadline || undefined,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Modifier l'objectif">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Titre"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Description
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent dark:border-gray-600 dark:bg-dark-card dark:text-gray-100 dark:placeholder-gray-500"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
            Type
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['SHORT', 'MEDIUM', 'LONG'] as GoalType[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                className={`rounded-lg border px-3 py-2 text-sm font-medium transition-colors ${
                  type === t
                    ? 'border-accent bg-accent/10 text-accent'
                    : 'border-gray-300 text-gray-700 hover:border-gray-400 dark:border-gray-600 dark:text-gray-300 dark:hover:border-gray-500'
                }`}
              >
                {t === 'SHORT' ? 'Court' : t === 'MEDIUM' ? 'Moyen' : 'Long'} terme
              </button>
            ))}
          </div>
        </div>

        <Input
          label="Échéance (optionnel)"
          type="date"
          value={deadline}
          onChange={(e) => setDeadline(e.target.value)}
        />

        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onClose} className="flex-1">
            Annuler
          </Button>
          <Button type="submit" className="flex-1">
            Enregistrer
          </Button>
        </div>
      </form>
    </Modal>
  );
}
