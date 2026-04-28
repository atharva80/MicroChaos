# Setup Guide

MicroChaos uses both Windows and WSL, so setup is split into two scripts:

- Windows script: installs Windows-side tools such as Java, Maven, Docker Desktop
- WSL script: installs Linux-side tools such as Java, Maven, and `psql`

## 1. Windows Setup

Run this from Windows PowerShell:

```powershell
cd D:\MicroChaos
powershell -ExecutionPolicy Bypass -File .\scripts\setup-windows.ps1
```

If you do not want Docker Desktop installed by the script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-windows.ps1 -SkipDocker
```

## 2. WSL Setup

Run this from WSL:

```bash
cd /mnt/d/MicroChaos
chmod +x scripts/setup-wsl.sh
./scripts/setup-wsl.sh
```

## 3. After Setup

Start PostgreSQL:

```bash
cd /mnt/d/MicroChaos
docker-compose up -d postgres
```

Load schema and seed:

```bash
docker-compose exec postgres psql -U microchaos -d microchaos -f /docker-entrypoint-initdb.d/10-schema.sql
docker-compose exec postgres psql -U microchaos -d microchaos -f /docker-entrypoint-initdb.d/20-seed.sql
```

Start backend:

```bash
cd /mnt/d/MicroChaos/backend
mvn compile
mvn exec:java -Dexec.mainClass="com.microchaos.backend.MicroChaosBackendApplication"
```

Start Swing frontend from Windows PowerShell:

```powershell
cd D:\MicroChaos\frontend-swing
mvn --% clean compile exec:java -Dexec.mainClass=com.microchaos.swing.MicroChaosSwingApp -Dapi.base=http://localhost:8080/api
```
