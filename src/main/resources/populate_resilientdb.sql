-- This script assumes that the database 'resilientdb' and user credentials are already configured.

-- Start a transaction
BEGIN;

-- Drop the table if it already exists
DROP TABLE IF EXISTS users;

-- Create a new table 'users'
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

-- Insert test data into the 'users' table
INSERT INTO users (name, email) VALUES
('Alice Johnson', 'alice.johnson@example.com'),
('Bob Smith', 'bob.smith@example.com'),
('Carol Taylor', 'carol.taylor@example.com'),
('David Brown', 'david.brown@example.com'),
('Eve White', 'eve.white@example.com'),
('Frank Harris', 'frank.harris@example.com'),
('Grace Lee', 'grace.lee@example.com'),
('Hank Green', 'hank.green@example.com'),
('Ivy Hall', 'ivy.hall@example.com'),
('Jack King', 'jack.king@example.com'),
('Kathy Long', 'kathy.long@example.com'),
('Leo Clark', 'leo.clark@example.com'),
('Mia Young', 'mia.young@example.com'),
('Nick Wright', 'nick.wright@example.com'),
('Olivia Moore', 'olivia.moore@example.com'),
('Pete Scott', 'pete.scott@example.com'),
('Quinn Adams', 'quinn.adams@example.com'),
('Rachel Baker', 'rachel.baker@example.com'),
('Steve Carter', 'steve.carter@example.com'),
('Tina Turner', 'tina.turner@example.com');

-- Commit the transaction
COMMIT;
