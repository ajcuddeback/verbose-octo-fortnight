import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';
import { Transaction, TransactionRequest, PagedResponse } from '../models/transaction.model';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  const mockTransaction: Transaction = {
    id: 1,
    amount: 50.00,
    description: 'Coffee shop',
    transactionDate: '2024-01-15',
    type: 'EXPENSE',
    categoryId: 1,
    categoryName: 'Food',
    categoryColor: '#ef4444',
    createdAt: '2024-01-15T10:00:00'
  };

  const mockPagedResponse: PagedResponse<Transaction> = {
    content: [mockTransaction],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 10
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TransactionService]
    });
    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getTransactions', () => {
    it('should fetch all transactions without filters', () => {
      service.getTransactions().subscribe(res => {
        expect(res.content.length).toBe(1);
        expect(res.content[0].description).toBe('Coffee shop');
      });

      const req = httpMock.expectOne(r => r.url === 'http://localhost:8080/api/transactions');
      expect(req.request.method).toBe('GET');
      req.flush(mockPagedResponse);
    });

    it('should include filter params when provided', () => {
      service.getTransactions({ type: 'EXPENSE', from: '2024-01-01', to: '2024-01-31', page: 0, size: 20 }).subscribe();

      const req = httpMock.expectOne(r =>
        r.url === 'http://localhost:8080/api/transactions' &&
        r.params.get('type') === 'EXPENSE' &&
        r.params.get('from') === '2024-01-01' &&
        r.params.get('to') === '2024-01-31' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '20'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockPagedResponse);
    });
  });

  describe('createTransaction', () => {
    it('should POST and return new transaction', () => {
      const req: TransactionRequest = {
        amount: 50,
        description: 'Coffee shop',
        transactionDate: '2024-01-15',
        type: 'EXPENSE',
        categoryId: 1
      };

      service.createTransaction(req).subscribe(tx => {
        expect(tx.id).toBe(1);
        expect(tx.description).toBe('Coffee shop');
      });

      const httpReq = httpMock.expectOne('http://localhost:8080/api/transactions');
      expect(httpReq.request.method).toBe('POST');
      expect(httpReq.request.body).toEqual(req);
      httpReq.flush(mockTransaction);
    });
  });

  describe('deleteTransaction', () => {
    it('should send DELETE request', () => {
      service.deleteTransaction(1).subscribe(() => {});

      const req = httpMock.expectOne('http://localhost:8080/api/transactions/1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
