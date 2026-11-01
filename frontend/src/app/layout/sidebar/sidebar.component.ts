import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { IconComponent, IconName } from '../../shared/components/icon/icon.component';
import { LayoutService } from '../../core/services/layout.service';

interface NavItem {
  label: string;
  path: string;
  icon: IconName;
  badge?: string;
}

interface NavSection {
  title?: string;
  items: NavItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  readonly layoutService = inject(LayoutService);

  readonly navSections: NavSection[] = [
    {
      title: 'Core Management',
      items: [
        { label: 'Dashboard', path: '/dashboard', icon: 'dashboard' },
        { label: 'Products', path: '/products', icon: 'products' },
        { label: 'Categories', path: '/categories', icon: 'tag' },
        { label: 'Inventory', path: '/inventory', icon: 'inventory' },
        { label: 'Locations', path: '/locations', icon: 'locations' }
      ]
    },
    {
      title: 'Operations',
      items: [
        { label: 'Orders', path: '/orders', icon: 'orders' },
        { label: 'Receiving', path: '/purchase-receivings', icon: 'boxes' },
        { label: 'Transfers', path: '/stock-transfers', icon: 'arrow-right' },
        { label: 'Suppliers', path: '/suppliers', icon: 'suppliers' },
        { label: 'Movements', path: '/movements', icon: 'movements' }
      ]
    },
    {
      title: 'Analytics & Config',
      items: [
        { label: 'Reports', path: '/reports', icon: 'reports' }
      ]
    }
  ];

  onNavClick(): void {
    // When a link is clicked on mobile screen, close drawer
    if (this.layoutService.isSidebarOpenMobile()) {
      this.layoutService.closeSidebarMobile();
    }
  }
}
