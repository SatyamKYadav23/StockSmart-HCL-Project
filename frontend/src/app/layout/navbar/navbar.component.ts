import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { IconComponent } from '../../shared/components/icon/icon.component';
import { LayoutService } from '../../core/services/layout.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {
  readonly layoutService = inject(LayoutService);
  private readonly router = inject(Router);

  currentPageTitle = 'Dashboard';
  unreadNotificationsCount = 3;

  private readonly routeTitles: Record<string, string> = {
    '/dashboard': 'Dashboard Overview',
    '/products': 'Product Catalog',
    '/categories': 'Category Management',
    '/inventory': 'Inventory Management',
    '/locations': 'Warehouse & Store Locations',
    '/orders': 'Purchase & Transfer Orders',
    '/purchase-receivings': 'Purchase Receiving Operations',
    '/stock-transfers': 'Inter-Facility Stock Transfers',
    '/suppliers': 'Supplier Directory',
    '/movements': 'Stock Movement Audit Log',
    '/reports': 'Inventory Reports & KPIs',
    '/settings': 'System Settings'
  };

  constructor() {
    this.updateTitle(this.router.url);

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.updateTitle(event.urlAfterRedirects);
      });
  }

  private updateTitle(url: string): void {
    const matchedPath = Object.keys(this.routeTitles).find(path =>
      url === path || url.startsWith(path + '/')
    );
    this.currentPageTitle = matchedPath ? this.routeTitles[matchedPath] : 'Dashboard';
  }

  onSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.layoutService.searchQuery.set(input.value);
  }
}
