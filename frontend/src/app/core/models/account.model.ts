export type AccountType = 'CHECKING' | 'SAVINGS' | 'CREDIT_CARD' | 'INVESTMENT' | 'CASH' | 'OTHER';

export interface Account {
  id: number;
  name: string;
  bankName: string;
  accountType: AccountType;
  balance: number;
  lastFourDigits?: string;
  isActive: boolean;
  createdAt: string;
}

export interface AccountRequest {
  name: string;
  bankName: string;
  accountType: AccountType;
  balance: number;
  lastFourDigits?: string;
}
