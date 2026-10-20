package com.parkease.service;

import com.parkease.db.DatabaseManager;
import com.parkease.exception.ParkingFullException;
import com.parkease.model.*;
import com.parkease.util.AnsiColor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

public class ParkingLot {
    public static final int DEFAULT_TOTAL_SLOTS = 20;

    private final int totalSlots;
    private final List<ParkingSlot> slots;
    private final Map<String, Ticket> activeTicketsById;
    private final Map<String, Ticket> activeTicketsByPlate;
    private final List<Ticket> completedTickets;
    private final Map<VehicleType, Double> revenueByType;
    private int ticketCounter;
    private double totalRevenueCollected;
    private int completedTicketsCount = 0;

    private final DatabaseManager dbManager;

    public ParkingLot() {
        this(DEFAULT_TOTAL_SLOTS);
    }

    public ParkingLot(int totalSlots) {
        if (totalSlots < 2) {
            throw new IllegalArgumentException("Total slots must be at least 2.");
        }
        this.totalSlots = totalSlots;
        this.slots = new ArrayList<>(totalSlots);
        for (int i = 1; i <= totalSlots; i++) {
            this.slots.add(new ParkingSlot(i));
        }
        this.activeTicketsById = new LinkedHashMap<>();
        this.activeTicketsByPlate = new HashMap<>();
        this.completedTickets = new ArrayList<>();
        this.revenueByType = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            revenueByType.put(vt, 0.0);
        }
        this.totalRevenueCollected = 0.0;

        this.dbManager = DatabaseManager.getInstance();

