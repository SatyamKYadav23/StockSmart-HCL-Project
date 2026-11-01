# StockSmart — System Architecture Document

## 1. System Overview

**StockSmart** is an internal retail inventory management system designed as a high-integrity, monolithic client-server application. It delivers deterministic inventory ledgering, inter-facility stock transfers, purchase receiving, customer order fulfillment, and operational analytics.

```mermaid
flowchart TD
    Client["Angular 22 Single Page App (Browser)"]
    API["Spring Boot 4.1.1 REST Controllers"]
    Security["Spring Security (CORS + Security Headers)"]
    ServiceLayer["Transactional Service Layer (@Transactional)"]
    RepoLayer["Spring Data JPA Repositories"]
    DB[("MySQL 8.0 Database (InnoDB Engine)")]

    Client -->|HTTPS / JSON REST API| Security
    Security --> API
    API --> ServiceLayer
    ServiceLayer -->|Pessimistic Locking & ACID Mutations| RepoLayer
    RepoLayer -->|Hibernate ORM / JDBC| DB
```

---

## 2. Architectural Layers & Responsibilities

### 2.1 Frontend Architecture (Angular 22)
- **Framework**: Angular 22 with Standalone Components and reactive Signals (`signal`, `computed`, `effect`).
- **State Management**: Local component signals coupled with injectable feature services. Signals drive reactivity without unnecessary zone churn or full component rebuilds.
- **Routing**: Angular Router with lazy-loaded route chunking for high performance.
- **UI & Layout**: Tailored enterprise design system utilizing a dark navy sidebar (`#0f172a`), slate borders (`#cbd5e1`), and clean light workspace surfaces.

