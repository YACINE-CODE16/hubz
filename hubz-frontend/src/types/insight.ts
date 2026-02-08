export type InsightType =
  | 'PRODUCTIVITY_TIP'
  | 'HABIT_SUGGESTION'
  | 'GOAL_ALERT'
  | 'WORKLOAD_WARNING'
  | 'CELEBRATION'
  | 'PATTERN_DETECTED';

export interface Insight {
  id: string;
  type: InsightType;
  title: string;
  message: string;
  priority: number;
  actionable: boolean;
  actionUrl: string | null;
  createdAt: string;
}
