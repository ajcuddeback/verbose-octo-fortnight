export interface Category {
  id: number;
  name: string;
  color: string;
  icon?: string;
  type: 'INCOME' | 'EXPENSE' | 'BOTH';
}
