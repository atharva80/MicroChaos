# MicroChaos Swing Frontend

Java Swing desktop application frontend for MicroChaos.

## Prerequisites

- Java 21
- Maven 3.6+
- backend running on `http://localhost:8080/api`

## Build

```bash
mvn clean package
```

This creates an executable JAR in `target/`.

## Run

### Command (PowerShell)

```powershell
cd D:\MicroChaos\frontend-swing
mvn clean compile exec:java -Dexec.mainClass=com.microchaos.swing.MicroChaosSwingApp -Dapi.base=http://localhost:8080/api
```

### Option 2: JAR

```bash
java -jar target/frontend-swing-1.0.0.jar
```

### Option 3: Custom API Base

```bash
mvn clean compile exec:java \
  -Dexec.mainClass="com.microchaos.swing.MicroChaosSwingApp" \
  -Dapi.base="http://your-backend-host:8080/api"
```

Or:

```bash
java -Dapi.base="http://your-backend-host:8080/api" -jar target/frontend-swing-1.0.0.jar
```

## PowerShell Recommendation

If you run from Windows PowerShell, use:

```powershell
mvn --% clean compile exec:java -Dexec.mainClass=com.microchaos.swing.MicroChaosSwingApp -Dapi.base=http://localhost:8080/api
```

This avoids PowerShell misparsing `-Dapi.base=...`.

## WSL GUI Note

If you start this app inside WSL and get:

```text
No X11 DISPLAY variable was set
```

run the Swing frontend from Windows PowerShell instead of WSL.

## Features

- dashboard overview
- services list
- experiments list
- monitoring view
- experiment run history and metrics

## API Connection

Default API base:

- `http://localhost:8080/api`

To change the API base:

```text
-Dapi.base=http://your-custom-url/api
```

## Database

The Swing frontend does not connect to PostgreSQL directly. It talks to the backend API, and the backend writes and reads the database.

## Architecture

```text
MicroChaos Frontend (Swing)
-> Backend API (http://localhost:8080/api)
-> PostgreSQL Database
```

For full stack startup steps, see the repo root [README.md](/d:/MicroChaos/README.md).
