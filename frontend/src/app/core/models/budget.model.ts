export interface BudgetItem {
  id: number;
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  allocatedAmount: number;
  spentAmount: number;
  percentage: number;
  month: number;
  year: number;
  createdAt: string;
}

export interface BudgetRequest {
  categoryId: number;
  allocatedAmount: number;
  month: number;
  year: number;
}

export interface BudgetSummary {
  totalAllocated: number;
  totalSpent: number;
  totalIncome: number;
  unallocated: number;
}
