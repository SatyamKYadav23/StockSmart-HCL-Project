# StockSmart — REST API Documentation

Base URL: `http://localhost:8080/api`

All requests and responses use `Content-Type: application/json` unless otherwise noted (e.g. CSV exports use `text/csv`).

---

## 1. Standard Error Responses

Errors follow a uniform JSON structure across all endpoints:

```json
{
  "status": 400,
  "message": "Detailed description of the validation or business rule violation",
  "timestamp": "2026-10-03T00:00:00.000"
}
```

| HTTP Status | Trigger Condition |
| :--- | :--- |
| **400 Bad Request** | Bean Validation failure, malformed JSON, URL path/query parameter type mismatch, invalid argument |
| **404 Not Found** | Target resource ID does not exist |
| **409 Conflict** | Business integrity conflict, duplicate unique constraint violation (SKU, barcode, name), insufficient stock, immutability violation |
| **500 Internal Error** | Shielded server error (details safely recorded in server logs) |

---

## 2. Product Management (`/api/products`)

### `GET /api/products`
Returns all products.
- **Response**: `200 OK` — `List<ProductResponse>`

### `GET /api/products/{id}`
Returns product by primary key ID.
- **Response**: `200 OK` — `ProductResponse` or `404 Not Found`

### `POST /api/products`
Creates a new product record.
- **Request Body**:
```json
{
  "sku": "ELEC-LOGI-MX3",
  "name": "Logitech MX Master 3S",
  "description": "Ergonomic wireless mouse",
  "categoryId": 1,
  "supplierId": 1,
  "price": 8995.00,
  "costPrice": 6200.00,
  "barcode": "8901001000101",
  "reorderLevel": 15
}
```
- **Response**: `201 Created` — `ProductResponse`

### `PUT /api/products/{id}`
Updates an existing product.
- **Response**: `200 OK` — `ProductResponse`

### `DELETE /api/products/{id}`
Deletes a product. If associated with inventory records, returns `409 Conflict`.
- **Response**: `204 No Content` or `409 Conflict`

---

## 3. Location Management (`/api/locations`)

### `GET /api/locations`
Returns all registered warehouse, retail, or hub facilities.

### `POST /api/locations`
Registers a facility.
```json
{
  "name": "Delhi Central Warehouse",
  "address": "Plot 42, Okhla Industrial Area, New Delhi",
  "locationType": "WAREHOUSE"
}
```
- **Response**: `201 Created` — `LocationResponse`

### `DELETE /api/locations/{id}`
Deletes a location. If the facility contains existing stock records, returns `409 Conflict`.

---

## 4. Inventory & Stock Adjustments (`/api/inventory`)

### `GET /api/inventory`
Returns all inventory records across all locations.

### `GET /api/inventory/location/{locationId}`
Filters inventory balances for a specific facility.

### `POST /api/inventory/adjust`
Atomically adjusts stock at a facility and registers an audit movement record (`StockMovement`).
- **Request Body**:
```json
{
  "productId": 1,
  "locationId": 1,
  "quantityChange": 20,
  "referenceNumber": "CYCLE-COUNT-2026",
  "notes": "Verified inventory during quarterly count"
}
```
- **Validation**:
  - `quantityChange` must not be zero.
  - Decreases (`quantityChange < 0`) cannot reduce available stock below 0 or below reserved quantity.
  - Automatically initializes inventory record if positive adjustment for a new product/location pair.
- **Response**: `200 OK` — `InventoryResponse`

---

## 5. Orders (`/api/orders`)

### `GET /api/orders`
Retrieves all customer orders with line items.

### `POST /api/orders`
Creates and optionally fulfills a customer order.
- **Request Body**:
```json
{
  "orderNumber": "ORD-2026-001",
  "status": "COMPLETED",
  "customerName": "Sharma Retailers",
  "locationId": 1,
  "totalAmount": 17990.00,
  "remarks": "Priority local dispatch",
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "unitPrice": 8995.00
    }
  ]
}
```
- **Behavior**:
  - If `status == "COMPLETED"`, inventory is atomically checked and deducted with lock, and a `SALE` movement (`direction = OUT`) is created.
  - If stock is insufficient, transaction aborts with `409 Conflict`.
