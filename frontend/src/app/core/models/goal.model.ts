export type GoalStatus = 'IN_PROGRESS' | 'COMPLETED' | 'PAUSED';

export interface Goal {
  id: number;
  name: string;
  description?: string;
  targetAmount: number;
  currentAmount: number;
  progressPercentage: number;
  targetDate: string;
  projectedCompletionDate?: string;
  status: GoalStatus;
  category: string;
  isOnTrack: boolean;
  createdAt: string;
}

export interface GoalRequest {
  name: string;
  description?: string;
  targetAmount: number;
  currentAmount?: number;
  targetDate: string;
  category: string;
}

export interface GoalContribution {
  amount: number;
  note?: string;
}
