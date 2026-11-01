import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell/app-shell.component';

export const routes: Routes = [
  {
    path: '',
    component: AppShellComponent,
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        title: 'StockSmart — Dashboard'
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/products/products.component').then((m) => m.ProductsComponent),
        title: 'StockSmart — Products'
      },
      {
        path: 'categories',
        loadComponent: () =>
          import('./features/categories/categories.component').then((m) => m.CategoriesComponent),
        title: 'StockSmart — Categories'
      },
      {
        path: 'inventory',
        loadComponent: () =>
          import('./features/inventory/inventory.component').then((m) => m.InventoryComponent),
        title: 'StockSmart — Inventory'
      },
      {
        path: 'locations',
        loadComponent: () =>
          import('./features/locations/locations.component').then((m) => m.LocationsComponent),
        title: 'StockSmart — Locations'
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/orders/orders.component').then((m) => m.OrdersComponent),
        title: 'StockSmart — Orders'
      },
      {
        path: 'purchase-receivings',
        loadComponent: () =>
          import('./features/purchase-receivings/purchase-receivings.component').then((m) => m.PurchaseReceivingsComponent),
        title: 'StockSmart — Purchase Receiving'
      },
      {
        path: 'stock-transfers',
        loadComponent: () =>
          import('./features/stock-transfers/stock-transfers.component').then((m) => m.StockTransfersComponent),
        title: 'StockSmart — Inter-Facility Stock Transfers'
      },
      {
        path: 'suppliers',
        loadComponent: () =>
          import('./features/suppliers/suppliers.component').then((m) => m.SuppliersComponent),
        title: 'StockSmart — Suppliers'
      },
      {
        path: 'movements',
        loadComponent: () =>
          import('./features/movements/movements.component').then((m) => m.MovementsComponent),
        title: 'StockSmart — Stock Movements'
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/reports.component').then((m) => m.ReportsComponent),
        title: 'StockSmart — Reports & KPIs'
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then((m) => m.SettingsComponent),
        title: 'StockSmart — System Settings'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
