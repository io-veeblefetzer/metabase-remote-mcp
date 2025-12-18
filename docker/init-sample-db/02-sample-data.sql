-- Sample data for Metabase testing
-- Generates realistic e-commerce data for analytics demonstrations

-- Insert product categories
INSERT INTO categories (name, description) VALUES
    ('Electronics', 'Electronic devices, gadgets, and accessories'),
    ('Clothing', 'Apparel, fashion items, and accessories'),
    ('Books', 'Physical and digital books across all genres'),
    ('Home & Garden', 'Home improvement, furniture, and garden supplies'),
    ('Sports & Outdoors', 'Sports equipment, outdoor gear, and fitness accessories');

-- Insert customers (150 customers across 6 countries)
INSERT INTO customers (email, first_name, last_name, city, country, created_at, is_active)
SELECT 
    'customer' || n || '@example.com',
    (ARRAY['James', 'Emma', 'Oliver', 'Sophia', 'William', 'Ava', 'Benjamin', 'Isabella', 'Lucas', 'Mia', 'Henry', 'Charlotte', 'Alexander', 'Amelia', 'Sebastian'])[1 + (n % 15)],
    (ARRAY['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Anderson', 'Taylor', 'Thomas', 'Moore', 'Jackson'])[1 + ((n * 7) % 15)],
    (ARRAY['New York', 'Los Angeles', 'Chicago', 'London', 'Manchester', 'Birmingham', 'Paris', 'Lyon', 'Marseille', 'Tokyo', 'Osaka', 'Kyoto', 'Sydney', 'Melbourne', 'Brisbane', 'Berlin', 'Munich', 'Hamburg'])[1 + (n % 18)],
    (ARRAY['USA', 'USA', 'USA', 'UK', 'UK', 'UK', 'France', 'France', 'France', 'Japan', 'Japan', 'Japan', 'Australia', 'Australia', 'Australia', 'Germany', 'Germany', 'Germany'])[1 + (n % 18)],
    NOW() - ((n * 2.5 + random() * 30)::integer || ' days')::interval,
    CASE WHEN random() > 0.1 THEN true ELSE false END
FROM generate_series(1, 150) AS n;

