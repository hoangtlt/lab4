-- Chạy SAU khi ứng dụng tạo bảng bằng Hibernate.
USE OrchidDB;
GO
IF NOT EXISTS (SELECT 1 FROM dbo.orchid_categories WHERE category_name = N'Cattleya')
    INSERT INTO dbo.orchid_categories (category_name) VALUES (N'Cattleya');
IF NOT EXISTS (SELECT 1 FROM dbo.orchid_categories WHERE category_name = N'Dendrobium')
    INSERT INTO dbo.orchid_categories (category_name) VALUES (N'Dendrobium');

-- Dùng ID thực tế trong body Postman, không mặc định luôn là 1 và 2.
SELECT category_id, category_name FROM dbo.orchid_categories ORDER BY category_id;
