import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="spinner-wrapper" [class.fullscreen]="fullscreen">
      <div class="spinner"></div>
      @if (message) {
        <p class="spinner-message">{{ message }}</p>
      }
    </div>
  `,
  styles: [`
    .spinner-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      padding: 2rem;
    }
    .spinner-wrapper.fullscreen {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.8);
      z-index: 9999;
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid var(--border-color);
      border-top-color: var(--accent-primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .spinner-message {
      color: var(--text-secondary);
      font-size: 0.875rem;
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() message?: string;
  @Input() fullscreen = false;
}
