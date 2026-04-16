import { TestBed } from '@angular/core/testing';
import { TransactionStore } from './transaction.store';
import { Transaction } from '../models/transaction.model';

describe('TransactionStore', () => {
  let store: TransactionStore;

  const makeTransaction = (id: number, type: 'INCOME' | 'EXPENSE', amount: number): Transaction => ({
    id,
    amount,
    description: `Transaction ${id}`,
    transactionDate: '2024-01-15',
    type,
    createdAt: '2024-01-15T10:00:00'
  });

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(TransactionStore);
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  describe('addTransaction', () => {
    it('should prepend transaction to the list', () => {
      const tx1 = makeTransaction(1, 'EXPENSE', 50);
      const tx2 = makeTransaction(2, 'INCOME', 100);
      store.addTransaction(tx1);
      store.addTransaction(tx2);
      expect(store.transactions()[0].id).toBe(2);
      expect(store.transactions()[1].id).toBe(1);
    });
  });

  describe('removeTransaction', () => {
    it('should remove transaction by id', () => {
      store.setTransactions([
        makeTransaction(1, 'EXPENSE', 50),
        makeTransaction(2, 'INCOME', 100)
      ]);
      store.removeTransaction(1);
      expect(store.transactions().length).toBe(1);
      expect(store.transactions()[0].id).toBe(2);
    });

    it('should not affect other transactions', () => {
      store.setTransactions([makeTransaction(1, 'EXPENSE', 50)]);
      store.removeTransaction(999);
      expect(store.transactions().length).toBe(1);
    });
  });

  describe('updateTransaction', () => {
    it('should replace transaction with matching id', () => {
      store.setTransactions([makeTransaction(1, 'EXPENSE', 50)]);
      const updated = { ...makeTransaction(1, 'EXPENSE', 75), description: 'Updated' };
      store.updateTransaction(updated);
      expect(store.transactions()[0].amount).toBe(75);
      expect(store.transactions()[0].description).toBe('Updated');
    });
  });

  describe('totalIncome computed', () => {
    it('should sum only INCOME transactions', () => {
      store.setTransactions([
        makeTransaction(1, 'INCOME', 1000),
        makeTransaction(2, 'INCOME', 500),
        makeTransaction(3, 'EXPENSE', 200)
      ]);
      expect(store.totalIncome()).toBe(1500);
    });

    it('should return 0 when no income', () => {
      store.setTransactions([makeTransaction(1, 'EXPENSE', 100)]);
      expect(store.totalIncome()).toBe(0);
    });
  });

  describe('totalExpenses computed', () => {
    it('should sum only EXPENSE transactions', () => {
      store.setTransactions([
        makeTransaction(1, 'EXPENSE', 200),
        makeTransaction(2, 'EXPENSE', 150),
        makeTransaction(3, 'INCOME', 1000)
      ]);
      expect(store.totalExpenses()).toBe(350);
    });

    it('should return 0 when no expenses', () => {
      store.setTransactions([makeTransaction(1, 'INCOME', 1000)]);
      expect(store.totalExpenses()).toBe(0);
    });
  });

  describe('netSavings computed', () => {
    it('should calculate income minus expenses', () => {
      store.setTransactions([
        makeTransaction(1, 'INCOME', 1000),
        makeTransaction(2, 'EXPENSE', 400)
      ]);
      expect(store.netSavings()).toBe(600);
    });

    it('should be negative when expenses exceed income', () => {
      store.setTransactions([
        makeTransaction(1, 'INCOME', 200),
        makeTransaction(2, 'EXPENSE', 500)
      ]);
      expect(store.netSavings()).toBe(-300);
    });
  });
});
