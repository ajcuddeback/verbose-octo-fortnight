export interface Transaction {
  id: number;
  amount: number;
  description: string;
  transactionDate: string;
  type: 'INCOME' | 'EXPENSE';
  categoryId?: number;
  categoryName?: string;
  categoryColor?: string;
  accountId?: number;
  accountName?: string;
  notes?: string;
  tags?: string;
  createdAt: string;
}

export interface TransactionRequest {
  amount: number;
  description: string;
  transactionDate: string;
  type: 'INCOME' | 'EXPENSE';
  categoryId?: number;
  accountId?: number;
  notes?: string;
  tags?: string;
}

export interface TransactionFilter {
  type?: 'INCOME' | 'EXPENSE';
  from?: string;
  to?: string;
  search?: string;
  page?: number;
  size?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
