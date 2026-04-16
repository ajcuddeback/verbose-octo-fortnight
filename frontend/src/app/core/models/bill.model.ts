export type BillFrequency = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'ANNUALLY';
export type BillStatus = 'PENDING' | 'PAID' | 'OVERDUE';

export interface Bill {
  id: number;
  name: string;
  amount: number;
  dueDayOfMonth: number;
  frequency: BillFrequency;
  categoryId?: number;
  categoryName?: string;
  lastPaidDate?: string;
  nextDueDate: string;
  status: BillStatus;
  autopay: boolean;
  notes?: string;
  createdAt: string;
}

export interface BillRequest {
  name: string;
  amount: number;
  dueDayOfMonth: number;
  frequency: BillFrequency;
  categoryId?: number;
  autopay?: boolean;
  notes?: string;
}
