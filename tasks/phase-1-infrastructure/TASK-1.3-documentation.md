# Task 1.3: Infrastructure Documentation

**Task ID:** 1.3  
**Phase:** 1 - Infrastructure Setup  
**GitHub Issue:** [#1](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/1)  
**Status:** ⬜ Not Started

---

## Objective

Create comprehensive documentation for the Docker development environment, including setup instructions, configuration details, and troubleshooting guidance.

---

## Prerequisites

- Task 1.1 completed (Docker Compose configuration)
- Task 1.2 completed (Sample data population)
- All services tested and working

---

## Technical Details

### Files to Create

**Primary Documentation:** `docker/README.md`

### Documentation Structure

```markdown
# Docker Development Environment

## Overview
Brief description of the environment and its purpose.

## Quick Start
Step-by-step instructions to get up and running.

## Services
Description of each service in the stack.

## Configuration
Environment variables and customization options.

## Default Credentials
Login information for all services.

## Useful Commands
Common Docker Compose operations.

## Sample Data
Description of the test data schema.

## Troubleshooting
Solutions to common issues.

## Connecting from Application
How to configure the MCP server to connect.
```

---

## Content Requirements

### 1. Quick Start Section

```markdown
## Quick Start

1. Ensure Docker and Docker Compose are installed
2. Clone the repository
3. Start all services:
   ```bash
   docker-compose up -d
   ```
4. Wait for Metabase to initialize (1-2 minutes)
5. Access Metabase at http://localhost:3000
6. Complete the initial setup wizard
```

### 2. Services Section

| Service | Port | Description |
|---------|------|-------------|
| metabase | 3000 | Metabase web interface |
| postgres | 5432 | Metabase application database |
| sample-db | 5433 | Sample business data database |

### 3. Default Credentials Section

Document all default usernames and passwords:
- PostgreSQL (metabase app): `metabase`/`metabase`
- PostgreSQL (sample data): `sample`/`sample`
- Metabase admin: (created during setup)

### 4. Sample Data Section

Include:
- ER diagram (ASCII or link to diagram)
- Table descriptions
- Key relationships
- Example queries

### 5. Troubleshooting Section

Common issues to address:
- Port already in use
- Container fails to start
- Metabase can't connect to database
- Data not appearing after restart
- Permission issues on Linux

---

## Implementation Checklist

- [ ] Create `docker/README.md` file
- [ ] Write Overview section
- [ ] Write Quick Start section with step-by-step instructions
- [ ] Document all services with ports and purposes
- [ ] List all environment variables with descriptions
- [ ] Document default credentials
- [ ] Create useful commands reference
- [ ] Describe sample data schema with table details
- [ ] Add ER diagram (ASCII format)
- [ ] Write troubleshooting section with 5+ common issues
- [ ] Add section on connecting from the MCP application
- [ ] Review documentation for completeness
- [ ] Test Quick Start instructions from scratch

---

## Acceptance Criteria

- [ ] `docker/README.md` exists and is well-formatted
- [ ] Quick Start instructions are complete and accurate
- [ ] All services are documented with ports
- [ ] All credentials are documented
- [ ] Sample data schema is clearly explained
- [ ] At least 5 troubleshooting scenarios covered
- [ ] A developer can set up the environment using only the documentation
- [ ] No broken links or formatting issues

---

## Documentation Template

```markdown
# Docker Development Environment

This directory contains the Docker Compose configuration for running Metabase with a sample PostgreSQL database for local development and testing.

## Overview

The development environment consists of three services:
- **Metabase**: Business intelligence and analytics platform
- **PostgreSQL (App DB)**: Stores Metabase application data
- **PostgreSQL (Sample DB)**: Contains sample e-commerce data for testing

## Quick Start

### Prerequisites

- Docker Engine 20.10+
- Docker Compose V2+
- 4GB RAM available for containers

### Starting the Environment

```bash
# Start all services in background
docker-compose up -d

# View logs (wait for "Metabase Initialization COMPLETE")
docker-compose logs -f metabase

# Stop all services
docker-compose down

# Stop and remove all data
docker-compose down -v
```

### Initial Metabase Setup

1. Open http://localhost:3000
2. Click "Let's get started"
3. Create admin account
4. Add the sample database:
   - Database type: PostgreSQL
   - Host: sample-db
   - Port: 5432
   - Database name: sample_data
   - Username: sample
   - Password: sample

## Services

| Service | Container Name | Internal Port | External Port | Purpose |
|---------|---------------|---------------|---------------|---------|
| metabase | metabase | 3000 | 3000 | Web UI |
| postgres | metabase-postgres | 5432 | 5432 | App DB |
| sample-db | metabase-sample-db | 5432 | 5433 | Test Data |

## Configuration

### Environment Variables

| Variable | Service | Default | Description |
|----------|---------|---------|-------------|
| MB_DB_TYPE | metabase | postgres | Database type |
| MB_DB_HOST | metabase | postgres | Database host |
| MB_DB_PORT | metabase | 5432 | Database port |
| MB_DB_DBNAME | metabase | metabase_app | Database name |
| MB_DB_USER | metabase | metabase | Database user |
| MB_DB_PASS | metabase | metabase | Database password |
| POSTGRES_USER | postgres | metabase | PostgreSQL user |
| POSTGRES_PASSWORD | postgres | metabase | PostgreSQL password |
| POSTGRES_DB | postgres | metabase_app | PostgreSQL database |

## Default Credentials

### PostgreSQL (Application Database)
- **Host**: localhost
- **Port**: 5432
- **Database**: metabase_app
- **Username**: metabase
- **Password**: metabase

### PostgreSQL (Sample Database)
- **Host**: localhost
- **Port**: 5433
- **Database**: sample_data
- **Username**: sample
- **Password**: sample

### Metabase
- Created during initial setup wizard

## Sample Data Schema

### Entity Relationship Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  customers  │     │   orders    │     │ order_items │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id (PK)     │◄────│ customer_id │     │ id (PK)     │
│ email       │     │ id (PK)     │◄────│ order_id    │
│ first_name  │     │ order_date  │     │ product_id  │────►┐
│ last_name   │     │ status      │     │ quantity    │     │
│ city        │     │ total_amount│     │ unit_price  │     │
│ country     │     │ shipping_   │     │ subtotal    │     │
│ created_at  │     │   address   │     └─────────────┘     │
│ is_active   │     └─────────────┘                         │
└─────────────┘                                             │
                    ┌─────────────┐     ┌─────────────┐     │
                    │ categories  │     │  products   │◄────┘
                    ├─────────────┤     ├─────────────┤
                    │ id (PK)     │◄────│ category_id │
                    │ name        │     │ id (PK)     │
                    │ description │     │ name        │
                    └─────────────┘     │ price       │
                                        │ cost        │
                                        │ stock_qty   │
                                        │ created_at  │
                                        └─────────────┘
```

### Tables

| Table | Records | Description |
|-------|---------|-------------|
| customers | 150 | Customer profiles |
| categories | 5 | Product categories |
| products | 60 | Product catalog |
| orders | 250 | Order records |
| order_items | 600+ | Order line items |

## Useful Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f metabase

# Restart a service
docker-compose restart metabase

# Connect to sample database
docker-compose exec sample-db psql -U sample -d sample_data

# Connect to app database
docker-compose exec postgres psql -U metabase -d metabase_app

# Check service health
docker-compose ps

# Remove all data and start fresh
docker-compose down -v
docker-compose up -d
```

## Troubleshooting

### Port Already in Use

**Error**: `bind: address already in use`

**Solution**:
```bash
# Find process using the port
sudo lsof -i :3000
# Kill the process or change the port in docker-compose.yml
```

### Metabase Not Starting

**Symptom**: Metabase container keeps restarting

**Solution**:
1. Check logs: `docker-compose logs metabase`
2. Ensure PostgreSQL is healthy: `docker-compose ps`
3. Increase memory: Add `mem_limit: 2g` to metabase service

### Database Connection Failed

**Symptom**: "Unable to connect to database"

**Solution**:
1. Ensure database container is running
2. Check network: `docker-compose exec metabase ping postgres`
3. Verify credentials in environment variables

### Data Missing After Restart

**Symptom**: Sample data tables are empty

**Solution**:
```bash
# Init scripts only run on first start
# Remove volumes and restart
docker-compose down -v
docker-compose up -d
```

### Permission Denied (Linux)

**Symptom**: Volume mount permission errors

**Solution**:
```bash
# Fix ownership
sudo chown -R 1000:1000 ./docker
```

## Connecting from Application

When running the MCP server application, use these connection settings:

```properties
# application.properties
metabase.url=http://localhost:3000
metabase.api.key=YOUR_API_KEY
```

To generate an API key in Metabase:
1. Log in as admin
2. Go to Settings → Admin → API Keys
3. Create a new API key
4. Copy the key to your application.properties
```

---

## Commit Template

```
docs(infrastructure): add Docker environment documentation

- Create docker/README.md with comprehensive setup guide
- Document all services, ports, and credentials
- Include sample data schema with ER diagram
- Add troubleshooting section for common issues
- Provide useful Docker Compose commands

Closes #1
```

---

## Notes

- Keep documentation updated as configuration changes
- Include screenshots if helpful (store in `docker/images/`)
- Test all commands before documenting them
- Consider adding a video walkthrough link for complex setups