-- Insert products (60 products across categories)
INSERT INTO products (name, category_id, price, cost, stock_quantity, created_at) VALUES
    -- Electronics (category_id = 1)
    ('Wireless Bluetooth Headphones', 1, 79.99, 35.00, 150, NOW() - interval '300 days'),
    ('Smart Watch Pro', 1, 299.99, 150.00, 75, NOW() - interval '280 days'),
    ('Portable Power Bank 20000mAh', 1, 49.99, 22.00, 200, NOW() - interval '260 days'),
    ('USB-C Hub 7-in-1', 1, 39.99, 18.00, 120, NOW() - interval '240 days'),
    ('Wireless Charging Pad', 1, 29.99, 12.00, 180, NOW() - interval '220 days'),
    ('Noise Canceling Earbuds', 1, 149.99, 65.00, 90, NOW() - interval '200 days'),
    ('4K Webcam', 1, 89.99, 40.00, 60, NOW() - interval '180 days'),
    ('Mechanical Keyboard RGB', 1, 119.99, 55.00, 85, NOW() - interval '160 days'),
    ('Gaming Mouse Wireless', 1, 69.99, 30.00, 110, NOW() - interval '140 days'),
    ('Portable SSD 1TB', 1, 109.99, 60.00, 95, NOW() - interval '120 days'),
    ('Smart Home Speaker', 1, 59.99, 28.00, 130, NOW() - interval '100 days'),
    ('Tablet Stand Adjustable', 1, 34.99, 15.00, 200, NOW() - interval '80 days'),
    
    -- Clothing (category_id = 2)
    ('Classic Cotton T-Shirt', 2, 24.99, 8.00, 500, NOW() - interval '290 days'),
    ('Slim Fit Jeans', 2, 59.99, 25.00, 300, NOW() - interval '270 days'),
    ('Wool Blend Sweater', 2, 79.99, 35.00, 150, NOW() - interval '250 days'),
    ('Running Shoes Pro', 2, 129.99, 55.00, 200, NOW() - interval '230 days'),
    ('Waterproof Jacket', 2, 149.99, 65.00, 100, NOW() - interval '210 days'),
    ('Casual Sneakers', 2, 89.99, 40.00, 180, NOW() - interval '190 days'),
    ('Leather Belt Premium', 2, 44.99, 18.00, 250, NOW() - interval '170 days'),
    ('Cotton Hoodie', 2, 54.99, 22.00, 220, NOW() - interval '150 days'),
    ('Dress Shirt Slim Fit', 2, 49.99, 20.00, 280, NOW() - interval '130 days'),
    ('Summer Shorts', 2, 34.99, 14.00, 350, NOW() - interval '110 days'),
    ('Winter Beanie', 2, 19.99, 7.00, 400, NOW() - interval '90 days'),
    ('Athletic Socks 6-Pack', 2, 24.99, 9.00, 450, NOW() - interval '70 days'),
    
    -- Books (category_id = 3)
    ('The Art of Programming', 3, 49.99, 20.00, 100, NOW() - interval '285 days'),
    ('Modern Data Science', 3, 54.99, 22.00, 80, NOW() - interval '265 days'),
    ('Business Strategy Guide', 3, 39.99, 16.00, 120, NOW() - interval '245 days'),
    ('Creative Writing Workshop', 3, 29.99, 12.00, 150, NOW() - interval '225 days'),
    ('History of Innovation', 3, 34.99, 14.00, 90, NOW() - interval '205 days'),
    ('Mindfulness and Meditation', 3, 24.99, 10.00, 200, NOW() - interval '185 days'),
    ('Financial Freedom', 3, 27.99, 11.00, 180, NOW() - interval '165 days'),
    ('Leadership Excellence', 3, 32.99, 13.00, 130, NOW() - interval '145 days'),
    ('The Science of Habits', 3, 28.99, 12.00, 160, NOW() - interval '125 days'),
    ('Digital Marketing Mastery', 3, 44.99, 18.00, 110, NOW() - interval '105 days'),
    ('Cookbook: World Cuisines', 3, 35.99, 15.00, 140, NOW() - interval '85 days'),
    ('Photography Essentials', 3, 42.99, 17.00, 95, NOW() - interval '65 days'),
    
    -- Home & Garden (category_id = 4)
    ('Smart LED Bulb Set (4)', 4, 39.99, 18.00, 300, NOW() - interval '275 days'),
    ('Indoor Plant Pot Set', 4, 29.99, 12.00, 250, NOW() - interval '255 days'),
    ('Cordless Vacuum Cleaner', 4, 199.99, 90.00, 60, NOW() - interval '235 days'),
    ('Air Purifier HEPA', 4, 149.99, 70.00, 80, NOW() - interval '215 days'),
    ('Bamboo Cutting Board Set', 4, 34.99, 14.00, 200, NOW() - interval '195 days'),
    ('Stainless Steel Cookware Set', 4, 129.99, 60.00, 50, NOW() - interval '175 days'),
    ('Memory Foam Pillow', 4, 49.99, 20.00, 180, NOW() - interval '155 days'),
    ('Blackout Curtains', 4, 44.99, 18.00, 160, NOW() - interval '135 days'),
    ('Garden Tool Set', 4, 59.99, 25.00, 120, NOW() - interval '115 days'),
    ('Outdoor String Lights', 4, 24.99, 10.00, 280, NOW() - interval '95 days'),
    ('Robot Vacuum', 4, 299.99, 140.00, 40, NOW() - interval '75 days'),
    ('Essential Oil Diffuser', 4, 34.99, 15.00, 220, NOW() - interval '55 days'),
    
    -- Sports & Outdoors (category_id = 5)
    ('Yoga Mat Premium', 5, 39.99, 16.00, 200, NOW() - interval '278 days'),
    ('Resistance Bands Set', 5, 24.99, 10.00, 350, NOW() - interval '258 days'),
    ('Adjustable Dumbbells', 5, 199.99, 95.00, 45, NOW() - interval '238 days'),
    ('Camping Tent 4-Person', 5, 149.99, 70.00, 60, NOW() - interval '218 days'),
    ('Hiking Backpack 40L', 5, 89.99, 40.00, 100, NOW() - interval '198 days'),
    ('Cycling Helmet', 5, 59.99, 25.00, 120, NOW() - interval '178 days'),
    ('Swimming Goggles Pro', 5, 29.99, 12.00, 200, NOW() - interval '158 days'),
    ('Fitness Tracker Band', 5, 79.99, 35.00, 150, NOW() - interval '138 days'),
    ('Foam Roller', 5, 24.99, 10.00, 250, NOW() - interval '118 days'),
    ('Jump Rope Speed', 5, 14.99, 5.00, 400, NOW() - interval '98 days'),
    ('Insulated Water Bottle', 5, 29.99, 12.00, 300, NOW() - interval '78 days'),
    ('Outdoor Folding Chair', 5, 44.99, 20.00, 140, NOW() - interval '58 days');

