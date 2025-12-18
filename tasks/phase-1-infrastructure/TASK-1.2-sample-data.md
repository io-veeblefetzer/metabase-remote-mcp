# Task 1.2: Sample Data Population

**Task ID:** 1.2  
**Phase:** 1 - Infrastructure Setup  
**GitHub Issue:** [#1](https://github.com/io-veeblefetzer/metabase-remote-mcp/issues/1)  
**Status:** ⬜ Not Started

---

## Objective

Create database schema and sample data SQL scripts that populate the sample database with realistic business data for testing Metabase queries and dashboards.

---

## Prerequisites

- Task 1.1 completed (Docker Compose configuration)
- `docker/init-sample-db/` directory exists

---

## Technical Details

### Files to Create

**Location:** `docker/init-sample-db/`

Files are executed in alphabetical order by PostgreSQL during container initialization.

### 1. Schema Script

**File:** `docker/init-sample-db/01-schema.sql`

```sql
-- E-commerce sample schema for Metabase testing

-- Customers table
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Product categories
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- Products table
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id INTEGER REFERENCES categories(id),
    price DECIMAL(10, 2) NOT NULL,
    cost DECIMAL(10, 2),
    stock_quantity INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'pending',
    total_amount DECIMAL(10, 2),
    shipping_address TEXT
);

-- Order items (line items)
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) GENERATED ALWAYS AS (quantity * unit_price) STORED
);

-- Create indexes for common queries
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_date ON orders(order_date);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_products_category ON products(category_id);
```

### 2. Sample Data Script

**File:** `docker/init-sample-db/02-sample-data.sql`

Generate meaningful sample data:
- 100+ customers from various countries
- 5-10 product categories
- 50+ products across categories
- 200+ orders spanning the last 12 months
- 500+ order items

Data should support:
- Time-series analysis (orders over time)
- Geographic analysis (customers by country)
- Product performance analysis
- Customer segmentation

### Data Generation Guidelines

```sql
-- Example: Insert categories
INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices and accessories'),
    ('Clothing', 'Apparel and fashion items'),
    ('Books', 'Physical and digital books'),
    ('Home & Garden', 'Home improvement and garden supplies'),
    ('Sports', 'Sports equipment and accessories');

-- Example: Insert customers (use generate_series for bulk)
INSERT INTO customers (email, first_name, last_name, city, country, created_at)
SELECT 
    'customer' || n || '@example.com',
    (ARRAY['John', 'Jane', 'Bob', 'Alice', 'Charlie', 'Emma'])[1 + (n % 6)],
    (ARRAY['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia'])[1 + (n % 6)],
    (ARRAY['New York', 'London', 'Paris', 'Tokyo', 'Sydney', 'Berlin'])[1 + (n % 6)],
    (ARRAY['USA', 'UK', 'France', 'Japan', 'Australia', 'Germany'])[1 + (n % 6)],
    NOW() - (random() * interval '365 days')
FROM generate_series(1, 150) AS n;
```

---

## Implementation Checklist

- [ ] Create `docker/init-sample-db/01-schema.sql` with table definitions
- [ ] Define customers table with appropriate columns
- [ ] Define categories table
- [ ] Define products table with category relationship
- [ ] Define orders table with customer relationship
- [ ] Define order_items table with order and product relationships
- [ ] Add appropriate indexes for query performance
- [ ] Create `docker/init-sample-db/02-sample-data.sql`
- [ ] Generate 100+ customer records
- [ ] Generate 5-10 category records
- [ ] Generate 50+ product records
- [ ] Generate 200+ order records (spanning 12 months)
- [ ] Generate 500+ order_item records
- [ ] Test: Restart containers with `docker-compose down -v && docker-compose up -d`
- [ ] Test: Connect to sample-db and verify tables exist
- [ ] Test: Verify data counts match expectations
- [ ] Test: Run sample queries to verify data quality

---

## Acceptance Criteria

- [ ] Schema script creates all tables without errors
- [ ] All foreign key relationships are properly defined
- [ ] Sample data script populates all tables
- [ ] Customer count >= 100
- [ ] Product count >= 50
- [ ] Order count >= 200
- [ ] Order item count >= 500
- [ ] Data spans at least 12 months for time-series analysis
- [ ] Data includes multiple countries for geographic analysis
- [ ] All queries in verification section execute successfully

---

## Verification Queries

```sql
-- Connect to sample database
docker-compose exec sample-db psql -U sample -d sample_data

-- Verify table existence
\dt

-- Count records in each table
SELECT 'customers' as table_name, COUNT(*) as count FROM customers
UNION ALL
SELECT 'categories', COUNT(*) FROM categories
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;

-- Sample analytics query: Monthly revenue
SELECT 
    DATE_TRUNC('month', order_date) as month,
    COUNT(*) as order_count,
    SUM(total_amount) as revenue
FROM orders
WHERE status = 'completed'
GROUP BY DATE_TRUNC('month', order_date)
ORDER BY month;

-- Sample analytics query: Top customers
SELECT 
    c.first_name || ' ' || c.last_name as customer_name,
    COUNT(o.id) as total_orders,
    SUM(o.total_amount) as total_spent
FROM customers c
JOIN orders o ON c.id = o.customer_id
GROUP BY c.id, customer_name
ORDER BY total_spent DESC
LIMIT 10;

-- Sample analytics query: Products by category
SELECT 
    cat.name as category,
    COUNT(p.id) as product_count,
    AVG(p.price) as avg_price
FROM categories cat
LEFT JOIN products p ON cat.id = p.category_id
GROUP BY cat.id, cat.name
ORDER BY product_count DESC;
```

---

## Commit Template

```
feat(infrastructure): add sample database schema and data

- Create e-commerce schema (customers, products, orders)
- Add 150 customers across 6 countries
- Add 5 product categories with 60 products
- Generate 250 orders with 600+ line items
- Include 12 months of order history for analytics

Part of #1
```

---

## Notes

- SQL scripts run only on first container initialization
- To re-run scripts: `docker-compose down -v && docker-compose up -d`
- Keep generated data realistic for meaningful Metabase visualizations
- Include variety in order statuses (pending, completed, cancelled)
- Ensure price and cost values allow for profit margin analysis
