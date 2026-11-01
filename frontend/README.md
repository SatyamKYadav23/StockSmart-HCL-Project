# StockSmart — Frontend Application

This directory contains the Angular 22 frontend for **StockSmart**, an internal Retail Inventory Management System.

## Architecture & Technology
- **Framework**: Angular 22 (Standalone Components)
- **Reactivity**: Angular Signals (`signal`, `computed`, `effect`)
- **State & Communication**: Injectable reactive services communicating via HttpClient with the Spring Boot backend (`http://localhost:8080/api`)
- **Styling**: Enterprise SaaS design system (Dark navy navigation `#0f172a`, Slate borders `#cbd5e1`, responsive tables)

---

## Development Server

To run the application locally:
```bash
npm install
npx ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any source files.

---

## Production Build

To build the optimized production artifacts:
```bash
npx ng build
```
Build artifacts will be stored in the `dist/` directory.

---

## Quality & Audits

- **Double-Submit Protection**: All forms feature dual-layer submit protection (TypeScript signal check + disabled button state).
- **Global Error Handling**: Centralized `errorInterceptor` processes all HTTP errors with user-friendly notices.

---

## Documentation Links
For backend and system-wide documentation:
- [Master Project README](../README.md)
- [System Architecture](../stocksmart/docs/ArchitectureDocument.md)
- [Database Design & ER Diagram](../stocksmart/docs/DatabaseDesign.md)
- [REST API Reference](../stocksmart/docs/API_DOCUMENTATION.md)
- [Testing Guide](../stocksmart/docs/TESTING.md)
- [Production Deployment](../stocksmart/docs/DEPLOYMENT.md)
