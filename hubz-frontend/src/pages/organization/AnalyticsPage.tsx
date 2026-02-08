import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  BarChart3,
  Users,
  Target,
  TrendingUp,
  AlertTriangle,
  Clock,
  CheckCircle2,
  Activity,
  Timer,
  UserX,
  Trophy,
  Calendar,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { analyticsService } from '../../services/analytics.service';
import { organizationService } from '../../services/organization.service';
import type {
  TaskAnalytics,
  MemberAnalytics,
  GoalAnalytics,
  OrganizationAnalytics,
} from '../../types/analytics';
import type { Member } from '../../types/organization';
import Card from '../../components/ui/Card';
import {
  ChartContainer,
  SimpleLineChart,
  SimplePieChart,
  SimpleBarChart,
  StackedAreaChart,
  VelocityChart,
  BurndownChart,
  BurnupChart,
  ThroughputChart,
  CycleTimeHistogram,
  LeadTimeTrendChart,
  WIPChart,
  ProgressGauge,
  StatCard,
} from '../../components/ui/Charts';
import TreemapCard from '../../components/features/TreemapCard';
import DashboardFilters from '../../components/features/DashboardFilters';
import { useAnalyticsFilters } from '../../hooks/useAnalyticsFilters';
import { cn } from '../../lib/utils';

type TabType = 'overview' | 'tasks' | 'members' | 'goals';

export default function AnalyticsPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const [loading, setLoading] = useState(true);
  const [members, setMembers] = useState<Member[]>([]);

  const { filters, effectiveDateRange, setFilters, resetFilters } = useAnalyticsFilters();

  const [orgAnalytics, setOrgAnalytics] = useState<OrganizationAnalytics | null>(null);
  const [taskAnalytics, setTaskAnalytics] = useState<TaskAnalytics | null>(null);
  const [memberAnalytics, setMemberAnalytics] = useState<MemberAnalytics | null>(null);
  const [goalAnalytics, setGoalAnalytics] = useState<GoalAnalytics | null>(null);

  // Fetch members for the filter dropdown
  useEffect(() => {
    if (!orgId) return;
    organizationService.getMembers(orgId).then(setMembers).catch(console.error);
  }, [orgId]);

  const fetchData = useCallback(async () => {
    if (!orgId) return;
    setLoading(true);
    try {
      const [org, tasks, membersAnalytics, goals] = await Promise.all([
        analyticsService.getOrganizationAnalytics(orgId),
        analyticsService.getTaskAnalytics(orgId, filters, effectiveDateRange),
        analyticsService.getMemberAnalytics(orgId, filters, effectiveDateRange),
        analyticsService.getGoalAnalytics(orgId, filters, effectiveDateRange),
      ]);
      setOrgAnalytics(org);
      setTaskAnalytics(tasks);
      setMemberAnalytics(membersAnalytics);
      setGoalAnalytics(goals);
    } catch (error) {
      toast.error('Erreur lors du chargement des analytics');
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [orgId, filters, effectiveDateRange]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="text-center">
          <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-gray-200 border-t-accent dark:border-gray-700" />
          <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">Chargement des analytics...</p>
        </div>
      </div>
    );
  }

  const tabs = [
    { id: 'overview', label: 'Vue d\'ensemble', icon: Activity },
    { id: 'tasks', label: 'Taches', icon: CheckCircle2 },
    { id: 'members', label: 'Membres', icon: Users },
    { id: 'goals', label: 'Objectifs', icon: Target },
  ] as const;

  return (
    <div className="flex h-full flex-col gap-6 overflow-auto p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Analytics</h2>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Statistiques et indicateurs de performance
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-gray-200 dark:border-gray-700 pb-2">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={cn(
              'flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors',
              activeTab === tab.id
                ? 'bg-accent text-white'
                : 'text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-gray-800',
            )}
          >
            <tab.icon className="h-4 w-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Dynamic Filters */}
      <DashboardFilters
        filters={filters}
        onFiltersChange={setFilters}
        onReset={resetFilters}
        members={members}
      />

      {/* Tab Content */}
      {activeTab === 'overview' && orgAnalytics && taskAnalytics && (
        <OverviewTab orgAnalytics={orgAnalytics} taskAnalytics={taskAnalytics} />
      )}
      {activeTab === 'tasks' && taskAnalytics && <TasksTab taskAnalytics={taskAnalytics} />}
      {activeTab === 'members' && memberAnalytics && <MembersTab memberAnalytics={memberAnalytics} />}
      {activeTab === 'goals' && goalAnalytics && <GoalsTab goalAnalytics={goalAnalytics} />}
    </div>
  );
}

