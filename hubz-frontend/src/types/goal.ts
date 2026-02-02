export type GoalType = 'SHORT' | 'MEDIUM' | 'LONG';

export interface Goal {
  id: string;
  title: string;
  description?: string;
  type: GoalType;
  deadline?: string;
  organizationId?: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
  // Calculated fields
  totalTasks: number;
  completedTasks: number;
}

export interface CreateGoalRequest {
  title: string;
  description?: string;
  type: GoalType;
  deadline?: string;
}

export interface UpdateGoalRequest {
  title?: string;
  description?: string;
  type?: GoalType;
  deadline?: string;
}

// Analytics Types
export interface GoalProgress {
  goalId: string;
  title: string;
  description: string | null;
  type: string;
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