### 2.2 Backend Architecture (Spring Boot 4.1.1 / Java 21)
- **Controller Layer (`com.hcl.stocksmart.controller`)**: Exposes RESTful HTTP endpoints, enforces input validation via `@Valid` annotations, and returns structured `ResponseEntity` payloads.
- **Service Layer (`com.hcl.stocksmart.services`)**: Encapsulates all business rules, concurrency locks, and transactional boundaries (`@Transactional`).
- **Repository Layer (`com.hcl.stocksmart.repository`)**: Spring Data JPA repositories with explicit queries and pessimistic write locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
- **Entity Domain Model (`com.hcl.stocksmart.entity`)**: JPA entity models mapping 1-to-1 with MySQL InnoDB tables with strict foreign key constraints.
- **Exception & Security Layer (`com.hcl.stocksmart.exception`, `config`)**: Unified `GlobalExceptionHandler` shielding internal database stack traces, configured CORS policies, and security headers (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`).

---

## 3. Core Business Transaction Flows

### 3.1 Inventory Mutation & Locking Strategy
All inventory changes must acquire a row-level pessimistic write lock on the corresponding `inventory` row (`SELECT ... FOR UPDATE`). When multiple inventory rows are involved (e.g., transfers), locks are acquired in a consistent ordering (by `product_id` and `location_id`) to eliminate deadlock hazards.

```mermaid
sequenceDiagram
    autonumber
    participant UI as Angular UI
    participant Ctrl as InventoryController
    participant Svc as InventoryService
    participant Repo as InventoryRepository
    participant DB as MySQL (InnoDB)

    UI->>Ctrl: POST /api/inventory/adjust (StockAdjustmentRequest)
    Ctrl->>Svc: adjustStock(request)
    Svc->>Repo: findByProductIdAndLocationIdWithLock(pId, locId)
    Repo->>DB: SELECT * FROM inventory WHERE product_id=? AND location_id=? FOR UPDATE
    DB-->>Svc: Locked Inventory Record
    Svc->>Svc: Validate: (current + delta >= 0) and (current + delta >= reserved)
    Svc->>Repo: save(updatedInventory)
    Svc->>Repo: save(StockMovement: direction=IN/OUT, type=ADJUSTMENT)
    Repo-->>DB: COMMIT Transaction
    Svc-->>Ctrl: InventoryResponse
    Ctrl-->>UI: 200 OK (Updated Balance)
```

---

### 3.2 Customer Order Fulfillment Flow
Orders can be recorded in `PENDING` or `COMPLETED` state. When completed, stock is atomically deducted from the assigned facility and a `SALE` movement with `direction = 'OUT'` is registered. Completed orders are immutable.

```mermaid
sequenceDiagram
    autonumber
    participant UI as Angular UI
    participant Ctrl as OrderController
    participant Svc as OrderService
    participant DB as MySQL Database

    UI->>Ctrl: POST /api/orders (OrderRequest)
    Ctrl->>Svc: createOrder(request)
    Svc->>DB: Save Order & OrderItems
    alt Order Status == COMPLETED
        loop For each OrderItem
            Svc->>DB: Lock inventory row (Pessimistic Write)
            Svc->>Svc: Verify quantity >= order item quantity
            Svc->>DB: Deduct inventory quantity
            Svc->>DB: Insert StockMovement (type=SALE, direction=OUT)
        end
    end
    DB-->>Svc: Commit Transaction
    Svc-->>Ctrl: OrderResponse
    Ctrl-->>UI: 201 Created
```

---

### 3.3 Purchase Receiving Intake Flow
Receiving records log inbound stock shipments from approved suppliers. Upon confirmation (`status = RECEIVED`), stock is increased at the destination facility and a `RECEIPT` movement with `direction = 'IN'` is committed.

```mermaid
sequenceDiagram
    autonumber
    participant UI as Angular UI
    participant Ctrl as PurchaseReceivingController
    participant Svc as PurchaseReceivingService
    participant DB as MySQL Database

    UI->>Ctrl: POST /api/purchase-receivings (PurchaseReceivingRequest)
    Ctrl->>Svc: createReceiving(request)
    Svc->>DB: Save PurchaseReceiving & Items
    alt Status == RECEIVED
        loop For each item
            Svc->>DB: Lock or Initialize Inventory row
            Svc->>DB: Increase quantity by received amount
            Svc->>DB: Insert StockMovement (type=RECEIPT, direction=IN)
        end
    end
    DB-->>Svc: Commit Transaction
    Svc-->>Ctrl: PurchaseReceivingResponse
    Ctrl-->>UI: 201 Created
```

---

### 3.4 Inter-Facility Stock Transfer Flow
Transfers move stock atomically between two facilities. Source inventory is deducted, destination inventory is increased (or initialized), and two linked `StockMovement` records (`OUT` for source, `IN` for destination) are generated.

```mermaid
sequenceDiagram
    autonumber
    participant UI as Angular UI
    participant Ctrl as StockTransferController
    participant Svc as StockTransferService
    participant DB as MySQL Database

    UI->>Ctrl: POST /api/stock-transfers (StockTransferRequest)
    Ctrl->>Svc: createTransfer(request)
    Svc->>Svc: Order lock acquisition by location ID
    Svc->>DB: Lock source inventory row
    Svc->>DB: Lock/Initialize destination inventory row
    Svc->>Svc: Validate source available stock (quantity - reserved >= transferQty)
    Svc->>DB: Deduct transferQty from source
    Svc->>DB: Add transferQty to destination
    Svc->>DB: Insert source StockMovement (type=TRANSFER, direction=OUT)
    Svc->>DB: Insert destination StockMovement (type=TRANSFER, direction=IN)
    Svc->>DB: Save StockTransfer (status=COMPLETED)
    DB-->>Svc: Commit Transaction
    Svc-->>Ctrl: StockTransferResponse
    Ctrl-->>UI: 201 Created
```

---

### 3.5 Reporting & Analytics Pipeline
All seven report endpoints in [`ReportService.java`](file:///D:/stocksmart/src/main/java/com/hcl/stocksmart/services/ReportService.java) compute live, non-cached metrics directly from transaction and inventory tables without side effects or mutations.

```mermaid
flowchart LR
    ReportsUI["Reports Angular Component"]
    ReportCtrl["ReportController (/api/reports/...)"]
    ReportSvc["ReportService"]
    DB[("MySQL Database")]

    ReportsUI -->|Filter Params (Dates, Facility, Status)| ReportCtrl
    ReportCtrl --> ReportSvc
    ReportSvc -->|Read-only Queries| DB
    DB -->|Raw Entities & Movements| ReportSvc
    ReportSvc -->|Compute KPIs, Grouping, Net Flow| ReportCtrl
    ReportCtrl -->|JSON DTO| ReportsUI
```
