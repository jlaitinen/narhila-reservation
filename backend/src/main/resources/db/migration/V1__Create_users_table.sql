CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL
);

-- Create admin user (password: admin123)
INSERT INTO users (id, username, email, password_hash, role)
VALUES (
  'f47ac10b-58cc-4372-a567-0e02b2c3d479',
  'admin',
  'admin@example.com',
  '$2a$10$Z63mO52Wbc7moZ2jwdWHfOpuBXpO7eiGUfsSrLb40OlPYbcEjlIhG',
  'Admin'
);