-- Generate orders (250+ orders spanning 12 months)
-- First, create a temporary function to generate orders
DO $$
DECLARE
    v_customer_id INTEGER;
    v_order_id INTEGER;
    v_product_id INTEGER;
    v_product_price DECIMAL(10,2);
    v_quantity INTEGER;
    v_order_total DECIMAL(10,2);
    v_items_count INTEGER;
    v_status VARCHAR(50);
    v_order_date TIMESTAMP;
    i INTEGER;
    j INTEGER;
BEGIN
    -- Generate 250 orders
    FOR i IN 1..250 LOOP
        -- Random customer (1-150)
        v_customer_id := 1 + floor(random() * 150)::integer;
        
        -- Random order date in last 365 days
        v_order_date := NOW() - ((random() * 365)::integer || ' days')::interval - ((random() * 24)::integer || ' hours')::interval;
        
        -- Random status with weighted distribution
        v_status := CASE 
            WHEN random() < 0.70 THEN 'completed'
            WHEN random() < 0.85 THEN 'shipped'
            WHEN random() < 0.95 THEN 'pending'
            ELSE 'cancelled'
        END;
        
        -- Insert order
        INSERT INTO orders (customer_id, order_date, status, total_amount, shipping_address)
        VALUES (v_customer_id, v_order_date, v_status, 0, 
                (SELECT city || ', ' || country FROM customers WHERE id = v_customer_id))
        RETURNING id INTO v_order_id;
        
        -- Random number of items (1-5)
        v_items_count := 1 + floor(random() * 5)::integer;
        v_order_total := 0;
        
        -- Add order items
        FOR j IN 1..v_items_count LOOP
            -- Random product
            v_product_id := 1 + floor(random() * 60)::integer;
            SELECT price INTO v_product_price FROM products WHERE id = v_product_id;
            
            -- Random quantity (1-3)
            v_quantity := 1 + floor(random() * 3)::integer;
            
            -- Insert order item (ignore if duplicate product in same order)
            BEGIN
                INSERT INTO order_items (order_id, product_id, quantity, unit_price)
                VALUES (v_order_id, v_product_id, v_quantity, v_product_price);
                
                v_order_total := v_order_total + (v_quantity * v_product_price);
            EXCEPTION WHEN unique_violation THEN
                -- Skip duplicate products in the same order
                NULL;
            END;
        END LOOP;
        
        -- Update order total
        UPDATE orders SET total_amount = v_order_total WHERE id = v_order_id;
    END LOOP;
END $$;

-- Add some additional order items to ensure we have 600+
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT 
    1 + floor(random() * 250)::integer as order_id,
    1 + floor(random() * 60)::integer as product_id,
    1 + floor(random() * 2)::integer as quantity,
    (SELECT price FROM products WHERE id = 1 + floor(random() * 60)::integer LIMIT 1) as unit_price
FROM generate_series(1, 200)
ON CONFLICT DO NOTHING;

-- Update order totals to include any additional items
UPDATE orders o
SET total_amount = (
    SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0)
    FROM order_items oi
    WHERE oi.order_id = o.id
);

-- Display summary
DO $$
DECLARE
    v_customers INTEGER;
    v_categories INTEGER;
    v_products INTEGER;
    v_orders INTEGER;
    v_order_items INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_customers FROM customers;
    SELECT COUNT(*) INTO v_categories FROM categories;
    SELECT COUNT(*) INTO v_products FROM products;
    SELECT COUNT(*) INTO v_orders FROM orders;
    SELECT COUNT(*) INTO v_order_items FROM order_items;
    
    RAISE NOTICE 'Sample data generation complete:';
    RAISE NOTICE '  Customers: %', v_customers;
    RAISE NOTICE '  Categories: %', v_categories;
    RAISE NOTICE '  Products: %', v_products;
    RAISE NOTICE '  Orders: %', v_orders;
    RAISE NOTICE '  Order Items: %', v_order_items;
END $$;
