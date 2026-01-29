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
