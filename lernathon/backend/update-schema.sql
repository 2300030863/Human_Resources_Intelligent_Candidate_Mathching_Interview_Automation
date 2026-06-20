-- Update the role column to accommodate longer role names
USE recruitment_db;

ALTER TABLE users MODIFY COLUMN role VARCHAR(20);

-- Verify the change
DESCRIBE users;
