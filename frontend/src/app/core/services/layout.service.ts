import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LayoutService {
  /**
   * Tracks whether the sidebar is collapsed in mobile/tablet viewport drawer mode
   */
  readonly isSidebarOpenMobile = signal(false);

  /**
   * Tracks whether the sidebar is in compact / mini mode on desktop
   */
  readonly isSidebarCompact = signal(false);

  /**
   * Global search input query
   */
  readonly searchQuery = signal('');

  toggleSidebarMobile(): void {
    this.isSidebarOpenMobile.update(open => !open);
  }

  closeSidebarMobile(): void {
    this.isSidebarOpenMobile.set(false);
  }

  toggleSidebarCompact(): void {
    this.isSidebarCompact.update(compact => !compact);
  }
}
