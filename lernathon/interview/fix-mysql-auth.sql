-- Fix MySQL Authentication for Python 3.13 Compatibility
-- This changes the root user authentication to mysql_native_password
-- which doesn't require the cryptography package

-- Change root authentication method
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '12345';

-- Flush privileges to apply changes
FLUSH PRIVILEGES;

-- Verify the change
SELECT user, host, plugin FROM mysql.user WHERE user = 'root';
