-- Chạy trong SSMS hoặc sqlcmd trước khi khởi động ứng dụng.
USE master;
GO
IF DB_ID(N'OrchidDB') IS NULL
    CREATE DATABASE OrchidDB;
GO