- **Response**: `201 Created` — `OrderResponse`

### `PUT /api/orders/{id}`
Updates order information. If already `COMPLETED`, status and location cannot be modified.

### `DELETE /api/orders/{id}`
Deletes an order. Rejects deletion with `409 Conflict` if the order is `COMPLETED`.

---

## 6. Purchase Receiving (`/api/purchase-receivings`)

### `GET /api/purchase-receivings`
Retrieves inbound vendor shipment receipts.

### `POST /api/purchase-receivings`
Records inbound inventory intake.
- **Request Body**:
```json
{
  "receivingNumber": "REC-2026-101",
  "receivingDate": "2026-10-02T10:00:00",
  "supplierId": 1,
  "locationId": 1,
  "status": "RECEIVED",
  "totalAmount": 124000.00,
  "remarks": "Q4 Stock Shipment",
  "items": [
    {
      "productId": 1,
      "quantity": 20,
      "unitCost": 6200.00
    }
  ]
}
```
- **Behavior**: When `status == "RECEIVED"`, destination inventory is incremented and an audit record (`RECEIPT`, `direction = IN`) is persisted. Received records are immutable.
- **Response**: `201 Created` — `PurchaseReceivingResponse`

### `PUT /api/purchase-receivings/{id}/cancel`
Cancels a draft/pending receipt. Rejects cancellation of `RECEIVED` shipments with `409 Conflict`.

---

## 7. Inter-Facility Stock Transfers (`/api/stock-transfers`)

### `GET /api/stock-transfers`
Lists all inter-facility transfers.

### `POST /api/stock-transfers`
Executes an atomic transfer between facilities.
- **Request Body**:
```json
{
  "transferNumber": "TRF-2026-501",
  "transferDate": "2026-10-02T14:30:00",
  "productId": 1,
  "sourceLocationId": 1,
  "destinationLocationId": 2,
  "quantity": 10,
  "status": "COMPLETED",
  "remarks": "Rebalancing stock between regional warehouses"
}
```
- **Behavior**:
  - Source inventory is locked and deducted.
  - Destination inventory is locked/initialized and incremented.
  - Dual `StockMovement` records are created: source `OUT`, destination `IN`.
  - Completed transfers are immutable.
- **Response**: `201 Created` — `StockTransferResponse`

---

## 8. Reports & Analytics (`/api/reports`)

All report queries are strictly read-only and compute live values dynamically.

| Endpoint | Parameters | Purpose |
| :--- | :--- | :--- |
| `GET /api/reports/inventory-overview` | — | Total products, total on-hand, reserved, available, total valuation, and facility summaries |
| `GET /api/reports/stock-status` | `locationId`, `categoryId`, `productId`, `status` | Stock balances vs reorder thresholds (`IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`) |
| `GET /api/reports/stock-movements` | `startDate`, `endDate`, `locationId`, `movementType` | Granular movement audit log with structured `direction` (`IN`/`OUT`) and net flow |
| `GET /api/reports/stock-movements/csv` | `startDate`, `endDate`, `locationId`, `movementType` | RFC 4180 compliant CSV stream with sanitized escape quotes |
| `GET /api/reports/facility-inventory` | `locationId` | Breakdown of stock quantities, capacity, and valuations by facility |
| `GET /api/reports/stock-transfers` | `startDate`, `endDate`, `productId`, `sourceLocationId`, `destinationLocationId`, `status` | Transfer history and metrics |
| `GET /api/reports/purchase-receiving` | `startDate`, `endDate`, `supplierId`, `locationId`, `status` | Supplier procurement analytics and intake logs |
| `GET /api/reports/order-fulfillment` | `startDate`, `endDate`, `locationId`, `status` | Order fulfillment throughput and revenue totals |
