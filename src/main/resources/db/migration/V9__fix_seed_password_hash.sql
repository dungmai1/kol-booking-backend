-- V9__fix_seed_password_hash.sql
-- Fix bcrypt hash cho password "password123"
-- Bcrypt hash with strength 10
UPDATE app_user 
SET password_hash = '$2a$10$xGX57X/Ye7u9ZLNM8N.qgeLvB9zPZSDcAMhfFNtfSJZmLVZy9SqLK'
WHERE email LIKE '%@seed.local';
