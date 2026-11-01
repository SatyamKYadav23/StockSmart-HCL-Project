-- =============================================================================
-- StockSmart — Demonstration Seed Data
-- =============================================================================

USE `stocksmart`;

SET FOREIGN_KEY_CHECKS = 0;

-- Categories
INSERT INTO `categories` (`id`, `name`, `description`) VALUES
(1, 'Electronics & Appliances', 'Consumer electronics, peripherals, and office appliances'),
(2, 'Footwear & Apparel', 'Branded retail apparel, footwear, and casual clothing'),
(3, 'Home & Kitchen', 'Modern home accessories, kitchenware, and storage solutions'),
(4, 'Health & Personal Care', 'Personal care goods, hygiene essentials, and grooming kits'),
(5, 'Sports & Fitness', 'Sporting equipment, gym gear, and outdoor athletic merchandise')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- Locations / Facilities
INSERT INTO `locations` (`id`, `name`, `address`, `location_type`) VALUES
(1, 'Delhi Central Warehouse', 'Plot 42, Okhla Industrial Area Phase III, New Delhi 110020', 'WAREHOUSE'),
(2, 'Mumbai Fulfillment Hub', 'Bldg 7, Bhiwandi Logistics Park, Mumbai 421302', 'FULFILLMENT_CENTER'),
(3, 'Bangalore Regional Center', 'Electronic City Phase II, Hosur Road, Bangalore 560100', 'DISTRIBUTION_CENTER'),
(4, 'Hyderabad Retail Outlet', 'Road No 36, Jubilee Hills, Hyderabad 500033', 'RETAIL_STORE')
ON DUPLICATE KEY UPDATE `address` = VALUES(`address`);

-- Suppliers
INSERT INTO `suppliers` (`id`, `name`, `email`, `phone`, `address`) VALUES
(1, 'Apex Tech Distributors', 'contact@apextech.in', '+91 98110 23456', 'Connaught Place, New Delhi'),
(2, 'Vanguard Lifestyle Brands', 'orders@vanguardstyle.com', '+91 98200 45678', 'Lower Parel, Mumbai'),
(3, 'Nexus Home Products', 'support@nexushome.co.in', '+91 98450 78901', 'Koramangala, Bangalore'),
(4, 'Pinnacle Sports Wholesale', 'sales@pinnaclesports.in', '+91 98310 12345', 'Salt Lake Sector V, Kolkata')
ON DUPLICATE KEY UPDATE `email` = VALUES(`email`);

-- Products
INSERT INTO `products` (`id`, `sku`, `name`, `description`, `category_id`, `supplier_id`, `price`, `cost_price`, `barcode`, `reorder_level`, `active`) VALUES
(1, 'ELEC-LOGI-MX3', 'Logitech MX Master 3S Wireless Mouse', 'Ergonomic performance wireless mouse with 8K DPI sensor', 1, 1, 8995.00, 6200.00, '8901001000101', 15, 1),
(2, 'ELEC-DELL-U27', 'Dell UltraSharp 27" 4K Monitor', 'Professional 4K IPS display with 99% sRGB color gamut', 1, 1, 38500.00, 29000.00, '8901001000102', 10, 1),
(3, 'APPL-NIK-AF1', 'Nike Air Force 1 07 Casual Shoes', 'Classic leather white casual sneaker with cushioned midsole', 2, 2, 7495.00, 4800.00, '8901001000103', 20, 1),
(4, 'HOME-PREST-COOK', 'Prestige 5L Deluxe Induction Cooker', 'Hard-anodized high-efficiency pressure cooker', 3, 3, 2450.00, 1600.00, '8901001000104', 25, 1),
(5, 'SPRT-YON-ASTX', 'Yonex Astrox 88D Pro Badminton Racket', 'Heavy head rotational generator balance professional racket', 5, 4, 14200.00, 10500.00, '8901001000105', 8, 1)
ON DUPLICATE KEY UPDATE `price` = VALUES(`price`);

-- Initial Inventory Allocations
INSERT INTO `inventory` (`product_id`, `location_id`, `quantity`, `reserved_quantity`) VALUES
(1, 1, 120, 10),
(1, 2, 85, 5),
(2, 1, 45, 2),
(2, 2, 30, 0),
(3, 1, 90, 15),
(3, 3, 60, 5),
(4, 1, 150, 20),
(4, 4, 40, 0),
(5, 1, 35, 5),
(5, 3, 25, 0)
ON DUPLICATE KEY UPDATE `quantity` = VALUES(`quantity`);

-- Sample Stock Movements
INSERT INTO `stock_movements` (`product_id`, `location_id`, `movement_type`, `direction`, `quantity`, `reference_type`, `remarks`) VALUES
(1, 1, 'RECEIPT', 'IN', 120, 'REC-INITIAL-01', 'Initial stock intake from Apex Tech'),
(2, 1, 'RECEIPT', 'IN', 45, 'REC-INITIAL-02', 'Initial stock intake from Apex Tech'),
(3, 1, 'RECEIPT', 'IN', 90, 'REC-INITIAL-03', 'Initial stock intake from Vanguard'),
(4, 1, 'RECEIPT', 'IN', 150, 'REC-INITIAL-04', 'Initial stock intake from Nexus Home'),
(5, 1, 'RECEIPT', 'IN', 35, 'REC-INITIAL-05', 'Initial stock intake from Pinnacle Sports')
ON DUPLICATE KEY UPDATE `quantity` = VALUES(`quantity`);

SET FOREIGN_KEY_CHECKS = 1;
