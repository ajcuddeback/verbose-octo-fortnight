export interface SpendingTrend {
  month: string;
  year: number;
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  savingsRate: number;
}

export interface CategoryBreakdown {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  totalAmount: number;
  percentage: number;
  transactionCount: number;
}

export interface Prediction {
  month: string;
  year: number;
  predictedIncome: number;
  predictedExpenses: number;
  predictedSavings: number;
  confidenceScore: number;
}

export interface HealthScoreComponent {
  name: string;
  score: number;
  maxScore: number;
  description: string;
}

export interface FinancialHealthScore {
  overallScore: number;
  grade: string;
  components: HealthScoreComponent[];
  lastUpdated: string;
}

export interface HealthScoreHistory {
  date: string;
  score: number;
}

export interface SpendingDna {
  personalityType: string;
  personalityIcon: string;
  description: string;
  topCategories: Array<{ name: string; percentage: number; color: string }>;
  insights: string[];
}
