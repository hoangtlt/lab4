USE OrchidDB;
GO
SELECT DB_NAME() AS database_name, @@SERVERNAME AS server_name;
SELECT category_id, category_name FROM dbo.orchid_categories ORDER BY category_id;
SELECT o.orchid_id, o.orchid_name, o.is_natural, o.is_attractive,
       o.orchid_description, o.orchid_url, o.category_id, c.category_name
FROM dbo.orchids o
JOIN dbo.orchid_categories c ON c.category_id = o.category_id
ORDER BY o.orchid_id;

-- Kiểm tra PK, identity, NOT NULL và độ dài cột.
SELECT t.name AS table_name, c.name AS column_name, ty.name AS data_type,
       c.max_length, c.is_nullable, c.is_identity
FROM sys.tables t
JOIN sys.columns c ON c.object_id = t.object_id
JOIN sys.types ty ON ty.user_type_id = c.user_type_id
WHERE t.name IN ('orchids', 'orchid_categories')
ORDER BY t.name, c.column_id;

SELECT OBJECT_NAME(parent_object_id) AS table_name, name AS primary_key
FROM sys.key_constraints
WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) IN ('orchids', 'orchid_categories');

SELECT fk.name AS foreign_key,
       OBJECT_NAME(fkc.parent_object_id) AS child_table,
       COL_NAME(fkc.parent_object_id, fkc.parent_column_id) AS child_column,
       OBJECT_NAME(fkc.referenced_object_id) AS parent_table,
       COL_NAME(fkc.referenced_object_id, fkc.referenced_column_id) AS parent_column
FROM sys.foreign_keys fk
JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
WHERE OBJECT_NAME(fkc.parent_object_id) = 'orchids';
