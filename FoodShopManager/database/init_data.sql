-- ============================================================
-- FoodShop Manager - Script tạo database MySQL
-- Chạy script này trong MySQL Workbench hoặc phpMyAdmin
-- ============================================================

-- Tạo database
CREATE DATABASE IF NOT EXISTS foodshop_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE foodshop_db;

-- ============================================================
-- Hibernate sẽ tự tạo bảng khi chạy ứng dụng (hbm2ddl.auto=update)
-- Script này chỉ cần thiết để seed dữ liệu mẫu
-- ============================================================

-- Chạy ứng dụng một lần để Hibernate tạo bảng, sau đó insert dữ liệu mẫu:

-- ===== DANH MỤC SẢN PHẨM =====
INSERT INTO categories (name, description, active) VALUES
('Rau củ quả',    'Rau, củ, quả tươi các loại',           true),
('Thịt & Cá',     'Thịt heo, bò, gà, cá tươi sống',       true),
('Hải sản',       'Tôm, cua, mực, nghêu, ốc...',           true),
('Trái cây',      'Trái cây trong nước và nhập khẩu',       true),
('Đồ khô',        'Gạo, nếp, đậu, mì, bún khô...',        true),
('Gia vị nước mắm','Nước mắm, dầu ăn, muối, đường, gia vị',true),
('Đồ uống',       'Nước ngọt, trà, cà phê, nước ép...',    true),
('Bánh kẹo',      'Bánh quy, kẹo, snack các loại',         true),
('Sữa & Trứng',   'Sữa tươi, sữa hộp, trứng gà, trứng vịt',true),
('Đóng hộp',      'Đồ hộp, đồ ăn liền, thực phẩm chế biến',true);

-- ===== SẢN PHẨM MẪU =====
-- (Chạy sau khi có categories)
INSERT INTO products (product_code, name, description, category_id, unit, price_import, price_sell,
                      quantity_in_stock, min_stock_level, origin, brand, status, active) VALUES
('SP001', 'Cà chua bi đỏ',     'Cà chua bi tươi ngon',        1, 'kg',   15000, 25000,  50, 5, 'Đà Lạt',     '',        'AVAILABLE', true),
('SP002', 'Rau muống',          'Rau muống sạch 500g',         1, 'bó',    5000,  8000, 100, 10,'Hà Nội',     '',        'AVAILABLE', true),
('SP003', 'Thịt heo ba chỉ',   'Thịt heo tươi',               2, 'kg',  100000,140000,  30, 3, 'Việt Nam',   '',        'AVAILABLE', true),
('SP004', 'Cá basa fillet',    'Cá basa phi lê đông lạnh 1kg', 2, 'kg',   60000, 85000,  20, 3, 'Việt Nam',   '',        'AVAILABLE', true),
('SP005', 'Tôm thẻ sống',      'Tôm thẻ tươi 500g',           3, 'kg',  120000,160000,  15, 2, 'Cà Mau',     '',        'AVAILABLE', true),
('SP006', 'Xoài cát Hòa Lộc',  'Xoài ngọt thơm',              4, 'kg',   35000, 55000,  40, 5, 'Tiền Giang', '',        'AVAILABLE', true),
('SP007', 'Gạo ST25',          'Gạo ngon nhất thế giới',       5, 'kg',   22000, 32000, 200,20, 'Sóc Trăng',  'ST25',    'AVAILABLE', true),
('SP008', 'Nước mắm Phú Quốc', 'Nước mắm 40 độ đạm 500ml',    6, 'chai',  25000, 38000,  80,10, 'Phú Quốc',   'Khải Hoàn','AVAILABLE',true),
('SP009', 'Pepsi lon 330ml',   'Nước ngọt Pepsi',              7, 'lon',    7000, 12000, 150,20, 'Việt Nam',   'Pepsi',   'AVAILABLE', true),
('SP010', 'Oreo socola',       'Bánh quy kem socola',          8, 'gói',   18000, 28000,  60,10, 'Việt Nam',   'Oreo',    'AVAILABLE', true),
('SP011', 'Sữa TH True Milk',  'Sữa tươi nguyên chất 1 lít',  9, 'hộp',   22000, 32000,  80,15, 'Nghệ An',    'TH',      'AVAILABLE', true),
('SP012', 'Trứng gà ta',       'Trứng gà ta 10 quả',          9, 'vỉ',    28000, 40000,  50,10, 'Việt Nam',   '',        'AVAILABLE', true),
('SP013', 'Mì Hảo Hảo tôm chua','Mì ăn liền Hảo Hảo',        10,'gói',    3500,  5500, 200,30, 'Việt Nam',   'Acecook', 'AVAILABLE', true),
('SP014', 'Cá hộp 3 con cua',  'Cá trích sốt cà',            10,'hộp',   15000, 22000,  90,15, 'Việt Nam',   '3 con cua','AVAILABLE',true),
('SP015', 'Ớt đà lạt',         'Ớt chuông đỏ',                1, 'kg',   20000, 35000,  25, 5, 'Đà Lạt',     '',        'AVAILABLE', true);

-- ===== KHÁCH HÀNG MẪU =====
INSERT INTO customers (full_name, phone, email, address, district, city, customer_type, loyalty_points) VALUES
('Nguyễn Thị Lan',    '0901234567', 'lan@gmail.com',    '123 Lê Lợi',     'Q.1',    'TP.HCM', 'VIP',     500),
('Trần Văn Minh',     '0912345678', 'minh@gmail.com',   '45 Hai Bà Trưng', 'Q.3',   'TP.HCM', 'REGULAR', 120),
('Lê Thị Hoa',        '0923456789', 'hoa@gmail.com',    '78 Đinh Tiên Hoàng','Q.Bình Thạnh','TP.HCM','REGULAR',80),
('Phạm Quốc Hùng',    '0934567890', 'hung@gmail.com',   '12 Nguyễn Huệ',  'Q.1',    'TP.HCM', 'WHOLESALE',1200),
('Hoàng Thanh Tú',    '0945678901', 'tu@gmail.com',     '56 CMT8',        'Q.10',   'TP.HCM', 'NEW',     0);
