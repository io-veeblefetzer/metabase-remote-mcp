# Task 1.1: Docker Compose Configuration

**Task ID:** 1.1  
**Phase:** 1 - Infrastructure Setup  
**GitHub Issue:** [#1](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/1)  
**Status:** ⬜ Not Started

---

## Objective

Create a Docker Compose configuration that orchestrates a Metabase instance and a PostgreSQL database for local development and testing.

---

## Prerequisites

- Docker and Docker Compose installed on the development machine
- Basic understanding of Docker networking and volumes

---

## Technical Details

### File to Create

**File:** `docker-compose.yml` (project root)

### Services Configuration

#### 1. PostgreSQL Database Service

```yaml
postgres:
  image: postgres:15-alpine
  container_name: metabase-postgres
  environment:
    POSTGRES_USER: metabase
    POSTGRES_PASSWORD: metabase
    POSTGRES_DB: metabase_app
  ports:
    - "5432:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
    - ./docker/init-db:/docker-entrypoint-initdb.d
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U metabase"]
    interval: 10s
    timeout: 5s
    retries: 5
```

#### 2. Sample Data Database Service

```yaml
sample-db:
  image: postgres:15-alpine
  container_name: metabase-sample-db
  environment:
    POSTGRES_USER: sample
    POSTGRES_PASSWORD: sample
    POSTGRES_DB: sample_data
  ports:
    - "5433:5432"
  volumes:
    - sample_data:/var/lib/postgresql/data
    - ./docker/init-sample-db:/docker-entrypoint-initdb.d
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U sample"]
    interval: 10s
    timeout: 5s
    retries: 5
```

#### 3. Metabase Service

```yaml
metabase:
  image: metabase/metabase:latest
  container_name: metabase
  environment:
    MB_DB_TYPE: postgres
    MB_DB_DBNAME: metabase_app
    MB_DB_PORT: 5432
    MB_DB_USER: metabase
    MB_DB_PASS: metabase
    MB_DB_HOST: postgres
  ports:
    - "3000:3000"
  depends_on:
    postgres:
      condition: service_healthy
    sample-db:
      condition: service_healthy
  healthcheck:
    test: curl --fail -I http://localhost:3000/api/health || exit 1
    interval: 15s
    timeout: 5s
    retries: 10
    start_period: 120s
```

### Volume Definitions

```yaml
volumes:
  postgres_data:
  sample_data:
```

### Network Configuration

Use default Docker Compose network for inter-service communication.

---

## Implementation Checklist

- [ ] Create `docker-compose.yml` in project root
- [ ] Configure PostgreSQL service for Metabase application database
- [ ] Configure PostgreSQL service for sample business data
- [ ] Configure Metabase service with proper environment variables
- [ ] Set up service dependencies with health checks
- [ ] Define named volumes for data persistence
- [ ] Create `docker/init-db/` directory (empty for now)
- [ ] Create `docker/init-sample-db/` directory (empty for now)
- [ ] Test: Run `docker-compose config` to validate syntax
- [ ] Test: Run `docker-compose up -d` to start services
- [ ] Test: Verify PostgreSQL is accessible on port 5432
- [ ] Test: Verify sample-db is accessible on port 5433
- [ ] Test: Verify Metabase UI loads at http://localhost:3000

---

## Acceptance Criteria

- [ ] `docker-compose.yml` exists in project root
- [ ] All three services are properly configured
- [ ] Health checks are defined for all services
- [ ] Services start successfully with `docker-compose up -d`
- [ ] Metabase web interface is accessible at http://localhost:3000
- [ ] PostgreSQL databases are accessible on their respective ports
- [ ] Data persists across container restarts (volumes working)

---

## Verification Commands

```bash
# Validate docker-compose syntax
docker-compose config

# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f metabase

# Test database connectivity
docker-compose exec postgres pg_isready -U metabase
docker-compose exec sample-db pg_isready -U sample

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

---

## Commit Template

```
feat(infrastructure): add Docker Compose configuration

- Add docker-compose.yml with Metabase and PostgreSQL services
- Configure application database for Metabase metadata
- Configure sample database for business data
- Set up health checks and service dependencies
- Create init-db directories for SQL scripts

Part of #1
```

---

## Notes

- Metabase takes 1-2 minutes to fully initialize on first startup
- Port 5433 is used for sample-db to avoid conflict with local PostgreSQL
- The `start_period` in Metabase health check accounts for initialization time
- Sample data will be populated in Task 1.2
