import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { TransactionService } from '../../core/services/transaction.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { BillService } from '../../core/services/bill.service';
import { TransactionStore } from '../../core/store/transaction.store';
import { AnalyticsStore } from '../../core/store/analytics.store';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let transactionService: jasmine.SpyObj<TransactionService>;
  let analyticsService: jasmine.SpyObj<AnalyticsService>;
  let billService: jasmine.SpyObj<BillService>;

  const mockPagedResponse = {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 10
  };

  const mockHealthScore = {
    overallScore: 72,
    grade: 'B',
    components: [
      { name: 'Savings Rate', score: 15, maxScore: 25, description: 'Good savings rate' },
      { name: 'Debt Management', score: 18, maxScore: 25, description: 'Low debt' }
    ],
    lastUpdated: '2024-01-15'
  };

  beforeEach(async () => {
    const txServiceSpy = jasmine.createSpyObj('TransactionService', ['getTransactions']);
    const analyticsServiceSpy = jasmine.createSpyObj('AnalyticsService', [
      'getFinancialHealth', 'getSpendingTrends', 'getCategoryBreakdown'
    ]);
    const billServiceSpy = jasmine.createSpyObj('BillService', ['getBills']);

    txServiceSpy.getTransactions.and.returnValue(of(mockPagedResponse));
    analyticsServiceSpy.getFinancialHealth.and.returnValue(of(mockHealthScore));
    analyticsServiceSpy.getSpendingTrends.and.returnValue(of([]));
    analyticsServiceSpy.getCategoryBreakdown.and.returnValue(of([]));
    billServiceSpy.getBills.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: TransactionService, useValue: txServiceSpy },
        { provide: AnalyticsService, useValue: analyticsServiceSpy },
        { provide: BillService, useValue: billServiceSpy },
        TransactionStore,
        AnalyticsStore
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    transactionService = TestBed.inject(TransactionService) as jasmine.SpyObj<TransactionService>;
    analyticsService = TestBed.inject(AnalyticsService) as jasmine.SpyObj<AnalyticsService>;
    billService = TestBed.inject(BillService) as jasmine.SpyObj<BillService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start with loading = true', () => {
    expect(component.loading()).toBeTrue();
  });

  it('should load data on init and set loading to false', fakeAsync(() => {
    fixture.detectChanges();
    tick(100);
    expect(transactionService.getTransactions).toHaveBeenCalled();
    expect(analyticsService.getFinancialHealth).toHaveBeenCalled();
    expect(analyticsService.getSpendingTrends).toHaveBeenCalledWith(6);
    expect(component.loading()).toBeFalse();
  }));

  it('should set health score from analytics service', fakeAsync(() => {
    fixture.detectChanges();
    tick(100);
    expect(component.healthScore()).toBeTruthy();
    expect(component.healthScore()!.overallScore).toBe(72);
    expect(component.healthScore()!.grade).toBe('B');
  }));

  it('should format currency correctly', () => {
    expect(component.formatCurrency(1234.56)).toBe('$1,234.56');
    expect(component.formatCurrency(0)).toBe('$0.00');
    expect(component.formatCurrency(-500)).toBe('-$500.00');
  });

  it('should format dates correctly', () => {
    const formatted = component.formatDate('2024-01-15');
    expect(formatted).toContain('Jan');
    expect(formatted).toContain('15');
    expect(formatted).toContain('2024');
  });
});
