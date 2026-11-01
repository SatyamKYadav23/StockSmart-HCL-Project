import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent, IconName } from '../icon/icon.component';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="empty-state-container">
      <div class="empty-state-icon">
        <app-icon [name]="icon" [size]="24" [color]="'var(--color-slate-500)'"></app-icon>
      </div>
      <h3 class="empty-state-title">{{ title }}</h3>
      <p class="empty-state-description">{{ description }}</p>
      <div *ngIf="actionLabel" class="empty-state-action">
        <button type="button" class="ss-btn ss-btn-secondary ss-btn-sm" (click)="actionClicked.emit()">
          <app-icon *ngIf="actionIcon" [name]="actionIcon" [size]="13"></app-icon>
          <span>{{ actionLabel }}</span>
        </button>
      </div>
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .empty-state-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px 24px;
      text-align: center;
      background-color: var(--bg-surface);
      border-radius: var(--radius-md);
      border: 1px dashed var(--border-strong);
    }

    .empty-state-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      background-color: var(--color-slate-100);
      border: 1px solid var(--border-subtle);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
      box-shadow: var(--shadow-xs);
    }

    .empty-state-title {
      font-size: 0.9375rem;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 4px;
      letter-spacing: -0.01em;
    }

    .empty-state-description {
      font-size: 0.8125rem;
      color: var(--text-muted);
      max-width: 360px;
      line-height: 1.45;
      margin-bottom: 14px;
    }

    .empty-state-action {
      display: flex;
      gap: 8px;
    }
  `]
})
export class EmptyStateComponent {
  @Input() icon: IconName = 'inventory';
  @Input() title: string = 'No records found';
  @Input() description: string = 'There are no items matching your criteria or filters.';
  @Input() actionLabel?: string;
  @Input() actionIcon?: IconName;
  @Output() actionClicked = new EventEmitter<void>();
}
