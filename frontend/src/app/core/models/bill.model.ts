export type BillFrequency = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'ANNUALLY';

export interface Bill {
  id: number;
  name: string;
  amount: number;
  dueDay: number;
  frequency: BillFrequency;
  categoryId?: number;
  categoryName?: string;
  lastPaidDate?: string;
  nextDueDate: string;
  isAutoPay: boolean;
  isActive: boolean;
  createdAt: string;
}

export interface BillRequest {
  name: string;
  amount: number;
  dueDay: number;
  frequency: BillFrequency;
  categoryId?: number;
  isAutoPay?: boolean;
  isActive?: boolean;
}
