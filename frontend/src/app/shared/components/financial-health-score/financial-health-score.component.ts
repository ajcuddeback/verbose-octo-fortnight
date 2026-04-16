import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinancialHealthScore, HealthScoreComponent } from '../../../core/models/analytics.model';

@Component({
  selector: 'app-financial-health-score',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="health-score-widget">
      <div class="score-circle-wrapper">
        <svg viewBox="0 0 120 120" class="score-svg">
          <circle cx="60" cy="60" r="54" fill="none" stroke="var(--bg-primary)" stroke-width="10"/>
          <circle
            cx="60" cy="60" r="54"
            fill="none"
            [attr.stroke]="scoreColor()"
            stroke-width="10"
            stroke-linecap="round"
            [attr.stroke-dasharray]="circumference"
            [attr.stroke-dashoffset]="dashOffset()"
            transform="rotate(-90 60 60)"
            style="transition: stroke-dashoffset 1s ease;"
          />
        </svg>
        <div class="score-center">
          <span class="score-number">{{ score }}</span>
          <span class="score-grade" [style.color]="scoreColor()">{{ grade }}</span>
        </div>
      </div>

      @if (breakdown && breakdown.length > 0) {
        <div class="score-components">
          @for (component of breakdown; track component.name) {
            <div class="component-item">
              <div class="component-header">
                <span class="component-name">{{ component.name }}</span>
                <span class="component-score" [style.color]="getComponentColor(component.score, component.maxScore)">
                  {{ component.score }}/{{ component.maxScore }}
                </span>
              </div>
              <div class="progress-bar-container">
                <div class="progress-bar"
                  [style.width]="getComponentPercent(component) + '%'"
                  [style.background]="getComponentColor(component.score, component.maxScore)">
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .health-score-widget {
      display: flex;
      align-items: flex-start;
      gap: 2rem;
    }
    .score-circle-wrapper {
      position: relative;
      flex-shrink: 0;
      width: 140px;
      height: 140px;
    }
    .score-svg {
      width: 100%;
      height: 100%;
    }
    .score-center {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
    }
    .score-number {
      font-size: 2rem;
      font-weight: 800;
      line-height: 1;
      color: var(--text-primary);
    }
    .score-grade {
      font-size: 1rem;
      font-weight: 700;
    }
    .score-components {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .component-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .component-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .component-name {
      font-size: 0.8125rem;
      color: var(--text-secondary);
    }
    .component-score {
      font-size: 0.8125rem;
      font-weight: 600;
    }
    @media (max-width: 600px) {
      .health-score-widget { flex-direction: column; align-items: center; }
      .score-components { width: 100%; }
    }
  `]
})
export class FinancialHealthScoreComponent {
  @Input() score = 0;
  @Input() grade = 'F';
  @Input() breakdown: HealthScoreComponent[] = [];

  readonly circumference = 2 * Math.PI * 54;

  dashOffset(): number {
    const percent = this.score / 100;
    return this.circumference - percent * this.circumference;
  }

  scoreColor(): string {
    if (this.score >= 80) return 'var(--accent-green)';
    if (this.score >= 60) return 'var(--accent-yellow)';
    return 'var(--accent-red)';
  }

  getComponentPercent(component: HealthScoreComponent): number {
    return (component.score / component.maxScore) * 100;
  }

  getComponentColor(score: number, max: number): string {
    const pct = (score / max) * 100;
    if (pct >= 80) return 'var(--accent-green)';
    if (pct >= 60) return 'var(--accent-yellow)';
    return 'var(--accent-red)';
  }
}
