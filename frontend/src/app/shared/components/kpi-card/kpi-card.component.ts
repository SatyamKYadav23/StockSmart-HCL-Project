import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent, IconName } from '../icon/icon.component';
import { BadgeComponent, BadgeVariant } from '../badge/badge.component';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule, IconComponent, BadgeComponent],
  template: `
    <div
      class="ss-card kpi-card"
      [class.kpi-card-warning]="accent === 'warning'"
      [class.kpi-card-danger]="accent === 'danger'"
      [class.kpi-card-primary]="accent === 'primary'"
    >
      <div class="kpi-header">
        <span class="kpi-label">{{ label }}</span>
        <div class="kpi-icon-wrapper" [ngClass]="'icon-' + (accent || 'neutral')">
          <app-icon [name]="icon" [size]="15"></app-icon>
        </div>
      </div>

      <div class="kpi-body">
        <div class="kpi-value font-mono">{{ value }}</div>

        <div class="kpi-footer">
          <app-badge
            *ngIf="badgeText"
            [variant]="badgeVariant"
            [size]="'sm'"
            [showDot]="badgeShowDot"
            [label]="badgeText"
          ></app-badge>

          <span *ngIf="subtext" class="kpi-subtext">{{ subtext }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .kpi-card {
      padding: 18px 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      position: relative;
      overflow: hidden;
      background-color: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm);
      transition: all var(--transition-fast);
    }

    .kpi-card:hover {
      border-color: var(--color-slate-300);
      box-shadow: var(--shadow-md);
      transform: translateY(-1px);
    }

    .kpi-card-warning {
      border-left: 3px solid var(--color-warning-solid);
    }

    .kpi-card-danger {
      border-left: 3px solid var(--color-danger-solid);
    }

    .kpi-card-primary {
      border-left: 3px solid var(--color-primary-600);
    }

    .kpi-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .kpi-label {
      font-size: 0.6875rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
    }

    .kpi-icon-wrapper {
      width: 30px;
      height: 30px;
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: var(--color-slate-50);
      border: 1px solid var(--border-subtle);
      color: var(--color-slate-600);
      transition: all var(--transition-fast);
    }

    .kpi-icon-wrapper.icon-primary {
      background-color: var(--color-primary-50);
      border-color: var(--color-primary-200);
      color: var(--color-primary-700);
    }

    .kpi-icon-wrapper.icon-warning {
      background-color: var(--color-warning-bg);
      border-color: var(--color-warning-border);
      color: var(--color-warning-solid);
    }

    .kpi-icon-wrapper.icon-danger {
      background-color: var(--color-danger-bg);
      border-color: var(--color-danger-border);
      color: var(--color-danger-solid);
    }

    .kpi-body {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .kpi-value {
      font-size: 1.625rem;
      font-weight: 700;
      color: var(--text-primary);
      line-height: 1.2;
      letter-spacing: -0.03em;
    }

    .kpi-footer {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      min-height: 20px;
    }

    .kpi-subtext {
      font-size: 0.75rem;
      color: var(--text-muted);
      line-height: 1.3;
    }
  `]
})
export class KpiCardComponent {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) value!: string | number | null;
  @Input({ required: true }) icon!: IconName;
  @Input() subtext?: string;
  @Input() badgeText?: string;
  @Input() badgeVariant: BadgeVariant = 'neutral';
  @Input() badgeShowDot: boolean = true;
  @Input() accent?: 'warning' | 'danger' | 'primary' | 'neutral';
}
