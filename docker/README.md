# Docker Development Environment

This directory contains the Docker Compose configuration for running Metabase with a sample PostgreSQL database for local development and testing.

## Overview

The development environment consists of three services:
- **Metabase**: Business intelligence and analytics platform
- **PostgreSQL (App DB)**: Stores Metabase application data (metadata, settings, user accounts)
- **PostgreSQL (Sample DB)**: Contains sample e-commerce data for testing queries and dashboards

## Quick Start

### Prerequisites

- Docker Engine 20.10+
- Docker Compose V2+
- 4GB RAM available for containers
- Ports 3000, 5432, and 5433 available

### Starting the Environment

```bash
# Navigate to project root
cd /path/to/metabase-remote-mcp

# Start all services in background
docker-compose up -d

# View logs (wait for "Metabase Initialization COMPLETE")
docker-compose logs -f metabase

# Stop all services
docker-compose down

# Stop and remove all data (fresh start)
docker-compose down -v
```

### Initial Metabase Setup

1. Open http://localhost:3000 in your browser
2. Click "Let's get started"
3. Select your preferred language
4. Create admin account:
   - Enter your email, password, and name
5. Add the sample database connection:
   - Database type: **PostgreSQL**
   - Display name: **Sample Data**
   - Host: **sample-db**
   - Port: **5432**
   - Database name: **sample_data**
   - Username: **sample**
   - Password: **sample**
6. Complete the setup wizard

## Services

| Service | Container Name | Internal Port | External Port | Purpose |
|---------|---------------|---------------|---------------|---------|
| metabase | metabase | 3000 | 3000 | Metabase Web UI |
| postgres | metabase-postgres | 5432 | 5432 | Metabase App Database |
| sample-db | metabase-sample-db | 5432 | 5433 | Sample E-commerce Data |

## Configuration

### Environment Variables

| Variable | Service | Default | Description |
|----------|---------|---------|-------------|
| MB_DB_TYPE | metabase | postgres | Database type for Metabase |
| MB_DB_HOST | metabase | postgres | Database host |
| MB_DB_PORT | metabase | 5432 | Database port |
| MB_DB_DBNAME | metabase | metabase_app | Database name |
| MB_DB_USER | metabase | metabase | Database user |
| MB_DB_PASS | metabase | metabase | Database password |
| POSTGRES_USER | postgres | metabase | PostgreSQL user |
| POSTGRES_PASSWORD | postgres | metabase | PostgreSQL password |
| POSTGRES_DB | postgres | metabase_app | PostgreSQL database |

### Customizing Ports

If you need to use different ports, edit `docker-compose.yml`:

```yaml
services:
  metabase:
    ports:
      - "8080:3000"  # Change 8080 to your preferred port
```

## Default Credentials

### PostgreSQL (Application Database)
- **Host**: localhost (from host) / postgres (from containers)
- **Port**: 5432
- **Database**: metabase_app
- **Username**: metabase
- **Password**: metabase

### PostgreSQL (Sample Database)
- **Host**: localhost (from host) / sample-db (from containers)
- **Port**: 5433 (from host) / 5432 (from containers)
- **Database**: sample_data
- **Username**: sample
- **Password**: sample

### Metabase Admin
- Created during initial setup wizard
- You choose your own email and password

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
| customers | 150 | Customer profiles from 6 countries |
| categories | 5 | Product categories |
| products | 60 | Product catalog across all categories |
| orders | 250+ | Order records spanning 12 months |
| order_items | 600+ | Order line items with quantities and prices |

### Categories

1. **Electronics** - Devices, gadgets, and accessories
2. **Clothing** - Apparel and fashion items
3. **Books** - Physical and digital books
4. **Home & Garden** - Home improvement and garden supplies
5. **Sports & Outdoors** - Sports equipment and outdoor gear

### Data Characteristics

- **Geographic distribution**: USA, UK, France, Japan, Australia, Germany
- **Time range**: Orders span the last 12 months
- **Order statuses**: completed (70%), shipped (15%), pending (10%), cancelled (5%)
- **Price range**: Products from $14.99 to $299.99

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

# Check service health
docker-compose ps

# Connect to sample database (interactive psql)
docker-compose exec sample-db psql -U sample -d sample_data

# Connect to app database
docker-compose exec postgres psql -U metabase -d metabase_app

# Run a query directly
docker-compose exec sample-db psql -U sample -d sample_data -c "SELECT COUNT(*) FROM customers;"

# Remove all data and start fresh
docker-compose down -v
docker-compose up -d

# View container resource usage
docker stats metabase metabase-postgres metabase-sample-db
```

### Sample Queries

```sql
-- Connect first: docker-compose exec sample-db psql -U sample -d sample_data

-- Count records in all tables
SELECT 'customers' as table_name, COUNT(*) as count FROM customers
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items;

-- Monthly revenue analysis
SELECT 
    DATE_TRUNC('month', order_date) as month,
    COUNT(*) as order_count,
    SUM(total_amount) as revenue
