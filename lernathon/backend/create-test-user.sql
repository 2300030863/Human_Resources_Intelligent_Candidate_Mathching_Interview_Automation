-- Create a test user for development/testing
-- Password is 'password123' hashed with BCrypt
-- Run this in MySQL after creating the database

-- Test Candidate User
INSERT INTO users (email, password, first_name, last_name, role, created_at, updated_at)
VALUES (
    'candidate@test.com',
    '$2a$10$rqB9yh7jU5KcN.OYjLmjJOxW9F8LX5qx8T5k1W5yP6AZj8Ly0h5Xy',  -- password: password123
    'Test',
    'Candidate',
    'CANDIDATE',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE email = email;

-- Test Recruiter User  
INSERT INTO users (email, password, first_name, last_name, role, created_at, updated_at)
VALUES (
    'recruiter@test.com',
    '$2a$10$rqB9yh7jU5KcN.OYjLmjJOxW9F8LX5qx8T5k1W5yP6AZj8Ly0h5Xy',  -- password: password123
    'Test',
    'Recruiter',
    'RECRUITER',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE email = email;

-- Test Admin User
INSERT INTO users (email, password, first_name, last_name, role, created_at, updated_at)
VALUES (
    'admin@test.com',
    '$2a$10$rqB9yh7jU5KcN.OYjLmjJOxW9F8LX5qx8T5k1W5yP6AZj8Ly0h5Xy',  -- password: password123
    'Test',
    'Admin',
    'ADMIN',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE email = email;

SELECT 'Test users created successfully!' AS message;
SELECT email, role FROM users WHERE email LIKE '%@test.com';