// Overview Tab Component
function OverviewTab({
  orgAnalytics,
  taskAnalytics,
}: {
  orgAnalytics: OrganizationAnalytics;
  taskAnalytics: TaskAnalytics;
}) {
  return (
    <div className="space-y-6">
      {/* Health Score and Key Metrics */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">Score de sante</p>
              <p className="mt-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
                {orgAnalytics.healthScore}/100
              </p>
            </div>
            <ProgressGauge value={orgAnalytics.healthScore} label="" size="sm" />
          </div>
        </Card>

        <StatCard
          title="Taches cette semaine"
          value={orgAnalytics.tasksCompletedThisWeek}
          subtitle={`${orgAnalytics.tasksCreatedThisWeek} creees`}
          trend={orgAnalytics.taskCompletionTrend}
          icon={<CheckCircle2 className="h-6 w-6" />}
          color="green"
        />

        <StatCard
          title="Taches actives"
          value={orgAnalytics.activeTasks}
          subtitle={`sur ${orgAnalytics.totalTasks} total`}
          icon={<Clock className="h-6 w-6" />}
          color="blue"
        />

        <StatCard
          title="Membres"
          value={orgAnalytics.totalMembers}
          icon={<Users className="h-6 w-6" />}
          color="purple"
        />
      </div>

      {/* Charts Row */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer
          title="Taches completees (30 derniers jours)"
          fullscreenContent={
            <SimpleLineChart
              data={taskAnalytics.tasksCompletedOverTime.map((d) => ({
                date: d.date.slice(5),
                value: d.count,
              }))}
              color="#22C55E"
              height={500}
            />
          }
        >
          <SimpleLineChart
            data={taskAnalytics.tasksCompletedOverTime.map((d) => ({
              date: d.date.slice(5),
              value: d.count,
            }))}
            color="#22C55E"
          />
        </ChartContainer>

        <ChartContainer
          title="Taches creees (30 derniers jours)"
          fullscreenContent={
            <SimpleLineChart
              data={taskAnalytics.tasksCreatedOverTime.map((d) => ({
                date: d.date.slice(5),
                value: d.count,
              }))}
              color="#3B82F6"
              height={500}
            />
          }
        >
          <SimpleLineChart
            data={taskAnalytics.tasksCreatedOverTime.map((d) => ({
              date: d.date.slice(5),
              value: d.count,
            }))}
            color="#3B82F6"
          />
        </ChartContainer>
      </div>

      {/* Monthly Growth */}
      {orgAnalytics.monthlyGrowth && orgAnalytics.monthlyGrowth.length > 0 && (
        <ChartContainer
          title="Croissance mensuelle"
          fullscreenContent={
            <SimpleBarChart
              data={orgAnalytics.monthlyGrowth.map((m) => ({
                name: m.month,
                value: m.tasksCompleted,
              }))}
              color="#3B82F6"
              height={500}
            />
          }
        >
          <SimpleBarChart
            data={orgAnalytics.monthlyGrowth.map((m) => ({
              name: m.month,
              value: m.tasksCompleted,
            }))}
            color="#3B82F6"
          />
        </ChartContainer>
      )}
    </div>
  );
}

// Tasks Tab Component
function TasksTab({ taskAnalytics }: { taskAnalytics: TaskAnalytics }) {
  const statusData = [
    { name: 'A faire', value: taskAnalytics.todoCount },
    { name: 'En cours', value: taskAnalytics.inProgressCount },
    { name: 'Termine', value: taskAnalytics.doneCount },
  ];

  const priorityData = Object.entries(taskAnalytics.tasksByPriority || {}).map(([key, value]) => ({
    name: key === 'LOW' ? 'Basse' : key === 'MEDIUM' ? 'Moyenne' : key === 'HIGH' ? 'Haute' : 'Urgente',
    value,
  }));

  const cfdData = taskAnalytics.cumulativeFlowDiagram.map((d) => ({
    date: d.date.slice(5),
    todo: d.todo,
    inProgress: d.inProgress,
    done: d.done,
  }));

  // Format lead time for display
  const formatLeadTime = (hours: number | null) => {
    if (hours === null) return 'N/A';
    if (hours < 24) return `${hours.toFixed(0)}h`;
    const days = hours / 24;
    return `${days.toFixed(1)}j`;
  };

  return (
    <div className="space-y-6">
      {/* Key Metrics Row 1 */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total des taches"
          value={taskAnalytics.totalTasks}
          icon={<BarChart3 className="h-6 w-6" />}
          color="blue"
        />
        <StatCard
          title="Taux de completion"
          value={`${taskAnalytics.completionRate.toFixed(1)}%`}
          icon={<TrendingUp className="h-6 w-6" />}
          color="green"
        />
        <StatCard
          title="Taches en retard"
          value={taskAnalytics.overdueCount}
          subtitle={`${taskAnalytics.overdueRate.toFixed(1)}% du total`}
          icon={<AlertTriangle className="h-6 w-6" />}
          color="red"
        />
        <StatCard
          title="Temps moyen"
          value={
            taskAnalytics.averageCompletionTimeHours
              ? `${taskAnalytics.averageCompletionTimeHours.toFixed(1)}h`
              : 'N/A'
          }
          subtitle="pour completer une tache"
          icon={<Clock className="h-6 w-6" />}
          color="yellow"
        />
      </div>

      {/* Key Metrics Row 2 - Advanced metrics */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Lead time moyen"
          value={formatLeadTime(taskAnalytics.averageLeadTimeHours)}
          subtitle="de creation a completion"
          icon={<Clock className="h-6 w-6" />}
          color="purple"
        />
        <StatCard
          title="WIP moyen"
          value={taskAnalytics.averageWIP?.toFixed(1) || '0'}
          subtitle="taches en cours"
          icon={<Activity className="h-6 w-6" />}
          color="blue"
        />
        <StatCard
          title="Temps en TODO"
          value={formatLeadTime(taskAnalytics.averageTimeInTodoHours)}
          subtitle="temps moyen d'attente"
          icon={<Clock className="h-6 w-6" />}
          color="gray"
        />
        <StatCard
          title="Temps en cours"
          value={formatLeadTime(taskAnalytics.averageTimeInProgressHours)}
          subtitle="temps moyen de travail"
          icon={<Clock className="h-6 w-6" />}
          color="blue"
        />
      </div>

      {/* Charts Row 1 - Distribution */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer title="Repartition par statut">
          <SimplePieChart data={statusData} innerRadius={40} />
        </ChartContainer>

        <ChartContainer title="Repartition par priorite">
          <SimplePieChart data={priorityData} innerRadius={40} />
        </ChartContainer>
      </div>

      {/* Throughput Chart */}
      <ChartContainer
        title="Throughput (taches completees par jour)"
        subtitle="Avec moyenne mobile sur 7 jours"
        fullscreenContent={<ThroughputChart data={taskAnalytics.throughputChart || []} height={500} />}
      >
        <ThroughputChart data={taskAnalytics.throughputChart || []} />
      </ChartContainer>

      {/* Velocity Chart */}
      <ChartContainer
        title="Velocite (taches completees par semaine)"
        subtitle="Performance sur les 12 dernieres semaines"
        fullscreenContent={<VelocityChart data={taskAnalytics.velocityChart} height={500} />}
      >
        <VelocityChart data={taskAnalytics.velocityChart} />
      </ChartContainer>

      {/* Burndown and Burnup Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer
          title="Burndown Chart"
          subtitle="Taches restantes vs progression ideale"
          fullscreenContent={<BurndownChart data={taskAnalytics.burndownChart} height={500} />}
        >
          <BurndownChart data={taskAnalytics.burndownChart} />
        </ChartContainer>

        <ChartContainer
          title="Burnup Chart"
          subtitle="Taches completees vs scope total"
          fullscreenContent={<BurnupChart data={taskAnalytics.burnupChart || []} height={500} />}
        >
          <BurnupChart data={taskAnalytics.burnupChart || []} />
        </ChartContainer>
      </div>

      {/* Cycle Time and Lead Time */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer
          title="Distribution du cycle time"
          subtitle="Temps de completion des taches"
          fullscreenContent={<CycleTimeHistogram data={taskAnalytics.cycleTimeDistribution || []} height={500} />}
        >
          <CycleTimeHistogram data={taskAnalytics.cycleTimeDistribution || []} />
        </ChartContainer>

        <ChartContainer
          title="Tendance du lead time"
          subtitle="Evolution du temps moyen de completion par semaine"
          fullscreenContent={<LeadTimeTrendChart data={taskAnalytics.leadTimeTrend || []} height={500} />}
        >
          <LeadTimeTrendChart data={taskAnalytics.leadTimeTrend || []} />
        </ChartContainer>
      </div>

      {/* WIP Chart */}
      <ChartContainer
        title="Work in Progress (WIP)"
        subtitle="Nombre de taches actives dans le temps"
        fullscreenContent={<WIPChart data={taskAnalytics.wipChart || []} height={500} />}
      >
        <WIPChart data={taskAnalytics.wipChart || []} />
      </ChartContainer>

      {/* Cumulative Flow Diagram */}
      <ChartContainer
        title="Cumulative Flow Diagram"
        subtitle="Evolution des taches par statut dans le temps"
        fullscreenContent={
          <StackedAreaChart
            data={cfdData}
            areas={[
              { key: 'done', color: '#22C55E', name: 'Termine' },
              { key: 'inProgress', color: '#3B82F6', name: 'En cours' },
              { key: 'todo', color: '#9CA3AF', name: 'A faire' },
            ]}
            height={500}
          />
        }
      >
        <StackedAreaChart
          data={cfdData}
          areas={[
            { key: 'done', color: '#22C55E', name: 'Termine' },
            { key: 'inProgress', color: '#3B82F6', name: 'En cours' },
            { key: 'todo', color: '#9CA3AF', name: 'A faire' },
          ]}
        />
      </ChartContainer>
    </div>
  );
}

// Members Tab Component
function MembersTab({ memberAnalytics }: { memberAnalytics: MemberAnalytics }) {
  const productivityData = memberAnalytics.memberProductivity.slice(0, 10).map((m) => ({
    name: m.memberName.split(' ')[0],
    value: m.tasksCompleted,
  }));

  const workloadData = memberAnalytics.memberWorkload.slice(0, 10).map((m) => ({
    name: m.memberName.split(' ')[0],
    value: m.activeTasks,
  }));

  return (
    <div className="space-y-6">
      {/* Top Performers */}
      <ChartContainer
        title="Top performers"
        subtitle="Membres ayant complete le plus de taches"
      >
        <div className="space-y-4">
          {memberAnalytics.topPerformers.map((member, index) => (
            <div
              key={member.memberId}
              className="flex items-center justify-between rounded-lg border border-gray-200/50 p-4 dark:border-white/10"
            >
              <div className="flex items-center gap-4">
                <div
                  className={cn(
                    'flex h-10 w-10 items-center justify-center rounded-full font-bold text-white',
                    index === 0
                      ? 'bg-yellow-500'
                      : index === 1
                        ? 'bg-gray-400'
                        : index === 2
                          ? 'bg-amber-600'
                          : 'bg-gray-600',
                  )}
                >
                  {index + 1}
                </div>
                <div>
                  <p className="font-medium text-gray-900 dark:text-gray-100">{member.memberName}</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">{member.memberEmail}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                  {member.tasksCompleted}
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-400">taches completees</p>
              </div>
            </div>
          ))}
        </div>
      </ChartContainer>

      {/* Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer title="Productivite par membre">
          <SimpleBarChart data={productivityData} color="#22C55E" horizontal />
        </ChartContainer>

        <ChartContainer title="Charge de travail par membre">
          <SimpleBarChart data={workloadData} color="#3B82F6" horizontal />
        </ChartContainer>
      </div>

      {/* Treemap - Task Distribution by Member */}
      <TreemapCard memberAnalytics={memberAnalytics} />

      {/* Overloaded Members Warning */}
      {memberAnalytics.overloadedMembers.length > 0 && (
        <Card className="border-red-200 bg-red-50 p-6 dark:border-red-900/40 dark:bg-red-900/20">
          <div className="flex items-start gap-4">
            <AlertTriangle className="h-6 w-6 shrink-0 text-red-600" />
            <div>
              <h4 className="font-semibold text-red-800 dark:text-red-400">
                Membres en surcharge
              </h4>
              <p className="mt-1 text-sm text-red-700 dark:text-red-300">
                Les membres suivants ont un nombre eleve de taches actives :
              </p>
              <ul className="mt-2 space-y-1">
                {memberAnalytics.overloadedMembers.map((m) => (
                  <li key={m.memberId} className="text-sm text-red-600 dark:text-red-400">
                    {m.memberName} - {m.activeTasks} taches actives ({m.overdueTasks} en retard)
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </Card>
      )}

      {/* Member Workload Table */}
      <ChartContainer title="Detail de la charge de travail">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="pb-3 text-sm font-semibold text-gray-500 dark:text-gray-400">
                  Membre
                </th>
                <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                  A faire
                </th>
                <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                  En cours
                </th>
                <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                  En retard
                </th>
                <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                  Completion
                </th>
              </tr>
            </thead>
            <tbody>
              {memberAnalytics.memberWorkload.map((member) => {
                const productivity = memberAnalytics.memberProductivity.find(
                  (p) => p.memberId === member.memberId,
                );
                return (
                  <tr
                    key={member.memberId}
                    className="border-b border-gray-100 dark:border-gray-800"
                  >
                    <td className="py-3">
                      <div>
                        <p className="font-medium text-gray-900 dark:text-gray-100">
                          {member.memberName}
                        </p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">
                          {member.memberEmail}
                        </p>
                      </div>
                    </td>
                    <td className="py-3 text-center text-gray-600 dark:text-gray-400">
                      {member.todoTasks}
                    </td>
                    <td className="py-3 text-center text-blue-600 dark:text-blue-400">
                      {member.inProgressTasks}
                    </td>
                    <td className="py-3 text-center">
                      <span
                        className={cn(
                          member.overdueTasks > 0 ? 'text-red-600' : 'text-gray-400',
                        )}
                      >
                        {member.overdueTasks}
                      </span>
                    </td>
                    <td className="py-3 text-center text-green-600 dark:text-green-400">
                      {productivity?.completionRate.toFixed(0)}%
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </ChartContainer>

      {/* Average Completion Time per Member */}
      {memberAnalytics.memberCompletionTimes && memberAnalytics.memberCompletionTimes.length > 0 && (
        <ChartContainer
          title="Temps moyen de completion par membre"
          subtitle="Classe du plus rapide au plus lent"
        >
          <div className="space-y-3">
            {memberAnalytics.memberCompletionTimes.map((member) => {
              const validTimes = memberAnalytics.memberCompletionTimes
                .filter((m) => m.averageCompletionTimeHours !== null)
                .map((m) => m.averageCompletionTimeHours!);
              const maxTime = validTimes.length > 0 ? Math.max(...validTimes) : 1;
              const barWidth =
                member.averageCompletionTimeHours !== null
                  ? (member.averageCompletionTimeHours / maxTime) * 100
                  : 0;

              const formatTime = (hours: number | null) => {
                if (hours === null) return 'N/A';
                if (hours < 24) return `${hours.toFixed(1)}h`;
                const days = hours / 24;
                return `${days.toFixed(1)}j`;
              };

              return (
                <div
                  key={member.memberId}
                  className="flex items-center gap-4 rounded-lg border border-gray-200/50 p-3 dark:border-white/10"
                >
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-100 text-sm font-bold text-blue-600 dark:bg-blue-900/40 dark:text-blue-400">
                    {member.rank}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between">
                      <p className="truncate text-sm font-medium text-gray-900 dark:text-gray-100">
                        {member.memberName}
                      </p>
                      <div className="ml-2 flex items-center gap-2 text-right">
                        <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                          {formatTime(member.averageCompletionTimeHours)}
                        </span>
                        <span className="text-xs text-gray-500 dark:text-gray-400">
                          ({member.tasksCompleted} taches)
                        </span>
                      </div>
                    </div>
                    <div className="mt-1 h-2 w-full overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                      <div
                        className="h-full rounded-full bg-blue-500 transition-all duration-500"
                        style={{ width: `${barWidth}%` }}
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </ChartContainer>
      )}

      {/* Inactive Members Warning */}
      {memberAnalytics.inactiveMembers && memberAnalytics.inactiveMembers.length > 0 && (
        <Card className="border-yellow-200 bg-yellow-50 p-6 dark:border-yellow-900/40 dark:bg-yellow-900/20">
          <div className="flex items-start gap-4">
            <UserX className="h-6 w-6 shrink-0 text-yellow-600 dark:text-yellow-400" />
            <div className="flex-1">
              <h4 className="font-semibold text-yellow-800 dark:text-yellow-400">
                Membres inactifs ({memberAnalytics.inactiveMembers.length})
              </h4>
              <p className="mt-1 text-sm text-yellow-700 dark:text-yellow-300">
                Les membres suivants n&apos;ont eu aucune activite depuis plus de 14 jours :
              </p>
              <div className="mt-3 space-y-2">
                {memberAnalytics.inactiveMembers.map((member) => (
                  <div
                    key={member.memberId}
                    className="flex items-center justify-between rounded-lg bg-yellow-100/60 px-3 py-2 dark:bg-yellow-900/30"
                  >
                    <div>
                      <p className="text-sm font-medium text-yellow-800 dark:text-yellow-300">
                        {member.memberName}
                      </p>
                      <p className="text-xs text-yellow-600 dark:text-yellow-400">
                        {member.memberEmail}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-semibold text-yellow-800 dark:text-yellow-300">
                        {member.inactiveDays} jours
                      </p>
                      <p className="text-xs text-yellow-600 dark:text-yellow-400">
                        {member.lastActivityDate
                          ? `Derniere activite : ${member.lastActivityDate}`
                          : 'Aucune activite'}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* Team Performance Comparison */}
      {memberAnalytics.teamPerformanceComparison && memberAnalytics.teamPerformanceComparison.length > 0 && (
        <ChartContainer
          title="Comparaison de performance entre equipes"
          subtitle="Metriques de performance par equipe"
        >
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="pb-3 text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Equipe
                  </th>
                  <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Membres
                  </th>
                  <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Taches completees
                  </th>
                  <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Moy. / membre
                  </th>
                  <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Temps moyen
                  </th>
                  <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Velocite
                  </th>
                  <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Completion
                  </th>
                </tr>
              </thead>
              <tbody>
                {memberAnalytics.teamPerformanceComparison.map((team, index) => (
                  <tr
                    key={team.teamId}
                    className="border-b border-gray-100 dark:border-gray-800"
                  >
                    <td className="py-3">
                      <div className="flex items-center gap-2">
                        {index === 0 && (
                          <Trophy className="h-4 w-4 text-yellow-500" />
                        )}
                        <p className="font-medium text-gray-900 dark:text-gray-100">
                          {team.teamName}
                        </p>
                      </div>
                    </td>
                    <td className="py-3 text-center text-gray-600 dark:text-gray-400">
                      {team.memberCount}
                    </td>
                    <td className="py-3 text-center font-semibold text-gray-900 dark:text-gray-100">
                      {team.totalTasksCompleted}
                    </td>
                    <td className="py-3 text-center text-gray-600 dark:text-gray-400">
                      {team.avgTasksCompletedPerMember.toFixed(1)}
                    </td>
                    <td className="py-3 text-center text-gray-600 dark:text-gray-400">
                      {team.avgCompletionTimeHours !== null
                        ? team.avgCompletionTimeHours < 24
                          ? `${team.avgCompletionTimeHours.toFixed(1)}h`
                          : `${(team.avgCompletionTimeHours / 24).toFixed(1)}j`
                        : 'N/A'}
                    </td>
                    <td className="py-3 text-center">
                      <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-semibold text-blue-700 dark:bg-blue-900/40 dark:text-blue-400">
                        {team.teamVelocity.toFixed(1)}/sem
                      </span>
                    </td>
                    <td className="py-3 text-center">
                      <span
                        className={cn(
                          'rounded-full px-2 py-0.5 text-xs font-semibold',
                          team.completionRate >= 80
                            ? 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-400'
                            : team.completionRate >= 50
                              ? 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-400'
                              : 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400',
                        )}
                      >
                        {team.completionRate.toFixed(0)}%
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </ChartContainer>
      )}

      {/* Member Workload Heatmap */}
      {memberAnalytics.memberWorkloadHeatmap && memberAnalytics.memberWorkloadHeatmap.length > 0 && (
        <ChartContainer
          title="Repartition de la charge par jour de la semaine"
          subtitle="Nombre de taches par membre et par jour"
        >
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700">
                  <th className="pb-3 text-left text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Membre
                  </th>
                  {['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'].map((day) => (
                    <th
                      key={day}
                      className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400"
                    >
                      {day}
                    </th>
                  ))}
                  <th className="pb-3 text-center text-sm font-semibold text-gray-500 dark:text-gray-400">
                    Total
                  </th>
                </tr>
              </thead>
              <tbody>
                {memberAnalytics.memberWorkloadHeatmap.map((entry) => {
                  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
                  const total = days.reduce(
                    (sum, day) => sum + (entry.tasksByDayOfWeek[day] || 0),
                    0,
                  );
                  const maxCount = Math.max(
                    ...Object.values(entry.tasksByDayOfWeek),
                    1,
                  );

                  return (
                    <tr
                      key={entry.memberId}
                      className="border-b border-gray-100 dark:border-gray-800"
                    >
                      <td className="py-3 text-sm font-medium text-gray-900 dark:text-gray-100">
                        {entry.memberName}
                      </td>
                      {days.map((day) => {
                        const count = entry.tasksByDayOfWeek[day] || 0;
                        const intensity = maxCount > 0 ? count / maxCount : 0;
                        return (
                          <td key={day} className="py-3 text-center">
                            <div
                              className={cn(
                                'mx-auto flex h-8 w-8 items-center justify-center rounded-md text-xs font-semibold',
                                intensity === 0
                                  ? 'bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500'
                                  : intensity < 0.33
                                    ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400'
                                    : intensity < 0.66
                                      ? 'bg-blue-200 text-blue-800 dark:bg-blue-800/50 dark:text-blue-300'
                                      : 'bg-blue-400 text-white dark:bg-blue-600 dark:text-white',
                              )}
                              title={`${count} tache(s)`}
                            >
                              {count}
                            </div>
                          </td>
                        );
                      })}
                      <td className="py-3 text-center text-sm font-semibold text-gray-900 dark:text-gray-100">
                        {total}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </ChartContainer>
      )}
    </div>
  );
}

// Goals Tab Component
function GoalsTab({ goalAnalytics }: { goalAnalytics: GoalAnalytics }) {
  const typeData = Object.entries(goalAnalytics.goalsByType || {}).map(([key, value]) => ({
    name: key === 'SHORT' ? 'Court terme' : key === 'MEDIUM' ? 'Moyen terme' : 'Long terme',
    value,
  }));

  return (
    <div className="space-y-6">
      {/* Key Metrics */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total des objectifs"
          value={goalAnalytics.totalGoals}
          icon={<Target className="h-6 w-6" />}
          color="blue"
        />
        <StatCard
          title="Progression globale"
          value={`${goalAnalytics.overallProgressPercentage.toFixed(1)}%`}
          icon={<TrendingUp className="h-6 w-6" />}
          color="green"
        />
        <StatCard
          title="Objectifs completes"
          value={goalAnalytics.completedGoals}
          subtitle={`${goalAnalytics.goalCompletionRate.toFixed(1)}% du total`}
          icon={<CheckCircle2 className="h-6 w-6" />}
          color="green"
        />
        <StatCard
          title="Objectifs a risque"
          value={goalAnalytics.atRiskGoals}
          icon={<AlertTriangle className="h-6 w-6" />}
          color="red"
        />
      </div>

      {/* Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <ChartContainer title="Repartition par type">
          <SimplePieChart data={typeData} innerRadius={40} />
        </ChartContainer>

        <ChartContainer title="Progression moyenne par type">
          <SimpleBarChart
            data={Object.entries(goalAnalytics.avgProgressByType || {}).map(([key, value]) => ({
              name:
                key === 'SHORT' ? 'Court terme' : key === 'MEDIUM' ? 'Moyen terme' : 'Long terme',
              value: Math.round(value),
            }))}
            color="#8B5CF6"
          />
        </ChartContainer>
      </div>

      {/* Goals at Risk */}
      {goalAnalytics.goalsAtRisk.length > 0 && (
        <Card className="border-red-200 bg-red-50 p-6 dark:border-red-900/40 dark:bg-red-900/20">
          <div className="flex items-start gap-4">
            <AlertTriangle className="h-6 w-6 shrink-0 text-red-600" />
            <div className="flex-1">
              <h4 className="font-semibold text-red-800 dark:text-red-400">Objectifs a risque</h4>
              <p className="mt-1 text-sm text-red-700 dark:text-red-300">
                Ces objectifs ont une faible progression avec une deadline proche :
              </p>
              <div className="mt-4 space-y-3">
                {goalAnalytics.goalsAtRisk.map((goal) => (
                  <div
                    key={goal.goalId}
                    className="rounded-lg bg-white p-4 dark:bg-gray-800"
                  >
                    <div className="flex items-center justify-between">
                      <p className="font-medium text-gray-900 dark:text-gray-100">{goal.title}</p>
                      <span className="text-sm text-red-600">{goal.daysRemaining} jours restants</span>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                      <div
                        className="h-full bg-red-500 transition-all"
                        style={{ width: `${goal.progressPercentage}%` }}
                      />
                    </div>
                    <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                      {goal.progressPercentage.toFixed(0)}% complete
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </Card>
      )}

      {/* All Goals Progress */}
      <ChartContainer title="Progression des objectifs">
        <div className="space-y-4">
          {goalAnalytics.goalProgressList.map((goal) => (
            <div key={goal.goalId} className="space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <p className="font-medium text-gray-900 dark:text-gray-100">{goal.title}</p>
                  {goal.type && (
                    <span
                      className={cn(
                        'rounded-full px-2 py-0.5 text-xs font-medium',
                        goal.type === 'SHORT'
                          ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-400'
                          : goal.type === 'MEDIUM'
                            ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/40 dark:text-purple-400'
                            : 'bg-orange-100 text-orange-700 dark:bg-orange-900/40 dark:text-orange-400',
                      )}
                    >
                      {goal.type === 'SHORT'
                        ? 'Court terme'
                        : goal.type === 'MEDIUM'
                          ? 'Moyen terme'
                          : 'Long terme'}
                    </span>
                  )}
                </div>
                <span className="text-sm text-gray-500 dark:text-gray-400">
                  {goal.completedTasks}/{goal.totalTasks} taches
                </span>
              </div>
              <div className="h-3 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                <div
                  className={cn(
                    'h-full transition-all',
                    goal.isAtRisk
                      ? 'bg-red-500'
                      : goal.progressPercentage >= 100
                        ? 'bg-green-500'
                        : 'bg-accent',
                  )}
                  style={{ width: `${Math.min(100, goal.progressPercentage)}%` }}
                />
              </div>
              <div className="flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
                <span>{goal.progressPercentage.toFixed(0)}% complete</span>
                {goal.deadline && (
                  <span>
                    Deadline: {goal.deadline} ({goal.daysRemaining} jours)
                  </span>
                )}
              </div>
            </div>
          ))}
          {goalAnalytics.goalProgressList.length === 0 && (
            <p className="text-center text-gray-500 dark:text-gray-400">Aucun objectif</p>
          )}
        </div>
      </ChartContainer>
    </div>
  );
}
