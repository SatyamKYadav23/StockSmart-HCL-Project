import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

export type IconName =
  | 'dashboard'
  | 'products'
  | 'inventory'
  | 'locations'
  | 'orders'
  | 'suppliers'
  | 'movements'
  | 'reports'
  | 'settings'
  | 'notifications'
  | 'search'
  | 'profile'
  | 'refresh'
  | 'filter'
  | 'plus'
  | 'download'
  | 'arrow-up-right'
  | 'arrow-down-right'
  | 'arrow-right'
  | 'alert-triangle'
  | 'check-circle'
  | 'x-circle'
  | 'clock'
  | 'layers'
  | 'chevron-down'
  | 'chevron-right'
  | 'chevron-left'
  | 'menu'
  | 'x'
  | 'building'
  | 'external-link'
  | 'warehouse'
  | 'dollar'
  | 'boxes'
  | 'trending-up'
  | 'check'
  | 'minus'
  | 'alert-circle'
  | 'tag'
  | 'tags';

@Component({
  selector: 'app-icon',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      [attr.width]="size"
      [attr.height]="size"
      viewBox="0 0 24 24"
      fill="none"
      [attr.stroke]="color || 'currentColor'"
      [attr.stroke-width]="strokeWidth"
      stroke-linecap="round"
      stroke-linejoin="round"
      [class]="extraClass"
      aria-hidden="true"
    >
      <ng-container [ngSwitch]="name">
        <!-- Dashboard / Layout Grid -->
        <g *ngSwitchCase="'dashboard'">
          <rect width="7" height="9" x="3" y="3" rx="1" />
          <rect width="7" height="5" x="14" y="3" rx="1" />
          <rect width="7" height="9" x="14" y="12" rx="1" />
          <rect width="7" height="5" x="3" y="16" rx="1" />
        </g>

        <!-- Products / Package -->
        <g *ngSwitchCase="'products'">
          <path d="m7.5 4.27 9 5.15" />
          <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
          <path d="m3.3 7 8.7 5 8.7-5" />
          <path d="M12 22V12" />
        </g>

        <!-- Inventory / Boxes -->
        <g *ngSwitchCase="'inventory'">
          <path d="M2.97 12.92A2 2 0 0 0 2 14.63v3.24a2 2 0 0 0 .97 1.71l6 3.43a2 2 0 0 0 1.96 0l6-3.43a2 2 0 0 0 .97-1.71v-3.24a2 2 0 0 0-.97-1.71L11 9.49a2 2 0 0 0-2.06 0l-5.97 3.43Z" />
          <path d="m17 13.5 4.03-2.3a2 2 0 0 0 .97-1.71V6.25a2 2 0 0 0-.97-1.71l-6-3.43a2 2 0 0 0-1.96 0l-6 3.43a2 2 0 0 0-.97 1.71v3.24a2 2 0 0 0 .97 1.71L11 13.5" />
          <path d="M10 21.5V14" />
          <path d="M16 10.5V3" />
        </g>

        <!-- Locations / Map Pin -->
        <g *ngSwitchCase="'locations'">
          <path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0" />
          <circle cx="12" cy="10" r="3" />
        </g>

        <!-- Orders / Shopping Bag or File Text -->
        <g *ngSwitchCase="'orders'">
          <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z" />
          <path d="M3 6h18" />
          <path d="M16 10a4 4 0 0 1-8 0" />
        </g>

        <!-- Suppliers / Truck -->
        <g *ngSwitchCase="'suppliers'">
          <path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2" />
          <path d="M15 18H9" />
          <path d="M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.62l-3.48-4.35A1 1 0 0 0 17.52 8H14v10Z" />
          <circle cx="17" cy="18" r="2" />
          <circle cx="7" cy="18" r="2" />
        </g>

        <!-- Movements / Arrow Left Right -->
        <g *ngSwitchCase="'movements'">
          <path d="m16 3 4 4-4 4" />
          <path d="M20 7H4" />
          <path d="m8 21-4-4 4-4" />
          <path d="M4 17h16" />
        </g>

        <!-- Reports / Bar Chart -->
        <g *ngSwitchCase="'reports'">
          <path d="M3 3v18h18" />
          <path d="M18 17V9" />
          <path d="M13 17V5" />
          <path d="M8 17v-3" />
        </g>

        <!-- Settings / Sliders or Cog -->
        <g *ngSwitchCase="'settings'">
          <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
          <circle cx="12" cy="12" r="3" />
        </g>

        <!-- Notifications / Bell -->
        <g *ngSwitchCase="'notifications'">
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
          <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </g>

        <!-- Search -->
        <g *ngSwitchCase="'search'">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.3-4.3" />
        </g>

        <!-- Profile / User -->
        <g *ngSwitchCase="'profile'">
          <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </g>

        <!-- Refresh -->
        <g *ngSwitchCase="'refresh'">
          <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
          <path d="M21 3v5h-5" />
          <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
          <path d="M8 16H3v5" />
        </g>

        <!-- Filter -->
        <g *ngSwitchCase="'filter'">
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
        </g>

        <!-- Plus -->
        <g *ngSwitchCase="'plus'">
          <path d="M5 12h14" />
          <path d="M12 5v14" />
        </g>

        <!-- Download -->
        <g *ngSwitchCase="'download'">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="7 10 12 15 17 10" />
          <line x1="12" x2="12" y1="15" y2="3" />
        </g>

        <!-- Arrow Up Right -->
        <g *ngSwitchCase="'arrow-up-right'">
          <path d="M7 7h10v10" />
          <path d="M7 17 17 7" />
        </g>

        <!-- Arrow Down Right -->
        <g *ngSwitchCase="'arrow-down-right'">
          <path d="M7 7 17 17" />
          <path d="M17 7v10H7" />
        </g>

        <!-- Arrow Right -->
        <g *ngSwitchCase="'arrow-right'">
          <path d="M5 12h14" />
          <path d="m12 5 7 7-7 7" />
        </g>

        <!-- Alert Triangle -->
        <g *ngSwitchCase="'alert-triangle'">
          <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
          <line x1="12" x2="12" y1="9" y2="13" />
          <line x1="12" x2="12.01" y1="17" y2="17" />
        </g>

        <!-- Alert Circle -->
        <g *ngSwitchCase="'alert-circle'">
          <circle cx="12" cy="12" r="10" />
          <line x1="12" x2="12" y1="8" y2="12" />
          <line x1="12" x2="12.01" y1="16" y2="16" />
        </g>

        <!-- Check Circle -->
        <g *ngSwitchCase="'check-circle'">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </g>

        <!-- X Circle -->
        <g *ngSwitchCase="'x-circle'">
          <circle cx="12" cy="12" r="10" />
          <path d="m15 9-6 6" />
          <path d="m9 9 6 6" />
        </g>

        <!-- Clock -->
        <g *ngSwitchCase="'clock'">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </g>

        <!-- Chevron Down -->
        <g *ngSwitchCase="'chevron-down'">
          <path d="m6 9 6 6 6-6" />
        </g>

        <!-- Chevron Right -->
        <g *ngSwitchCase="'chevron-right'">
          <path d="m9 18 6-6-6-6" />
        </g>

        <!-- Chevron Left -->
        <g *ngSwitchCase="'chevron-left'">
          <path d="m15 18-6-6 6-6" />
        </g>

        <!-- Menu / Hamburger -->
        <g *ngSwitchCase="'menu'">
          <line x1="4" x2="20" y1="12" y2="12" />
          <line x1="4" x2="20" y1="6" y2="6" />
          <line x1="4" x2="20" y1="18" y2="18" />
        </g>

        <!-- X / Close -->
        <g *ngSwitchCase="'x'">
          <path d="M18 6 6 18" />
          <path d="m6 6 12 12" />
        </g>

        <!-- Warehouse / Building -->
        <g *ngSwitchCase="'warehouse'">
          <path d="M22 8.35V20a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8.35A2 2 0 0 1 3.26 6.5l8-3.2a2 2 0 0 1 1.48 0l8 3.2A2 2 0 0 1 22 8.35Z" />
          <path d="M6 18h12" />
          <path d="M6 14h12" />
          <path d="M6 10h12" />
        </g>

        <!-- Dollar / Currency -->
        <g *ngSwitchCase="'dollar'">
          <line x1="12" x2="12" y1="2" y2="22" />
          <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
        </g>

        <!-- Trending Up -->
        <g *ngSwitchCase="'trending-up'">
          <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
          <polyline points="16 7 22 7 22 13" />
        </g>

        <!-- Check -->
        <g *ngSwitchCase="'check'">
          <polyline points="20 6 9 17 4 12" />
        </g>

        <!-- Minus -->
        <g *ngSwitchCase="'minus'">
          <line x1="5" x2="19" y1="12" y2="12" />
        </g>

        <!-- Tag -->
        <g *ngSwitchCase="'tag'">
          <path d="M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z" />
          <circle cx="7.5" cy="7.5" r=".5" fill="currentColor" />
        </g>

        <!-- Tags -->
        <g *ngSwitchCase="'tags'">
          <path d="m15 5 6.3 6.3a2.4 2.4 0 0 1 0 3.4L17 19" />
          <path d="M9.586 5.586A2 2 0 0 0 8.172 5H3a2 2 0 0 0-2 2v5.172a2 2 0 0 0 .586 1.414L8.29 20.29a2.426 2.426 0 0 0 3.42 0l5.58-5.58a2.426 2.426 0 0 0 0-3.42z" />
          <circle cx="6.5" cy="9.5" r=".5" fill="currentColor" />
        </g>

        <!-- Default fallback -->
        <g *ngSwitchDefault>
          <circle cx="12" cy="12" r="10" />
        </g>
      </ng-container>
    </svg>
  `,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
      vertical-align: middle;
      flex-shrink: 0;
    }
  `]
})
export class IconComponent {
  @Input({ required: true }) name!: IconName;
  @Input() size: number = 18;
  @Input() color?: string;
  @Input() strokeWidth: number = 2;
  @Input() extraClass: string = '';
}
