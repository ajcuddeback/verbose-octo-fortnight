import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick($event)">
      <div class="modal" style="max-width: 400px;">
        <div class="modal-header">
          <h3 class="modal-title">{{ title }}</h3>
        </div>
        <p style="color: var(--text-secondary); margin-bottom: 1.5rem; font-size: 0.9375rem;">{{ message }}</p>
        <div style="display: flex; gap: 0.75rem; justify-content: flex-end;">
          <button class="btn btn-secondary" (click)="cancelled.emit()">Cancel</button>
          <button class="btn" [class]="confirmButtonClass" (click)="confirmed.emit()">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  `
})
export class ConfirmDialogComponent {
  @Input() title = 'Confirm Action';
  @Input() message = 'Are you sure you want to proceed?';
  @Input() confirmText = 'Confirm';
  @Input() confirmButtonClass = 'btn-danger';
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.cancelled.emit();
    }
  }
}