FROM orders
WHERE status = 'completed'
GROUP BY DATE_TRUNC('month', order_date)
ORDER BY month;

-- Top 10 customers by spend
SELECT 
    c.first_name || ' ' || c.last_name as customer_name,
    c.country,
    COUNT(o.id) as total_orders,
    SUM(o.total_amount) as total_spent
FROM customers c
JOIN orders o ON c.id = o.customer_id
WHERE o.status = 'completed'
GROUP BY c.id, customer_name, c.country
ORDER BY total_spent DESC
LIMIT 10;

-- Revenue by category
SELECT 
    cat.name as category,
    COUNT(DISTINCT o.id) as order_count,
    SUM(oi.quantity) as items_sold,
    SUM(oi.subtotal) as revenue
FROM categories cat
JOIN products p ON cat.id = p.category_id
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON oi.order_id = o.id
WHERE o.status = 'completed'
GROUP BY cat.id, cat.name
ORDER BY revenue DESC;

-- Orders by country
SELECT 
    c.country,
    COUNT(o.id) as order_count,
    SUM(o.total_amount) as total_revenue
FROM customers c
JOIN orders o ON c.id = o.customer_id
GROUP BY c.country
ORDER BY total_revenue DESC;
```

## Troubleshooting

### Port Already in Use

**Error**: `bind: address already in use`

**Solution**:
```bash
# Find process using the port (e.g., 3000)
sudo lsof -i :3000

# Kill the process or use a different port in docker-compose.yml
kill <PID>

# Or change the port mapping
# ports:
#   - "3001:3000"
```

### Metabase Not Starting

**Symptom**: Metabase container keeps restarting or exits immediately

**Solutions**:
1. Check logs for specific errors:
   ```bash
   docker-compose logs metabase
   ```
2. Ensure PostgreSQL is healthy first:
   ```bash
   docker-compose ps
   docker-compose exec postgres pg_isready -U metabase
   ```
3. Check if the app database was created:
   ```bash
   docker-compose exec postgres psql -U metabase -c "\\l"
   ```
4. Increase memory limit (add to metabase service):
   ```yaml
   mem_limit: 2g
   ```

### Database Connection Failed

**Symptom**: "Unable to connect to database" in Metabase UI

**Solutions**:
1. Ensure the database container is running:
   ```bash
   docker-compose ps sample-db
   ```
2. Test network connectivity:
   ```bash
   docker-compose exec metabase ping sample-db
   ```
3. Verify credentials (use container name `sample-db`, not `localhost`)
4. Check if sample data was loaded:
   ```bash
   docker-compose exec sample-db psql -U sample -d sample_data -c "\\dt"
   ```

### Data Missing After Restart

**Symptom**: Sample data tables are empty after restarting containers

**Explanation**: SQL init scripts only run when the volume is first created.

**Solution**:
```bash
# Remove volumes to trigger reinitialization
docker-compose down -v
docker-compose up -d
```

### Permission Denied (Linux)

**Symptom**: Volume mount permission errors

**Solutions**:
```bash
# Option 1: Fix ownership of docker directory
sudo chown -R $USER:$USER ./docker

# Option 2: Make scripts executable
chmod +x ./docker/init-sample-db/*.sql
```

### Container Health Check Failing

**Symptom**: Container marked as unhealthy

**Solutions**:
1. View detailed health status:
   ```bash
   docker inspect --format='{{json .State.Health}}' metabase | jq
   ```
2. For Metabase, wait longer (it can take 2+ minutes on first start)
3. Check if all dependencies are healthy first

### Out of Disk Space

**Symptom**: Containers fail to start, "no space left on device"

**Solution**:
```bash
# Clean up unused Docker resources
docker system prune -a --volumes
```

## Connecting from Application

When running the MCP server application, use these connection settings:

### application.properties

```properties
# Metabase connection
metabase.url=http://localhost:3000
metabase.api.key=YOUR_API_KEY
```

### Generating a Metabase API Key

1. Log in to Metabase as admin
2. Click the gear icon → **Admin settings**
3. Go to **Settings** → **Authentication** → **API Keys**
4. Click **Create API Key**
5. Give it a name (e.g., "MCP Server")
6. Copy the generated key to your `application.properties`

> **Note**: API keys provide full access. Keep them secure and never commit them to version control.

### Connection from Docker Network

If your MCP server runs in Docker on the same network:

```properties
metabase.url=http://metabase:3000
```

## Directory Structure

```
docker/
├── README.md                    # This documentation
├── init-db/                     # Metabase app DB init scripts
│   └── .gitkeep                 # (empty - Metabase manages its schema)
└── init-sample-db/              # Sample data DB init scripts
    ├── 01-schema.sql            # Table definitions
    └── 02-sample-data.sql       # Sample data generation
```

## Additional Resources

- [Metabase Documentation](https://www.metabase.com/docs/latest/)
- [Metabase API Reference](https://www.metabase.com/docs/latest/api-documentation)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/15/)
