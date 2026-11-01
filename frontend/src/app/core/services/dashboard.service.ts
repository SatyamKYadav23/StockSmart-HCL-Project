import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ProductService } from './product.service';
import { InventoryService } from './inventory.service';
import { LocationService } from './location.service';
import { OrderService } from './order.service';
import { StockMovementService } from './stock-movement.service';
import {
  DashboardSummary,
  DashboardKPIs,
  LocationInventorySummary,
  StockAlert,
  StockMovement,
  OrderResponse,
  Product,
  InventoryResponse,
  Location,
  StockMovementResponse
} from '../models';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly productService = inject(ProductService);
  private readonly inventoryService = inject(InventoryService);
  private readonly locationService = inject(LocationService);
  private readonly orderService = inject(OrderService);
  private readonly stockMovementService = inject(StockMovementService);

  /**
   * Composes real dashboard data from live Spring Boot backend APIs:
   * - GET /api/products
   * - GET /api/inventory
   * - GET /api/locations
   * - GET /api/orders
   * - GET /api/stock-movements
   *
   * Partial failures on non-critical endpoints (orders, movements) are gracefully tolerated.
   */
  getSummary(): Observable<DashboardSummary> {
    const partialErrors: string[] = [];

    return forkJoin({
      products: this.productService.getProducts().pipe(
        catchError(err => {
          partialErrors.push('Products service unreachable');
          return throwError(() => err);
        })
      ),
      inventory: this.inventoryService.getInventory().pipe(
        catchError(err => {
          partialErrors.push('Inventory service unreachable');
          return throwError(() => err);
        })
      ),
      locations: this.locationService.getLocations().pipe(
        catchError(err => {
          partialErrors.push('Locations service unreachable');
          return of([] as Location[]);
        })
      ),
      orders: this.orderService.getOrders().pipe(
        catchError(err => {
          partialErrors.push('Orders service unreachable');
          return of([] as OrderResponse[]);
        })
      ),
      movements: this.stockMovementService.getMovements().pipe(
        catchError(err => {
          partialErrors.push('Stock movements service unreachable');
          return of([] as StockMovementResponse[]);
        })
      )
    }).pipe(
      map(({ products, inventory, locations, orders, movements }) => {
        return this.computeDashboardSummary(
          products,
          inventory,
          locations,
          orders,
          movements,
          partialErrors
        );
      })
    );
  }

  /**
   * Pure aggregation and calculation engine for dashboard metrics
   */
  private computeDashboardSummary(
    products: Product[],
    inventory: InventoryResponse[],
    locations: Location[],
    orders: OrderResponse[],
    movements: StockMovementResponse[],
    partialErrors: string[]
  ): DashboardSummary {
    // 1. Build product lookup map
    const productMap = new Map<number, Product>();
    for (const p of products) {
      productMap.set(p.id, p);
    }

    // 2. Active Products count & recent additions (last 30 days)
    const now = new Date();
    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

    const activeProducts = products.filter(p => p.active !== false);
    const totalProducts = activeProducts.length;

    let recentProductsCount = 0;
    for (const p of products) {
      if (p.createdAt) {
        const createdDate = new Date(p.createdAt);
        if (createdDate >= thirtyDaysAgo) {
          recentProductsCount++;
        }
      }
    }

    // 3. Inventory Aggregation & Valuation
    let totalInventoryUnits = 0;
    let totalInventoryValue = 0;
    let lowStockItemsCount = 0;
    let outOfStockItemsCount = 0;
    const stockAlerts: StockAlert[] = [];

    for (const inv of inventory) {
      const qty = inv.quantity ?? 0;
      totalInventoryUnits += qty;

      const product = productMap.get(inv.productId);
      const reorderLevel = product?.reorderLevel ?? 10;

      // Documented Valuation Rule:
      // Primary: product.costPrice (if available and > 0)
      // Fallback: product.price (selling price)
      // Final: 0
      const unitCost = (product?.costPrice != null && product.costPrice > 0)
        ? product.costPrice
        : ((product?.price != null && product.price > 0) ? product.price : 0);

      totalInventoryValue += qty * unitCost;

      // Low Stock & Out of Stock classification
      if (qty <= 0) {
        outOfStockItemsCount++;
        stockAlerts.push({
          id: inv.id,
          productId: inv.productId,
          productName: inv.productName || product?.name || `Product #${inv.productId}`,
          sku: product?.sku || `SKU-${inv.productId}`,
          locationId: inv.locationId,
          locationName: inv.locationName || `Location #${inv.locationId}`,
          currentStock: qty,
          reorderLevel: reorderLevel,
          deficit: Math.max(0, reorderLevel - qty),
          status: 'OUT_OF_STOCK'
        });
      } else if (qty <= reorderLevel) {
        lowStockItemsCount++;
        const status = qty <= Math.floor(reorderLevel / 2) ? 'CRITICAL' : 'LOW_STOCK';
        stockAlerts.push({
          id: inv.id,
          productId: inv.productId,
          productName: inv.productName || product?.name || `Product #${inv.productId}`,
          sku: product?.sku || `SKU-${inv.productId}`,
          locationId: inv.locationId,
          locationName: inv.locationName || `Location #${inv.locationId}`,
          currentStock: qty,
          reorderLevel: reorderLevel,
          deficit: Math.max(0, reorderLevel - qty),
          status: status
        });
      }
    }

    // Sort stock alerts: OUT_OF_STOCK first, then CRITICAL, then LOW_STOCK
    stockAlerts.sort((a, b) => {
      const severityMap: Record<string, number> = { OUT_OF_STOCK: 1, CRITICAL: 2, LOW_STOCK: 3 };
      return (severityMap[a.status] || 99) - (severityMap[b.status] || 99);
    });

    // 4. Locations Overview Calculation
    const locationsOverview: LocationInventorySummary[] = locations.map(loc => {
      const locInventory = inventory.filter(i => i.locationId === loc.id);
      const locUnits = locInventory.reduce((sum, i) => sum + (i.quantity ?? 0), 0);
      const locSkus = new Set(
        locInventory.filter(i => (i.quantity ?? 0) > 0).map(i => i.productId)
      ).size;

      const locValue = locInventory.reduce((sum, i) => {
        const p = productMap.get(i.productId);
        const cost = (p?.costPrice != null && p.costPrice > 0)
          ? p.costPrice
          : ((p?.price != null && p.price > 0) ? p.price : 0);
        return sum + (i.quantity ?? 0) * cost;
      }, 0);

      // Check if location has any low-stock items
      const hasLowStock = locInventory.some(i => {
        const p = productMap.get(i.productId);
        const threshold = p?.reorderLevel ?? 10;
        return (i.quantity ?? 0) <= threshold;
      });

      return {
        locationId: loc.id,
        locationName: loc.name,
        code: `LOC-${loc.id.toString().padStart(2, '0')}`,
        type: loc.locationType || 'WAREHOUSE',
        totalSkus: locSkus,
        totalUnits: locUnits,
        totalValue: locValue,
        status: hasLowStock ? 'LOW_STOCK' : 'OPTIMAL'
      };
    });

    // 5. Recent Movements formatting
    const recentMovements: StockMovement[] = movements
      .map(m => {
        const prod = productMap.get(m.productId);
        return {
          id: m.id,
          referenceNumber: m.referenceType || `MOV-${m.id}`,
          type: m.movementType,
          direction: m.direction,
          productId: m.productId,
          productName: m.productName || prod?.name || `Product #${m.productId}`,
          sku: m.productSku || prod?.sku || `SKU-${m.productId}`,
          quantity: m.quantity,
          locationName: m.locationName,
          timestamp: m.createdAt,
          notes: m.remarks
        };
      })
      .sort((a, b) => {
        const dateA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
        const dateB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 10);

    // 6. Recent Orders formatting
    const recentOrders = [...orders]
      .sort((a, b) => {
        const dateA = a.orderDate || a.createdAt ? new Date(a.orderDate || a.createdAt).getTime() : 0;
        const dateB = b.orderDate || b.createdAt ? new Date(b.orderDate || b.createdAt).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 8);

    const kpis: DashboardKPIs = {
      totalProducts,
      totalProductsDeltaMonth: recentProductsCount,
      totalInventoryUnits,
      totalLocationsCount: locations.length,
      lowStockItemsCount,
      outOfStockItemsCount,
      totalInventoryValue,
      inventoryValuationCurrency: 'INR'
    };

    return {
      kpis,
      locationsOverview,
      stockAlerts,
      recentMovements,
      recentOrders,
      lastUpdated: new Date().toISOString(),
      partialErrors: partialErrors.length > 0 ? partialErrors : undefined
    };
  }

  /**
   * Client-side CSV export of real dashboard metrics and location summaries
   */
  exportSummaryCsv(summary: DashboardSummary): void {
    const lines: string[] = [];

    // Header
    lines.push('StockSmart — Inventory Optimization System');
    lines.push(`Dashboard Operational & Valuation Summary Export`);
    lines.push(`Generated At,${new Date().toISOString()}`);
    lines.push('');

    // KPIs section
    lines.push('--- EXECUTIVE KPIS ---');
    lines.push('Metric,Value');
    lines.push(`Total Active Products,${summary.kpis.totalProducts}`);
    lines.push(`Total Inventory Units On Hand,${summary.kpis.totalInventoryUnits}`);
    lines.push(`Total Inventory Valuation (INR),₹${summary.kpis.totalInventoryValue.toFixed(2)}`);
    lines.push(`Low Stock Items (Below Reorder Threshold),${summary.kpis.lowStockItemsCount}`);
    lines.push(`Out of Stock Items,${summary.kpis.outOfStockItemsCount}`);
    lines.push(`Active Facilities / Locations,${summary.kpis.totalLocationsCount}`);
    lines.push('');

    // Facilities breakdown section
    lines.push('--- FACILITY INVENTORY BREAKDOWN ---');
    lines.push('Facility Code,Facility Name,Type,Units on Hand,Active SKUs,Valuation (INR),Status');
    for (const loc of summary.locationsOverview) {
      lines.push(
        `"${loc.code || ''}","${loc.locationName}","${loc.type}",${loc.totalUnits},${loc.totalSkus},₹${loc.totalValue.toFixed(2)},"${loc.status}"`
      );
    }
    lines.push('');

    // Stock Alerts section
    if (summary.stockAlerts.length > 0) {
      lines.push('--- STOCK ALERTS (ACTION REQUIRED) ---');
      lines.push('Product,SKU,Facility,Current Stock,Reorder Level,Deficit,Status');
      for (const alert of summary.stockAlerts) {
        lines.push(
          `"${alert.productName}","${alert.sku}","${alert.locationName}",${alert.currentStock},${alert.reorderLevel},${alert.deficit},"${alert.status}"`
        );
      }
      lines.push('');
    }

    // Recent Movements section
    if (summary.recentMovements.length > 0) {
      lines.push('--- RECENT STOCK MOVEMENTS ---');
      lines.push('Timestamp,Reference,Type,Product,SKU,Quantity,Location,Notes');
      for (const m of summary.recentMovements) {
        lines.push(
          `"${m.timestamp}","${m.referenceNumber}","${m.type}","${m.productName}","${m.sku}",${m.quantity},"${m.locationName}","${m.notes || ''}"`
        );
      }
      lines.push('');
    }

    const csvContent = '\uFEFF' + lines.join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const dateStr = new Date().toISOString().split('T')[0];
    link.setAttribute('href', url);
    link.setAttribute('download', `stocksmart-dashboard-summary-${dateStr}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}
