-- Create the database
CREATE DATABASE IF NOT EXISTS recruitment_db;

-- Use the database
USE recruit_db;

-- Note: Spring Boot will automatically create tables based on your entities
-- when you run the application with spring.jpa.hibernate.ddl-auto=update

-- Verify database creation
SHOW DATABASES LIKE 'recruit_db';
