# MicroChaos Complete Setup Guide - Java Swing Frontend

Complete step-by-step guide to run the entire MicroChaos project with Java Swing desktop frontend.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Architecture](#project-architecture)
3. [Complete Execution Steps](#complete-execution-steps)
4. [Features Overview](#features-overview)
5. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before starting, ensure you have:

- **Java 11+** installed
- **Maven 3.6+** installed
- **Git** installed
- **PostgreSQL** (optional - only if using database persistence)
- **4 Terminal windows** (one for each service + one for monitoring)

### Verify Your Setup

Check Java version:
```bash
java -version
```

Check Maven version:
```bash
mvn -version
```

---

## Project Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    MicroChaos Platform                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐    ┌──────────────┐    ┌────────────┐ │
│  │  Demo Services   │    │   Backend    │    │  Swing     │ │
│  │  (6 services)    │───→│   Control    │←───│  Frontend  │ │
│  │  Ports: 9000-5   │    │   (8080)     │    │  (Desktop) │ │
│  └──────────────────┘    └──────┬───────┘    └────────────┘ │
│                                  │                             │
│                           ┌──────▼───────┐                    │
│                           │  PostgreSQL  │                    │
│                           │  (Optional)  │                    │
│                           └──────────────┘                    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Complete Execution Steps

### Step 1: Navigate to Project Root

Open Git Bash or PowerShell and navigate to the project:

```bash
cd d:\MicroChaos
```

Verify you're on the correct branch:

```bash
git branch
```

You should see: `* frontend-javaswing`

If not, switch to it:
```bash
git checkout frontend-javaswing
```

---

### Step 2: Terminal 1 - Start Demo Services

The demo services simulate a microservice architecture with 6 services.

**Open Terminal 1 and run:**

```bash
cd d:\MicroChaos\demo-services
./scripts/run-demo-stack.sh
```

**Expected Output:**
```
Demo stack running:
api-gateway: http://localhost:9000
order-service: http://localhost:9001
payment-service: http://localhost:9002
inventory-service: http://localhost:9003
notification-service: http://localhost:9004
auth-service: http://localhost:9005
```

**⚠️ Important**: Keep this terminal open and running. Do NOT close it.

---

### Step 3: Terminal 2 - Start Backend API

The backend is the control plane that manages experiments and connects to the database.

**Open Terminal 2 and run:**

```bash
cd d:\MicroChaos\backend
./scripts/run-backend.sh
```

**Expected Output:**
```
MicroChaos backend running on http://localhost:8080
```

**⚠️ Important**: Keep this terminal open and running. Do NOT close it.

---

### Step 4: Terminal 3 - Build Java Swing Frontend

The Java Swing frontend is a desktop application that communicates with the backend.

**Open Terminal 3 and run:**

```bash
cd d:\MicroChaos\frontend-swing
mvn clean package
```

**What happens:**
- Maven downloads all dependencies (libraries)
- Compiles Java source code
- Creates an executable JAR file
- Takes 2-3 minutes on first run

**Expected Output (at the end):**
```
BUILD SUCCESS
```

---

### Step 5: Terminal 3 - Run Java Swing Frontend

After the build succeeds, run the application:

```bash
java -jar target/frontend-swing.jar
```

**Expected Result:**
A desktop window opens showing the MicroChaos dashboard with 5 tabs:
- Dashboard (metrics and topology graph)
- Services (list of all microservices)
- Experiments (chaos experiments)
- Monitoring (live service health)
- Runs (experiment execution history)

---

## Features Overview

### Dashboard Tab
- **System Overview Cards**:
  - Total Services registered
  - Total Experiments created
  - Active Runs in progress
  - Average Resilience Score
  - Service Health Breakdown (Healthy, Degraded, Down)
  
- **Topology Graph**: Visual representation of service nodes and connections
- **Auto-refresh**: Updates every 5 seconds

### Services Tab
- **Service Registry Table** showing:
  - Service ID
  - Service Name
  - Base URL
  - Environment
  - Current Status
  
- **Add Service Form**:
  - Service Name input
  - Base URL input
  - Environment input
  - "Add Service" button

**Example:**
```
Service Name: order-processor
Base URL: http://localhost:9010
Environment: dev
```

### Experiments Tab
- **Experiments Table** showing:
  - Experiment ID
  - Experiment Name
  - Target Service
  - Fault Type (LATENCY, TIMEOUT, ERROR, CRASH)
  - Stress Type
  - Intensity (0-100)
  - Duration in seconds
  
- **Create Experiment Form**:
  - Experiment Name input
  - Target Service dropdown
  - Fault Type dropdown (LATENCY, TIMEOUT, ERROR, CRASH)
  - Intensity slider (0-100)
  - "Create" button

**Example:**
```
Name: "chaos-latency-test"
Target: "order-service"
Fault Type: "LATENCY"
Intensity: 75
Duration: 60 seconds
```

### Monitoring Tab
- **Real-time Service Monitoring Table** showing:
  - Service Name
  - Health Status (HEALTHY, DEGRADED, DOWN)
  - Response Time (ms)
  - Error Rate (%)
  - Throughput (requests/sec)
  - Availability (%)
  - Action Buttons (Down, Latency, Recover)
  
- **Auto-refresh**: Updates every 3 seconds

### Runs Tab
- **Experiment Runs History Table** showing:
  - Run ID
  - Associated Experiment ID
  - Run Status (RUNNING, COMPLETED, FAILED)
  - Resilience Score
  - MTTR - Mean Time To Recover (seconds)
  - Started At (timestamp)
  - Ended At (timestamp)

---

## API Communication Flow

The Java Swing frontend communicates with the backend through REST APIs:

```
Frontend (Swing GUI)
    ↓ (HTTP Requests)
Backend API Service (http://localhost:8080/api)
    ├── GET /dashboard/overview              → Dashboard metrics
    ├── GET /services                        → List services
    ├── POST /services                       → Create service
    ├── GET /experiments                     → List experiments
    ├── POST /experiments                    → Create experiment
    ├── POST /experiments/{id}/run           → Run experiment
    ├── GET /runs                            → List runs
    ├── GET /monitoring/services             → Live monitoring
    ├── GET /runs/{id}/metrics               → Run metrics
    └── GET /runs/{id}/scorecard             → Resilience scorecard
    ↓
PostgreSQL Database (Optional)
```

**Important**: The Swing frontend does NOT connect directly to the database. All data access goes through the backend API, which ensures:
- ✅ Centralized business logic
- ✅ Consistent data management
- ✅ Easy API layer maintenance
- ✅ Clean separation of concerns

---

## Complete End-to-End Workflow

### Scenario: Run a Chaos Experiment

1. **Demo services** running (Terminal 1) → All 6 microservices online
2. **Backend** running (Terminal 2) → Control plane ready
3. **Swing Frontend** open (Terminal 3) → Desktop application running

#### Steps:

**Step A: View Services**
- Click "Services" tab
- See all 6 microservices listed (api-gateway, order-service, payment-service, etc.)
- Check their health status

**Step B: Create an Experiment**
- Click "Experiments" tab
- Fill in form:
  - Name: "Latency Test on Order Service"
  - Target Service: "order-service"
  - Fault Type: "LATENCY"
  - Intensity: 50
- Click "Create" button

**Step C: Check Live Monitoring**
- Click "Monitoring" tab
- See all services and their current health metrics
- Check response times, error rates, availability

**Step D: View Run History**
- Click "Runs" tab
- See the experiment run you just created
- Monitor its status and resilience score

**Step E: View Dashboard**
- Click "Dashboard" tab
- See updated metrics
- View the topology graph showing service relationships

---

## Troubleshooting

### Error: "Connection refused" in Swing Frontend

**Cause**: Backend is not running

**Solution**:
1. Check Terminal 2 (backend terminal) is still running
2. Verify backend is accessible:
   ```bash
   curl http://localhost:8080/api/services
   ```
3. If not running, restart backend in Terminal 2:
   ```bash
   cd d:\MicroChaos\backend
   ./scripts/run-backend.sh
   ```

### Error: "maven: command not found" in Terminal 3

**Cause**: Maven not installed or not in PATH

**Solution**:
1. Install Maven from: https://maven.apache.org/install.html
2. Or use `-Dapi.base` parameter:
   ```bash
   java -jar target/frontend-swing.jar
   ```

### No data showing in tables (Services, Experiments, etc.)

**Cause**: Backend hasn't loaded data yet or API connection issue

**Solution**:
1. Wait 5 seconds and click "Refresh" button on tabs
2. Check backend logs in Terminal 2 for errors
3. Verify backend is running:
   ```bash
   curl http://localhost:8080/api/services
   ```

### Demo services not starting (Terminal 1)

**Cause**: Port 9000-9005 already in use

**Solution**:
1. Use custom ports:
   ```bash
   cd d:\MicroChaos\demo-services
   DEMO_BASE_PORT=9100 ./scripts/run-demo-stack.sh
   ```

2. If backend on custom port too, update Terminal 2:
   ```bash
   cd d:\MicroChaos\backend
   DEMO_BASE_PORT=9100 ./scripts/run-backend.sh
   ```

### "BUILD FAILURE" in Maven build

**Cause**: Java compilation error

**Solution**:
1. Check error message carefully
2. Ensure Java 11+ is installed:
   ```bash
   java -version
   ```
3. Clean and rebuild:
   ```bash
   cd d:\MicroChaos\frontend-swing
   mvn clean install
   ```

### Swing window not opening

**Cause**: Java AWT display issue or missing dependencies

**Solution**:
1. Run with explicit display output:
   ```bash
   java -Djava.awt.headless=false -jar target/frontend-swing.jar
   ```

2. Check Java can access display:
   ```bash
   java -version
   ```

---

## Git Operations

### Commit Your Changes

After everything is working, commit to your branch:

```bash
git status
git add .
git commit -m "Add Java Swing frontend for MicroChaos

- Complete Swing desktop application
- 5 tabs: Dashboard, Services, Experiments, Monitoring, Runs
- Real-time data refresh
- Form-based service and experiment creation
- Topology graph visualization
- Connects to backend API on port 8080"
```

### Push to Remote

Push your branch to GitHub:

```bash
git push origin frontend-javaswing
```

### Create Pull Request

On GitHub, create a Pull Request from `frontend-javaswing` to `master` for your teammate to review.

---

## Project Structure

```
d:\MicroChaos\
├── demo-services/               → 6 microservices (ports 9000-9005)
├── backend/                     → Control plane (port 8080)
├── frontend/                    → Original web frontend (not used)
├── frontend-swing/              → NEW Java Swing frontend
│   ├── pom.xml                  → Maven configuration
│   ├── target/
│   │   └── frontend-swing.jar   → Executable JAR
│   └── src/main/java/com/microchaos/swing/
│       ├── MicroChaosSwingApp.java
│       ├── api/
│       │   └── ApiClient.java
│       ├── model/
│       │   ├── Service.java
│       │   ├── Experiment.java
│       │   ├── ExperimentRun.java
│       │   ├── MonitoringData.java
│       │   └── DashboardOverview.java
│       └── ui/
│           ├── DashboardPanel.java
│           ├── ServicesPanel.java
│           ├── ExperimentsPanel.java
│           ├── MonitoringPanel.java
│           └── RunsPanel.java
└── README.md                    → Main project README
```

---

## Summary

**You now have:**
- ✅ Demo services running (mimics production microservices)
- ✅ Backend control plane (manages experiments and database)
- ✅ Java Swing desktop frontend (user interface)
- ✅ All components communicating via REST API
- ✅ Real-time monitoring and visualization
- ✅ Form-based experiment creation
- ✅ Complete chaos engineering platform

**All in 5 terminal commands!**

---

## Next Steps

- Explore the dashboard and create chaos experiments
- Run experiments and observe service behavior
- Check resilience scores and metrics
- Commit changes to your branch and request code review
- Deploy to production (future enhancement)
