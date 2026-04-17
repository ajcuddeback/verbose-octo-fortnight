export type AccountType = 'CHECKING' | 'SAVINGS' | 'CREDIT_CARD' | 'INVESTMENT' | 'LOAN' | 'OTHER';

export interface Account {
  id: number;
  name: string;
  bankName: string;
  type: AccountType;
  balance: number;
  accountNumberLast4?: string;
  isActive: boolean;
  createdAt: string;
}

export interface AccountRequest {
  name: string;
  bankName: string;
  type: AccountType;
  balance: number;
  accountNumberLast4?: string;
}