        int startSeq = 1001;
        if (this.dbManager != null && this.dbManager.isDriverAvailable()) {
            startSeq = Math.max(1001, this.dbManager.getNextTicketSequence());
            loadFromDatabase();
        }
        this.ticketCounter = startSeq;
    }

    private void loadFromDatabase() {
        if (dbManager == null || !dbManager.isDriverAvailable()) return;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("UPDATE tickets SET is_active = 0 WHERE is_active = 1 AND ticket_id NOT IN (SELECT current_ticket_id FROM parking_slots WHERE is_occupied = 1)");

            try (ResultSet rs = stmt.executeQuery("SELECT vehicle_type, COUNT(*), SUM(fee_charged) FROM tickets WHERE is_active = 0 GROUP BY vehicle_type")) {
                while (rs.next()) {
                    String vType = rs.getString(1);
                    int count = rs.getInt(2);
                    double fee = rs.getDouble(3);
                    try {
                        VehicleType vt = VehicleType.valueOf(vType);
                        revenueByType.put(vt, fee);
                        totalRevenueCollected += fee;
                        completedTicketsCount += count;
                    } catch (Exception ignored) {}
                }
            }

            String activeSql = "SELECT t.ticket_id, t.plate_number, v.owner_name, t.vehicle_type, t.entry_time " +
                               "FROM tickets t LEFT JOIN vehicles v ON t.plate_number = v.plate_number " +
                               "WHERE t.is_active = 1";
            try (ResultSet rs = stmt.executeQuery(activeSql)) {
                while (rs.next()) {
                    String ticketId = rs.getString(1);
                    String plate = rs.getString(2);
                    String owner = rs.getString(3);
                    if (owner == null || owner.isEmpty()) owner = "Walk-in Guest";
                    String typeStr = rs.getString(4);
                    String entryStr = rs.getString(5);

                    VehicleType vt;
                    try {
                        vt = VehicleType.valueOf(typeStr);
                    } catch (Exception e) {
                        vt = VehicleType.CAR;
                    }

                    Vehicle vehicle;
                    switch (vt) {
                        case BIKE -> vehicle = new Bike(plate, owner);
                        case CAR -> vehicle = new Car(plate, owner);
                        case TRUCK -> vehicle = new Truck(plate, owner);
                        default -> vehicle = new Car(plate, owner);
                    }

                    LocalDateTime entryTime;
                    try {
                        entryTime = LocalDateTime.parse(entryStr);
                    } catch (Exception e) {
                        entryTime = LocalDateTime.now();
                    }

                    List<Integer> allocatedSlots = new ArrayList<>();
                    try (Statement slotStmt = conn.createStatement();
                         ResultSet slotRs = slotStmt.executeQuery("SELECT slot_number FROM ticket_slots WHERE ticket_id = '" + ticketId + "' ORDER BY slot_number")) {
                        while (slotRs.next()) {
                            allocatedSlots.add(slotRs.getInt(1));
                        }
                    }

                    if (allocatedSlots.isEmpty()) {
                        try (Statement psStmt = conn.createStatement();
                             ResultSet psRs = psStmt.executeQuery("SELECT slot_number FROM parking_slots WHERE current_ticket_id = '" + ticketId + "' ORDER BY slot_number")) {
                            while (psRs.next()) {
                                allocatedSlots.add(psRs.getInt(1));
                            }
                        }
                    }

                    if (!allocatedSlots.isEmpty()) {
                        Ticket ticket = new Ticket(ticketId, vehicle, allocatedSlots, entryTime);
                        activeTicketsById.put(ticketId, ticket);
                        activeTicketsByPlate.put(plate, ticket);

                        boolean isTruck = (vt == VehicleType.TRUCK && allocatedSlots.size() == 2);
                        if (isTruck) {
                            int s1 = allocatedSlots.get(0);
                            int s2 = allocatedSlots.get(1);
                            if (s1 <= totalSlots && s2 <= totalSlots) {
                                slots.get(s1 - 1).occupy(vehicle, ticketId, true, s2);
                                slots.get(s2 - 1).occupy(vehicle, ticketId, true, s1);
                            }
                        } else {
                            for (int s : allocatedSlots) {
                                if (s <= totalSlots) {
                                    slots.get(s - 1).occupy(vehicle, ticketId, false, 0);
                                }
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading state from database: " + e.getMessage());
        }
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public List<ParkingSlot> getSlots() {
        return slots;
    }

    public int getOccupiedSlotsCount() {
        int count = 0;
        for (ParkingSlot slot : slots) {
            if (slot.isOccupied()) count++;
        }
        return count;
    }

    public int getVacantSlotsCount() {
        return totalSlots - getOccupiedSlotsCount();
    }

    public Collection<Ticket> getActiveTickets() {
        return activeTicketsById.values();
    }

    public List<Ticket> getCompletedTickets() {
        return completedTickets;
    }

    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public synchronized Ticket parkVehicle(Vehicle vehicle) throws ParkingFullException {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null.");
        }

        String plate = vehicle.getPlateNumber();

        if (activeTicketsByPlate.containsKey(plate)) {
            Ticket existing = activeTicketsByPlate.get(plate);
            throw new IllegalArgumentException("Vehicle with plate '" + plate + "' is already parked in slot " + existing.getFormattedSlots());
        }

        VehicleType type = vehicle.getVehicleType();
        List<Integer> allocatedSlotIndices = new ArrayList<>();

        if (type == VehicleType.TRUCK) {
            int foundIndex = -1;
            for (int i = 0; i < totalSlots - 1; i++) {
                if (slots.get(i).isVacant() && slots.get(i + 1).isVacant()) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex == -1) {
                int vacantCount = getVacantSlotsCount();
                if (vacantCount == 0) {
                    throw new ParkingFullException(VehicleType.TRUCK, "Parking full: 0 slots free.");
                } else {
                    throw new ParkingFullException(VehicleType.TRUCK, "Parking full for Truck: Requires 2 consecutive slots, but only " + vacantCount + " separate slot(s) free.");
                }
            }

            allocatedSlotIndices.add(foundIndex);
            allocatedSlotIndices.add(foundIndex + 1);
        } else {
            int foundIndex = -1;
            for (int i = 0; i < totalSlots; i++) {
                if (slots.get(i).isVacant()) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex == -1) {
                throw new ParkingFullException(type, "Parking full: No vacant slots available for " + type.getDisplayName() + ".");
            }

            allocatedSlotIndices.add(foundIndex);
        }

        String ticketId = "TKT-" + (ticketCounter++);
        List<Integer> slotNumbers = new ArrayList<>();

        if (type == VehicleType.TRUCK) {
            int slot1Num = slots.get(allocatedSlotIndices.get(0)).getSlotNumber();
            int slot2Num = slots.get(allocatedSlotIndices.get(1)).getSlotNumber();
            slotNumbers.add(slot1Num);
            slotNumbers.add(slot2Num);

            slots.get(allocatedSlotIndices.get(0)).occupy(vehicle, ticketId, true, slot2Num);
            slots.get(allocatedSlotIndices.get(1)).occupy(vehicle, ticketId, true, slot1Num);
        } else {
            int slotNum = slots.get(allocatedSlotIndices.get(0)).getSlotNumber();
            slotNumbers.add(slotNum);
            slots.get(allocatedSlotIndices.get(0)).occupy(vehicle, ticketId, false, 0);
        }

        Ticket ticket = new Ticket(ticketId, vehicle, slotNumbers, vehicle.getEntryTime());
        activeTicketsById.put(ticketId, ticket);
        activeTicketsByPlate.put(plate, ticket);

        if (dbManager != null && dbManager.isDriverAvailable()) {
            try (Connection conn = dbManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    dbManager.saveVehicle(vehicle, conn);
                    dbManager.saveTicket(ticket, conn);
                    dbManager.occupySlots(slotNumbers, ticketId, type == VehicleType.TRUCK, conn);
                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    System.err.println("[Database] Rolled back: " + ex.getMessage());
                }
            } catch (SQLException e) {
                System.err.println("[Database] Error: " + e.getMessage());
            }
        }

        return ticket;
    }

    public synchronized Ticket exitVehicle(String identifier, LocalDateTime exitTime) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty.");
        }

        String searchKey = identifier.trim().toUpperCase();
        Ticket ticket = activeTicketsById.get(searchKey);

        if (ticket == null) {
            try {
                String normalizedPlate = Vehicle.validateAndNormalizePlate(identifier);
                ticket = activeTicketsByPlate.get(normalizedPlate);
            } catch (Exception ignored) {
                ticket = activeTicketsByPlate.get(searchKey);
            }
        }

        if (ticket == null) {
            throw new IllegalArgumentException("No parked vehicle found matching '" + identifier + "'.");
        }

        ticket.closeTicket(exitTime);

        for (int slotNum : ticket.getAllocatedSlots()) {
            slots.get(slotNum - 1).vacate();
        }

        activeTicketsById.remove(ticket.getTicketId());
        activeTicketsByPlate.remove(ticket.getVehicle().getPlateNumber());

        completedTickets.add(ticket);
        completedTicketsCount++;
        totalRevenueCollected += ticket.getFeeCharged();
        revenueByType.merge(ticket.getVehicle().getVehicleType(), ticket.getFeeCharged(), Double::sum);

        if (dbManager != null && dbManager.isDriverAvailable()) {
            try (Connection conn = dbManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    dbManager.closeTicket(ticket, conn);
                    dbManager.vacateSlots(ticket.getAllocatedSlots(), conn);
                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    System.err.println("[Database] Rolled back: " + ex.getMessage());
                }
            } catch (SQLException e) {
                System.err.println("[Database] Error: " + e.getMessage());
            }
        }

        return ticket;
    }

    public synchronized Optional<Ticket> findActiveTicket(String identifier) {
        if (identifier == null) return Optional.empty();
        String searchKey = identifier.trim().toUpperCase();
        if (activeTicketsById.containsKey(searchKey)) {
            return Optional.of(activeTicketsById.get(searchKey));
        }
        try {
            String normalizedPlate = Vehicle.validateAndNormalizePlate(identifier);
            if (activeTicketsByPlate.containsKey(normalizedPlate)) {
                return Optional.of(activeTicketsByPlate.get(normalizedPlate));
            }
        } catch (Exception ignored) {}
        return Optional.ofNullable(activeTicketsByPlate.get(searchKey));
    }

    public synchronized SummaryStatistics getSummary() {
        int bikeCount = 0;
        int carCount = 0;
        int truckCount = 0;

        for (Ticket t : activeTicketsById.values()) {
            switch (t.getVehicle().getVehicleType()) {
                case BIKE -> bikeCount++;
                case CAR -> carCount++;
                case TRUCK -> truckCount++;
            }
        }

        int totalIssued = (ticketCounter > 1001) ? (ticketCounter - 1001) : (completedTicketsCount + activeTicketsById.size());

        return new SummaryStatistics(
                totalSlots,
                getOccupiedSlotsCount(),
                getVacantSlotsCount(),
                bikeCount,
                carCount,
                truckCount,
                totalIssued,
                completedTicketsCount,
                totalRevenueCollected,
                new EnumMap<>(revenueByType)
        );
    }

    public synchronized String renderLiveSlotMap() {
        StringBuilder sb = new StringBuilder();
        String headerBorder = "+" + "-".repeat(93) + "+\n";
        sb.append(AnsiColor.orange(headerBorder));
        sb.append(AnsiColor.bold(AnsiColor.orange(String.format("|%32sPARKEASE LIVE PARKING LOT MAP%32s|\n", "", ""))));
        sb.append(AnsiColor.orange(headerBorder));

        int cols = 4;
        int rows = (int) Math.ceil((double) totalSlots / cols);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    sb.append(AnsiColor.orange("+---------------------+"));
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    ParkingSlot slot = slots.get(index);
                    String slotId = slot.getFormattedSlotId();
                    if (slot.isVacant()) {
                        String inner = String.format(" %-4s  %-14s", slotId, "[VACANT]");
                        sb.append(AnsiColor.orange("|")).append(AnsiColor.orange(inner)).append(AnsiColor.orange("|"));
                    } else {
                        VehicleType vt = slot.getCurrentVehicle().getVehicleType();
                        String tag = "[" + vt.name() + "]";
                        String inner = String.format(" %-4s  %-14s", slotId, tag);
                        sb.append(AnsiColor.orange("|")).append(AnsiColor.bold(AnsiColor.orange(inner))).append(AnsiColor.orange("|"));
                    }
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    ParkingSlot slot = slots.get(index);
                    if (slot.isVacant()) {
                        sb.append(AnsiColor.orange("|")).append(AnsiColor.gray(String.format(" %-20s", "(Ready to park)"))).append(AnsiColor.orange("|"));
                    } else {
                        String plate = slot.getCurrentVehicle().getPlateNumber();
                        sb.append(AnsiColor.orange("|")).append(AnsiColor.orange(" Plate: ")).append(AnsiColor.bold(AnsiColor.white(String.format("%-13s", plate)))).append(AnsiColor.orange("|"));
                    }
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    ParkingSlot slot = slots.get(index);
                    if (slot.isVacant()) {
                        sb.append(AnsiColor.orange("|                     |"));
                    } else if (slot.isLinkedTandem()) {
                        String tandem = String.format(" %-8s <-> %-6s",
                                slot.getCurrentTicketId(), String.format("S-%02d", slot.getLinkedSlotNumber()));
                        sb.append(AnsiColor.orange("|")).append(AnsiColor.white(String.format("%-21s", tandem))).append(AnsiColor.orange("|"));
                    } else {
                        sb.append(AnsiColor.orange("|")).append(AnsiColor.orange(" Ticket: ")).append(AnsiColor.white(String.format("%-12s", slot.getCurrentTicketId()))).append(AnsiColor.orange("|"));
                    }
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    sb.append(AnsiColor.orange("+---------------------+"));
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n\n");
        }

        sb.append(AnsiColor.orange("[VACANT]") + AnsiColor.white(" Vacant   ") +
                AnsiColor.orange("[BIKE]") + AnsiColor.white(" Bike (Rs. 10/hr)   ") +
                AnsiColor.orange("[CAR]") + AnsiColor.white(" Car (Rs. 30/hr)   ") +
                AnsiColor.orange("[TRUCK]") + AnsiColor.white(" Truck (Rs. 60/hr, 2 Slots)\n"));

        return sb.toString();
    }
}
