import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-error-state',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="error-state-container">
      <div class="error-state-icon">
        <app-icon name="alert-triangle" [size]="20" [color]="'var(--color-danger-solid)'"></app-icon>
      </div>
      <h3 class="error-state-title">{{ title }}</h3>
      <p class="error-state-description">{{ description }}</p>
      <div *ngIf="showRetry" class="error-state-action">
        <button type="button" class="ss-btn ss-btn-secondary ss-btn-sm" (click)="retry.emit()">
          <app-icon name="refresh" [size]="13"></app-icon>
          <span>{{ retryLabel }}</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .error-state-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 36px 24px;
      text-align: center;
      background-color: var(--color-danger-bg);
      border-radius: var(--radius-md);
      border: 1px solid var(--color-danger-border);
    }

    .error-state-icon {
      width: 42px;
      height: 42px;
      border-radius: var(--radius-md);
      background-color: #ffffff;
      border: 1px solid var(--color-danger-border);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12px;
      box-shadow: var(--shadow-xs);
    }

    .error-state-title {
      font-size: 0.9375rem;
      font-weight: 600;
      color: var(--color-danger-text);
      margin-bottom: 4px;
    }

    .error-state-description {
      font-size: 0.8125rem;
      color: var(--text-secondary);
      max-width: 380px;
      line-height: 1.4;
      margin-bottom: 14px;
    }

    .error-state-action {
      display: flex;
    }
  `]
})
export class ErrorStateComponent {
  @Input() title: string = 'Unable to load data';
  @Input() description: string = 'Something went wrong while retrieving inventory data.';
  @Input() showRetry: boolean = true;
  @Input() retryLabel: string = 'Try again';
  @Output() retry = new EventEmitter<void>();
}
