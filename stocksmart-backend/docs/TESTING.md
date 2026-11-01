# StockSmart — Quality Assurance & Testing Guide

## 1. Test Strategy Overview
StockSmart utilizes a multi-layered testing strategy combining:
1. **Unit & Slice Tests**: Validating isolated logic, DTO mapping, and domain invariant methods (e.g. `StockMovement.getEffectiveDirection()`).
2. **Spring Boot MockMvc Integration Tests**: Testing the HTTP controller layer, Bean Validation enforcement, security headers, CORS preflights, and unified exception handling.
3. **Database Transaction & Concurrency Tests**: Verifying `@Transactional` atomic rollbacks and row-level pessimistic locking (`SELECT ... FOR UPDATE`) under concurrent execution.
4. **End-to-End Build & Compilation Verification**: Validating frontend production bundling with Angular CLI (`npx ng build`) and zero NPM vulnerability compliance.

---

## 2. Automated Test Execution

### 2.1 Backend Test Suite (Java 21 / Maven)
To execute all backend tests:
```powershell
# Set Java 21 environment if not default
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.12.1"
$env:Path="C:\Program Files\Java\jdk-21.0.12.1\bin;$env:Path"

# Run all automated tests
.\mvnw.cmd test
```

To run a specific test class:
```powershell
.\mvnw.cmd test "-Dtest=SecurityAndValidationTest"
```

### 2.2 Test Suite Composition (55 Tests Total)
| Test Class | Category | Test Count | What It Verifies |
| :--- | :--- | :--- | :--- |
| [`SecurityAndValidationTest`](file:///D:/stocksmart/src/test/java/com/hcl/stocksmart/SecurityAndValidationTest.java) | Security & Validation | 12 | Security headers (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`), CORS preflight, Bean validation failures (400), malformed JSON (400), type mismatch (400), not found (404), location/product deletion with stock (409), `direction` assignment and legacy fallbacks. |
| [`StockTransferTransactionTest`](file:///D:/stocksmart/src/test/java/com/hcl/stocksmart/StockTransferTransactionTest.java) | Concurrency & Integrity | 16 | Atomic source deduction + destination addition, dual `StockMovement` creation (`OUT`/`IN`), pessimistic locking order, deadlock avoidance, rollback on failure, immutability of completed transfers. |
| [`PurchaseReceivingTransactionTest`](file:///D:/stocksmart/src/test/java/com/hcl/stocksmart/PurchaseReceivingTransactionTest.java) | Inventory & Vendor Intake | 8 | Stock increment on receiving, audit movement (`RECEIPT`, `IN`), transactional rollback, immutability of received shipments, rejection of delete/cancel on received records. |
| [`StockAdjustmentTransactionTest`](file:///D:/stocksmart/src/test/java/com/hcl/stocksmart/StockAdjustmentTransactionTest.java) | Inventory Adjustments | 5 | Positive adjustments, negative adjustments, insufficient stock rejection, reserved quantity violation rejection, transactional rollback. |
| [`OrderTransactionTest`](file:///D:/stocksmart/src/test/java/com/hcl/stocksmart/OrderTransactionTest.java) | Sales & Fulfillment | 5 | Inventory deduction on fulfillment, insufficient stock rejection, rollback on failure, immutability of completed orders (cannot delete, cannot alter status/location). |
| [`ReportServiceTest`](file:///D:/stocksmart/src/test/java/com/hcl/stocksmart/ReportServiceTest.java) | Analytics & Reports | 8 | All 7 report categories (inventory overview, stock status, movement report, facility breakdown, transfers, receiving, order fulfillment), CSV export formatting, and zero mutation verification. |
| [`StocksmartApplicationTests`](file:///D:/stocksmart/src/test/java/com/hcl/stocksmart/StocksmartApplicationTests.java) | Smoke Test | 1 | Spring Application Context boots successfully. |

---

## 3. Frontend Verification & Build

### 3.1 Angular Production Build
To verify frontend TypeScript types, standalone component templates, and production bundle generation:
```powershell
cd d:\ss
npx ng build
```
- **Expected Result**: Application bundle generated successfully with 0 errors.

### 3.2 Security Audit
```powershell
cd d:\ss
npm audit
```
- **Expected Result**: 0 vulnerabilities.

---

## 4. Manual Regression Checklist

### 4.1 Products
- [ ] Create a new product with valid SKU, name, price, category, and reorder level.
- [ ] Attempt creating a product with duplicate SKU: verify `409 Conflict` error dialog.
- [ ] Attempt creating a product with negative price: verify client-side validation marks input red and prevents submission.
- [ ] Edit product details: verify changes persist across page refresh.
- [ ] Attempt deleting a product with existing inventory: verify system prevents deletion with clear conflict message.

### 4.2 Locations
- [ ] Register a new facility location (e.g. `Kolkata Regional Hub`).
- [ ] Verify facility appears in all dropdowns (Adjustments, Orders, Transfers, Receiving).
- [ ] Attempt deleting a location with existing stock: verify rejection with clear explanation.

### 4.3 Inventory & Adjustments
- [ ] Open Inventory table: verify available units calculation (`Quantity - Reserved`).
- [ ] Perform a positive adjustment (+10 units): verify stock increases and movement record appears in table with `IN` badge.
- [ ] Attempt decreasing stock by more than available balance: verify form shows red error and cannot submit.

### 4.4 Orders
- [ ] Record a new order with multiple line items.
- [ ] Verify total amount calculates automatically.
- [ ] Confirm order with status `COMPLETED`: verify inventory decreases immediately.
- [ ] Verify order details modal: confirm status and facility cannot be modified on completed orders.

### 4.5 Purchase Receiving
- [ ] Record an intake shipment with status `RECEIVED`.
- [ ] Verify destination stock increases by received line item quantities.
- [ ] Verify movement table shows `RECEIPT` with direction `IN`.
- [ ] Confirm received shipment cannot be deleted or cancelled.

### 4.6 Stock Transfers
- [ ] Initiate transfer of 5 units from Facility A to Facility B.
- [ ] Verify Facility A decreases by 5 and Facility B increases by 5.
- [ ] Verify movements ledger displays two records: Transfer Out (`OUT`) and Transfer In (`IN`).
- [ ] Attempt transfer exceeding source available stock: verify operation is blocked.

### 4.7 Reports & Dashboard
- [ ] Open Dashboard: verify metrics match real backend database counts.
- [ ] Open Reports module: verify all 7 report tabs load correctly.
- [ ] Test Date Range and Facility filters.
- [ ] Click "Export CSV": verify downloaded CSV file contains escaped fields, proper column headers, and correct net deltas.

---

## 5. Known Limitations & Operating Boundaries
1. **Pessimistic Locking Scope**: Row-level locking requires an active MySQL InnoDB connection. In-memory H2 configurations should not be used in production as locking semantics differ.
2. **Single Currency**: Monetary calculations currently assume INR (₹). Multi-currency conversions are not within project scope.
3. **Manual Order Intake**: StockSmart is an internal retail ERP and does not feature e-commerce shopping carts or self-service customer authentication.
