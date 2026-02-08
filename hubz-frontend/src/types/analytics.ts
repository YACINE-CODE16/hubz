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

// Burnup chart data
export interface BurnupData {
  date: string;
  cumulativeCompleted: number;
  totalScope: number;
}

// Throughput chart data
export interface ThroughputData {
  date: string;
  completedCount: number;
  rollingAverage: number | null;
}

// Cycle time distribution bucket
export interface CycleTimeBucket {
  bucket: string;
  minHours: number;
  maxHours: number;
  count: number;
  percentage: number;
}

// Lead time trend data
export interface LeadTimeData {
  date: string;
  averageLeadTimeHours: number | null;
  taskCount: number;
}

// Work in Progress data
export interface WIPData {
  date: string;
  wipCount: number;
  todoCount: number;
  totalActive: number;
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
  averageTimeInTodoHours: number | null;
  averageTimeInProgressHours: number | null;
  tasksByPriority: Record<string, number>;
  tasksByStatus: Record<string, number>;
  tasksCreatedOverTime: TimeSeriesData[];
  tasksCompletedOverTime: TimeSeriesData[];
  burndownChart: BurndownData[];
  burnupChart: BurnupData[];
  velocityChart: VelocityData[];
  cumulativeFlowDiagram: CumulativeFlowData[];
  throughputChart: ThroughputData[];
  cycleTimeDistribution: CycleTimeBucket[];
  leadTimeTrend: LeadTimeData[];
  averageLeadTimeHours: number | null;
  wipChart: WIPData[];
  averageWIP: number;
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

export interface MemberCompletionTime {
  memberId: string;
  memberName: string;
  memberEmail: string;
  averageCompletionTimeHours: number | null;
  tasksCompleted: number;
  rank: number;
}

export interface InactiveMember {
  memberId: string;
  memberName: string;
  memberEmail: string;
  lastActivityDate: string | null;
  inactiveDays: number;
}

export interface TeamPerformanceComparison {
  teamId: string;
  teamName: string;
  memberCount: number;
  totalTasksCompleted: number;
  avgTasksCompletedPerMember: number;
  avgCompletionTimeHours: number | null;
  teamVelocity: number;
  completionRate: number;
}

export interface MemberWorkloadHeatmapEntry {
  memberId: string;
  memberName: string;
  tasksByDayOfWeek: Record<string, number>;
}

export interface MemberAnalytics {
  memberProductivity: MemberProductivity[];
  memberWorkload: MemberWorkload[];
  topPerformers: MemberProductivity[];
  overloadedMembers: MemberWorkload[];
  activityHeatmap: ActivityData[];
  memberCompletionTimes: MemberCompletionTime[];
  inactiveMembers: InactiveMember[];
  teamPerformanceComparison: TeamPerformanceComparison[];
  memberWorkloadHeatmap: MemberWorkloadHeatmapEntry[];
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

// Personal Productivity Stats Types
export interface DailyTaskCount {
  date: string;
  count: number;
}

export interface ProductivityStats {
  tasksCompletedThisWeek: number;
  tasksCompletedThisMonth: number;
  totalTasksThisWeek: number;
  totalTasksThisMonth: number;
  weeklyCompletionRate: number;
  monthlyCompletionRate: number;
  averageCompletionTimeHours: number | null;
  productiveStreak: number;
  longestProductiveStreak: number;
  weeklyChange: number;
  monthlyChange: number;
  insight: string;
  productivityScore: number;
  dailyTasksCompleted: DailyTaskCount[];
  mostProductiveDay: string | null;
  urgentTasksCompleted: number;
  highPriorityTasksCompleted: number;
  mediumPriorityTasksCompleted: number;
  lowPriorityTasksCompleted: number;
}

// Calendar Analytics Types
export interface EventsPerPeriod {
  period: string;
  label: string;
  eventCount: number;
  totalHours: number;
}

export interface DailyOccupancy {
  date: string;
  occupiedHours: number;
  availableHours: number;
  occupancyRate: number;
  eventCount: number;
}

export interface DayHeatmapData {
  dayOfWeek: string;
  dayIndex: number;
  averageHours: number;
  averageEvents: number;
  intensity: 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';
}

export interface TimeSlotData {
  hour: number;
  timeSlot: string;
  eventCount: number;
  percentage: number;
}

export interface AgendaConflict {
  event1Id: string;
  event1Title: string;
  event2Id: string;
  event2Title: string;
  conflictDate: string;
  conflictTime: string;
  overlapMinutes: number;
}

export interface CalendarAnalytics {
  totalEvents: number;
  eventsInPeriod: number;
  totalHoursScheduled: number;
  averageEventDurationHours: number;
  eventsPerWeek: EventsPerPeriod[];
  eventsPerMonth: EventsPerPeriod[];
  timeDistribution: Record<string, number>;
  occupancyRate: number;
  dailyOccupancy: DailyOccupancy[];
  busiestDaysOfWeek: Record<string, number>;
  weeklyHeatmap: DayHeatmapData[];
  timeSlotDistribution: TimeSlotData[];
  mostUsedTimeSlot: string;
  leastUsedTimeSlot: string;
  meetingHours: number;
  personalEventHours: number;
  meetingVsPersonalRatio: number;
  conflictCount: number;
  conflicts: AgendaConflict[];
  availabilityScore: number;
  availabilityInsight: string;
  forecastedHoursNextWeek: number;
  forecastedEventsNextWeek: number;
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

// Activity Heatmap Types
export interface DailyActivity {
  date: string;
  count: number;
  level: number; // 0-4 for color intensity
}

export interface ActivityHeatmap {
  activities: DailyActivity[];
  totalContributions: number;
  currentStreak: number;
  longestStreak: number;
  averagePerDay: number;
  mostActiveDay: string | null;
  activeDays: number;
  totalDays: number;
}
