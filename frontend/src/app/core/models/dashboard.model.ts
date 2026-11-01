import { StockAlert } from './inventory.model';
import { StockMovement } from './movement.model';
import { OrderResponse } from './order.model';

export interface DashboardKPIs {
  totalProducts: number;
  totalProductsDeltaMonth?: number;
  totalInventoryUnits: number;
  totalLocationsCount: number;
  lowStockItemsCount: number;
  outOfStockItemsCount: number;
  totalInventoryValue: number;
  inventoryValuationCurrency: string;
}

export interface LocationInventorySummary {
  locationId: number;
  locationName: string;
  code?: string;
  type: string;
  totalSkus: number;
  totalUnits: number;
  totalValue: number;
  status: 'OPTIMAL' | 'LOW_STOCK' | 'CRITICAL' | 'ACTIVE';
}

export interface DashboardSummary {
  kpis: DashboardKPIs;
  locationsOverview: LocationInventorySummary[];
  stockAlerts: StockAlert[];
  recentMovements: StockMovement[];
  recentOrders: OrderResponse[];
  lastUpdated: string;
  partialErrors?: string[];
}
