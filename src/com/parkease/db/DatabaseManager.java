package com.parkease.db;

import com.parkease.model.Ticket;
import com.parkease.model.Vehicle;

import java.sql.*;
import java.util.List;

public class DatabaseManager {
    public static final String DEFAULT_DB_URL = "jdbc:sqlite:parkease.db";
    private static DatabaseManager instance;

    private final String dbUrl;
    private boolean driverAvailable;

    public DatabaseManager() {
        this(DEFAULT_DB_URL);
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        try {
            Class.forName("org.sqlite.JDBC");
            this.driverAvailable = true;
            initSchema();
        } catch (ClassNotFoundException e) {
            this.driverAvailable = false;
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public boolean isDriverAvailable() {
        return driverAvailable;
    }

    public Connection getConnection() throws SQLException {
        if (!driverAvailable) {
            throw new SQLException("SQLite JDBC Driver not found.");
        }
        return DriverManager.getConnection(dbUrl);
    }

    public void initSchema() {
        if (!driverAvailable) return;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicles (plate_number TEXT PRIMARY KEY, owner_name TEXT, vehicle_type TEXT);");

            stmt.execute("CREATE TABLE IF NOT EXISTS parking_slots (slot_number INTEGER PRIMARY KEY, is_occupied INTEGER, current_ticket_id TEXT, is_linked_tandem INTEGER, linked_slot_number INTEGER);");

            stmt.execute("CREATE TABLE IF NOT EXISTS tickets (ticket_id TEXT PRIMARY KEY, plate_number TEXT, vehicle_type TEXT, entry_time TEXT, exit_time TEXT, duration_minutes INTEGER, billed_hours INTEGER, hourly_rate REAL, daily_cap REAL, fee_charged REAL, is_active INTEGER);");

            stmt.execute("CREATE TABLE IF NOT EXISTS ticket_slots (ticket_id TEXT, slot_number INTEGER, PRIMARY KEY (ticket_id, slot_number));");

            stmt.execute("CREATE VIEW IF NOT EXISTS vw_revenue_summary AS SELECT vehicle_type, COUNT(*) AS total_tickets_issued, SUM(CASE WHEN is_active=0 THEN 1 ELSE 0 END) AS completed_checkouts, SUM(CASE WHEN is_active=1 THEN 1 ELSE 0 END) AS currently_parked, ROUND(SUM(fee_charged), 2) AS total_revenue FROM tickets GROUP BY vehicle_type;");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM parking_slots;");
            if (rs.next() && rs.getInt(1) == 0) {
                for (int i = 1; i <= 20; i++) {
                    stmt.executeUpdate("INSERT INTO parking_slots (slot_number, is_occupied) VALUES (" + i + ", 0);");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database init error: " + e.getMessage());
        }
    }

    public void saveVehicle(Vehicle vehicle, Connection conn) throws SQLException {
        String sql = "INSERT OR REPLACE INTO vehicles (plate_number, owner_name, vehicle_type) VALUES (?, ?, ?);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vehicle.getPlateNumber());
            stmt.setString(2, vehicle.getOwnerName());
            stmt.setString(3, vehicle.getVehicleType().name());
            stmt.executeUpdate();
        }
    }

    public void saveTicket(Ticket ticket, Connection conn) throws SQLException {
        String sql = "INSERT INTO tickets (ticket_id, plate_number, vehicle_type, entry_time, hourly_rate, daily_cap, fee_charged, is_active) VALUES (?, ?, ?, ?, ?, ?, 0.0, 1);";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticket.getTicketId());
            stmt.setString(2, ticket.getVehicle().getPlateNumber());
            stmt.setString(3, ticket.getVehicle().getVehicleType().name());
            stmt.setString(4, ticket.getEntryTime().toString());
            stmt.setDouble(5, ticket.getVehicle().getHourlyRate());
            stmt.setDouble(6, ticket.getVehicle().getDailyCap());
            stmt.executeUpdate();
        }

        String slotSql = "INSERT INTO ticket_slots (ticket_id, slot_number) VALUES (?, ?);";
        for (int slotNum : ticket.getAllocatedSlots()) {
            try (PreparedStatement stmt = conn.prepareStatement(slotSql)) {
                stmt.setString(1, ticket.getTicketId());
                stmt.setInt(2, slotNum);
                stmt.executeUpdate();
            }
        }
    }

    public void occupySlots(List<Integer> slotNumbers, String ticketId, boolean isTruck, Connection conn) throws SQLException {
        String sql = "UPDATE parking_slots SET is_occupied = 1, current_ticket_id = ?, is_linked_tandem = ?, linked_slot_number = ? WHERE slot_number = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (slotNumbers.size() == 2) {
                int s1 = slotNumbers.get(0);
                int s2 = slotNumbers.get(1);

                stmt.setString(1, ticketId);
                stmt.setInt(2, 1);
                stmt.setInt(3, s2);
                stmt.setInt(4, s1);
                stmt.executeUpdate();

                stmt.setString(1, ticketId);
                stmt.setInt(2, 1);
                stmt.setInt(3, s1);
                stmt.setInt(4, s2);
                stmt.executeUpdate();
            } else {
                int s = slotNumbers.get(0);
                stmt.setString(1, ticketId);
                stmt.setInt(2, 0);
                stmt.setInt(3, 0);
                stmt.setInt(4, s);
                stmt.executeUpdate();
            }
        }
    }

    public void vacateSlots(List<Integer> slotNumbers, Connection conn) throws SQLException {
        String sql = "UPDATE parking_slots SET is_occupied = 0, current_ticket_id = NULL, is_linked_tandem = 0, linked_slot_number = 0 WHERE slot_number = ?;";
        for (int s : slotNumbers) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, s);
                stmt.executeUpdate();
            }
        }
    }

    public void closeTicket(Ticket ticket, Connection conn) throws SQLException {
        String sql = "UPDATE tickets SET exit_time = ?, duration_minutes = ?, billed_hours = ?, fee_charged = ?, is_active = 0 WHERE ticket_id = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ticket.getExitTime() != null ? ticket.getExitTime().toString() : "");
            stmt.setLong(2, ticket.getDurationMinutes());
            stmt.setLong(3, ticket.getBilledHours());
            stmt.setDouble(4, ticket.getFeeCharged());
            stmt.setString(5, ticket.getTicketId());
            stmt.executeUpdate();
        }
    }

    public int getNextTicketSequence() {
        String sql = "SELECT MAX(CAST(SUBSTR(ticket_id, 5) AS INTEGER)) FROM tickets;";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt(1);
                if (max > 0) return max + 1;
            }
        } catch (SQLException ignored) {}
        return 1001;
    }

    public String executeQueryToAscii(String sql) {
        if (!driverAvailable) return "Database offline";
        StringBuilder sb = new StringBuilder();
        sb.append("+------------+------------+------------+------------+------------+\n");
        sb.append("| Vehicle    | Tickets    | Exits      | Parked     | Revenue    |\n");
        sb.append("+------------+------------+------------+------------+------------+\n");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT vehicle_type, total_tickets_issued, completed_checkouts, currently_parked, total_revenue FROM vw_revenue_summary")) {
            while (rs.next()) {
                sb.append(String.format("| %-10s | %-10d | %-10d | %-10d | Rs. %-7.2f |\n",
                        rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5)));
            }
        } catch (SQLException e) {
            return "Error reading view: " + e.getMessage();
        }
        sb.append("+------------+------------+------------+------------+------------+\n");
        return sb.toString();
    }
}
