-- =====================================================================
-- DATABASE & TABLE INITIALIZATION
-- =====================================================================

-- "Where is the database created?"
-- (CREATE DATABASE IF NOT EXISTS lrt1_db;)

-- "Point to where entities in your ERD are stored in MySQL."
-- 1. STATIONS Table: Holds station metadata (station_id, station_name, max_capacity).
-- 2. TAP_LOGS Table: Holds transaction records (tap_id, station_id, tap_type, direction, timestamp).

-- "Where is the relationship (Foreign Key) defined?"
-- (FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE CASCADE;)

-- 1. Create the database schema
CREATE DATABASE IF NOT EXISTS lrt1_db;

-- 2. Select the database to use
USE lrt1_db;

-- 3: Create the STATIONS metadata table
CREATE TABLE IF NOT EXISTS stations (
    station_id INT PRIMARY KEY AUTO_INCREMENT,
    station_name VARCHAR(100) NOT NULL,
    max_capacity INT NOT NULL
);

-- 4: Create the TAP_LOGS real-time transaction table
CREATE TABLE IF NOT EXISTS tap_logs (
    tap_id INT PRIMARY KEY AUTO_INCREMENT,
    station_id INT NOT NULL,
    tap_type VARCHAR(5) NOT NULL,
    direction VARCHAR(5) NOT NULL,
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (station_id) REFERENCES stations(station_id) ON DELETE CASCADE
);

-- Verify database tables exist
SHOW TABLES;


-- =====================================================================
-- LIVE MONITOR QUERY
-- =====================================================================

SELECT 
    t.tap_id, 
    s.station_name, 
    t.tap_type, 
    t.direction, 
    t.timestamp 
FROM tap_logs t
JOIN stations s ON t.station_id = s.station_id
ORDER BY t.tap_id DESC 
LIMIT 20;