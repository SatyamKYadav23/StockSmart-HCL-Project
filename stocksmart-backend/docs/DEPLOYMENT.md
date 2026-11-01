# StockSmart — Production Deployment Guide

## 1. Prerequisites
To deploy and run the StockSmart system, the following runtime dependencies must be present on the host environment:

- **Java Runtime**: OpenJDK 21 LTS (or Oracle JDK 21)
- **Node.js**: v20.x or v22.x LTS (with NPM v10+)
- **Database**: MySQL Server 8.0+ (InnoDB engine, UTF-8/utf8mb4 encoding)
- **OS**: Linux (Ubuntu 22.04 LTS / RHEL 9), Windows Server 2022, or macOS

---

## 2. Database Provisioning

### Step 2.1: Initialize MySQL Database
Log into MySQL with administrative credentials:
```bash
mysql -u root -p
```

Create the database and a dedicated application user:
```sql
CREATE DATABASE IF NOT EXISTS `stocksmart`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'stocksmart_app'@'%' IDENTIFIED BY 'StrongProductionPassword!2026';
GRANT ALL PRIVILEGES ON `stocksmart`.* TO 'stocksmart_app'@'%';
FLUSH PRIVILEGES;
```

### Step 2.2: Apply Schema DDL
Execute the master schema script located in `database/schema.sql`:
```bash
mysql -u stocksmart_app -p stocksmart < database/schema.sql
```

*(Optional for demonstration/staging environments)* Apply sample seed data:
```bash
mysql -u stocksmart_app -p stocksmart < database/seed-data.sql
```

---

## 3. Backend Deployment (Spring Boot 4.1.1)

### Step 3.1: Environment Configuration
Create an environment file or export environment variables on the deployment host:
```bash
export SERVER_PORT=8080
export DB_URL="jdbc:mysql://localhost:3306/stocksmart?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DB_USERNAME="stocksmart_app"
export DB_PASSWORD="StrongProductionPassword!2026"
export DDL_AUTO="validate"
export SHOW_SQL="false"
export FORMAT_SQL="false"
export CORS_ALLOWED_ORIGINS="http://localhost:4200,https://inventory.yourcompany.internal"
```

### Step 3.2: Package the Application
Build the executable production JAR using Maven:
```powershell
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```
The resulting executable JAR will be located at:
`target/stocksmart-0.0.1-SNAPSHOT.jar`

### Step 3.3: Run the Application
Run the packaged JAR as a background service (or via systemd on Linux):
```bash
java -jar -Dspring.profiles.active=production target/stocksmart-0.0.1-SNAPSHOT.jar
```

Verify backend health by querying the API:
```bash
curl -I http://localhost:8080/api/products
```
Should return `HTTP/1.1 200 OK` along with security headers `X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff`.

---

## 4. Frontend Deployment (Angular 22)

### Step 4.1: Production Build
Navigate to the frontend workspace (`d:\ss`) and run the Angular production build:
```powershell
cd d:\ss
npm install
npx ng build
```
This compiles the application with tree-shaking, minification, and Ahead-of-Time (AOT) compilation into:
`d:\ss\dist\stocksmart`

### Step 4.2: Web Server Serving (Nginx Example)
In production, serve the static files using Nginx or an equivalent web server:

```nginx
server {
    listen 80;
    server_name inventory.yourcompany.internal;

    root /var/www/stocksmart/dist/stocksmart/browser;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 5. Optional Docker Deployment

For containerized environments, an optional multi-stage `Dockerfile` and `docker-compose.yml` can be utilized.

### `Dockerfile` (Backend)
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/stocksmart-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 6. Troubleshooting & Operational Runbook

| Symptom | Probable Cause | Remediation |
| :--- | :--- | :--- |
| `SchemaManagementException: Schema-validation: missing column [...]` | Database schema does not match JPA entities (`ddl-auto=validate`) | Run `database/schema.sql` or `database/migration_add_direction.sql` against the database. |
| `CommunicationsLinkException: Communications link failure` | MySQL service stopped or port 3306 unreachable | Verify MySQL service status and check firewall / bind address in `my.cnf`. |
| `CORS Error in Browser DevTools` | Frontend origin is not listed in `cors.allowed-origins` | Update `CORS_ALLOWED_ORIGINS` environment variable to include the frontend origin. |
| `DataIntegrityViolationException (409 Conflict)` | Attempting to delete a record referenced by inventory, orders, or transfers | StockSmart preserves audit integrity; deactivated records should remain in place rather than hard deleted. |
