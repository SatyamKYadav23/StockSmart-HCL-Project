import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'primary';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="ss-badge"
      [ngClass]="'ss-badge-' + variant"
      [class.ss-badge-sm]="size === 'sm'"
    >
      <span *ngIf="showDot" class="ss-badge-dot"></span>
      <ng-content></ng-content>
      {{ label }}
    </span>
  `,
  styles: [`
    :host {
      display: inline-flex;
      vertical-align: middle;
    }
    .ss-badge-sm {
      font-size: 0.6875rem;
      padding: 1.5px 7px;
    }
  `]
})
export class BadgeComponent {
  @Input() variant: BadgeVariant = 'neutral';
  @Input() label?: string;
  @Input() showDot: boolean = true;
  @Input() size: 'sm' | 'md' = 'md';
}
