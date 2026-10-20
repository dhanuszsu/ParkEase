-- =====================================================================
-- ParkEase: Smart Parking Automation & Billing System
-- Database Management Systems (DBMS) - Relational Schema Definition
-- Compatible with: SQLite and MySQL (3NF Normalized)
-- =====================================================================

-- 1. Table: vehicles
-- Stores registered vehicle details. Plate number is the Primary Key.
CREATE TABLE IF NOT EXISTS vehicles (
    plate_number    VARCHAR(20) PRIMARY KEY,
    owner_name      VARCHAR(100) NOT NULL,
    vehicle_type    VARCHAR(10) NOT NULL
);

-- 2. Table: parking_slots
-- Represents the 20 physical parking bays in the lot.
CREATE TABLE IF NOT EXISTS parking_slots (
    slot_number        INTEGER PRIMARY KEY,
    is_occupied        INTEGER DEFAULT 0,
    current_ticket_id  VARCHAR(20),
    is_linked_tandem   INTEGER DEFAULT 0,
    linked_slot_number INTEGER DEFAULT 0
);

-- 3. Table: tickets
-- Master lifecycle record for every issued parking ticket.
CREATE TABLE IF NOT EXISTS tickets (
    ticket_id         VARCHAR(20) PRIMARY KEY,
    plate_number      VARCHAR(20) NOT NULL,
    vehicle_type      VARCHAR(10) NOT NULL,
    entry_time        VARCHAR(30) NOT NULL,
    exit_time         VARCHAR(30),
    duration_minutes  INTEGER DEFAULT 0,
    billed_hours      INTEGER DEFAULT 0,
    hourly_rate       REAL NOT NULL,
    daily_cap         REAL NOT NULL,
    fee_charged       REAL DEFAULT 0.0,
    is_active         INTEGER DEFAULT 1,
    FOREIGN KEY (plate_number) REFERENCES vehicles(plate_number)
);

-- 4. Table: ticket_slots (Junction Table for 3NF normalization)
-- Maps tickets to slots: Cars/Bikes map to 1 slot; Trucks map to 2 slots.
CREATE TABLE IF NOT EXISTS ticket_slots (
    ticket_id   VARCHAR(20) NOT NULL,
    slot_number INTEGER NOT NULL,
    PRIMARY KEY (ticket_id, slot_number),
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id),
    FOREIGN KEY (slot_number) REFERENCES parking_slots(slot_number)
);

-- 5. View: vw_revenue_summary
-- Aggregate financial metrics grouped by vehicle classification.
CREATE VIEW IF NOT EXISTS vw_revenue_summary AS
SELECT 
    vehicle_type,
    COUNT(*) AS total_tickets_issued,
    SUM(CASE WHEN is_active = 0 THEN 1 ELSE 0 END) AS completed_checkouts,
    SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) AS currently_parked,
    ROUND(SUM(fee_charged), 2) AS total_revenue
FROM tickets
GROUP BY vehicle_type;

-- 6. Initial Seed: 20 Parking Slots
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (1, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (2, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (3, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (4, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (5, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (6, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (7, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (8, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (9, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (10, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (11, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (12, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (13, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (14, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (15, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (16, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (17, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (18, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (19, 0);
INSERT OR IGNORE INTO parking_slots (slot_number, is_occupied) VALUES (20, 0);
