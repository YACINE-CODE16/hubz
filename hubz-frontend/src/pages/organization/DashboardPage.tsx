import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CheckSquare, Users, AlertTriangle, TrendingUp } from 'lucide-react';
import toast from 'react-hot-toast';
import { taskService } from '../../services/task.service';
import { organizationService } from '../../services/organization.service';
import type { Task } from '../../types/task';
import type { Member } from '../../types/organization';
import Card from '../../components/ui/Card';
import { cn } from '../../lib/utils';

interface StatsCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: string;
  onClick?: () => void;
}

function StatsCard({ title, value, icon, color, onClick }: StatsCardProps) {
  return (
    <Card
      onClick={onClick}
      className={cn(
        'p-6 transition-all',
        onClick && 'cursor-pointer hover:shadow-lg hover:scale-105',
      )}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{title}</p>
          <p className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
        </div>
        <div className={cn('rounded-full p-3', color)}>{icon}</div>
      </div>
    </Card>
  );
}

export default function DashboardPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!orgId) return;
    setLoading(true);
    try {
      const [tasksData, membersData] = await Promise.all([
        taskService.getByOrganization(orgId),
        organizationService.getMembers(orgId),
      ]);
      setTasks(tasksData);
      setMembers(membersData);
    } catch (error) {
      toast.error('Erreur lors du chargement des données');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [orgId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const todoCount = tasks.filter((t) => t.status === 'TODO').length;
  const inProgressCount = tasks.filter((t) => t.status === 'IN_PROGRESS').length;
  const doneCount = tasks.filter((t) => t.status === 'DONE').length;
  const overdueCount = tasks.filter(
    (t) => t.dueDate && new Date(t.dueDate) < new Date() && t.status !== 'DONE',
  ).length;

  const recentTasks = tasks
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  const formatDate = (date: string) =>
    new Date(date).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-center">
          <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-gray-200 border-t-accent dark:border-gray-700" />
          <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">Chargement...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Dashboard</h2>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Vue d'ensemble de votre organisation
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatsCard
          title="A faire"
          value={todoCount}
          icon={<CheckSquare className="h-6 w-6 text-gray-600" />}
          color="bg-gray-100 dark:bg-gray-800"
          onClick={() => navigate(`/organization/${orgId}/tasks`)}
        />
        <StatsCard
          title="En cours"
          value={inProgressCount}
          icon={<TrendingUp className="h-6 w-6 text-blue-600" />}
          color="bg-blue-100 dark:bg-blue-900/40"
          onClick={() => navigate(`/organization/${orgId}/tasks`)}
        />
        <StatsCard
          title="Terminées"
          value={doneCount}
          icon={<CheckSquare className="h-6 w-6 text-green-600" />}
          color="bg-green-100 dark:bg-green-900/40"
          onClick={() => navigate(`/organization/${orgId}/tasks`)}
        />
        <StatsCard
          title="En retard"
          value={overdueCount}
          icon={<AlertTriangle className="h-6 w-6 text-red-600" />}
          color="bg-red-100 dark:bg-red-900/40"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Tasks */}
        <Card className="p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
            Tâches récentes
          </h3>
          <div className="mt-4 space-y-3">
            {recentTasks.length === 0 ? (
              <p className="text-sm text-gray-500 dark:text-gray-400">Aucune tâche</p>
            ) : (
              recentTasks.map((task) => (
                <div
                  key={task.id}
                  className="flex items-start justify-between rounded-lg border border-gray-200/50 dark:border-white/10 p-3 hover:bg-light-hover dark:hover:bg-dark-hover transition-colors cursor-pointer"
                  onClick={() => navigate(`/organization/${orgId}/tasks`)}
                >
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 line-clamp-1">
                      {task.title}
                    </p>
                    <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                      {formatDate(task.createdAt)}
                    </p>
                  </div>
                  <span
                    className={cn(
                      'ml-2 shrink-0 rounded-full px-2 py-0.5 text-xs font-medium',
                      task.status === 'TODO' &&
                        'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400',
                      task.status === 'IN_PROGRESS' &&
                        'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400',
                      task.status === 'DONE' &&
                        'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-400',
                    )}
                  >
                    {task.status === 'TODO' && 'A faire'}
                    {task.status === 'IN_PROGRESS' && 'En cours'}
                    {task.status === 'DONE' && 'Terminé'}
                  </span>
                </div>
              ))
            )}
          </div>
        </Card>

        {/* Team Members */}
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Équipe</h3>
            <span className="flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400">
              <Users className="h-4 w-4" />
              {members.length}
            </span>
          </div>
          <div className="mt-4 space-y-3">
            {members.slice(0, 5).map((member) => (
              <div
                key={member.id}
                className="flex items-center gap-3 rounded-lg border border-gray-200/50 dark:border-white/10 p-3"
              >
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-accent/10 text-sm font-semibold text-accent">
                  {member.firstName?.[0]}
                  {member.lastName?.[0]}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {member.firstName} {member.lastName}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">{member.email}</p>
                </div>
                <span
                  className={cn(
                    'shrink-0 rounded-full px-2 py-0.5 text-xs font-medium',
                    member.role === 'OWNER' &&
                      'bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-400',
                    member.role === 'ADMIN' &&
                      'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400',
                    (member.role === 'MEMBER' || member.role === 'VIEWER') &&
                      'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400',
                  )}
                >
                  {member.role === 'OWNER' && 'Propriétaire'}
                  {member.role === 'ADMIN' && 'Admin'}
                  {member.role === 'MEMBER' && 'Membre'}
                  {member.role === 'VIEWER' && 'Observateur'}
                </span>
              </div>
            ))}
            {members.length > 5 && (
              <p className="text-center text-xs text-gray-500 dark:text-gray-400">
                +{members.length - 5} autres membres
              </p>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}
