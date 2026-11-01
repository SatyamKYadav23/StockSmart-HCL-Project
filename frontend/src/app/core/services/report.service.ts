import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InventoryOverviewReportResponse,
  StockStatusReportResponse,
  InventoryValuationReportResponse,
  StockMovementReportResponse,
  OrderSummaryReportResponse,
  PurchaseReceivingReportResponse,
  StockTransferReportResponse
} from '../models/report.model';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/reports';

  // 1. Inventory Overview
  getInventoryOverview(): Observable<InventoryOverviewReportResponse> {
    return this.http.get<InventoryOverviewReportResponse>(`${this.baseUrl}/inventory-overview`);
  }

  // 2. Stock Status
  getStockStatus(filters?: {
    locationId?: number | null;
    categoryId?: number | null;
    productId?: number | null;
    status?: string | null;
  }): Observable<StockStatusReportResponse> {
    let params = new HttpParams();
    if (filters?.locationId) params = params.set('locationId', filters.locationId.toString());
    if (filters?.categoryId) params = params.set('categoryId', filters.categoryId.toString());
    if (filters?.productId) params = params.set('productId', filters.productId.toString());
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);

    return this.http.get<StockStatusReportResponse>(`${this.baseUrl}/stock-status`, { params });
  }

  // 3. Inventory Valuation
  getInventoryValuation(filters?: {
    locationId?: number | null;
    categoryId?: number | null;
  }): Observable<InventoryValuationReportResponse> {
    let params = new HttpParams();
    if (filters?.locationId) params = params.set('locationId', filters.locationId.toString());
    if (filters?.categoryId) params = params.set('categoryId', filters.categoryId.toString());

    return this.http.get<InventoryValuationReportResponse>(`${this.baseUrl}/inventory-valuation`, { params });
  }

  // 4. Stock Movements
  getStockMovements(filters?: {
    startDate?: string | null;
    endDate?: string | null;
    productId?: number | null;
    locationId?: number | null;
    movementType?: string | null;
    reference?: string | null;
  }): Observable<StockMovementReportResponse> {
    let params = new HttpParams();
    if (filters?.startDate) params = params.set('startDate', filters.startDate);
    if (filters?.endDate) params = params.set('endDate', filters.endDate);
    if (filters?.productId) params = params.set('productId', filters.productId.toString());
    if (filters?.locationId) params = params.set('locationId', filters.locationId.toString());
    if (filters?.movementType && filters.movementType !== 'ALL') params = params.set('movementType', filters.movementType);
    if (filters?.reference) params = params.set('reference', filters.reference);

    return this.http.get<StockMovementReportResponse>(`${this.baseUrl}/stock-movements`, { params });
  }

  // 5. Order Summary
  getOrderSummary(filters?: {
    startDate?: string | null;
    endDate?: string | null;
    status?: string | null;
    locationId?: number | null;
  }): Observable<OrderSummaryReportResponse> {
    let params = new HttpParams();
    if (filters?.startDate) params = params.set('startDate', filters.startDate);
    if (filters?.endDate) params = params.set('endDate', filters.endDate);
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);
    if (filters?.locationId) params = params.set('locationId', filters.locationId.toString());

    return this.http.get<OrderSummaryReportResponse>(`${this.baseUrl}/order-summary`, { params });
  }

  // 6. Purchase Receiving
  getPurchaseReceivings(filters?: {
    startDate?: string | null;
    endDate?: string | null;
    supplierId?: number | null;
    locationId?: number | null;
    status?: string | null;
  }): Observable<PurchaseReceivingReportResponse> {
    let params = new HttpParams();
    if (filters?.startDate) params = params.set('startDate', filters.startDate);
    if (filters?.endDate) params = params.set('endDate', filters.endDate);
    if (filters?.supplierId) params = params.set('supplierId', filters.supplierId.toString());
    if (filters?.locationId) params = params.set('locationId', filters.locationId.toString());
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);

    return this.http.get<PurchaseReceivingReportResponse>(`${this.baseUrl}/purchase-receivings`, { params });
  }

  // 7. Stock Transfers
  getStockTransfers(filters?: {
    startDate?: string | null;
    endDate?: string | null;
    productId?: number | null;
    sourceLocationId?: number | null;
    destinationLocationId?: number | null;
    status?: string | null;
  }): Observable<StockTransferReportResponse> {
    let params = new HttpParams();
    if (filters?.startDate) params = params.set('startDate', filters.startDate);
    if (filters?.endDate) params = params.set('endDate', filters.endDate);
    if (filters?.productId) params = params.set('productId', filters.productId.toString());
    if (filters?.sourceLocationId) params = params.set('sourceLocationId', filters.sourceLocationId.toString());
    if (filters?.destinationLocationId) params = params.set('destinationLocationId', filters.destinationLocationId.toString());
    if (filters?.status && filters.status !== 'ALL') params = params.set('status', filters.status);

    return this.http.get<StockTransferReportResponse>(`${this.baseUrl}/stock-transfers`, { params });
  }

  // ==========================================
  // CSV EXPORT FUNCTIONS
  // ==========================================

  exportInventoryOverviewCsv(data: InventoryOverviewReportResponse): void {
    const lines: string[] = [];
    lines.push('StockSmart — Inventory Overview Report');
    lines.push(`Export Date,${new Date().toISOString()}`);
    lines.push(`Report Type,Current-State Snapshot`);
    lines.push('');
    lines.push('--- EXECUTIVE SUMMARY ---');
    lines.push(`Total Active Products,${data.totalActiveProducts}`);
    lines.push(`Total Units on Hand,${data.totalUnitsOnHand}`);
    lines.push(`Total Reserved Units,${data.totalReservedUnits}`);
    lines.push(`Total Available Units,${data.totalAvailableUnits}`);
    lines.push(`Active Facilities,${data.totalFacilities}`);
    lines.push(`Total Inventory Valuation (INR),₹${data.totalValuation.toFixed(2)}`);
    lines.push('');
    lines.push('--- FACILITY BREAKDOWN ---');
    lines.push('Facility Name,Type,Products Stocked,Units on Hand,Reserved Units,Available Units,Valuation (INR)');

    for (const f of data.facilitySummaries) {
      lines.push(
        `"${this.escapeCsv(f.locationName)}","${this.escapeCsv(f.locationType)}",${f.productsStocked},${f.unitsOnHand},${f.reservedUnits},${f.availableUnits},₹${f.valuation.toFixed(2)}`
      );
    }

    this.downloadCsv(lines, 'stocksmart-inventory-overview');
  }

  exportStockStatusCsv(data: StockStatusReportResponse): void {
    const lines: string[] = [];
    lines.push('StockSmart — Stock Status Report');
    lines.push(`Export Date,${new Date().toISOString()}`);
    lines.push(`Total Items,${data.totalItems}`);
    lines.push(`In Stock,${data.inStockCount},Low Stock,${data.lowStockCount},Out of Stock,${data.outOfStockCount},Not Stocked,${data.uninitializedCount}`);
    lines.push('');
    lines.push('SKU,Product Name,Category,Facility,Units on Hand,Reserved Units,Available Units,Reorder Level,Status,Initialized');

    for (const i of data.items) {
      lines.push(
        `"${this.escapeCsv(i.sku)}","${this.escapeCsv(i.productName)}","${this.escapeCsv(i.categoryName || '')}","${this.escapeCsv(i.locationName)}",${i.onHand},${i.reserved},${i.available},${i.reorderLevel},"${i.status}",${i.initialized}`
      );
    }

    this.downloadCsv(lines, 'stocksmart-stock-status-report');
  }

  exportInventoryValuationCsv(data: InventoryValuationReportResponse): void {
    const lines: string[] = [];
    lines.push('StockSmart — Inventory Valuation Report');
    lines.push(`Export Date,${new Date().toISOString()}`);
    lines.push(`Total Valuation (INR),₹${data.totalValuation.toFixed(2)}`);
    lines.push(`Total Units on Hand,${data.totalUnitsOnHand}`);
    lines.push(`Products with Missing Cost,${data.productsWithMissingCost}`);
    lines.push('');
    lines.push('--- FACILITY VALUATION SUMMARY ---');
    lines.push('Facility,Total Units,Valuation (INR),Missing Cost SKUs');
    for (const f of data.facilityBreakdown) {
      lines.push(`"${this.escapeCsv(f.locationName)}",${f.totalUnits},₹${f.valuation.toFixed(2)},${f.missingCostCount}`);
    }
    lines.push('');
    lines.push('--- PRODUCT VALUATION AUDIT ---');
    lines.push('SKU,Product Name,Category,Facility,Units on Hand,Cost Basis (INR),Total Valuation (INR),Missing Cost');

    for (const i of data.items) {
      const costStr = i.costPrice != null ? `₹${i.costPrice.toFixed(2)}` : 'NOT_SET';
      lines.push(
        `"${this.escapeCsv(i.sku)}","${this.escapeCsv(i.productName)}","${this.escapeCsv(i.categoryName)}","${this.escapeCsv(i.locationName)}",${i.quantity},${costStr},₹${i.valuation.toFixed(2)},${i.missingCost}`
      );
    }

    this.downloadCsv(lines, 'stocksmart-inventory-valuation-report');
  }

  exportStockMovementsCsv(data: StockMovementReportResponse): void {
    const lines: string[] = [];
    lines.push('StockSmart — Stock Movements Audit Report');
    lines.push(`Export Date,${new Date().toISOString()}`);
    lines.push(`Total Movements,${data.totalMovements}`);
    lines.push(`Total Inflow Units,+${data.totalInflowUnits}`);
    lines.push(`Total Outflow Units,-${data.totalOutflowUnits}`);
    lines.push(`Net Delta Units,${data.netDeltaUnits >= 0 ? '+' : ''}${data.netDeltaUnits}`);
    lines.push('');
    lines.push('Timestamp,Reference,Type,Direction,Product Name,SKU,Facility,Quantity,Signed Quantity,Remarks');

    for (const m of data.items) {
      lines.push(
        `"${m.createdAt}","${this.escapeCsv(m.referenceType || '')}","${m.movementType}","${m.direction}","${this.escapeCsv(m.productName)}","${this.escapeCsv(m.sku)}","${this.escapeCsv(m.locationName)}",${m.quantity},${m.signedQuantity},"${this.escapeCsv(m.remarks)}"`
      );
    }

    this.downloadCsv(lines, 'stocksmart-stock-movements-report');
  }

  exportOrderSummaryCsv(data: OrderSummaryReportResponse): void {
    const lines: string[] = [];
    lines.push('StockSmart — Order Summary Report');
    lines.push(`Export Date,${new Date().toISOString()}`);
    lines.push(`Total Orders,${data.totalOrders}`);
    lines.push(`Completed Orders,${data.completedOrders}`);
    lines.push(`Pending Orders,${data.pendingOrders}`);
    lines.push(`Cancelled Orders,${data.cancelledOrders}`);
    lines.push(`Total Order Quantity,${data.totalQuantity}`);
    lines.push(`Realized Sales Value (INR),₹${data.completedOrderValue.toFixed(2)}`);
    lines.push(`Total Order Value (INR),₹${data.totalOrderValue.toFixed(2)}`);
    lines.push('');
    lines.push('Order Number,Order Date,Customer,Facility,Status,Line Items,Total Quantity,Order Value (INR)');

    for (const o of data.items) {
      lines.push(
        `"${this.escapeCsv(o.orderNumber)}","${o.orderDate}","${this.escapeCsv(o.customerName)}","${this.escapeCsv(o.locationName || '')}","${o.status}",${o.itemCount},${o.totalQuantity},₹${o.orderValue.toFixed(2)}`
      );
    }

    this.downloadCsv(lines, 'stocksmart-order-summary-report');
  }

  exportPurchaseReceivingsCsv(data: PurchaseReceivingReportResponse): void {
    const lines: string[] = [];
    lines.push('StockSmart — Purchase Receiving Report');
    lines.push(`Export Date,${new Date().toISOString()}`);
    lines.push(`Total Receipts,${data.totalReceipts}`);
    lines.push(`Received Receipts,${data.completedReceipts}`);
    lines.push(`Cancelled Receipts,${data.cancelledReceipts}`);
    lines.push(`Total Received Units,${data.receivedQuantity}`);
    lines.push(`Total Received Value (INR),₹${data.receivedValue.toFixed(2)}`);
    lines.push('');
    lines.push('--- SUPPLIER BREAKDOWN ---');
    lines.push('Supplier Name,Receipts Count,Received Units,Total Value (INR)');
    for (const s of data.supplierBreakdown) {
      lines.push(`"${this.escapeCsv(s.supplierName)}",${s.receiptsCount},${s.receivedUnits},₹${s.totalAmount.toFixed(2)}`);
    }
    lines.push('');
    lines.push('--- RECEIVING TRANSACTIONS ---');
    lines.push('Receiving Number,Date,Supplier,Facility,Status,Items,Received Qty,Total Value (INR),Remarks');

    for (const r of data.items) {
      lines.push(
        `"${this.escapeCsv(r.receivingNumber)}","${r.receivingDate}","${this.escapeCsv(r.supplierName)}","${this.escapeCsv(r.locationName)}","${r.status}",${r.itemCount},${r.receivedQuantity},₹${r.totalAmount.toFixed(2)},"${this.escapeCsv(r.remarks || '')}"`
      );
    }

    this.downloadCsv(lines, 'stocksmart-purchase-receiving-report');
  }

  exportStockTransfersCsv(data: StockTransferReportResponse): void {
    const lines: string[] = [];
    lines.push('StockSmart — Inter-Facility Stock Transfers Report');
    lines.push(`Export Date,${new Date().toISOString()}`);
    lines.push(`Total Transfers,${data.totalTransfers}`);
    lines.push(`Completed Transfers,${data.completedTransfers}`);
    lines.push(`Cancelled Transfers,${data.cancelledTransfers}`);
    lines.push(`Total Relocated Units,${data.totalUnitsRelocated}`);
    lines.push('');
    lines.push('--- SOURCE FACILITY DISPATCHES ---');
    lines.push('Facility,Dispatches Count,Units Relocated');
    for (const s of data.sourceBreakdown) {
      lines.push(`"${this.escapeCsv(s.locationName)}",${s.transferCount},${s.units}`);
    }
    lines.push('');
    lines.push('--- DESTINATION FACILITY RECEIPTS ---');
    lines.push('Facility,Receipts Count,Units Received');
    for (const d of data.destinationBreakdown) {
      lines.push(`"${this.escapeCsv(d.locationName)}",${d.transferCount},${d.units}`);
    }
    lines.push('');
    lines.push('--- TRANSFER TRANSACTIONS ---');
    lines.push('Transfer Number,Date,Product Name,SKU,Source Facility,Destination Facility,Quantity,Status,Remarks');

    for (const t of data.items) {
      lines.push(
        `"${this.escapeCsv(t.transferNumber)}","${t.transferDate}","${this.escapeCsv(t.productName)}","${this.escapeCsv(t.sku)}","${this.escapeCsv(t.sourceLocationName)}","${this.escapeCsv(t.destinationLocationName)}",${t.quantity},"${t.status}","${this.escapeCsv(t.remarks || '')}"`
      );
    }

    this.downloadCsv(lines, 'stocksmart-stock-transfers-report');
  }

  private escapeCsv(value: string): string {
    if (!value) return '';
    return value.replace(/"/g, '""');
  }

  private downloadCsv(lines: string[], filenamePrefix: string): void {
    const csvContent = '\uFEFF' + lines.join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const dateStr = new Date().toISOString().split('T')[0];
    link.setAttribute('href', url);
    link.setAttribute('download', `${filenamePrefix}-${dateStr}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}
