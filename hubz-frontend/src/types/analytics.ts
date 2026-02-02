// Task Analytics Types
export interface TimeSeriesData {
  date: string;
  count: number;
}

export interface BurndownData {
  date: string;
  remainingTasks: number;
  completedTasks: number;
  totalTasks: number;
}

export interface VelocityData {
  weekStart: string;
  completedTasks: number;
}

export interface CumulativeFlowData {
  date: string;
  todo: number;
  inProgress: number;
  done: number;
}

export interface TaskAnalytics {
  totalTasks: number;
  todoCount: number;
  inProgressCount: number;
  doneCount: number;
  completionRate: number;
  overdueCount: number;
  overdueRate: number;
  averageCompletionTimeHours: number | null;
  tasksByPriority: Record<string, number>;
  tasksByStatus: Record<string, number>;
  tasksCreatedOverTime: TimeSeriesData[];
  tasksCompletedOverTime: TimeSeriesData[];
  burndownChart: BurndownData[];
  velocityChart: VelocityData[];
  cumulativeFlowDiagram: CumulativeFlowData[];
}

// Member Analytics Types
export interface MemberProductivity {
  memberId: string;
  memberName: string;
  memberEmail: string;
  tasksCompleted: number;
  totalTasksAssigned: number;
  completionRate: number;
  averageCompletionTimeHours: number | null;
  productivityScore: number;
}

export interface MemberWorkload {
  memberId: string;
  memberName: string;
  memberEmail: string;
  activeTasks: number;
  todoTasks: number;
  inProgressTasks: number;
  overdueTasks: number;
  isOverloaded: boolean;
}

export interface ActivityData {
  date: string;
  memberId: string;
  memberName: string;
  activityCount: number;
}

export interface MemberAnalytics {
  memberProductivity: MemberProductivity[];
  memberWorkload: MemberWorkload[];
  topPerformers: MemberProductivity[];
  overloadedMembers: MemberWorkload[];
  activityHeatmap: ActivityData[];
}

// Goal Analytics Types
export interface GoalProgress {
  goalId: string;
  title: string;
  description: string | null;
  type: string | null;
  totalTasks: number;
  completedTasks: number;
  progressPercentage: number;
  deadline: string | null;
  daysRemaining: number;
  daysElapsed: number;
  velocityPerDay: number;
  requiredVelocity: number;
  predictedCompletionDate: string | null;
  isAtRisk: boolean;
  isOnTrack: boolean;
  riskReason: string | null;
}

export interface GoalCompletionData {
  month: string;
  completedCount: number;
}

export interface GoalAnalytics {
  totalGoals: number;
  completedGoals: number;
  inProgressGoals: number;
  atRiskGoals: number;
  overallProgressPercentage: number;
  goalCompletionRate: number;
  goalsByType: Record<string, number>;
  avgProgressByType: Record<string, number>;
  goalProgressList: GoalProgress[];
  goalsAtRisk: GoalProgress[];
  completionHistory: GoalCompletionData[];
  goalsOnTrack: number;
  goalsBehindSchedule: number;
  averageVelocity: number;
}

// Habit Analytics Types
export interface HabitStats {
  habitId: string;
  habitName: string;
  habitIcon: string | null;
  frequency: string | null;
  currentStreak: number;
  longestStreak: number;
  completionRate: number;
  totalCompletions: number;
  lastCompletedDate: string | null;
}

export interface HeatmapData {
  date: string;
  completedCount: number;
  totalHabits: number;
  completionRate: number;
}

export interface TrendData {
  date: string;
  completionRate: number;
  completed: number;
  total: number;
}

export interface HabitAnalytics {
  totalHabits: number;
  dailyCompletionRate: number;
  weeklyCompletionRate: number;
  monthlyCompletionRate: number;
  longestStreak: number;
  currentStreak: number;
  bestStreakHabitName: string | null;
  habitStats: HabitStats[];
  completionHeatmap: HeatmapData[];
  completionByDayOfWeek: Record<string, number>;
  last30DaysTrend: TrendData[];
  last90DaysTrend: TrendData[];
}

// Organization Analytics Types
export interface ActivityTimelineEntry {
  timestamp: string;
  type: string;
  description: string;
  actorName: string;
}

export interface TeamPerformance {
  teamId: string;
  teamName: string;
  memberCount: number;
  tasksCompleted: number;
  activeTasks: number;
  completionRate: number;
}

export interface MonthlyGrowth {
  month: string;
  tasksCreated: number;
  tasksCompleted: number;
  newMembers: number;
  goalsCompleted: number;
}

export interface OrganizationAnalytics {
  healthScore: number;
  totalMembers: number;
  activeMembers: number;
  totalTasks: number;
  activeTasks: number;
  totalGoals: number;
  totalEvents: number;
  totalNotes: number;
  totalTeams: number;
  tasksCreatedThisWeek: number;
  tasksCompletedThisWeek: number;
  eventsThisWeek: number;
  taskCompletionTrend: number;
  memberActivityTrend: number;
  recentActivity: ActivityTimelineEntry[];
  teamPerformance: TeamPerformance[];
  monthlyGrowth: MonthlyGrowth[];
}
