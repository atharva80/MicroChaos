# MicroChaos Swing Frontend

Java Swing desktop application frontend for MicroChaos chaos engineering platform.

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Backend running on `http://localhost:8080/api` (or configure with `-Dapi.base` parameter)

## Build

```bash
mvn clean package
```

This will create an executable JAR in the `target/` directory.

## Run

### Option 1: Via Maven
```bash
mvn clean compile exec:java -Dexec.mainClass="com.microchaos.swing.MicroChaosSwingApp"
```

### Option 2: Via JAR (after building)
```bash
java -jar target/frontend-swing-1.0.0.jar
```

### Option 3: With Custom API Base
If your backend is running on a different port or host:

```bash
mvn clean compile exec:java \
  -Dexec.mainClass="com.microchaos.swing.MicroChaosSwingApp" \
  -Dapi.base="http://your-backend-host:8080/api"
```

Or with JAR:
```bash
java -Dapi.base="http://your-backend-host:8080/api" -jar target/frontend-swing-1.0.0.jar
```

## Features

- **Dashboard**: Overview statistics (services, experiments, active runs, resilience score)
- **Services**: View all registered microservices and their status
- **Experiments**: List and manage chaos experiments
- **Monitoring**: Real-time monitoring of service health and metrics
- **Runs**: View experiment execution history and metrics

## API Connection

The application connects to the MicroChaos backend API. Make sure the backend is running before starting the Swing frontend.

### Default Configuration
- API Base: `http://localhost:8080/api`
- Auto-refresh: Every 3-5 seconds depending on the panel

### Configuration
To change the API base URL, use the system property:
```
-Dapi.base=http://your-custom-url/api
```

## Database

The frontend does not directly access the database. All data is retrieved through the backend API, which manages the PostgreSQL database as per the schema in `backend/db/schema.sql`.

## Architecture

```
MicroChaos Frontend (Swing)
    ↓
    Backend API (http://localhost:8080/api)
    ↓
    PostgreSQL Database
```

The frontend is stateless and communicates only through REST API endpoints.
