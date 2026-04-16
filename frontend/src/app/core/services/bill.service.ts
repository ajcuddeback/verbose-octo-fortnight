import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Bill, BillRequest } from '../models/bill.model';

const API_BASE = 'http://localhost:8080/api';

@Injectable({ providedIn: 'root' })
export class BillService {
  private http = inject(HttpClient);

  getBills(): Observable<Bill[]> {
    return this.http.get<Bill[]>(`${API_BASE}/bills`);
  }

  getBill(id: number): Observable<Bill> {
    return this.http.get<Bill>(`${API_BASE}/bills/${id}`);
  }

  createBill(req: BillRequest): Observable<Bill> {
    return this.http.post<Bill>(`${API_BASE}/bills`, req);
  }

  updateBill(id: number, req: BillRequest): Observable<Bill> {
    return this.http.put<Bill>(`${API_BASE}/bills/${id}`, req);
  }

  deleteBill(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/bills/${id}`);
  }

  markBillPaid(id: number): Observable<Bill> {
    return this.http.post<Bill>(`${API_BASE}/bills/${id}/pay`, {});
  }
}
