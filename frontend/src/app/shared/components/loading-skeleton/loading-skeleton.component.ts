import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="ss-skeleton"
      [style.width]="width"
      [style.height]="height"
      [style.border-radius]="borderRadius"
      [class]="extraClass"
    ></div>
  `,
  styles: [`
    :host {
      display: inline-block;
      width: 100%;
    }
  `]
})
export class LoadingSkeletonComponent {
  @Input() width: string = '100%';
  @Input() height: string = '16px';
  @Input() borderRadius: string = 'var(--radius-sm)';
  @Input() extraClass: string = '';
}
